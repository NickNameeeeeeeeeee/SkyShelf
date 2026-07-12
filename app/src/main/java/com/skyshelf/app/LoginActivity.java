package com.skyshelf.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {

    private EditText editTextUsername, editTextPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextUsername = findViewById(R.id.username_input);
        editTextPassword = findViewById(R.id.password_input);
        Button buttonLogin = findViewById(R.id.signin_btn);
        Button buttonSignup = findViewById(R.id.signup_btn);

        buttonLogin.setOnClickListener(v -> loginUser());
        buttonSignup.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignupActivity.class)));
    }

    private void loginUser() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.toast_enter_username_password_legacy, Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences preferences = getSharedPreferences(AppPrefs.PREFS_NAME, MODE_PRIVATE);
        String registeredPassword = preferences.getString(AppPrefs.passwordKey(username), null);

        if (registeredPassword != null && password.equals(registeredPassword)) {
            Toast.makeText(this, R.string.toast_login_successful, Toast.LENGTH_SHORT).show();

            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(AppPrefs.KEY_IS_LOGGED_IN, true);
            editor.putString(AppPrefs.KEY_CURRENT_USER, username);
            editor.apply();

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, R.string.toast_invalid_credentials_short, Toast.LENGTH_SHORT).show();
        }
    }
}
