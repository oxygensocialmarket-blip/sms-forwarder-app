package com.zynex.smsforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.os.Handler;
import android.os.Looper;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SmsReceiver extends BroadcastReceiver {

    // Apnar Serverix host er Webhook URL
    private static final String WEBHOOK_URL = "http://node2.serverix.cloud:2936/sms-webhook";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        String sender = smsMessage.getDisplayOriginatingAddress();
                        String message = smsMessage.getMessageBody();

                        // Background theke Webhook e JSON pathano
                        sendToWebhook(sender, message);
                        
                        // HTML Design er vitor log update kora
                        if (MainActivity.myWebView != null) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                String safeMessage = message.replace("\n", " ").replace("'", "\\'");
                                MainActivity.myWebView.evaluateJavascript("javascript:updateLog('" + sender + "\\n" + safeMessage + "')", null);
                            });
                        }
                    }
                }
            }
        }
    }

    private void sendToWebhook(String sender, String message) {
        new Thread(() -> {
            try {
                URL url = new URL(WEBHOOK_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // JSON format toiri
                String safeMsg = message.replace("\n", " ").replace("\"", "\\\"");
                String jsonInputString = "{\"sender\": \"" + sender + "\", \"message\": \"" + safeMsg + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                System.out.println("Webhook Response: " + responseCode);
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
