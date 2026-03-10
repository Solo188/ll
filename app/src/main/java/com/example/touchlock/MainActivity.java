package com.example.touchlock;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Проверяем, включена ли наша Accessibility-служба
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Включите 'Touch Blocker' в разделе Специальные возможности", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        } else {
            // Если служба включена, запускаем сервис бота
            startService(new Intent(this, TouchLockService.class));
            Toast.makeText(this, "Сервис запущен!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + TouchLockAccessibilityService.class.getCanonicalName();
        String enabledServices = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabledServices)) return false;
        return enabledServices.contains(service);
    }
}
