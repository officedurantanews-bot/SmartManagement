package co.median.android.rdzdamq;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class NMApp extends Application {

    // ★★★ Firebase Project Info (google-services.json থেকে) ★★★
    public static final String FCM_SENDER_ID  = "755655691722";
    public static final String FCM_API_KEY    = "AIzaSyB-9JyLCLh2-VscGm3bdtXemY8QJtqIKyg";
    public static final String FCM_APP_ID     = "1:755655691722:android:714481bf487ce949e1d333";

    // GCM/FCM Registration endpoint
    public static final String GCM_REGISTER_URL = "https://android.googleapis.com/gcm/registrationv2";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannels();
        }
    }

    private void createChannels() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // কল channel
        NotificationChannel call = new NotificationChannel(
            "nm_calls", "ইনকামিং কল", NotificationManager.IMPORTANCE_HIGH);
        call.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        call.enableVibration(true);
        call.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 500});
        Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes aa = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
            call.setSound(ringtone, aa);
        }
        nm.createNotificationChannel(call);

        // General channel
        NotificationChannel gen = new NotificationChannel(
            "nm_general", "নোটিফিকেশন", NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(gen);
    }
}
