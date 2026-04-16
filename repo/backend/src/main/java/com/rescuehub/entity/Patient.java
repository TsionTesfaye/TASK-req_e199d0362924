package com.rescuehub.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "patient")
public class Patient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long organizationId;
    @Column(nullable = false) private String medicalRecordNumber;
    private byte[] firstNameCiphertext;
    private byte[] firstNameIv;
    private byte[] lastNameCiphertext;
    private byte[] lastNameIv;
    @Column(nullable = false) private LocalDate dateOfBirth;
    private String sex;
    private byte[] phoneCiphertext;
    private byte[] phoneIv;
    private String phoneLast4;
    private byte[] addressCiphertext;
    private byte[] addressIv;
    private String emergencyContactName;
    private byte[] emergencyContactPhoneCiphertext;
    private byte[] emergencyContactPhoneIv;
    @Column(nullable = false) private boolean isMinor = false;
    @Column(nullable = false) private boolean isProtectedCase = false;
    private Instant archivedAt;
    private Instant legalHoldUntil;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @Version private Long version;

    @PrePersist protected void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getMedicalRecordNumber() { return medicalRecordNumber; }
    public void setMedicalRecordNumber(String medicalRecordNumber) { this.medicalRecordNumber = medicalRecordNumber; }
    public byte[] getFirstNameCiphertext() { return firstNameCiphertext; }
    public void setFirstNameCiphertext(byte[] firstNameCiphertext) { this.firstNameCiphertext = firstNameCiphertext; }
    public byte[] getFirstNameIv() { return firstNameIv; }
    public void setFirstNameIv(byte[] firstNameIv) { this.firstNameIv = firstNameIv; }
    public byte[] getLastNameCiphertext() { return lastNameCiphertext; }
    public void setLastNameCiphertext(byte[] lastNameCiphertext) { this.lastNameCiphertext = lastNameCiphertext; }
    public byte[] getLastNameIv() { return lastNameIv; }
    public void setLastNameIv(byte[] lastNameIv) { this.lastNameIv = lastNameIv; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public byte[] getPhoneCiphertext() { return phoneCiphertext; }
    public void setPhoneCiphertext(byte[] phoneCiphertext) { this.phoneCiphertext = phoneCiphertext; }
    public byte[] getPhoneIv() { return phoneIv; }
    public void setPhoneIv(byte[] phoneIv) { this.phoneIv = phoneIv; }
    public String getPhoneLast4() { return phoneLast4; }
    public void setPhoneLast4(String phoneLast4) { this.phoneLast4 = phoneLast4; }
    public byte[] getAddressCiphertext() { return addressCiphertext; }
    public void setAddressCiphertext(byte[] addressCiphertext) { this.addressCiphertext = addressCiphertext; }
    public byte[] getAddressIv() { return addressIv; }
    public void setAddressIv(byte[] addressIv) { this.addressIv = addressIv; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public byte[] getEmergencyContactPhoneCiphertext() { return emergencyContactPhoneCiphertext; }
    public void setEmergencyContactPhoneCiphertext(byte[] emergencyContactPhoneCiphertext) { this.emergencyContactPhoneCiphertext = emergencyContactPhoneCiphertext; }
    public byte[] getEmergencyContactPhoneIv() { return emergencyContactPhoneIv; }
    public void setEmergencyContactPhoneIv(byte[] emergencyContactPhoneIv) { this.emergencyContactPhoneIv = emergencyContactPhoneIv; }
    public boolean isMinor() { return isMinor; }
    public void setMinor(boolean minor) { isMinor = minor; }
    public boolean isProtectedCase() { return isProtectedCase; }
    public void setProtectedCase(boolean protectedCase) { isProtectedCase = protectedCase; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public Instant getLegalHoldUntil() { return legalHoldUntil; }
    public void setLegalHoldUntil(Instant legalHoldUntil) { this.legalHoldUntil = legalHoldUntil; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
