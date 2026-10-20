package com.dermify.dermify.telemedicine;

/**
 * A single referral case, matching the package described in your report's
 * Section 3.8: image, AI prediction, confidence, severity, an optional
 * clinical note, and an anonymized facility ID. Deliberately has NO
 * patient name/contact fields — de-identification by design, not by policy.
 */
public class ReferralCase {
    public String caseId;
    public String imagePath;
    public String predictedClass;
    public float confidence;
    public String severity;
    public String clinicalNote;
    public long timestampMillis;
    public String facilityId;
    public String status;        // "PENDING" or "SENT"

    public ReferralCase() {}     // required for JSON deserialization

    public ReferralCase(String caseId, String imagePath, String predictedClass,
                         float confidence, String severity, String clinicalNote,
                         String facilityId) {
        this.caseId = caseId;
        this.imagePath = imagePath;
        this.predictedClass = predictedClass;
        this.confidence = confidence;
        this.severity = severity;
        this.clinicalNote = clinicalNote;
        this.facilityId = facilityId;
        this.timestampMillis = System.currentTimeMillis();
        this.status = "PENDING";
    }
}
