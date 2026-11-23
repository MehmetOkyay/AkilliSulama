package com.example.akillisulama;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class BildirimAlici extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String kanalId = "sulama_kanali";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel kanal = new NotificationChannel(kanalId, "Sulama Bildirimleri", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(kanal);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, kanalId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🌱 Sulama Vakti Yaklaşıyor!")
                .setContentText("Sulamaya son 10 dakika! Lütfen ESP32 adaptörünü fişe takın.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }
}