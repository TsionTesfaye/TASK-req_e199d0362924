package com.rescuehub.service;

import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.ConflictException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.repository.UserRepository;
import com.rescuehub.security.RoleGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AdminService {

    private final UserRepository userRepo;
    private final RoleGuard roleGuard;
    private final AuditService auditService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminService(UserRepository userRepo, RoleGuard roleGuard, AuditService auditService) {
        this.userRepo = userRepo;
        this.roleGuard = roleGuard;
        this.auditService = auditService;
    }

    @Transactional
    public User createUser(User actor, String username, String password, String displayName, Role role,
                            String ip, String workstationId) {
        roleGuard.require(actor, Role.ADMIN);
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,64}")) {
            throw new BusinessRuleException("username must be 3-64 chars of [A-Za-z0-9._-]");
        }
        if (password == null || password.length() < 8) {
            throw new BusinessRuleException("password must be at least 8 characters");
        }
        if (userRepo.existsByUsername(username)) throw new ConflictException("Username already exists: " + username);
        User user = new User();
        user.setOrganizationId(actor.getOrganizationId());
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setActive(true);
        user.setFrozen(false);
        user.setPasswordChangedAt(Instant.now());
        user = userRepo.save(user);
        auditService.log(actor.getId(), actor.getUsername(), "USER_CREATE",
                "User", String.valueOf(user.getId()), actor.getOrganizationId(), ip, workstationId, null,
                "{\"username\":\"" + username + "\",\"role\":\"" + role + "\"}");
        return user;
    }

    @Transactional
    public User updateUser(User actor, Long userId, String displayName, Role role, boolean isActive, boolean isFrozen,
                            String ip, String workstationId) {
        roleGuard.require(actor, Role.ADMIN);
        User user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (!user.getOrganizationId().equals(actor.getOrganizationId())) throw new ForbiddenEx();
        String before = "{\"role\":\"" + user.getRole() + "\",\"isActive\":" + user.isActive() + "}";
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setActive(isActive);
        user.setFrozen(isFrozen);
        user = userRepo.save(user);
        auditService.log(actor.getId(), actor.getUsername(), "USER_UPDATE",
                "User", String.valueOf(userId), actor.getOrganizationId(), ip, workstationId, before,
                "{\"role\":\"" + role + "\"}");
        return user;
    }

    @Transactional(readOnly = true)
    public Page<User> listUsers(User actor, Pageable pageable) {
        roleGuard.require(actor, Role.ADMIN);
        return userRepo.findByOrganizationId(actor.getOrganizationId(), pageable);
    }

    @Transactional(readOnly = true)
    public User getUser(User actor, Long userId) {
        roleGuard.require(actor, Role.ADMIN);
        User user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (!user.getOrganizationId().equals(actor.getOrganizationId())) throw new ForbiddenEx();
        return user;
    }

    @Transactional
    public void deleteUser(User actor, Long userId, String ip, String workstationId) {
        roleGuard.require(actor, Role.ADMIN);
        if (actor.getId().equals(userId)) throw new BusinessRuleException("Cannot delete your own account");
        User user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (!user.getOrganizationId().equals(actor.getOrganizationId())) throw new ForbiddenEx();
        auditService.log(actor.getId(), actor.getUsername(), "USER_DELETE",
                "User", String.valueOf(userId), actor.getOrganizationId(), ip, workstationId, null,
                "{\"username\":\"" + user.getUsername() + "\"}");
        userRepo.delete(user);
    }

    private static class ForbiddenEx extends com.rescuehub.exception.ForbiddenException {
        ForbiddenEx() { super("User not in your organization"); }
    }
}
