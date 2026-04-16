package com.rescuehub;

import com.rescuehub.entity.Appointment;
import com.rescuehub.entity.Patient;
import com.rescuehub.entity.Organization;
import com.rescuehub.entity.User;
import com.rescuehub.enums.AppointmentStatus;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.repository.PatientRepository;
import com.rescuehub.service.AppointmentService;
import com.rescuehub.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentServiceTest extends BaseIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepo;

    private Patient makePatient() {
        long nanos = System.nanoTime();
        return patientService.register(
                frontDeskUser, "Appt", "Patient" + nanos,
                LocalDate.of(1990, 1, 1), "F", null, null, null, null,
                false, false, "127.0.0.1", "ws");
    }

    @Test
    @Transactional
    void create_frontDeskUserCreatesAppointmentForPatient() {
        Patient p = makePatient();

        Appointment appt = appointmentService.create(
                frontDeskUser, p.getId(),
                LocalDate.now().plusDays(3), LocalTime.of(10, 0),
                null, "127.0.0.1", "ws");

        assertNotNull(appt);
        assertNotNull(appt.getId());
        assertEquals(p.getId(), appt.getPatientId());
        assertEquals(AppointmentStatus.SCHEDULED, appt.getStatus());
        assertEquals(frontDeskUser.getOrganizationId(), appt.getOrganizationId());
    }

    @Test
    @Transactional
    void create_billingUserThrowsForbidden() {
        Patient p = makePatient();

        assertThrows(ForbiddenException.class, () ->
                appointmentService.create(
                        billingUser, p.getId(),
                        LocalDate.now().plusDays(1), LocalTime.NOON,
                        null, "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void create_invalidPatientIdThrowsNotFoundException() {
        assertThrows(NotFoundException.class, () ->
                appointmentService.create(
                        frontDeskUser, Long.MAX_VALUE,
                        LocalDate.now().plusDays(1), LocalTime.NOON,
                        null, "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void list_returnsAllAppointments() {
        Patient p = makePatient();
        appointmentService.create(
                frontDeskUser, p.getId(),
                LocalDate.now().plusDays(1), LocalTime.of(9, 0),
                null, "127.0.0.1", "ws");

        Page<Appointment> page = appointmentService.list(
                frontDeskUser, null, PageRequest.of(0, 20));

        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
    }

    @Test
    @Transactional
    void list_filteredByDate_returnsMatchingAppointments() {
        Patient p = makePatient();
        LocalDate targetDate = LocalDate.now().plusDays(7);
        appointmentService.create(
                frontDeskUser, p.getId(),
                targetDate, LocalTime.of(14, 0),
                null, "127.0.0.1", "ws");

        Page<Appointment> filtered = appointmentService.list(
                frontDeskUser, targetDate, PageRequest.of(0, 20));

        assertNotNull(filtered);
        assertTrue(filtered.getTotalElements() >= 1);
        filtered.getContent().forEach(a ->
                assertEquals(targetDate, a.getScheduledDate()));
    }

    @Test
    @Transactional
    void updateStatus_transitionToCancelled() {
        Patient p = makePatient();
        Appointment appt = appointmentService.create(
                frontDeskUser, p.getId(),
                LocalDate.now().plusDays(2), LocalTime.of(11, 0),
                null, "127.0.0.1", "ws");

        Appointment cancelled = appointmentService.updateStatus(
                frontDeskUser, appt.getId(), AppointmentStatus.CANCELED,
                "127.0.0.1", "ws");

        assertEquals(AppointmentStatus.CANCELED, cancelled.getStatus());
    }

    @Test
    @Transactional
    void getById_correctFetch() {
        Patient p = makePatient();
        Appointment created = appointmentService.create(
                frontDeskUser, p.getId(),
                LocalDate.now().plusDays(4), LocalTime.of(15, 0),
                null, "127.0.0.1", "ws");

        Appointment fetched = appointmentService.getById(frontDeskUser, created.getId());

        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
    }

    @Test
    @Transactional
    void getById_wrongOrgThrowsNotFoundException() {
        Patient p = makePatient();
        Appointment appt = appointmentService.create(
                frontDeskUser, p.getId(),
                LocalDate.now().plusDays(5), LocalTime.of(16, 0),
                null, "127.0.0.1", "ws");

        // Create a user from a different org
        Organization otherOrg = new Organization();
        otherOrg.setCode("APPT-OTHER-" + System.nanoTime());
        otherOrg.setName("Other Org Appt");
        otherOrg.setActive(true);
        otherOrg = orgRepo.save(otherOrg);

        User otherUser = new User();
        otherUser.setOrganizationId(otherOrg.getId());
        otherUser.setUsername("other_fd_appt_" + System.nanoTime());
        otherUser.setPasswordHash("x");
        otherUser.setDisplayName("Other FD");
        otherUser.setRole(Role.FRONT_DESK);
        otherUser.setActive(true);
        otherUser.setFrozen(false);
        otherUser.setPasswordChangedAt(Instant.now());
        otherUser = userRepo.save(otherUser);

        final User finalOther = otherUser;
        final Long apptId = appt.getId();
        assertThrows(NotFoundException.class,
                () -> appointmentService.getById(finalOther, apptId));
    }
}
