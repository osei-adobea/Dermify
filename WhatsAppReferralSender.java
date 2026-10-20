package com.dermify.dermify.telemedicine;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Sends a referral case to a specialist via WhatsApp, using Android's
 * standard share intent rather than the WhatsApp Business API.
 *
 * WHY NOT THE BUSINESS API: the Cloud API needs Meta Business
 * verification, a registered phone number, a backend server, and
 * webhook handling to parse specialist replies — infrastructure that
 * doesn't fit inside a Phase 1 academic timeline. This share-intent
 * approach still delivers the case (image + de-identified summary) to
 * a specialist's WhatsApp in one tap, and is the honest, achievable
 * "store-and-forward" implementation for Phase 1. Full Business API
 * automation, including parsing structured specialist replies back
 * into the app, is documented as Phase 2 future work.
 */
public class WhatsAppReferralSender {

    private static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    /**
     * @param authority must match android:authorities in your
     *                  AndroidManifest.xml's FileProvider entry (see guide)
     */
    public static boolean sendCase(Context context, ReferralCase referralCase, String authority) {
        File imageFile = new File(referralCase.imagePath);
        if (!imageFile.exists()) {
            Toast.makeText(context, "Referral image not found", Toast.LENGTH_SHORT).show();
            return false;
        }

        Uri imageUri = FileProvider.getUriForFile(context, authority, imageFile);
        String message = buildDeIdentifiedMessage(referralCase);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_STREAM, imageUri);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Requires a <queries> block in AndroidManifest.xml on API 30+,
        // otherwise these lookups always report "not installed".
        if (isPackageInstalled(context, "com.whatsapp")) {
            intent.setPackage("com.whatsapp");
        } else if (isPackageInstalled(context, "com.whatsapp.w4b")) {
            intent.setPackage("com.whatsapp.w4b");   // WhatsApp Business
        } else {
            intent = Intent.createChooser(intent, "Send referral via");
        }

        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Toast.makeText(context, "Could not open WhatsApp — is it installed?", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Builds the message sent alongside the image. Deliberately excludes
     * any patient-identifying information (name, contact details, exact
     * location) — only the de-identified case data described in Section
     * 3.8 of the report.
     */
    private static String buildDeIdentifiedMessage(ReferralCase c) {
        StringBuilder sb = new StringBuilder();
        sb.append("DERMIFY Referral\n");
        sb.append("Case ID: ").append(c.caseId).append("\n");
        sb.append("Facility: ").append(c.facilityId).append("\n");
        sb.append("Date: ").append(DATE_FORMAT.format(new Date(c.timestampMillis))).append("\n");
        sb.append("AI Prediction: ").append(c.predictedClass)
          .append(" (").append(Math.round(c.confidence * 100)).append("% confidence)\n");
        if (c.severity != null && !c.severity.isEmpty()) {
            sb.append("Severity: ").append(c.severity).append("\n");
        }
        if (c.clinicalNote != null && !c.clinicalNote.isEmpty()) {
            sb.append("Note: ").append(c.clinicalNote).append("\n");
        }
        sb.append("\n(Sent via Dermify — decision support tool, clinical judgment required)");
        return sb.toString();
    }

    private static boolean isPackageInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
