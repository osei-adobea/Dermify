package com.dermify.dermify.telemedicine;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The "store" half of store-and-forward: keeps a queue of referral cases
 * in a JSON file in the app's private storage (survives app restarts,
 * needs no network). Cases stay PENDING until the clinician taps "Send"
 * on a queued case (see WhatsAppReferralSender), at which point the case
 * is marked SENT.
 */
public class ReferralQueueManager {

    private static final String QUEUE_FILE = "referral_queue.json";
    private final Context context;

    public ReferralQueueManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public String addCase(String imagePath, String predictedClass, float confidence,
                           String severity, String clinicalNote, String facilityId) {
        List<ReferralCase> cases = loadAll();
        String caseId = UUID.randomUUID().toString().substring(0, 8);
        cases.add(new ReferralCase(caseId, imagePath, predictedClass, confidence,
                severity, clinicalNote, facilityId));
        saveAll(cases);
        return caseId;
    }

    public List<ReferralCase> getPendingCases() {
        List<ReferralCase> pending = new ArrayList<>();
        for (ReferralCase c : loadAll()) {
            if ("PENDING".equals(c.status)) pending.add(c);
        }
        return pending;
    }

    public List<ReferralCase> getSentCases() {
        List<ReferralCase> sent = new ArrayList<>();
        for (ReferralCase c : loadAll()) {
            if ("SENT".equals(c.status)) sent.add(c);
        }
        return sent;
    }

    public void markSent(String caseId) {
        List<ReferralCase> cases = loadAll();
        for (ReferralCase c : cases) {
            if (c.caseId.equals(caseId)) c.status = "SENT";
        }
        saveAll(cases);
    }

    public void deleteCase(String caseId) {
        List<ReferralCase> cases = loadAll();
        cases.removeIf(c -> c.caseId.equals(caseId));
        saveAll(cases);
    }

    private List<ReferralCase> loadAll() {
        List<ReferralCase> result = new ArrayList<>();
        File file = new File(context.getFilesDir(), QUEUE_FILE);
        if (!file.exists()) return result;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            JSONArray arr = new JSONArray(new String(data, StandardCharsets.UTF_8));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                ReferralCase c = new ReferralCase();
                c.caseId = o.getString("caseId");
                c.imagePath = o.getString("imagePath");
                c.predictedClass = o.getString("predictedClass");
                c.confidence = (float) o.getDouble("confidence");
                c.severity = o.optString("severity", "");
                c.clinicalNote = o.optString("clinicalNote", "");
                c.facilityId = o.optString("facilityId", "");
                c.timestampMillis = o.getLong("timestampMillis");
                c.status = o.getString("status");
                result.add(c);
            }
        } catch (Exception e) {
            // Corrupt or unreadable queue file: start fresh rather than crash the app
            e.printStackTrace();
        }
        return result;
    }

    private void saveAll(List<ReferralCase> cases) {
        try {
            JSONArray arr = new JSONArray();
            for (ReferralCase c : cases) {
                JSONObject o = new JSONObject();
                o.put("caseId", c.caseId);
                o.put("imagePath", c.imagePath);
                o.put("predictedClass", c.predictedClass);
                o.put("confidence", c.confidence);
                o.put("severity", c.severity);
                o.put("clinicalNote", c.clinicalNote);
                o.put("facilityId", c.facilityId);
                o.put("timestampMillis", c.timestampMillis);
                o.put("status", c.status);
                arr.put(o);
            }
            File file = new File(context.getFilesDir(), QUEUE_FILE);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(arr.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
