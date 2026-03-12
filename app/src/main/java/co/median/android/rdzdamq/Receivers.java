package co.median.android.rdzdamq;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.webkit.JavascriptInterface;

// ── JavaScript Bridge ──────────────────────────────────────────
class NMBridge {
    private final Context ctx;
    NMBridge(Context ctx) { this.ctx = ctx; }

    @JavascriptInterface
    public String getFCMToken() {
        return ctx.getSharedPreferences("nm_prefs", Context.MODE_PRIVATE)
            .getString("fcm_token", "");
    }

    @JavascriptInterface
    public String getPlatform() { return "android"; }
}

// ── Call Reject Receiver ───────────────────────────────────────
class CallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if ("REJECT_CALL".equals(intent.getAction())) {
            ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(999);
        }
    }
}

// ── Boot Receiver — ফোন চালু হলে token re-register ──────────
class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            RegistrationService.start(ctx);
        }
    }
}
