package com.rescuehub;

import com.rescuehub.entity.Patient;
import com.rescuehub.entity.PatientIdentityVerification;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatientServiceTest extends BaseIntegrationTest {

    @Autowired
    private PatientService patientService;

    private Patient registerPatient(String suffix) {
        return patientService.register(
                adminUser,
                "Jane", "Doe" + suffix,
                LocalDate.of(1990, 5, 10),
                "F", "5551234000", "123 Main St",
                "Emergency Contact", "5559876543",
                false, false,
                "127.0.0.1", "ws-test");
    }

    @Test
    @Transactional
    void register_createsPatientWithMRN() {
        Patient p = registerPatient("-reg-" + System.nanoTime());
        assertNotNull(p.getId());
        assertNotNull(p.getMedicalRecordNumber());
        assertTrue(p.getMedicalRecordNumber().startsWith("MRN-"));
        assertEquals(testOrg.getId(), p.getOrganizationId());
    }

    @Test
    @Transactional
    void getById_returnsPatient() {
        Patient p = registerPatient("-get-" + System.nanoTime());
        Patient fetched = patientService.getById(adminUser, p.getId());
        assertEquals(p.getId(), fetched.getId());
    }

    @Test
    @Transactional
    void getById_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class,
                () -> patientService.getById(adminUser, Long.MAX_VALUE));
    }

    @Test
    @Transactional
    void list_returnsPageOfPatients() {
        registerPatient("-list-" + System.nanoTime());
        Page<Patient> page = patientService.list(adminUser, null, null, PageRequest.of(0, 20));
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
    }

    @Test
    @Transactional
    void list_withQuery_returnsFilteredResults() {
        Patient p = registerPatient("-qry-" + System.nanoTime());
        // Query by exact MRN — should find at least this patient
        Page<Patient> page = patientService.list(adminUser, p.getMedicalRecordNumber(), null, PageRequest.of(0, 20));
        assertNotNull(page);
        assertTrue(page.getContent().stream().anyMatch(r -> r.getId().equals(p.getId())));
    }

    @Test
    @Transactional
    void archive_setsArchivedAt() {
        Patient p = registerPatient("-arch-" + System.nanoTime());
        Patient archived = patientService.archive(adminUser, p.getId(), "127.0.0.1", "ws-test");
        assertNotNull(archived.getArchivedAt());
    }

    @Test
    @Transactional
    void archive_clinicianForbidden() {
        Patient p = registerPatient("-archfbd-" + System.nanoTime());
        assertThrows(ForbiddenException.class,
                () -> patientService.archive(clinicianUser, p.getId(), "127.0.0.1", "ws-test"));
    }

    @Test
    @Transactional
    void verifyIdentity_createsVerificationRecord() {
        Patient p = registerPatient("-vid-" + System.nanoTime());
        PatientIdentityVerification piv = patientService.verifyIdentity(
                adminUser, p.getId(),
                "PASSPORT", "1234",
                "Test note",
                "127.0.0.1", "ws-test");
        assertNotNull(piv.getId());
        assertEquals("PASSPORT", piv.getDocumentType());
        assertEquals("1234", piv.getDocumentLast4());
        assertEquals(p.getId(), piv.getPatientId());
    }

    @Test
    @Transactional
    void verifyIdentity_blankDocumentType_throwsBusinessRule() {
        Patient p = registerPatient("-vidbt-" + System.nanoTime());
        assertThrows(BusinessRuleException.class,
                () -> patientService.verifyIdentity(
                        adminUser, p.getId(),
                        "", "1234", null, "127.0.0.1", "ws-test"));
    }

    @Test
    @Transactional
    void verifyIdentity_invalidLast4_throwsBusinessRule() {
        Patient p = registerPatient("-vidl4-" + System.nanoTime());
        assertThrows(BusinessRuleException.class,
                () -> patientService.verifyIdentity(
                        adminUser, p.getId(),
                        "PASSPORT", "AB12", null, "127.0.0.1", "ws-test"));
    }

    @Test
    @Transactional
    void listVerifications_returnsVerifications() {
        Patient p = registerPatient("-lvr-" + System.nanoTime());
        patientService.verifyIdentity(adminUser, p.getId(), "DRIVERS_LICENSE", "5678",
                null, "127.0.0.1", "ws-test");
        List<PatientIdentityVerification> verifs = patientService.listVerifications(adminUser, p.getId());
        assertNotNull(verifs);
        assertEquals(1, verifs.size());
        assertEquals("DRIVERS_LICENSE", verifs.get(0).getDocumentType());
    }

    @Test
    @Transactional
    void reveal_adminRevealsDecryptedPii() {
        Patient p = registerPatient("-rev-" + System.nanoTime());
        PatientService.RevealResult result = patientService.reveal(
                adminUser, p.getId(), "127.0.0.1", "ws-test");
        assertNotNull(result);
        assertEquals("Jane", result.firstName());
        assertNotNull(result.lastName());
        assertNotNull(result.phone());
    }

    @Test
    @Transactional
    void reveal_clinicianOnNonProtectedCase_succeeds() {
        Patient p = registerPatient("-revclin-" + System.nanoTime());
        assertDoesNotThrow(() ->
                patientService.reveal(clinicianUser, p.getId(), "127.0.0.1", "ws-test"));
    }

    @Test
    @Transactional
    void reveal_clinicianOnProtectedCase_throwsForbidden() {
        Patient p = patientService.register(
                adminUser,
                "Protected", "Patient",
                LocalDate.of(2000, 1, 1),
                "M", null, null, null, null,
                false, true,
                "127.0.0.1", "ws-test");

        assertThrows(ForbiddenException.class,
                () -> patientService.reveal(clinicianUser, p.getId(), "127.0.0.1", "ws-test"));
    }
}
