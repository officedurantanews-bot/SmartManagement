import co.median.android.rdzdamq.R;
package com.durantanews.smartmanagement;
import co.median.android.rdzdamq.R;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * FCM Token নেওয়ার Service — Firebase SDK ছাড়াই কাজ করে।
 * Google Play Services এর GCM API ব্যবহার করে token নেয়।
 */
public class RegistrationService extends IntentService {

    static final String TAG = "NMReg";
    static final String ACTION_REGISTER = "co.median.android.rdzdamq.REGISTER";

    public RegistrationService() { super("RegistrationService"); }

    public static void start(Context ctx) {
        Intent i = new Intent(ctx, RegistrationService.class);
        i.setAction(ACTION_REGISTER);
        ctx.startService(i);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!ACTION_REGISTER.equals(intent.getAction())) return;

        SharedPreferences prefs = getSharedPreferences("nm_prefs", MODE_PRIVATE);
        String existing = prefs.getString("fcm_token", "");

        // Token আগে থেকে থাকলে skip করো
        if (!existing.isEmpty()) {
            Log.d(TAG, "Token already saved: " + existing.substring(0, 20) + "...");
            return;
        }

        try {
            String token = registerWithGCM();
            if (token != null && !token.isEmpty()) {
                prefs.edit().putString("fcm_token", token).apply();
                Log.d(TAG, "FCM Token saved: " + token.substring(0, 20) + "...");

                // MainActivity কে জানাও
                Intent broadcast = new Intent("co.median.android.rdzdamq.TOKEN_READY");
                broadcast.putExtra("token", token);
                sendBroadcast(broadcast);
            }
        } catch (Exception e) {
            Log.e(TAG, "Registration failed: " + e.getMessage());
        }
    }

    /**
     * Google Play Services GCM HTTP API দিয়ে FCM token নেওয়া।
     * এটা Firebase SDK ছাড়াই কাজ করে।
     */
    private String registerWithGCM() throws Exception {
        URL url = new URL("https://android.googleapis.com/gcm/registrationv2");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        // Android Device ID
        String androidId = Settings.Secure.getString(
            getContentResolver(), Settings.Secure.ANDROID_ID);

        String body = "device="    + URLEncoder.encode(androidId, "UTF-8")
            + "&app="       + URLEncoder.encode(getPackageName(), "UTF-8")
            + "&sender="    + URLEncoder.encode(NMApp.FCM_SENDER_ID, "UTF-8")
            + "&X-scope="   + URLEncoder.encode("*", "UTF-8")
            + "&X-appid="   + URLEncoder.encode(NMApp.FCM_APP_ID, "UTF-8")
            + "&X-kid="     + URLEncoder.encode("|ID|" + NMApp.FCM_APP_ID, "UTF-8")
            + "&info="      + URLEncoder.encode(androidId, "UTF-8");

        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Authorization", "AidLogin " + androidId + ":" + NMApp.FCM_API_KEY);

        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes("UTF-8"));
        os.close();

        int code = conn.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(
            code == 200 ? conn.getInputStream() : conn.getErrorStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();

        String response = sb.toString();
        Log.d(TAG, "GCM response: " + response);

        // Response format: "token=APA91b..."
        if (response.startsWith("token=")) {
            return response.substring(6).trim();
        }
        return null;
    }
}
