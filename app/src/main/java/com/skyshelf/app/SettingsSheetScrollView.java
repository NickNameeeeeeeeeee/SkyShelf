package com.skyshelf.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ScrollView;

public class SettingsSheetScrollView extends ScrollView {
    private boolean scrollingEnabled = true;
    private float downX;
    private float downY;
    private int touchSlop;

    public SettingsSheetScrollView(Context context) {
        super(context);
        init(context);
    }

    public SettingsSheetScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SettingsSheetScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setScrollingEnabled(boolean enabled) {
        scrollingEnabled = enabled;
        if (!enabled) {
            scrollTo(0, 0);
        }
    }

    public boolean isScrollingEnabled() {
        return scrollingEnabled;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (scrollingEnabled) {
            return super.onInterceptTouchEvent(ev);
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                // Let children still receive taps, but keep the gesture stream alive.
                super.onInterceptTouchEvent(ev);
                return false;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - downX);
                float dy = Math.abs(ev.getY() - downY);
                // In Account Info, do not scroll internally. Intercept only intentional
                // vertical drags so SettingsActivity can move/bounce the whole sheet.
                return dy > touchSlop && dy > dx;
            default:
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (scrollingEnabled) {
            return super.onTouchEvent(ev);
        }
        // Consume the intercepted drag sequence without scrolling content. The
        // Activity-level touch listener handles sheet height, snap, and bounce.
        return true;
    }
}
