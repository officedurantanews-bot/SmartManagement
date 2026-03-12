package com.durantanews.smartmanagement;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

public class NMFirebaseService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage msg) {
        Map<String,String> data = msg.getData();
        String type  = data.containsKey("type")  ? data.get("type")  : "";
        String title = data.containsKey("title") ? data.get("title") : "নোটিফিকেশন";
        String body  = data.containsKey("body")  ? data.get("body")  : "";
        if (msg.getNotification() != null) {
            if (title.equals("নোটিফিকেশন") && msg.getNotification().getTitle() != null)
                title = msg.getNotification().getTitle();
            if (body.isEmpty() && msg.getNotification().getBody() != null)
                body = msg.getNotification().getBody();
        }
        boolean isCall = type.equals("incoming_call") || type.equals("po_incoming_call") || type.equals("group_call");
        if (isCall) { wakeScreen(); showCall(title, body, data); }
        else showGeneral(title, body, data);
    }

    private void showCall(String title, String body, Map<String,String> data) {
        Intent ai = new Intent(this, CallActivity.class);
        ai.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        for (Map.Entry<String,String> e : data.entrySet()) ai.putExtra(e.getKey(), e.getValue());
        int flag = Build.VERSION.SDK_INT >= 23
            ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent ap = PendingIntent.getActivity(this, 1, ai, flag);
        Intent ri = new Intent(this, CallReceiver.class).setAction("REJECT_CALL");
        for (Map.Entry<String,String> e : data.entrySet()) ri.putExtra(e.getKey(), e.getValue());
        PendingIntent rp = PendingIntent.getBroadcast(this, 2, ri, flag);
        Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        nm().notify(999, new NotificationCompat.Builder(this, "nm_calls")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(title).setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true).setAutoCancel(false)
            .setFullScreenIntent(ap, true)
            .setSound(ringtone)
            .setVibrate(new long[]{0,500,200,500,200,500})
            .setContentIntent(ap)
            .addAction(android.R.drawable.ic_menu_call, "রিসিভ", ap)
            .addAction(android.R.drawable.ic_delete, "রিজেক্ট", rp)
            .build());
    }

    private void showGeneral(String title, String body, Map<String,String> data) {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        for (Map.Entry<String,String> e : data.entrySet()) i.putExtra(e.getKey(), e.getValue());
        int flag = Build.VERSION.SDK_INT >= 23
            ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, flag);
        nm().notify((int)System.currentTimeMillis(),
            new NotificationCompat.Builder(this, "nm_general")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title).setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true).setContentIntent(pi).build());
    }

    private void wakeScreen() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm == null) return;
        pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "NMApp:Call").acquire(15000);
    }

    @Override
    public void onNewToken(String token) {
        getSharedPreferences("nm_prefs", MODE_PRIVATE).edit().putString("fcm_token", token).apply();
    }

    private NotificationManager nm() { return (NotificationManager) getSystemService(NOTIFICATION_SERVICE); }
}
