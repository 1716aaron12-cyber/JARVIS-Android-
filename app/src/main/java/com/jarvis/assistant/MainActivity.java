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
    private static final int AUDIO_PERMISSION_REQUEST = 7;

    private WebView web;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.UK);
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
                tts.setSpeechRate(0.9f);
                tts.setPitch(0.85f);
            }
        });

        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient());
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    class Bridge {
        @JavascriptInterface
        public void listen() {
            runOnUiThread(() -> {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            AUDIO_PERMISSION_REQUEST
                    );
                    return;
                }
                startListening();
            });
        }

        @JavascriptInterface
        public void speak(String words) {
            runOnUiThread(() -> {
                if (tts != null && ttsReady && words != null) {
                    tts.speak(words, TextToSpeech.QUEUE_FLUSH, null, "jarvis");
                }
            });
        }

        @JavascriptInterface
        public void openSettings() {
            runOnUiThread(() -> {
                try {
                    startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                } catch (Exception e) {
                    show("Unable to open settings.");
                }
            });
        }

        @JavascriptInterface
        public void openBrowser() {
            runOnUiThread(() -> {
                try {
                    Intent i = new Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://www.google.com")
                    );
                    startActivity(i);
                } catch (Exception e) {
                    show("Unable to open browser.");
                }
            });
        }
    }

    private void startListening() {
        try {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            );
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-GB");
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS");
            startActivityForResult(i, SPEECH_REQUEST);
        } catch (Exception e) {
            show("Speech recognition is unavailable on this device.");
        }
    }

    private void show(String s) {
        if (web != null) {
            web.evaluateJavascript(
                    "if (typeof window.onSpeech === 'function') { window.onSpeech("
                            + org.json.JSONObject.quote(s) + "); }",
                    null
            );
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == SPEECH_REQUEST && res == RESULT_OK && data != null) {
            ArrayList<String> results =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                show(results.get(0));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults
        );
        if (requestCode == AUDIO_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                show("Microphone permission is required.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (web != null) {
            web.destroy();
        }
        super.onDestroy();
    }
}
