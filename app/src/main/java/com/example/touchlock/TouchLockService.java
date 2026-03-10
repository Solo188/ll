package com.example.touchlock;

import android.app.*;
import android.content.*;
import android.os.*;
import okhttp3.*;
import org.json.*;
import java.util.concurrent.TimeUnit;

public class TouchLockService extends Service {
    private static final String CHANNEL_ID = "TouchLockChannel";
    private static final String BOT_TOKEN = "8388799545:AAGPwGKOTs47C29s6PUDFsqZbAjNh9wdrgE";
    
    private OkHttpClient client;
    private long lastUpdateId = 0;
    private HandlerThread botThread;
    private Handler botHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Настройка клиента
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        
        createNotificationChannel();
        startForeground(1, getLockNotification("Бот запущен. Ожидание команд..."));
        
        // Поток для бота
        botThread = new HandlerThread("TelegramBotThread");
        botThread.start();
        botHandler = new Handler(botThread.getLooper());
        
        startTelegramPolling();
    }

    private void startTelegramPolling() {
        botHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/getUpdates?offset=" + (lastUpdateId + 1) + "&timeout=20";
                    Request request = new Request.Builder().url(url).build();
                    
                    try (Response response = client.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String jsonData = response.body().string();
                            JSONObject jsonObject = new JSONObject(jsonData);
                            JSONArray result = jsonObject.getJSONArray("result");

                            for (int i = 0; i < result.length(); i++) {
                                JSONObject update = result.getJSONObject(i);
                                lastUpdateId = update.getLong("update_id");
                                
                                if (update.has("message")) {
                                    String text = update.getJSONObject("message").optString("text", "");
                                    
                                    // Передача команд в службу доступности
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        if (TouchLockAccessibilityService.instance != null) {
                                            if (text.equalsIgnoreCase("/block")) {
                                                TouchLockAccessibilityService.instance.lock();
                                                updateNotification("Экран заблокирован");
                                            } else if (text.equalsIgnoreCase("/stop")) {
                                                TouchLockAccessibilityService.instance.unlock();
                                                updateNotification("Экран разблокирован");
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                botHandler.postDelayed(this, 1000);
            }
        });
    }

    private Notification getLockNotification(String text) {
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Touch Blocker")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_secure)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(1, getLockNotification(text));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Lock", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        if (botThread != null) botThread.quit();
        super.onDestroy();
    }
}
