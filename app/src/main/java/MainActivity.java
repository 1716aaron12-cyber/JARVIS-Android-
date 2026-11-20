package com.jarvis.assistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int SPEECH_REQUEST = 42;
    private WebView web;
    private TextToSpeech tts;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        tts = new TextToSpeech(this, status -> { if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.UK); });
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient());
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }
    class Bridge {
        @JavascriptInterface public void listen() { runOnUiThread(() -> {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 7); return;
            }
            try {
                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-GB");
                startActivityForResult(i, SPEECH_REQUEST);
            } catch (Exception e) { show("Speech recognition is unavailable on this device."); }
        }); }
        @JavascriptInterface public void speak(String words) { runOnUiThread(() -> { if (tts != null) tts.speak(words, TextToSpeech.QUEUE_FLUSH, null, "jarvis"); }); }
        @JavascriptInterface public void openSettings() { runOnUiThread(() -> startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS))); }
        @JavascriptInterface public void openBrowser() { runOnUiThread(() -> {
            Intent i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"));
            startActivity(i);
        }); }
    }
    private void show(String s) { web.evaluateJavascript("window.onSpeech(" + org.json.JSONObject.quote(s) + ")", null); }
    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == SPEECH_REQUEST && res == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) show(results.get(0));
        }
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 7 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) new Bridge().listen();
    }
    @Override protected void onDestroy() { if (tts != null) { tts.stop(); tts.shutdown(); } if (web != null) web.destroy(); super.onDestroy(); }
}
