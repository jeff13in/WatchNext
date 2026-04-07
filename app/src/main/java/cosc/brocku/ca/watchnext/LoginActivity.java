package cosc.brocku.ca.watchnext;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "watchnext_prefs";
    private TextInputEditText etEmail, etPassword, etConfirmPassword;
    private TextInputLayout tilConfirmPassword;
    private MaterialButton btnAction;
    private boolean isSignIn = true;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        // Skip login if already logged in
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean("is_logged_in", false) && auth.getCurrentUser() != null) {
            loadUserAndStart(auth.getCurrentUser(), prefs);
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        btnAction = findViewById(R.id.btn_action);

        TabLayout tabs = findViewById(R.id.tab_auth);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isSignIn = tab.getPosition() == 0;
                tilConfirmPassword.setVisibility(isSignIn ? View.GONE : View.VISIBLE);
                btnAction.setText(isSignIn ? getString(R.string.sign_in) : getString(R.string.sign_up));
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnAction.setOnClickListener(v -> handleAction(prefs));
    }

    private void handleAction(SharedPreferences prefs) {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isSignIn) {
            String confirmPwd = etConfirmPassword.getText() != null
                    ? etConfirmPassword.getText().toString() : "";
            if (!password.equals(confirmPwd)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        btnAction.setEnabled(false);

        if (isSignIn) {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        FirebaseUser user = result.getUser();
                        if (user != null) loadUserAndStart(user, prefs);
                    })
                    .addOnFailureListener(e -> {
                        btnAction.setEnabled(true);
                        Toast.makeText(this, "Sign in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        FirebaseUser user = result.getUser();
                        if (user == null) return;
                        SupabaseClient.registerUser(user.getUid(), email, new SupabaseClient.Callback() {
                            @Override public void onSuccess() {
                                loadUserAndStart(user, prefs);
                            }
                            @Override public void onFailure(String error) {
                                runOnUiThread(() -> {
                                    btnAction.setEnabled(true);
                                    Toast.makeText(LoginActivity.this,
                                            "Account created but DB sync failed: " + error,
                                            Toast.LENGTH_LONG).show();
                                    // Still proceed — user is authenticated
                                    loadUserAndStart(user, prefs);
                                });
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        btnAction.setEnabled(true);
                        Toast.makeText(this, "Sign up failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void loadUserAndStart(FirebaseUser firebaseUser, SharedPreferences prefs) {
        SupabaseClient.getUser(firebaseUser.getUid(), new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(org.json.JSONObject data) {
                try {
                    int supabaseId = data.getInt("id");
                    String email = data.getString("email");
                    UserSession.get().set(supabaseId, firebaseUser.getUid(), email);
                    runOnUiThread(() -> {
                        prefs.edit()
                                .putBoolean("is_logged_in", true)
                                .putString("email", email)
                                .apply();
                        startMain();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> startMain());
                }
            }
            @Override
            public void onFailure(String error) {
                // User not in Supabase yet — try registering them, then re-fetch
                String emailStr = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
                SupabaseClient.registerUser(firebaseUser.getUid(), emailStr,
                        new SupabaseClient.Callback() {
                            @Override
                            public void onSuccess() {
                                // Registered — now fetch their Supabase record
                                SupabaseClient.getUser(firebaseUser.getUid(),
                                        new SupabaseClient.DataCallback() {
                                            @Override
                                            public void onSuccess(org.json.JSONObject data) {
                                                try {
                                                    int id = data.getInt("id");
                                                    String em = data.getString("email");
                                                    UserSession.get().set(id, firebaseUser.getUid(), em);
                                                } catch (Exception ignored) {}
                                                runOnUiThread(() -> {
                                                    prefs.edit()
                                                            .putBoolean("is_logged_in", true)
                                                            .putString("email", emailStr).apply();
                                                    startMain();
                                                });
                                            }
                                            @Override
                                            public void onFailure(String e2) {
                                                runOnUiThread(() -> {
                                                    prefs.edit()
                                                            .putBoolean("is_logged_in", true)
                                                            .putString("email", emailStr).apply();
                                                    startMain();
                                                });
                                            }
                                        });
                            }
                            @Override
                            public void onFailure(String regError) {
                                // Already registered or unreachable — proceed anyway
                                runOnUiThread(() -> {
                                    prefs.edit()
                                            .putBoolean("is_logged_in", true)
                                            .putString("email", emailStr).apply();
                                    startMain();
                                });
                            }
                        });
            }
        });
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
