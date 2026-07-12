package com.skyshelf.app;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class DraggableSegmentedControl extends FrameLayout {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedIndex, int direction);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final View indicator;
    private final TextView firstLabel;
    private final TextView secondLabel;
    private final Runnable beginDragRunnable;

    private OnSelectionChangedListener listener;
    private int selectedIndex = 0;
    private float downRawX;
    private float indicatorDownTranslationX;
    private float indicatorGrabOffsetX;
    private boolean dragActive = false;
    private boolean touchStartedOnIndicator = false;
    private final int innerPadding;
    private static final int LONG_PRESS_DELAY_MS = 50;

    public DraggableSegmentedControl(Context context) {
        this(context, null);
    }

    public DraggableSegmentedControl(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DraggableSegmentedControl(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        setBackgroundResource(R.drawable.bg_nav_group);
        setClickable(true);
        post(this::disableAncestorClipping);
        innerPadding = dp(4);

        indicator = new View(context);
        indicator.setBackgroundResource(R.drawable.bg_nav_item_selected);
        indicator.setElevation(dp(1));
        addView(indicator);

        firstLabel = makeLabel(context);
        secondLabel = makeLabel(context);
        addView(firstLabel);
        addView(secondLabel);
        updateLabelColors();

        beginDragRunnable = () -> {
            if (!touchStartedOnIndicator) {
                return;
            }
            dragActive = true;
            indicator.animate().scaleX(1.16f).scaleY(1.34f).setDuration(120).start();
        };

        firstLabel.setOnClickListener(v -> chooseIndex(0, true));
        secondLabel.setOnClickListener(v -> chooseIndex(1, true));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        disableAncestorClipping();
    }

    private void disableAncestorClipping() {
        setClipChildren(false);
        setClipToPadding(false);
        ViewParent parent = getParent();
        int depth = 0;
        while (parent instanceof ViewGroup && depth < 8) {
            ViewGroup group = (ViewGroup) parent;
            group.setClipChildren(false);
            group.setClipToPadding(false);
            parent = group.getParent();
            depth++;
        }
    }

    private TextView makeLabel(Context context) {
        TextView label = new TextView(context);
        label.setGravity(Gravity.CENTER);
        label.setTextColor(getResources().getColor(R.color.weather_ink, context.getTheme()));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setIncludeFontPadding(false);
        label.setSingleLine(true);
        label.setClickable(true);
        label.setFocusable(true);
        label.setPadding(0, 0, 0, 0);
        return label;
    }

    public void setLabels(String first, String second) {
        firstLabel.setText(first);
        secondLabel.setText(second);
        updateLabelColors();
        requestLayout();
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index, boolean animate) {
        selectedIndex = index <= 0 ? 0 : 1;
        updateLabelColors();
        post(() -> moveIndicatorToSelected(animate));
    }

    public void chooseIndex(int index, boolean animate) {
        int target = index <= 0 ? 0 : 1;
        if (target == selectedIndex) {
            moveIndicatorToSelected(true);
            return;
        }
        int oldIndex = selectedIndex;
        selectedIndex = target;
        updateLabelColors();
        moveIndicatorToSelected(animate);
        if (listener != null) {
            listener.onSelectionChanged(selectedIndex, selectedIndex > oldIndex ? 1 : -1);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int segmentWidth = getOptionWidth(width);
        int segmentHeight = Math.max(0, height - innerPadding * 2);

        int childWidthSpec = MeasureSpec.makeMeasureSpec(segmentWidth, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(segmentHeight, MeasureSpec.EXACTLY);
        indicator.measure(childWidthSpec, childHeightSpec);
        firstLabel.measure(childWidthSpec, childHeightSpec);
        secondLabel.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        int segmentWidth = getOptionWidth(width);
        int segmentHeight = Math.max(0, height - innerPadding * 2);
        int childTop = innerPadding;
        int firstLeft = innerPadding;
        int secondLeft = innerPadding + segmentWidth;

        indicator.layout(firstLeft, childTop, firstLeft + segmentWidth, childTop + segmentHeight);
        firstLabel.layout(firstLeft, childTop, firstLeft + segmentWidth, childTop + segmentHeight);
        secondLabel.layout(secondLeft, childTop, secondLeft + segmentWidth, childTop + segmentHeight);
        moveIndicatorToSelected(false);
    }

    private void moveIndicatorToSelected(boolean animate) {
        float targetTranslationX = selectedIndex == 0 ? 0f : getOptionWidth();
        indicator.animate().cancel();
        if (animate) {
            indicator.animate()
                    .translationX(targetTranslationX)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180)
                    .start();
        } else {
            indicator.setTranslationX(targetTranslationX);
            indicator.setScaleX(1f);
            indicator.setScaleY(1f);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            return isTouchInsideIndicator(event.getX());
        }
        return dragActive || super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = event.getRawX();
                indicatorDownTranslationX = indicator.getTranslationX();
                indicatorGrabOffsetX = event.getX() - (innerPadding + indicatorDownTranslationX);
                touchStartedOnIndicator = isTouchInsideIndicator(event.getX());
                dragActive = false;
                if (touchStartedOnIndicator) {
                    handler.postDelayed(beginDragRunnable, LONG_PRESS_DELAY_MS);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (touchStartedOnIndicator && !dragActive) {
                    float moved = Math.abs(event.getRawX() - downRawX);
                    if (moved > dp(8)) {
                        handler.removeCallbacks(beginDragRunnable);
                    }
                }
                if (dragActive) {
                    float nextTranslationX = event.getX() - innerPadding - indicatorGrabOffsetX;
                    indicator.setTranslationX(clampIndicatorTranslationX(nextTranslationX));
                    updateLabelColorsForIndicatorPosition();
                    return true;
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(beginDragRunnable);
                if (dragActive) {
                    finishDrag();
                    return true;
                }
                if (!touchStartedOnIndicator && event.getActionMasked() == MotionEvent.ACTION_UP) {
                    performClick();
                    chooseIndex(event.getX() < getWidth() / 2f ? 0 : 1, true);
                } else {
                    moveIndicatorToSelected(true);
                }
                touchStartedOnIndicator = false;
                dragActive = false;
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private boolean isTouchInsideIndicator(float localX) {
        float left = innerPadding + indicator.getTranslationX();
        float right = left + getOptionWidth();
        return localX >= left && localX <= right;
    }

    private void finishDrag() {
        int oldIndex = selectedIndex;
        float centerX = innerPadding + indicator.getTranslationX() + getOptionWidth() / 2f;
        selectedIndex = centerX < getWidth() / 2f ? 0 : 1;
        updateLabelColors();
        moveIndicatorToSelected(true);
        touchStartedOnIndicator = false;
        dragActive = false;
        if (listener != null && selectedIndex != oldIndex) {
            listener.onSelectionChanged(selectedIndex, selectedIndex > oldIndex ? 1 : -1);
        }
    }

    private float clampIndicatorTranslationX(float translationX) {
        return Math.max(0f, Math.min(getOptionWidth(), translationX));
    }

    private int getOptionWidth() {
        return getOptionWidth(getWidth());
    }

    private int getOptionWidth(int width) {
        return Math.max(0, (width - innerPadding * 2) / 2);
    }



    private void updateLabelColorsForIndicatorPosition() {
        int hoverIndex = (innerPadding + indicator.getTranslationX() + getOptionWidth() / 2f) < getWidth() / 2f ? 0 : 1;
        int selectedColor = getResources().getColor(R.color.weather_ink, getContext().getTheme());
        int unselectedColor = getResources().getColor(R.color.white, getContext().getTheme());
        firstLabel.setTextColor(hoverIndex == 0 ? selectedColor : unselectedColor);
        secondLabel.setTextColor(hoverIndex == 1 ? selectedColor : unselectedColor);
    }

    private void updateLabelColors() {
        int selectedColor = getResources().getColor(R.color.weather_ink, getContext().getTheme());
        int unselectedColor = getResources().getColor(R.color.white, getContext().getTheme());
        firstLabel.setTextColor(selectedIndex == 0 ? selectedColor : unselectedColor);
        secondLabel.setTextColor(selectedIndex == 1 ? selectedColor : unselectedColor);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
