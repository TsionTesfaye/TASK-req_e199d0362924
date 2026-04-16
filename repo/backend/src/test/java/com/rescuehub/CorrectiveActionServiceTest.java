package com.rescuehub;

import com.rescuehub.entity.CorrectiveAction;
import com.rescuehub.enums.CorrectiveActionStatus;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.service.CorrectiveActionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

class CorrectiveActionServiceTest extends BaseIntegrationTest {

    @Autowired
    private CorrectiveActionService correctiveActionService;

    @Test
    @Transactional
    void create_qualityUserCreatesCAWithOpenStatus() {
        long nanos = System.nanoTime();

        CorrectiveAction ca = correctiveActionService.create(
                qualityUser,
                "Test corrective action description " + nanos,
                null, null,
                "127.0.0.1", "ws");

        assertNotNull(ca);
        assertNotNull(ca.getId());
        assertEquals(CorrectiveActionStatus.OPEN, ca.getStatus());
        assertEquals(qualityUser.getOrganizationId(), ca.getOrganizationId());
    }

    @Test
    @Transactional
    void create_clinicianThrowsForbidden() {
        assertThrows(ForbiddenException.class, () ->
                correctiveActionService.create(
                        clinicianUser,
                        "Forbidden CA",
                        null, null,
                        "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void list_qualityUserCanListCAs() {
        long nanos = System.nanoTime();
        correctiveActionService.create(
                qualityUser, "Listed CA " + nanos,
                null, null, "127.0.0.1", "ws");

        Page<CorrectiveAction> page = correctiveActionService.list(
                qualityUser, PageRequest.of(0, 20));

        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
    }

    @Test
    @Transactional
    void transition_openToAssignedIsValid() {
        long nanos = System.nanoTime();
        CorrectiveAction ca = correctiveActionService.create(
                qualityUser, "Transition CA " + nanos,
                null, null, "127.0.0.1", "ws");
        assertEquals(CorrectiveActionStatus.OPEN, ca.getStatus());

        CorrectiveAction assigned = correctiveActionService.transition(
                qualityUser, ca.getId(), CorrectiveActionStatus.ASSIGNED,
                null, qualityUser.getId(), "127.0.0.1", "ws");

        assertEquals(CorrectiveActionStatus.ASSIGNED, assigned.getStatus());
    }

    @Test
    @Transactional
    void transition_openToResolvedIsInvalidThrowsBusinessRuleException() {
        long nanos = System.nanoTime();
        CorrectiveAction ca = correctiveActionService.create(
                qualityUser, "Invalid Transition CA " + nanos,
                null, null, "127.0.0.1", "ws");
        assertEquals(CorrectiveActionStatus.OPEN, ca.getStatus());

        // OPEN -> RESOLVED is not a direct valid transition (must go OPEN->ASSIGNED->IN_PROGRESS->RESOLVED)
        assertThrows(BusinessRuleException.class, () ->
                correctiveActionService.transition(
                        qualityUser, ca.getId(), CorrectiveActionStatus.RESOLVED,
                        null, null, "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void transition_assignedToInProgressIsValid() {
        long nanos = System.nanoTime();
        CorrectiveAction ca = correctiveActionService.create(
                qualityUser, "Two-Step CA " + nanos,
                null, null, "127.0.0.1", "ws");

        // OPEN -> ASSIGNED
        CorrectiveAction assigned = correctiveActionService.transition(
                qualityUser, ca.getId(), CorrectiveActionStatus.ASSIGNED,
                null, qualityUser.getId(), "127.0.0.1", "ws");
        assertEquals(CorrectiveActionStatus.ASSIGNED, assigned.getStatus());

        // ASSIGNED -> IN_PROGRESS
        CorrectiveAction inProgress = correctiveActionService.transition(
                qualityUser, ca.getId(), CorrectiveActionStatus.IN_PROGRESS,
                null, null, "127.0.0.1", "ws");
        assertEquals(CorrectiveActionStatus.IN_PROGRESS, inProgress.getStatus());
    }

    @Test
    @Transactional
    void getById_correctFetch() {
        long nanos = System.nanoTime();
        CorrectiveAction created = correctiveActionService.create(
                qualityUser, "Fetchable CA " + nanos,
                null, null, "127.0.0.1", "ws");

        CorrectiveAction fetched = correctiveActionService.getById(qualityUser, created.getId());

        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
        assertEquals(created.getDescription(), fetched.getDescription());
    }

    @Test
    @Transactional
    void getById_notFoundThrowsNotFoundException() {
        assertThrows(NotFoundException.class,
                () -> correctiveActionService.getById(qualityUser, Long.MAX_VALUE));
    }
}
