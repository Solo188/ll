package com.example.touchlock;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.graphics.PixelFormat;
import android.view.WindowManager;
import android.view.View;

public class TouchLockAccessibilityService extends AccessibilityService {
    public static TouchLockAccessibilityService instance; // Статическая ссылка
    private WindowManager windowManager;
    private View overlayView;
    private boolean isLocked = false;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this; // Регистрируем службу
    }

    public void lock() {
        if (isLocked) return;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlayView = new View(this);
        overlayView.setBackgroundColor(0x02000000);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        overlayView.setOnTouchListener((v, event) -> true);
        windowManager.addView(overlayView, params);
        isLocked = true;
    }

    public void unlock() {
        if (isLocked && overlayView != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
            isLocked = false;
        }
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() { instance = null; }
    @Override public void onDestroy() { instance = null; super.onDestroy(); }
}
