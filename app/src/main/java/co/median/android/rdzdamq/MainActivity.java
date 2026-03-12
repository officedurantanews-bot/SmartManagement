package co.median.android.rdzdamq;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.*;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    WebView webView;
    SharedPreferences prefs;
    private BroadcastReceiver tokenReceiver;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        prefs   = getSharedPreferences("nm_prefs", MODE_PRIVATE);
        webView = (WebView) findViewById(R.id.webview);

        setupWebView();
        askPermissions();

        // Token পেলে inject করো
        tokenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent i) {
                injectToken();
            }
        };
        registerReceiver(tokenReceiver,
            new IntentFilter("co.median.android.rdzdamq.TOKEN_READY"));

        // FCM Token নেওয়া শুরু করো (background এ)
        if (prefs.getString("fcm_token", "").isEmpty()) {
            RegistrationService.start(this);
        }

        handleIntent(getIntent());
    }

    void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setDatabaseEnabled(true);
        s.setGeolocationEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        // Plugin এ Android app detect করতে পারবে এই User Agent দিয়ে
        s.setUserAgentString(s.getUserAgentString() + " NMApp/1.0 Android");

        webView.addJavascriptInterface(new NMBridge(this), "NMAndroid");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest req) {
                req.grant(req.getResources()); // WebRTC camera/mic
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView v, String url) {
                if (url != null && url.startsWith("http")) {
                    try {
                        Uri uri = Uri.parse(url);
                        String root = uri.getScheme() + "://" + uri.getHost();
                        prefs.edit()
                            .putString("site_url", root)
                            .putString("last_url", url)
                            .apply();
                    } catch (Exception ignored) {}
                }
                injectToken();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                String site = prefs.getString("site_url", "");
                if (!site.isEmpty() && !url.startsWith(site) && url.startsWith("http")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }
                return false;
            }
        });

        String lastUrl = prefs.getString("last_url", "");
        if (!lastUrl.isEmpty()) {
            webView.loadUrl(lastUrl);
        } else {
            webView.loadUrl("about:blank");
            Toast.makeText(this, "আপনার সাইটের URL দিয়ে login করুন", Toast.LENGTH_LONG).show();
        }
    }

    void injectToken() {
        String token = prefs.getString("fcm_token", "");
        if (token.isEmpty()) return;
        webView.evaluateJavascript(
            "(function(){" +
            "  if(typeof jQuery==='undefined'||typeof NM==='undefined') return;" +
            "  var k='nm_sent_'+location.hostname;" +
            "  if(localStorage.getItem(k)==='" + token + "') return;" +
            "  jQuery.post(NM.ajax,{" +
            "    action:'nm_save_fcm_token'," +
            "    nonce:NM.nonce," +
            "    fcm_token:'" + token + "'," +
            "    platform:'android'" +
            "  },function(r){" +
            "    if(r&&r.success) localStorage.setItem(k,'" + token + "');" +
            "  });" +
            "})()", null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    void handleIntent(Intent intent) {
        if (intent == null) return;

        // ✅ Deep Link — nmapp://open?site=https://example.com
        android.net.Uri data = intent.getData();
        if (data != null && "nmapp".equals(data.getScheme())) {
            String site = data.getQueryParameter("site");
            if (site != null && !site.isEmpty()) {
                prefs.edit()
                    .putString("site_url", site)
                    .putString("last_url", site)
                    .apply();
                final String u = site;
                webView.post(new Runnable() {
                    public void run() { webView.loadUrl(u); }
                });
                return;
            }
        }

        String directUrl = intent.getStringExtra("direct_url");
        if (directUrl != null && !directUrl.isEmpty()) {
            webView.post(new Runnable() {
                public void run() { webView.loadUrl(directUrl); }
            });
            return;
        }

        String type    = intent.getStringExtra("type");
        String siteUrl = intent.getStringExtra("site_url");
        if (type == null) return;
        if (siteUrl == null || siteUrl.isEmpty())
            siteUrl = prefs.getString("site_url", "");
        if (siteUrl.isEmpty()) return;

        final String site = siteUrl;
        String repId = d(intent.getStringExtra("reporter_id"), "");
        String ct    = d(intent.getStringExtra("call_type"),   "voice");
        String room  = d(intent.getStringExtra("room_id"),     "");
        String from  = d(intent.getStringExtra("from_uid"),    "");
        String conv  = d(intent.getStringExtra("conv_id"),     "");

        String url;
        switch (type) {
            case "incoming_call":    url = site+"/nm-reporter-dash/?incoming_call=1&reporter_id="+repId+"&ctype="+ct; break;
            case "po_incoming_call": url = site+"/nm-reporter-dash/?po_call=1&from="+from+"&ctype="+ct+"&conv_id="+conv; break;
            case "group_call":       url = site+"/nm-reporter-dash/?group_call=1&room_id="+room; break;
            case "message":
            case "po_message":       url = site+"/nm-reporter-dash/?open_chat=1"; break;
            default: return;
        }
        final String u = url;
        webView.post(new Runnable() { public void run() { webView.loadUrl(u); } });
    }

    void askPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        List<String> need = new ArrayList<>();
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        for (String p : perms)
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) need.add(p);
        if (Build.VERSION.SDK_INT >= 33)
            need.add("android.permission.POST_NOTIFICATIONS");
        if (!need.isEmpty())
            requestPermissions(need.toArray(new String[0]), 100);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(tokenReceiver); } catch (Exception ignored) {}
    }

    String d(String v, String def) { return (v == null || v.isEmpty()) ? def : v; }
}
