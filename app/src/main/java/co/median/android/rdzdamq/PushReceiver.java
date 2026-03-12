package com.durantanews.smartmanagement;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;

/**
 * GCM/FCM Push BroadcastReceiver — Firebase SDK ছাড়াই push পাবে।
 * Google Play Services সরাসরি এই receiver-এ message পাঠায়।
 */
public class PushReceiver extends BroadcastReceiver {

    static final String TAG = "NMPush";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received: " + action);

        if ("com.google.android.c2dm.intent.REGISTRATION".equals(action)) {
            // Token registration response
            String token = intent.getStringExtra("registration_id");
            if (token != null && !token.isEmpty()) {
                ctx.getSharedPreferences("nm_prefs", Context.MODE_PRIVATE)
                    .edit().putString("fcm_token", token).apply();
                Log.d(TAG, "Token registered: " + token.substring(0, Math.min(20, token.length())) + "...");
                Intent broadcast = new Intent("co.median.android.rdzdamq.TOKEN_READY");
                broadcast.putExtra("token", token);
                ctx.sendBroadcast(broadcast);
            }
            return;
        }

        if (!"com.google.android.c2dm.intent.RECEIVE".equals(action)) return;

        // Push message পেলাম
        Bundle extras = intent.getExtras();
        if (extras == null) return;

        String type  = s(extras.getString("type"));
        String title = s(extras.getString("title"), "নোটিফিকেশন");
        String body  = s(extras.getString("body"),  "");

        boolean isCall = type.equals("incoming_call")
                      || type.equals("po_incoming_call")
                      || type.equals("group_call");

        if (isCall) {
            wakeScreen(ctx);
            showCall(ctx, title, body, extras);
        } else {
            showGeneral(ctx, title, body, extras);
        }
    }

    private void showCall(Context ctx, String title, String body, Bundle data) {
        Intent ai = new Intent(ctx, CallActivity.class);
        ai.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        copyExtras(data, ai);

        int flag = Build.VERSION.SDK_INT >= 23
            ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent ap = PendingIntent.getActivity(ctx, 1, ai, flag);

        Intent ri = new Intent(ctx, CallReceiver.class).setAction("REJECT_CALL");
        copyExtras(data, ri);
        PendingIntent rp = PendingIntent.getBroadcast(ctx, 2, ri, flag);

        Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, "nm_calls")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(ap, true)
            .setSound(ringtone)
            .setVibrate(new long[]{0, 500, 200, 500, 200, 500})
            .setContentIntent(ap)
            .addAction(android.R.drawable.ic_menu_call, "রিসিভ", ap)
            .addAction(android.R.drawable.ic_delete,    "রিজেক্ট", rp);

        nm(ctx).notify(999, b.build());
    }

    private void showGeneral(Context ctx, String title, String body, Bundle data) {
        Intent i = new Intent(ctx, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        copyExtras(data, i);

        int flag = Build.VERSION.SDK_INT >= 23
            ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent pi = PendingIntent.getActivity(ctx, 0, i, flag);

        nm(ctx).notify((int) System.currentTimeMillis(),
            new NotificationCompat.Builder(ctx, "nm_general")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build());
    }

    private void wakeScreen(Context ctx) {
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        if (pm == null) return;
        PowerManager.WakeLock wl = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "NMApp:Call");
        wl.acquire(15000);
    }

    private void copyExtras(Bundle b, Intent i) {
        if (b == null) return;
        for (String k : b.keySet()) {
            Object v = b.get(k);
            if (v instanceof String) i.putExtra(k, (String) v);
        }
    }

    private NotificationManager nm(Context ctx) {
        return (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private String s(String v) { return v == null ? "" : v; }
    private String s(String v, String d) { return (v == null || v.isEmpty()) ? d : v; }
}
