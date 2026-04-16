package com.rescuehub;

import com.rescuehub.entity.AccessListEntry;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.repository.AccessListEntryRepository;
import com.rescuehub.service.AccessControlService;
import com.rescuehub.service.AccessControlService.Decision;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccessControlServiceTest extends BaseIntegrationTest {

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private AccessListEntryRepository accessListEntryRepo;

    /**
     * Remove all entries created in this test class's org after each test to avoid
     * cross-test interference (allowlist logic is "if any active ALLOW entry exists").
     */
    @AfterEach
    void cleanupEntries() {
        List<AccessListEntry> entries = accessListEntryRepo.findByOrganizationId(testOrg.getId());
        accessListEntryRepo.deleteAll(entries);
    }

    @Test
    void check_noEntries_allowed() {
        String uniqueIp = "10.0." + System.nanoTime() % 255 + ".1";
        Decision decision = accessControlService.check(
                testOrg.getId(), "IP", uniqueIp, "tester", uniqueIp);
        assertEquals(Decision.ALLOWED, decision);
    }

    @Test
    void check_denyEntryMatchingIp_denied() {
        long nanos = System.nanoTime();
        String blockedIp = "192.168." + (nanos % 200 + 1) + ".1";

        accessControlService.addEntry(
                adminUser, "DENY", "IP", blockedIp,
                "Blocked for testing", null);

        Decision decision = accessControlService.check(
                testOrg.getId(), "IP", blockedIp, "tester", blockedIp);
        assertEquals(Decision.DENIED, decision);
    }

    @Test
    void check_allowEntryMatchingIp_allowed() {
        long nanos = System.nanoTime();
        String allowedIp = "172.16." + (nanos % 200 + 1) + ".1";

        accessControlService.addEntry(
                adminUser, "ALLOW", "IP", allowedIp,
                "Allowed for testing", null);

        Decision decision = accessControlService.check(
                testOrg.getId(), "IP", allowedIp, "tester", allowedIp);
        assertEquals(Decision.ALLOWED, decision);
    }

    @Test
    void check_allowlistActiveButIpNotInIt_denied() {
        long nanos = System.nanoTime();
        String allowedIp = "10.100." + (nanos % 200 + 1) + ".1";
        String otherIp = "10.200." + (nanos % 200 + 1) + ".1";

        // Add an ALLOW entry for one IP — this activates the allowlist
        accessControlService.addEntry(
                adminUser, "ALLOW", "IP", allowedIp,
                "Allowlist entry", null);

        // Query a different IP — not in allowlist, must be DENIED
        Decision decision = accessControlService.check(
                testOrg.getId(), "IP", otherIp, "tester", otherIp);
        assertEquals(Decision.DENIED, decision);
    }

    @Test
    void addEntry_adminAddsEntry_appearsInListEntries() {
        long nanos = System.nanoTime();
        String subjectValue = "ws-test-" + nanos;

        accessControlService.addEntry(
                adminUser, "DENY", "WORKSTATION_ID", subjectValue,
                "Test workstation block", null);

        List<AccessListEntry> entries = accessControlService.listEntries(adminUser);
        boolean found = entries.stream()
                .anyMatch(e -> subjectValue.equals(e.getSubjectValue())
                        && "DENY".equals(e.getListType())
                        && "WORKSTATION_ID".equals(e.getSubjectType()));
        assertTrue(found, "Newly added entry must appear in listEntries");
    }

    @Test
    @Transactional
    void addEntry_invalidListTypeThrowsBusinessRuleException() {
        assertThrows(BusinessRuleException.class, () ->
                accessControlService.addEntry(
                        adminUser, "INVALID_TYPE", "IP", "1.2.3.4",
                        "Bad type", null));
    }

    @Test
    @Transactional
    void addEntry_invalidSubjectTypeThrowsBusinessRuleException() {
        assertThrows(BusinessRuleException.class, () ->
                accessControlService.addEntry(
                        adminUser, "DENY", "INVALID_SUBJECT", "value",
                        "Bad subject", null));
    }

    @Test
    void removeEntry_removesTheEntry() {
        long nanos = System.nanoTime();
        String subjectValue = "ws-remove-" + nanos;

        AccessListEntry entry = accessControlService.addEntry(
                adminUser, "DENY", "WORKSTATION_ID", subjectValue,
                "To be removed", null);

        Long entryId = entry.getId();
        assertTrue(accessListEntryRepo.findById(entryId).isPresent());

        accessControlService.removeEntry(adminUser, entryId);

        assertFalse(accessListEntryRepo.findById(entryId).isPresent(),
                "Removed entry must not exist in repository");
    }

    @Test
    void removeEntry_nonAdminBillingUserThrowsForbidden() {
        long nanos = System.nanoTime();
        String subjectValue = "ws-forbidden-" + nanos;

        AccessListEntry entry = accessControlService.addEntry(
                adminUser, "DENY", "WORKSTATION_ID", subjectValue,
                "Cannot be removed by billing", null);

        assertThrows(ForbiddenException.class,
                () -> accessControlService.removeEntry(billingUser, entry.getId()));
    }
}
