package com.durantanews.smartmanagement;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class CallActivity extends Activity {

    private Handler h;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        // লক স্ক্রিন ভেদ করে দেখাবে
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED  |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON    |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON    |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        setContentView(R.layout.activity_call);

        Intent i      = getIntent();
        SharedPreferences prefs = getSharedPreferences("nm_prefs", MODE_PRIVATE);

        final String type    = s(i.getStringExtra("type"));
        final String title   = s(i.getStringExtra("title"), "ইনকামিং কল");
        final String ct      = s(i.getStringExtra("call_type"), "voice");
        final String repId   = s(i.getStringExtra("reporter_id"));
        final String from    = s(i.getStringExtra("from_uid"));
        final String room    = s(i.getStringExtra("room_id"));
        final String conv    = s(i.getStringExtra("conv_id"));
        final String site    = s(i.getStringExtra("site_url"), prefs.getString("site_url", ""));

        ((TextView) findViewById(R.id.tv_caller)).setText(title);
        ((TextView) findViewById(R.id.tv_type)).setText(
            "video".equals(ct) ? "ভিডিও কল আসছে..." : "ভয়েস কল আসছে...");

        ((Button) findViewById(R.id.btn_answer)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                cancelNotif();
                if (h != null) h.removeCallbacksAndMessages(null);

                String url;
                if ("po_incoming_call".equals(type))
                    url = site+"/nm-reporter-dash/?po_call=1&from="+from+"&ctype="+ct+"&conv_id="+conv;
                else if ("group_call".equals(type))
                    url = site+"/nm-reporter-dash/?group_call=1&room_id="+room;
                else
                    url = site+"/nm-reporter-dash/?incoming_call=1&reporter_id="+repId+"&ctype="+ct;

                Intent main = new Intent(CallActivity.this, MainActivity.class);
                main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                main.putExtra("direct_url", url);
                startActivity(main);
                finish();
            }
        });

        ((Button) findViewById(R.id.btn_reject)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                cancelNotif();
                if (h != null) h.removeCallbacksAndMessages(null);
                finish();
            }
        });

        // ৪৫ সেকেন্ড পর missed call
        h = new Handler();
        h.postDelayed(new Runnable() {
            @Override public void run() { cancelNotif(); finish(); }
        }, 45000);
    }

    void cancelNotif() {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(999);
    }

    String s(String v) { return v == null ? "" : v; }
    String s(String v, String d) { return (v == null || v.isEmpty()) ? d : v; }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (h != null) h.removeCallbacksAndMessages(null);
    }
}
