package com.rescuehub;

import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.ConflictException;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.repository.UserRepository;
import com.rescuehub.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

public class AdminServiceTest extends BaseIntegrationTest {

    @Autowired private AdminService adminService;
    @Autowired private UserRepository userRepo;

    @Test
    @Transactional
    void createUser_adminCanCreateUser() {
        String username = "new_test_user_" + System.nanoTime();
        User created = adminService.createUser(adminUser, username, "password123",
                "New Test User", Role.FRONT_DESK, "127.0.0.1", "ws");

        assertNotNull(created.getId());
        assertEquals(username, created.getUsername());
        assertEquals(Role.FRONT_DESK, created.getRole());
        assertEquals(testOrg.getId(), created.getOrganizationId());
    }

    @Test
    @Transactional
    void createUser_nonAdminForbidden() {
        assertThrows(ForbiddenException.class, () ->
                adminService.createUser(billingUser, "not_allowed", "password123",
                        "Not Allowed", Role.FRONT_DESK, "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void createUser_duplicateUsernameFails() {
        String username = "dup_user_" + System.nanoTime();
        adminService.createUser(adminUser, username, "password123",
                "First User", Role.FRONT_DESK, "127.0.0.1", "ws");

        assertThrows(ConflictException.class, () ->
                adminService.createUser(adminUser, username, "password123",
                        "Second User", Role.CLINICIAN, "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void createUser_weakPasswordRejected() {
        assertThrows(BusinessRuleException.class, () ->
                adminService.createUser(adminUser, "weakpass_user_" + System.nanoTime(),
                        "short", "Weak Pass User", Role.FRONT_DESK, "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void createUser_invalidUsernameRejected() {
        assertThrows(BusinessRuleException.class, () ->
                adminService.createUser(adminUser, "ab", // too short
                        "password123", "Bad Username", Role.FRONT_DESK, "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void listUsers_adminCanListUsers() {
        Page<User> users = adminService.listUsers(adminUser, PageRequest.of(0, 50));
        assertNotNull(users);
        assertTrue(users.getTotalElements() > 0, "admin should see at least the base test users");
    }

    @Test
    @Transactional
    void listUsers_nonAdminForbidden() {
        assertThrows(ForbiddenException.class, () ->
                adminService.listUsers(clinicianUser, PageRequest.of(0, 10)));
    }

    @Test
    @Transactional
    void deleteUser_adminCanDeleteUser() {
        String username = "to_delete_" + System.nanoTime();
        User user = adminService.createUser(adminUser, username, "password123",
                "To Delete", Role.FRONT_DESK, "127.0.0.1", "ws");

        adminService.deleteUser(adminUser, user.getId(), "127.0.0.1", "ws");

        assertTrue(userRepo.findById(user.getId()).isEmpty(), "user should be deleted");
    }

    @Test
    @Transactional
    void deleteUser_nonAdminForbidden() {
        String username = "del_test_" + System.nanoTime();
        User user = adminService.createUser(adminUser, username, "password123",
                "Del Test", Role.FRONT_DESK, "127.0.0.1", "ws");

        assertThrows(ForbiddenException.class, () ->
                adminService.deleteUser(frontDeskUser, user.getId(), "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void getUser_adminCanFetchById() {
        String username = "get_user_" + System.nanoTime();
        User created = adminService.createUser(adminUser, username, "password123",
                "Get User Test", Role.CLINICIAN, "127.0.0.1", "ws");

        User fetched = adminService.getUser(adminUser, created.getId());
        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
        assertEquals(username, fetched.getUsername());
    }

    @Test
    @Transactional
    void getUser_nonAdminForbidden() {
        assertThrows(ForbiddenException.class, () ->
                adminService.getUser(clinicianUser, adminUser.getId()));
    }

    @Test
    @Transactional
    void updateUser_adminCanChangeRole() {
        String username = "role_change_" + System.nanoTime();
        User user = adminService.createUser(adminUser, username, "password123",
                "Role Change Test", Role.FRONT_DESK, "127.0.0.1", "ws");

        User updated = adminService.updateUser(adminUser, user.getId(),
                "Updated Name", Role.CLINICIAN, true, false, "127.0.0.1", "ws");

        assertEquals(Role.CLINICIAN, updated.getRole());
        assertEquals("Updated Name", updated.getDisplayName());
    }
}
