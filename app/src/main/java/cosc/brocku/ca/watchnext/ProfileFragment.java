package cosc.brocku.ca.watchnext;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

    private TextView tvUsername;
    private TextView tvEmail;
    private TextView tvTaste;
    private MaterialSwitch switchTheme;
    private MaterialButton btnSignOut;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvUsername = view.findViewById(R.id.tv_profile_username);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        tvTaste = view.findViewById(R.id.tv_profile_taste);
        switchTheme = view.findViewById(R.id.switch_theme);
        btnSignOut = view.findViewById(R.id.btn_sign_out);

        loadAccountInfo();
        loadTasteSummary();
        setupThemeToggle();

        btnSignOut.setOnClickListener(v -> signOut());
    }

    private void loadAccountInfo() {
        UserSession session = UserSession.get();
        String email;

        if (session.isLoaded() && session.getEmail() != null && !session.getEmail().isEmpty()) {
            email = session.getEmail();
        } else {
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            email = prefs.getString("email", "user@example.com");
        }

        tvEmail.setText(email);
        tvUsername.setText(email.contains("@") ? email.split("@")[0] : email);
    }

    private void loadTasteSummary() {
        // Placeholder until you add persistent profile storage
        tvTaste.setText("Built from your likes and watch activity");
    }

    private void setupThemeToggle() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);

        boolean darkMode = prefs.getBoolean("dark_mode", true);
        switchTheme.setChecked(darkMode);

        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();

            AppCompatDelegate.setDefaultNightMode(
                    isChecked
                            ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO
            );
        });
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        UserSession.get().clear();

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean("is_logged_in", false).apply();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}