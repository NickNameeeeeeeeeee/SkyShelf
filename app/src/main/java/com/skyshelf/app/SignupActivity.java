package com.skyshelf.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

public class SignupActivity extends Activity {

    private EditText editTextUsername, editTextPassword;
    private SwitchCompat switchTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        editTextUsername = findViewById(R.id.username_input);
        editTextPassword = findViewById(R.id.password_input);
        switchTheme = findViewById(R.id.switch_theme);
        Button buttonSignup = findViewById(R.id.signup_btn);
        Button buttonBack = findViewById(R.id.back_btn);

        buttonSignup.setOnClickListener(v -> registerUser());
        buttonBack.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.toast_enter_username_password_legacy, Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences preferences = getSharedPreferences(AppPrefs.PREFS_NAME, MODE_PRIVATE);
        if (preferences.contains(AppPrefs.passwordKey(username))) {
            Toast.makeText(this, R.string.toast_username_exists_short, Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(AppPrefs.passwordKey(username), password);
        editor.putString(AppPrefs.themeKey(username), Boolean.toString(switchTheme.isChecked()));
        editor.putBoolean(AppPrefs.KEY_IS_LOGGED_IN, true);
        editor.putString(AppPrefs.KEY_CURRENT_USER, username);
        editor.apply();

        Toast.makeText(this, R.string.toast_signup_successful, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(SignupActivity.this, MainActivity.class));
        finish();
    }
}
