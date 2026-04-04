package cosc.brocku.ca.watchnext;

import android.content.Intent;
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

    private TextInputEditText etEmail, etPassword, etConfirmPassword;
    private TextInputLayout tilConfirmPassword;
    private MaterialButton btnAction;
    private boolean isSignIn = true;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        // Skip login if already signed in — but still load session first
        if (auth.getCurrentUser() != null) {
            loadSessionThenStart(auth.getCurrentUser());
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail            = findViewById(R.id.et_email);
        etPassword         = findViewById(R.id.et_password);
        etConfirmPassword  = findViewById(R.id.et_confirm_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        btnAction          = findViewById(R.id.btn_action);

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

        btnAction.setOnClickListener(v -> handleAction());
    }

    private void handleAction() {
        String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
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
            signUp(email, password);
        } else {
            signIn(email, password);
        }
    }

    private void signIn(String email, String password) {
        btnAction.setEnabled(false);
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> {
                FirebaseUser user = result.getUser();
                if (user != null) loadSessionThenStart(user);
            })
            .addOnFailureListener(e -> {
                btnAction.setEnabled(true);
                Toast.makeText(this, "Sign in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    private void signUp(String email, String password) {
        btnAction.setEnabled(false);
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> {
                FirebaseUser user = result.getUser();
                if (user != null) {
                    SupabaseClient.registerUser(user.getUid(), email, new SupabaseClient.Callback() {
                        @Override public void onSuccess() { loadSessionThenStart(user); }
                        @Override public void onFailure(String error) { loadSessionThenStart(user); }
                    });
                }
            })
            .addOnFailureListener(e -> {
                btnAction.setEnabled(true);
                Toast.makeText(this, "Sign up failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    // Fetch Supabase user ID and store in UserSession BEFORE going to MainActivity
    private void loadSessionThenStart(FirebaseUser firebaseUser) {
        SupabaseClient.getUser(firebaseUser.getUid(), new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(org.json.JSONObject data) {
                try {
                    UserSession.get().set(
                        data.getInt("id"),
                        firebaseUser.getUid(),
                        firebaseUser.getEmail()
                    );
                } catch (Exception ignored) {}
                startMain();
            }
            @Override
            public void onFailure(String error) {
                android.util.Log.e("LoginActivity", "getUser failed: " + error);
                // Profile not in Supabase yet — register then retry
                SupabaseClient.registerUser(firebaseUser.getUid(), firebaseUser.getEmail(),
                    new SupabaseClient.Callback() {
                        @Override public void onSuccess() { loadSessionThenStart(firebaseUser); }
                        @Override public void onFailure(String e) {
                            android.util.Log.e("LoginActivity", "registerUser failed: " + e);
                            startMain();
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
