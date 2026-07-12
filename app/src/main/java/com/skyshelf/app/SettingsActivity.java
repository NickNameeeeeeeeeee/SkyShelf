package com.skyshelf.app;

import android.annotation.SuppressLint;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private View settingsRoot;
    private View dimView;
    private SettingsSheetScrollView settingsSheet;
    private View homePanel;
    private View accountPanel;
    private TextView avatarText;
    private TextView accountAvatarText;
    private ImageView avatarImage;
    private ImageView accountAvatarImage;
    private View accountAvatarFrame;
    private ActivityResultLauncher<Intent> profilePhotoLauncher;
    private TextView accountStatusText;
    private TextView accountSubtitleText;
    private TextView displayNameText;
    private EditText usernameInput;
    private EditText passwordInput;
    private EditText newPasswordInput;
    private View nameEditorRow;
    private View passwordEditorRow;
    private View editPasswordButton;
    private TextView changeUsernameButton;
    private TextView changePasswordButton;
    private View logoutButton;

    private View guestAuthPanel;
    private View loginEditorRow;
    private View signupEditorRow;
    private EditText guestLoginUsernameInput;
    private EditText guestLoginPasswordInput;
    private EditText guestSignupUsernameInput;
    private EditText guestSignupPasswordInput;
    private TextView guestLoginButton;
    private TextView guestSignupButton;

    private View profileTile;
    private View emailButton;
    private View phoneButton;
    private View notificationRow;
    private View privacyRow;

    private boolean isClosing = false;
    private boolean showingAccountPanel = false;
    private int collapsedHeight = 0;
    private int expandedHeight = 0;
    private boolean canExpand = false;
    private boolean isExpanded = false;
    private float dragStartY = 0f;
    private int dragStartHeight = 0;
    private boolean draggingSheet = false;
    private int keyboardLift = 0;
    private boolean keyboardFullscreenMode = false;
    private int sheetTouchSlop = 0;
    private float globalSheetDownX = 0f;
    private float globalSheetDownY = 0f;
    private int globalSheetDownHeight = 0;
    private boolean globalSheetDragActive = false;

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(AppPrefs.PREFS_NAME, MODE_PRIVATE);
        configureWindow();
        setContentView(R.layout.activity_settings);
        sheetTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        settingsRoot = findViewById(R.id.settingsRoot);
        dimView = findViewById(R.id.settingsDimView);
        settingsSheet = findViewById(R.id.settingsSheet);
        homePanel = findViewById(R.id.settingsHomePanel);
        accountPanel = findViewById(R.id.settingsAccountPanel);
        avatarText = findViewById(R.id.settingsAvatarText);
        accountAvatarText = findViewById(R.id.settingsAccountAvatarText);
        avatarImage = findViewById(R.id.settingsAvatarImage);
        accountAvatarImage = findViewById(R.id.settingsAccountAvatarImage);
        accountAvatarFrame = findViewById(R.id.settingsAccountAvatarFrame);
        accountStatusText = findViewById(R.id.accountStatusText);
        accountSubtitleText = findViewById(R.id.accountSubtitleText);
        displayNameText = findViewById(R.id.settingsDisplayNameText);
        usernameInput = findViewById(R.id.settingsUsernameInput);
        passwordInput = findViewById(R.id.settingsPasswordInput);
        newPasswordInput = findViewById(R.id.settingsNewPasswordInput);
        nameEditorRow = findViewById(R.id.settingsNameEditorRow);
        passwordEditorRow = findViewById(R.id.settingsPasswordEditorRow);
        editPasswordButton = findViewById(R.id.settingsEditPasswordButton);
        changeUsernameButton = findViewById(R.id.settingsChangeUsernameButton);
        changePasswordButton = findViewById(R.id.settingsChangePasswordButton);
        logoutButton = findViewById(R.id.settingsLogoutButton);

        guestAuthPanel = findViewById(R.id.settingsGuestAuthPanel);
        loginEditorRow = findViewById(R.id.settingsLoginEditorRow);
        signupEditorRow = findViewById(R.id.settingsSignupEditorRow);
        guestLoginUsernameInput = findViewById(R.id.settingsGuestLoginUsernameInput);
        guestLoginPasswordInput = findViewById(R.id.settingsGuestLoginPasswordInput);
        guestSignupUsernameInput = findViewById(R.id.settingsGuestSignupUsernameInput);
        guestSignupPasswordInput = findViewById(R.id.settingsGuestSignupPasswordInput);
        guestLoginButton = findViewById(R.id.settingsGuestLoginButton);
        guestSignupButton = findViewById(R.id.settingsGuestSignupButton);

        View closeButton = findViewById(R.id.settingsCloseButton);
        View accountBackButton = findViewById(R.id.settingsAccountBackButton);
        profileTile = findViewById(R.id.settingsProfileTile);
        View photoRow = findViewById(R.id.settingsPhotoRow);
        View editNameButton = findViewById(R.id.settingsEditNameButton);
        View loginRow = findViewById(R.id.settingsLoginRow);
        View signupRow = findViewById(R.id.settingsSignupRow);
        emailButton = findViewById(R.id.settingsEmailButton);
        phoneButton = findViewById(R.id.settingsPhoneButton);
        notificationRow = findViewById(R.id.settingsNotificationRow);
        privacyRow = findViewById(R.id.settingsPrivacyRow);

        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        guestLoginPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        guestSignupPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        View.OnFocusChangeListener keyboardFocusListener = (focusedView, hasFocus) -> {
            if (hasFocus && keyboardLift > 0) {
                focusedView.postDelayed(() -> applyKeyboardForFocusedInput(true), 80);
            }
        };
        usernameInput.setOnFocusChangeListener(keyboardFocusListener);
        passwordInput.setOnFocusChangeListener(keyboardFocusListener);
        newPasswordInput.setOnFocusChangeListener(keyboardFocusListener);
        guestLoginUsernameInput.setOnFocusChangeListener(keyboardFocusListener);
        guestLoginPasswordInput.setOnFocusChangeListener(keyboardFocusListener);
        guestSignupUsernameInput.setOnFocusChangeListener(keyboardFocusListener);
        guestSignupPasswordInput.setOnFocusChangeListener(keyboardFocusListener);

        profilePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        saveSelectedProfilePhoto(result.getData());
                    }
                }
        );

        dimView.setOnClickListener(view -> dismissSheet());
        settingsSheet.setOnTouchListener(this::handleSheetTouch);
        closeButton.setOnClickListener(view -> dismissSheet());
        accountBackButton.setOnClickListener(view -> showHomePanel());
        profileTile.setOnClickListener(view -> showAccountPanel());
        photoRow.setOnClickListener(view -> launchProfilePhotoPicker());
        accountAvatarText.setOnClickListener(view -> launchProfilePhotoPicker());
        accountAvatarFrame.setOnClickListener(view -> launchProfilePhotoPicker());
        editNameButton.setOnClickListener(view -> toggleNameEditor());
        editPasswordButton.setOnClickListener(view -> togglePasswordEditor());
        changeUsernameButton.setOnClickListener(view -> changeUsername());
        changePasswordButton.setOnClickListener(view -> changePassword());
        logoutButton.setOnClickListener(view -> logout());

        loginRow.setOnClickListener(view -> toggleGuestLogin());
        signupRow.setOnClickListener(view -> toggleGuestSignup());
        guestLoginButton.setOnClickListener(view -> signInFromGuestPanel());
        guestSignupButton.setOnClickListener(view -> createAccountFromGuestPanel());

        emailButton.setOnClickListener(view -> Toast.makeText(this, R.string.toast_placeholder_email, Toast.LENGTH_LONG).show());
        phoneButton.setOnClickListener(view -> Toast.makeText(this, R.string.toast_placeholder_phone, Toast.LENGTH_LONG).show());
        notificationRow.setOnClickListener(view -> Toast.makeText(this, R.string.toast_placeholder_notifications, Toast.LENGTH_SHORT).show());
        privacyRow.setOnClickListener(view -> Toast.makeText(this, R.string.toast_placeholder_privacy, Toast.LENGTH_SHORT).show());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackRequest();
            }
        });

        updateAccountUi();
        configureKeyboardSheetLift();
        updateSheetSnapPoints(false);
        animateSheetIn();
    }

    private void handleBackRequest() {
        if (showingAccountPanel) {
            showHomePanel();
        } else {
            dismissSheet();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Route intentional vertical drags that start anywhere inside the sheet
        // to the sheet physics. This prevents child rows/cards on the general
        // Settings page from stealing the gesture and moving content in the
        // opposite direction. Small movements still pass through as normal taps.
        if (settingsSheet != null && !isClosing && isRawTouchInside(settingsSheet, event)) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    globalSheetDownX = event.getRawX();
                    globalSheetDownY = event.getRawY();
                    globalSheetDownHeight = settingsSheet.getHeight();
                    globalSheetDragActive = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = Math.abs(event.getRawX() - globalSheetDownX);
                    float dy = Math.abs(event.getRawY() - globalSheetDownY);
                    if (globalSheetDragActive || shouldStartSheetDrag(event, dx, dy)) {
                        if (!globalSheetDragActive) {
                            dragStartY = globalSheetDownY;
                            dragStartHeight = globalSheetDownHeight;
                            draggingSheet = true;
                            globalSheetDragActive = true;
                        }
                        applySheetDrag(event.getRawY());
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (globalSheetDragActive) {
                        settleSheetAfterDrag();
                        draggingSheet = false;
                        globalSheetDragActive = false;
                        return true;
                    }
                    globalSheetDragActive = false;
                    break;
                default:
                    break;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private boolean shouldStartSheetDrag(MotionEvent event, float dx, float dy) {
        if (dy <= sheetTouchSlop || dy <= dx) {
            return false;
        }

        if (showingAccountPanel || keyboardLift > 0) {
            return true;
        }

        // In the general Settings page, let the ScrollView handle scrolling only
        // after the content is already scrolled. At the top, upward/downward
        // drags should manipulate the bottom sheet itself.
        return settingsSheet.getScrollY() <= 0;
    }

    private boolean isRawTouchInside(View target, MotionEvent event) {
        int[] location = new int[2];
        target.getLocationOnScreen(location);
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        return rawX >= location[0]
                && rawX <= location[0] + target.getWidth()
                && rawY >= location[1]
                && rawY <= location[1] + target.getHeight();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.setDimAmount(0f);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );
    }

    private void configureKeyboardSheetLift() {
        ViewCompat.setOnApplyWindowInsetsListener(settingsRoot, (view, insets) -> {
            int keyboardBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            keyboardLift = keyboardBottom > 0 ? keyboardBottom : 0;

            if (keyboardBottom > 0) {
                settingsSheet.postDelayed(() -> applyKeyboardForFocusedInput(true), 70);
            } else {
                keyboardFullscreenMode = false;
                settingsSheet.setBackgroundResource(R.drawable.bg_settings_sheet);
                applyKeyboardSheetLift(true);
                updateSheetSnapPoints(true);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(settingsRoot);
    }

    private void applyKeyboardForFocusedInput(boolean animate) {
        if (settingsSheet == null || isClosing || keyboardLift <= 0) {
            return;
        }

        View focused = getCurrentFocus();
        boolean accountInputFocused = showingAccountPanel
                && (focused == usernameInput || focused == passwordInput || focused == newPasswordInput);
        boolean wouldTouchTopAfterLift = settingsSheet.getHeight() + keyboardLift >= settingsRoot.getHeight() - dp(4);
        boolean shouldFillVisibleScreen = accountInputFocused && (isLowAccountInput(focused) || wouldTouchTopAfterLift);
        keyboardFullscreenMode = shouldFillVisibleScreen;
        settingsSheet.setBackgroundResource(shouldFillVisibleScreen
                ? R.drawable.bg_settings_sheet_fullscreen
                : R.drawable.bg_settings_sheet);

        if (shouldFillVisibleScreen) {
            int targetHeight = Math.max(collapsedHeight, availableHeightAboveKeyboard());
            if (targetHeight > 0) {
                if (animate) {
                    animateSheetHeight(targetHeight);
                } else {
                    setSheetHeight(targetHeight);
                }
            }
        } else if (showingAccountPanel && accountInputFocused) {
            if (animate) {
                animateSheetHeight(collapsedHeight);
            } else {
                setSheetHeight(collapsedHeight);
            }
        }

        applyKeyboardSheetLift(animate);
    }

    private boolean isLowAccountInput(View focused) {
        return focused == passwordInput || focused == newPasswordInput;
    }

    private int availableHeightAboveKeyboard() {
        return Math.max(dp(280), getResources().getDisplayMetrics().heightPixels - keyboardLift);
    }

    private void applyKeyboardSheetLift(boolean animate) {
        if (settingsSheet == null || isClosing) {
            return;
        }
        float targetTranslation = -keyboardLift;
        if (animate) {
            settingsSheet.animate()
                    .translationY(targetTranslation)
                    .setDuration(180)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } else {
            settingsSheet.setTranslationY(targetTranslation);
        }
    }

    private void ensureViewVisible(View target) {
        if (target == null || settingsSheet == null || showingAccountPanel) {
            return;
        }
        settingsSheet.postDelayed(() -> {
            if (target.getWindowToken() == null) {
                return;
            }
            Rect targetRect = new Rect();
            target.getDrawingRect(targetRect);
            settingsSheet.offsetDescendantRectToMyCoords(target, targetRect);
            int topPadding = dp(24);
            int visibleBottom = settingsSheet.getHeight() - dp(28);
            if (targetRect.bottom > visibleBottom) {
                settingsSheet.smoothScrollBy(0, targetRect.bottom - visibleBottom);
            } else if (targetRect.top < topPadding) {
                settingsSheet.smoothScrollBy(0, targetRect.top - topPadding);
            }
        }, 230);
    }

    private void updateSheetSnapPoints(boolean animate) {
        settingsSheet.post(() -> {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int maxCollapsed = Math.round(metrics.heightPixels * 0.60f);
            int maxExpanded = metrics.heightPixels - dp(28);

            View child = settingsSheet.getChildAt(0);
            int widthSpec = View.MeasureSpec.makeMeasureSpec(settingsSheet.getWidth(), View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            child.measure(widthSpec, heightSpec);
            int contentHeight = child.getMeasuredHeight()
                    + settingsSheet.getPaddingTop()
                    + settingsSheet.getPaddingBottom();

            collapsedHeight = Math.min(contentHeight, maxCollapsed);
            expandedHeight = Math.min(contentHeight, maxExpanded);
            canExpand = contentHeight > maxCollapsed + dp(12);
            int target = canExpand && isExpanded ? expandedHeight : collapsedHeight;
            if (target <= 0) {
                target = maxCollapsed;
            }

            if (keyboardLift > 0 && showingAccountPanel && keyboardFullscreenMode) {
                target = Math.max(target, availableHeightAboveKeyboard());
            }

            if (animate) {
                animateSheetHeight(target);
            } else {
                setSheetHeight(target);
            }
            if (keyboardLift > 0) {
                applyKeyboardForFocusedInput(animate);
            }
        });
    }

    private void animateSheetIn() {
        dimView.setAlpha(0f);
        settingsSheet.post(() -> {
            settingsSheet.setTranslationY(settingsSheet.getHeight() + dp(40));
            settingsSheet.animate()
                    .translationY(-keyboardLift)
                    .setDuration(360)
                    .setInterpolator(new DecelerateInterpolator(1.35f))
                    .start();
            dimView.animate()
                    .alpha(1f)
                    .setDuration(220)
                    .start();
        });
    }

    private boolean handleSheetTouch(View view, MotionEvent event) {
        if (isClosing) {
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragStartY = event.getRawY();
                dragStartHeight = settingsSheet.getHeight();
                draggingSheet = false;
                return false;
            case MotionEvent.ACTION_MOVE:
                float dragDistance = Math.abs(event.getRawY() - dragStartY);
                if (!draggingSheet && dragDistance < sheetTouchSlop) {
                    return false;
                }
                if (!showingAccountPanel && settingsSheet.getScrollY() > 0 && event.getRawY() > dragStartY) {
                    return false;
                }
                draggingSheet = true;
                applySheetDrag(event.getRawY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!draggingSheet) {
                    return false;
                }
                settleSheetAfterDrag();
                draggingSheet = false;
                return true;
            default:
                return false;
        }
    }

    private void applySheetDrag(float rawY) {
        float dragUpDistance = dragStartY - rawY;
        int newHeight = dragStartHeight + Math.round(dragUpDistance);

        if (keyboardLift > 0) {
            int keyboardMaxHeight = Math.max(collapsedHeight, availableHeightAboveKeyboard());
            if (newHeight > keyboardMaxHeight) {
                newHeight = keyboardMaxHeight + Math.round((newHeight - keyboardMaxHeight) * 0.16f);
            }
            int minKeyboardHeight = Math.max(dp(220), collapsedHeight - dp(220));
            newHeight = Math.max(minKeyboardHeight, newHeight);
            setSheetHeight(newHeight);
            return;
        }

        int closePreviewHeight = Math.max(dp(220), collapsedHeight - dp(260));
        if (canExpand) {
            if (newHeight > expandedHeight) {
                newHeight = expandedHeight + Math.round((newHeight - expandedHeight) * 0.18f);
            }
            newHeight = Math.max(closePreviewHeight, newHeight);
        } else {
            if (newHeight > collapsedHeight) {
                newHeight = collapsedHeight + Math.round((newHeight - collapsedHeight) * 0.18f);
            }
            newHeight = Math.max(closePreviewHeight, newHeight);
        }
        setSheetHeight(newHeight);
    }

    private void settleSheetAfterDrag() {
        int currentHeight = settingsSheet.getHeight();

        if (keyboardLift > 0) {
            int shrinkDistance = dragStartHeight - currentHeight;
            if (shrinkDistance > dp(120)) {
                hideKeyboardAndRestoreSheet();
                return;
            }

            if (keyboardFullscreenMode) {
                int keyboardFullHeight = Math.max(collapsedHeight, availableHeightAboveKeyboard());
                animateSheetHeight(keyboardFullHeight);
            } else {
                animateSheetHeight(collapsedHeight);
            }
            applyKeyboardSheetLift(true);
            return;
        }

        int dragDownDistance = dragStartHeight - currentHeight;
        if (dragDownDistance > dp(175) || currentHeight < collapsedHeight - dp(165)) {
            dismissSheet();
            return;
        }

        if (canExpand) {
            int midpoint = collapsedHeight + ((expandedHeight - collapsedHeight) / 2);
            isExpanded = currentHeight >= midpoint;
            animateSheetHeight(isExpanded ? expandedHeight : collapsedHeight);
            applyKeyboardSheetLift(true);
        } else {
            isExpanded = false;
            animateSheetHeight(collapsedHeight);
            applyKeyboardSheetLift(true);
        }
    }

    private void hideKeyboardAndRestoreSheet() {
        View focused = getCurrentFocus();
        if (focused != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
            }
            focused.clearFocus();
        }
        keyboardLift = 0;
        keyboardFullscreenMode = false;
        settingsSheet.setBackgroundResource(R.drawable.bg_settings_sheet);
        settingsSheet.animate()
                .translationY(0f)
                .setDuration(190)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        isExpanded = false;
        updateSheetSnapPoints(true);
    }

    private void setSheetHeight(int height) {
        ViewGroup.LayoutParams params = settingsSheet.getLayoutParams();
        if (params.height != height) {
            params.height = height;
            settingsSheet.setLayoutParams(params);
        }
    }

    private void animateSheetHeight(int targetHeight) {
        int startHeight = settingsSheet.getHeight();
        if (startHeight == targetHeight) {
            return;
        }
        ValueAnimator animator = ValueAnimator.ofInt(startHeight, targetHeight);
        animator.setDuration(240);
        animator.setInterpolator(new DecelerateInterpolator(1.4f));
        animator.addUpdateListener(animation -> setSheetHeight((Integer) animation.getAnimatedValue()));
        animator.start();
    }

    private void dismissSheet() {
        if (isClosing) {
            return;
        }
        isClosing = true;
        settingsSheet.animate()
                .translationY(settingsSheet.getHeight() + dp(40))
                .setDuration(260)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    finish();
                    overridePendingTransition(0, 0);
                })
                .start();
        dimView.animate()
                .alpha(0f)
                .setDuration(180)
                .start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showAccountPanel() {
        if (showingAccountPanel || !preferences.getBoolean(AppPrefs.KEY_IS_LOGGED_IN, false)) {
            return;
        }
        showingAccountPanel = true;
        settingsSheet.setScrollingEnabled(false);
        settingsSheet.scrollTo(0, 0);
        updateAccountUi();
        accountPanel.setTranslationX(dp(36));
        accountPanel.setAlpha(0f);
        accountPanel.setVisibility(View.VISIBLE);
        homePanel.animate()
                .alpha(0f)
                .translationX(-dp(36))
                .setDuration(170)
                .withEndAction(() -> homePanel.setVisibility(View.GONE))
                .start();
        accountPanel.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> updateSheetSnapPoints(true))
                .start();
        settingsSheet.scrollTo(0, 0);
    }

    private void showHomePanel() {
        if (!showingAccountPanel) {
            updateSheetSnapPoints(true);
            return;
        }
        showingAccountPanel = false;
        settingsSheet.setScrollingEnabled(true);
        isExpanded = false;
        updateAccountUi();
        homePanel.setTranslationX(-dp(36));
        homePanel.setAlpha(0f);
        homePanel.setVisibility(View.VISIBLE);
        accountPanel.animate()
                .alpha(0f)
                .translationX(dp(36))
                .setDuration(170)
                .withEndAction(() -> accountPanel.setVisibility(View.GONE))
                .start();
        homePanel.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> updateSheetSnapPoints(true))
                .start();
        settingsSheet.scrollTo(0, 0);
    }

    private void toggleNameEditor() {
        boolean shouldShow = nameEditorRow.getVisibility() != View.VISIBLE;
        if (shouldShow) {
            usernameInput.setText(preferences.getString(AppPrefs.KEY_CURRENT_USER, ""));
        }
        animateSectionVisibility(nameEditorRow, shouldShow);
        if (shouldShow) {
            usernameInput.requestFocus();
            usernameInput.postDelayed(() -> applyKeyboardForFocusedInput(true), 120);
        }
    }

    private void togglePasswordEditor() {
        boolean shouldShow = passwordEditorRow.getVisibility() != View.VISIBLE;
        animateSectionVisibility(passwordEditorRow, shouldShow);
        if (shouldShow) {
            passwordInput.requestFocus();
            passwordInput.postDelayed(() -> applyKeyboardForFocusedInput(true), 120);
        }
    }

    private void toggleGuestLogin() {
        boolean shouldShow = loginEditorRow.getVisibility() != View.VISIBLE;
        if (shouldShow && signupEditorRow.getVisibility() == View.VISIBLE) {
            signupEditorRow.setVisibility(View.GONE);
        }
        animateSectionVisibility(loginEditorRow, shouldShow);
        if (shouldShow) {
            guestLoginUsernameInput.requestFocus();
            ensureViewVisible(guestLoginUsernameInput);
        }
    }

    private void toggleGuestSignup() {
        boolean shouldShow = signupEditorRow.getVisibility() != View.VISIBLE;
        if (shouldShow && loginEditorRow.getVisibility() == View.VISIBLE) {
            loginEditorRow.setVisibility(View.GONE);
        }
        animateSectionVisibility(signupEditorRow, shouldShow);
        if (shouldShow) {
            guestSignupUsernameInput.requestFocus();
            ensureViewVisible(guestSignupUsernameInput);
        }
    }

    private void animateSectionVisibility(View section, boolean shouldShow) {
        section.animate().cancel();
        if (shouldShow) {
            section.setVisibility(View.VISIBLE);
            section.setAlpha(0f);
            section.setTranslationY(-dp(12));
            section.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(220)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> updateSheetSnapPoints(true))
                    .start();
        } else {
            section.animate()
                    .alpha(0f)
                    .translationY(-dp(10))
                    .setDuration(170)
                    .withEndAction(() -> {
                        section.setVisibility(View.GONE);
                        section.setAlpha(1f);
                        section.setTranslationY(0f);
                        updateSheetSnapPoints(true);
                    })
                    .start();
        }
    }

    private void launchProfilePhotoPicker() {
        if (!preferences.getBoolean(AppPrefs.KEY_IS_LOGGED_IN, false)) {
            Toast.makeText(this, R.string.toast_login_before_photo, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        profilePhotoLauncher.launch(intent);
    }

    private void saveSelectedProfilePhoto(Intent data) {
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
            // Some providers do not support persistable permissions; setImageURI still works for the current session.
        }
        String currentUser = preferences.getString(AppPrefs.KEY_CURRENT_USER, "");
        if (currentUser.trim().isEmpty()) {
            Toast.makeText(this, R.string.toast_login_before_photo, Toast.LENGTH_SHORT).show();
            return;
        }
        preferences.edit().putString(AppPrefs.profilePhotoKey(currentUser), uri.toString()).apply();
        updateProfilePhotoViews(currentUser, true);
        Toast.makeText(this, R.string.toast_photo_updated, Toast.LENGTH_SHORT).show();
    }

    private void updateProfilePhotoViews(String currentUser, boolean isLoggedIn) {
        String uriString = isLoggedIn ? preferences.getString(AppPrefs.profilePhotoKey(currentUser), "") : "";
        boolean hasPhoto = uriString != null && !uriString.trim().isEmpty();
        if (hasPhoto) {
            Uri uri = Uri.parse(uriString);
            showProfilePhoto(avatarImage, avatarText, uri);
            showProfilePhoto(accountAvatarImage, accountAvatarText, uri);
        } else {
            showProfileInitials(avatarImage, avatarText);
            showProfileInitials(accountAvatarImage, accountAvatarText);
        }
    }

    private void showProfilePhoto(ImageView imageView, TextView textView, Uri uri) {
        if (imageView == null || textView == null) {
            return;
        }
        try {
            imageView.setImageURI(uri);
            imageView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);
        } catch (Exception e) {
            imageView.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
        }
    }

    private void showProfileInitials(ImageView imageView, TextView textView) {
        if (imageView != null) {
            imageView.setImageDrawable(null);
            imageView.setVisibility(View.GONE);
        }
        if (textView != null) {
            textView.setVisibility(View.VISIBLE);
        }
    }

    private void updateAccountUi() {
        boolean isLoggedIn = preferences.getBoolean(AppPrefs.KEY_IS_LOGGED_IN, false);
        String currentUser = preferences.getString(AppPrefs.KEY_CURRENT_USER, getString(R.string.guest));
        String initials = isLoggedIn ? initialsFor(currentUser) : getString(R.string.profile_initial_guest);

        if (avatarText != null) {
            avatarText.setText(initials);
        }
        accountAvatarText.setText(initials);
        displayNameText.setText(isLoggedIn ? currentUser : getString(R.string.guest));
        updateProfilePhotoViews(currentUser, isLoggedIn);

        guestAuthPanel.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        profileTile.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        emailButton.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        phoneButton.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        notificationRow.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        privacyRow.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        logoutButton.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);

        if (isLoggedIn) {
            accountStatusText.setText(currentUser);
            accountSubtitleText.setText(R.string.account_subtitle);
            usernameInput.setText(currentUser);
            usernameInput.setEnabled(true);
            passwordInput.setText("");
            passwordInput.setHint(R.string.hint_current_password);
            newPasswordInput.setText("");
            newPasswordInput.setVisibility(View.VISIBLE);
            nameEditorRow.setVisibility(View.GONE);
            passwordEditorRow.setVisibility(View.GONE);
            editPasswordButton.setVisibility(View.VISIBLE);
            changeUsernameButton.setVisibility(View.VISIBLE);
            changePasswordButton.setVisibility(View.VISIBLE);
        } else {
            showingAccountPanel = false;
            accountPanel.setVisibility(View.GONE);
            homePanel.setVisibility(View.VISIBLE);
            loginEditorRow.setVisibility(View.GONE);
            signupEditorRow.setVisibility(View.GONE);
            guestLoginUsernameInput.setText("");
            guestLoginPasswordInput.setText("");
            guestSignupUsernameInput.setText("");
            guestSignupPasswordInput.setText("");
        }
    }

    private String initialsFor(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getString(R.string.profile_initial_guest);
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
        }
        return name.trim().substring(0, Math.min(2, name.trim().length())).toUpperCase(Locale.US);
    }

    private void signInFromGuestPanel() {
        String username = guestLoginUsernameInput.getText().toString().trim();
        String password = guestLoginPasswordInput.getText().toString();
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.toast_enter_username_password, Toast.LENGTH_SHORT).show();
            return;
        }
        String registeredPassword = preferences.getString(AppPrefs.passwordKey(username), null);
        if (registeredPassword != null && registeredPassword.equals(password)) {
            preferences.edit()
                    .putBoolean(AppPrefs.KEY_IS_LOGGED_IN, true)
                    .putString(AppPrefs.KEY_CURRENT_USER, username)
                    .apply();
            Toast.makeText(this, R.string.toast_signed_in, Toast.LENGTH_SHORT).show();
            updateAccountUi();
            updateSheetSnapPoints(true);
        } else {
            Toast.makeText(this, R.string.toast_invalid_credentials, Toast.LENGTH_SHORT).show();
        }
    }

    private void createAccountFromGuestPanel() {
        String username = guestSignupUsernameInput.getText().toString().trim();
        String password = guestSignupPasswordInput.getText().toString();
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.toast_enter_username_password, Toast.LENGTH_SHORT).show();
            return;
        }
        if (preferences.contains(AppPrefs.passwordKey(username))) {
            Toast.makeText(this, R.string.toast_username_exists, Toast.LENGTH_SHORT).show();
            return;
        }
        preferences.edit()
                .putString(AppPrefs.passwordKey(username), password)
                .putBoolean(AppPrefs.KEY_IS_LOGGED_IN, true)
                .putString(AppPrefs.KEY_CURRENT_USER, username)
                .apply();
        Toast.makeText(this, R.string.toast_account_created, Toast.LENGTH_SHORT).show();
        updateAccountUi();
        updateSheetSnapPoints(true);
    }

    private void logout() {
        preferences.edit().putBoolean(AppPrefs.KEY_IS_LOGGED_IN, false).apply();
        Toast.makeText(this, R.string.toast_logged_out, Toast.LENGTH_SHORT).show();
        updateAccountUi();
        updateSheetSnapPoints(true);
    }

    private void changeUsername() {
        boolean isLoggedIn = preferences.getBoolean(AppPrefs.KEY_IS_LOGGED_IN, false);
        String oldUsername = preferences.getString(AppPrefs.KEY_CURRENT_USER, "");
        String newUsername = usernameInput.getText().toString().trim();
        if (!isLoggedIn || oldUsername.trim().isEmpty()) {
            Toast.makeText(this, R.string.toast_sign_in_before_username, Toast.LENGTH_SHORT).show();
            return;
        }
        if (newUsername.isEmpty()) {
            Toast.makeText(this, R.string.toast_username_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (newUsername.equals(oldUsername)) {
            Toast.makeText(this, R.string.toast_username_current, Toast.LENGTH_SHORT).show();
            return;
        }
        if (preferences.contains(AppPrefs.passwordKey(newUsername))) {
            Toast.makeText(this, R.string.toast_username_exists, Toast.LENGTH_SHORT).show();
            return;
        }

        String existingPassword = preferences.getString(AppPrefs.passwordKey(oldUsername), null);
        Set<String> existingCities = preferences.getStringSet(AppPrefs.citiesKey(oldUsername), new HashSet<>());
        String existingPhotoUri = preferences.getString(AppPrefs.profilePhotoKey(oldUsername), null);
        SharedPreferences.Editor editor = preferences.edit()
                .putString(AppPrefs.KEY_CURRENT_USER, newUsername)
                .remove(AppPrefs.passwordKey(oldUsername))
                .remove(AppPrefs.citiesKey(oldUsername))
                .remove(AppPrefs.profilePhotoKey(oldUsername));
        if (existingPassword != null) {
            editor.putString(AppPrefs.passwordKey(newUsername), existingPassword);
        }
        editor.putStringSet(AppPrefs.citiesKey(newUsername), new HashSet<>(existingCities));
        if (existingPhotoUri != null && !existingPhotoUri.trim().isEmpty()) {
            editor.putString(AppPrefs.profilePhotoKey(newUsername), existingPhotoUri);
        }
        editor.apply();

        Toast.makeText(this, R.string.toast_username_updated, Toast.LENGTH_SHORT).show();
        nameEditorRow.setVisibility(View.GONE);
        updateAccountUi();
        updateSheetSnapPoints(true);
    }

    private void changePassword() {
        String currentUser = preferences.getString(AppPrefs.KEY_CURRENT_USER, "");
        String currentPassword = passwordInput.getText().toString();
        String newPassword = newPasswordInput.getText().toString();
        String registeredPassword = preferences.getString(AppPrefs.passwordKey(currentUser), null);

        if (registeredPassword == null || currentPassword.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, R.string.toast_enter_passwords, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!registeredPassword.equals(currentPassword)) {
            Toast.makeText(this, R.string.toast_current_password_incorrect, Toast.LENGTH_SHORT).show();
            return;
        }
        preferences.edit().putString(AppPrefs.passwordKey(currentUser), newPassword).apply();
        passwordInput.setText("");
        newPasswordInput.setText("");
        passwordEditorRow.setVisibility(View.GONE);
        Toast.makeText(this, R.string.toast_password_changed, Toast.LENGTH_SHORT).show();
        updateSheetSnapPoints(true);
    }
}
