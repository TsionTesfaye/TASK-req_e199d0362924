package com.rescuehub.service;

import com.rescuehub.entity.Organization;
import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.RateLimitException;
import com.rescuehub.repository.OrganizationRepository;
import com.rescuehub.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SystemBootstrapService {

    // Rate limit: 5 attempts / 10 min per workstation_id
    private static final int RATE_LIMIT_MAX = 5;
    private static final int RATE_LIMIT_WINDOW_SECONDS = 600;

    private final UserRepository userRepo;
    private final OrganizationRepository orgRepo;
    private final AuthService authService;
    private final AuditService auditService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, List<Instant>> rateLimitMap = new ConcurrentHashMap<>();

    public SystemBootstrapService(UserRepository userRepo, OrganizationRepository orgRepo,
                                  AuthService authService, AuditService auditService) {
        this.userRepo = userRepo;
        this.orgRepo = orgRepo;
        this.authService = authService;
        this.auditService = auditService;
    }

    public boolean isSystemInitialized() {
        return userRepo.count() > 0;
    }

    public record BootstrapUser(Long id, String username, String displayName, String role, Long organizationId) {}
    public record BootstrapResult(Long userId, Long organizationId, String sessionToken, String csrfToken, BootstrapUser user) {}

    /**
     * Atomic bootstrap. Uses SERIALIZABLE isolation so that two concurrent transactions
     * cannot both observe userRepo.count() == 0 before inserting. Rate-limited per
     * workstation_id to protect the uninitialized window from brute-force bootstrap.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public BootstrapResult initializeSystem(String username, String password,
                                            String displayName, String organizationName,
                                            String ip, String userAgent) {
        String workstationId = CryptoService.sha256Hex(ip + "|" + userAgent);
        checkRateLimit(workstationId);
        recordAttempt(workstationId);

        try {
            if (userRepo.count() > 0) {
                throw new BusinessRuleException("System is already initialized");
            }
            if (username == null || username.isBlank()) {
                throw new BusinessRuleException("username is required");
            }
            if (!username.matches("[A-Za-z0-9._-]{3,64}")) {
                throw new BusinessRuleException("username must be 3-64 chars of [A-Za-z0-9._-]");
            }
            if (password == null || password.length() < 8) {
                throw new BusinessRuleException("password must be at least 8 characters");
            }

            Organization org = orgRepo.findById(1L).orElseGet(Organization::new);
            if (org.getId() == null) {
                org.setCode("DEFAULT");
            }
            org.setName(organizationName != null && !organizationName.isBlank() ? organizationName : "Default Org");
            org.setActive(true);
            org = orgRepo.save(org);

            User admin = new User();
            admin.setOrganizationId(org.getId());
            admin.setUsername(username);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setDisplayName(displayName != null && !displayName.isBlank() ? displayName : username);
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            admin.setFrozen(false);
            admin.setPasswordChangedAt(Instant.now());
            admin = userRepo.saveAndFlush(admin);

            auditService.log(admin.getId(), admin.getUsername(), "SYSTEM_BOOTSTRAP",
                    "System", String.valueOf(org.getId()), org.getId(), ip, workstationId,
                    null, "{\"adminUsername\":\"" + username + "\",\"organizationId\":" + org.getId() + "}");

            AuthService.LoginResult lr = authService.loginWithCsrf(username, password, ip, userAgent);
            // Success — clear rate-limit budget for the admin's workstation.
            rateLimitMap.remove(workstationId);
            BootstrapUser userDto = new BootstrapUser(admin.getId(), admin.getUsername(),
                    admin.getDisplayName(), admin.getRole().name(), admin.getOrganizationId());
            return new BootstrapResult(admin.getId(), org.getId(), lr.sessionToken(), lr.csrfToken(), userDto);
        } catch (DataIntegrityViolationException dup) {
            // Unique constraint on username tripped — a concurrent transaction won the race.
            throw new BusinessRuleException("System is already initialized");
        }
    }

    private void checkRateLimit(String workstationId) {
        Instant cutoff = Instant.now().minusSeconds(RATE_LIMIT_WINDOW_SECONDS);
        List<Instant> attempts = rateLimitMap.computeIfAbsent(workstationId, k -> new ArrayList<>());
        synchronized (attempts) {
            attempts.removeIf(t -> t.isBefore(cutoff));
            if (attempts.size() >= RATE_LIMIT_MAX) {
                throw new RateLimitException("Too many bootstrap attempts. Try again in "
                        + RATE_LIMIT_WINDOW_SECONDS / 60 + " minutes.");
            }
        }
    }

    private void recordAttempt(String workstationId) {
        List<Instant> attempts = rateLimitMap.computeIfAbsent(workstationId, k -> new ArrayList<>());
        synchronized (attempts) {
            attempts.add(Instant.now());
        }
    }
}
