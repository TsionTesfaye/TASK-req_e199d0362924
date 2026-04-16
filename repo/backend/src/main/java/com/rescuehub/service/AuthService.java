package com.rescuehub.service;

import com.rescuehub.entity.User;
import com.rescuehub.entity.UserSession;
import com.rescuehub.exception.AuthException;
import com.rescuehub.exception.RateLimitException;
import com.rescuehub.repository.UserRepository;
import com.rescuehub.repository.UserSessionRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final int SESSION_HOURS = 8;
    private static final int RATE_LIMIT_MAX = 10;
    private static final int RATE_LIMIT_WINDOW_SECONDS = 600;

    private final UserRepository userRepo;
    private final UserSessionRepository sessionRepo;
    private final AuditService auditService;
    private final RiskScoreService riskScoreService;
    private final AccessControlService accessControlService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    // in-memory rate limiter: workstationId -> list of attempt timestamps
    private final Map<String, List<Instant>> rateLimitMap = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepo, UserSessionRepository sessionRepo,
                       AuditService auditService, RiskScoreService riskScoreService,
                       AccessControlService accessControlService) {
        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.auditService = auditService;
        this.riskScoreService = riskScoreService;
        this.accessControlService = accessControlService;
    }

    public record LoginResult(String sessionToken, String csrfToken) {}

    @Transactional
    public LoginResult loginWithCsrf(String username, String password, String ip, String userAgent) {
        String raw = login(username, password, ip, userAgent);
        // Attach a CSRF token to the just-issued session
        UserSession session = sessionRepo.findBySessionTokenHash(CryptoService.sha256Hex(raw))
                .orElseThrow(() -> new AuthException("Session not found after login"));
        byte[] csrfBytes = new byte[32];
        secureRandom.nextBytes(csrfBytes);
        String csrf = Base64.getUrlEncoder().withoutPadding().encodeToString(csrfBytes);
        session.setCsrfTokenHash(CryptoService.sha256Hex(csrf));
        sessionRepo.save(session);
        return new LoginResult(raw, csrf);
    }

    /** Returns true if the raw CSRF token matches the hash stored on the session for the given raw session token. */
    @Transactional(readOnly = true)
    public boolean validateCsrf(String rawSessionToken, String rawCsrfToken) {
        if (rawSessionToken == null || rawCsrfToken == null) return false;
        String tokenHash = CryptoService.sha256Hex(rawSessionToken);
        return sessionRepo.findBySessionTokenHash(tokenHash)
                .map(s -> s.getCsrfTokenHash() != null
                        && s.getCsrfTokenHash().equals(CryptoService.sha256Hex(rawCsrfToken)))
                .orElse(false);
    }

    @Transactional
    public String login(String username, String password, String ip, String userAgent) {
        String workstationId = CryptoService.sha256Hex(ip + "|" + userAgent);
        checkRateLimit(workstationId);

        if (userRepo.count() == 0) {
            throw new AuthException("System not initialized. Bootstrap required at POST /api/bootstrap");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> {
                    recordFailedAttempt(workstationId);
                    return new AuthException("Invalid credentials");
                });

        if (!user.isActive() || user.isFrozen()) {
            recordFailedAttempt(workstationId);
            throw new AuthException("Account is inactive or frozen");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            recordFailedAttempt(workstationId);
            throw new AuthException("Invalid credentials");
        }

        // Allowlist / denylist enforcement (IP and username checked against org's lists)
        if (accessControlService.check(user.getOrganizationId(), "IP", ip,
                username, ip) == AccessControlService.Decision.DENIED) {
            recordFailedAttempt(workstationId);
            throw new AuthException("Access denied by security policy");
        }
        if (accessControlService.check(user.getOrganizationId(), "USERNAME", username,
                username, ip) == AccessControlService.Decision.DENIED) {
            recordFailedAttempt(workstationId);
            throw new AuthException("Access denied by security policy");
        }

        clearRateLimit(workstationId);

        // generate session token
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String tokenHash = CryptoService.sha256Hex(rawToken);

        UserSession session = new UserSession();
        session.setUserId(user.getId());
        session.setSessionTokenHash(tokenHash);
        session.setWorkstationId(workstationId);
        session.setIpAddress(ip);
        session.setIssuedAt(Instant.now());
        session.setExpiresAt(Instant.now().plusSeconds(SESSION_HOURS * 3600L));
        session.setLastSeenAt(Instant.now());
        sessionRepo.save(session);

        auditService.log(user.getId(), user.getUsername(), "LOGIN", "User",
                String.valueOf(user.getId()), user.getOrganizationId(), ip, workstationId, null, null);

        return rawToken;
    }

    @Transactional
    public void logout(String rawToken, String ip, String userAgent) {
        String tokenHash = CryptoService.sha256Hex(rawToken);
        sessionRepo.findBySessionTokenHash(tokenHash).ifPresent(session -> {
            session.setRevokedAt(Instant.now());
            sessionRepo.save(session);
        });
    }

    public record SessionContext(User user, String workstationId) {}

    @Transactional
    public SessionContext validateToken(String rawToken) {
        String tokenHash = CryptoService.sha256Hex(rawToken);
        UserSession session = sessionRepo.findBySessionTokenHash(tokenHash)
                .orElseThrow(() -> new AuthException("Invalid session token"));

        if (session.getRevokedAt() != null) throw new AuthException("Session revoked");
        if (session.getExpiresAt().isBefore(Instant.now())) throw new AuthException("Session expired");

        session.setLastSeenAt(Instant.now());
        sessionRepo.save(session);

        User user = userRepo.findById(session.getUserId())
                .orElseThrow(() -> new AuthException("User not found"));

        if (!user.isActive() || user.isFrozen()) throw new AuthException("Account is inactive or frozen");

        return new SessionContext(user, session.getWorkstationId());
    }

    private void checkRateLimit(String workstationId) {
        Instant cutoff = Instant.now().minusSeconds(RATE_LIMIT_WINDOW_SECONDS);
        List<Instant> attempts = rateLimitMap.computeIfAbsent(workstationId, k -> new ArrayList<>());
        attempts.removeIf(t -> t.isBefore(cutoff));
        if (attempts.size() >= RATE_LIMIT_MAX) {
            throw new RateLimitException("Too many login attempts. Try again in " + RATE_LIMIT_WINDOW_SECONDS / 60 + " minutes.");
        }
    }

    private void recordFailedAttempt(String workstationId) {
        rateLimitMap.computeIfAbsent(workstationId, k -> new ArrayList<>()).add(Instant.now());
        // Feed risk telemetry. Multiple failed logins from the same workstation
        // raise the risk score; it's non-enforcing but surfaces to ADMIN.
        riskScoreService.recordEvent(workstationId, "ANOMALOUS_LOGIN");
    }

    private void clearRateLimit(String workstationId) {
        rateLimitMap.remove(workstationId);
    }

    public BCryptPasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
}
