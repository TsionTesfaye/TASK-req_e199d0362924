package com.rescuehub;

import com.rescuehub.entity.Organization;
import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.repository.OrganizationRepository;
import com.rescuehub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected OrganizationRepository orgRepo;
    @Autowired
    protected UserRepository userRepo;

    protected Organization testOrg;
    protected User adminUser;
    protected User billingUser;
    protected User qualityUser;
    protected User clinicianUser;
    protected User frontDeskUser;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setupBase() {
        testOrg = orgRepo.findAll().stream().findFirst().orElseGet(() -> {
            Organization o = new Organization();
            o.setCode("TEST");
            o.setName("Test Org");
            o.setActive(true);
            return orgRepo.save(o);
        });

        adminUser = getOrCreateUser("test_admin", Role.ADMIN);
        billingUser = getOrCreateUser("test_billing", Role.BILLING);
        qualityUser = getOrCreateUser("test_quality", Role.QUALITY);
        clinicianUser = getOrCreateUser("test_clinician", Role.CLINICIAN);
        frontDeskUser = getOrCreateUser("test_frontdesk", Role.FRONT_DESK);
    }

    protected User getOrCreateUser(String username, Role role) {
        return userRepo.findByUsername(username).orElseGet(() -> {
            User u = new User();
            u.setOrganizationId(testOrg.getId());
            u.setUsername(username);
            u.setPasswordHash(encoder.encode("password"));
            u.setDisplayName(username);
            u.setRole(role);
            u.setActive(true);
            u.setFrozen(false);
            u.setPasswordChangedAt(Instant.now());
            return userRepo.save(u);
        });
    }
}
