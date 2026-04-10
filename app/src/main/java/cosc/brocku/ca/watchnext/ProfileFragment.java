package cosc.brocku.ca.watchnext;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

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

        TextView tvEmail = view.findViewById(R.id.tv_profile_email);

        UserSession session = UserSession.get();
        if (session.isLoaded()) {
            tvEmail.setText(session.getEmail());
        } else {
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            tvEmail.setText(prefs.getString("email", "user@example.com"));
        }

        Button btnSignOut = view.findViewById(R.id.btn_sign_out);
        btnSignOut.setOnClickListener(v -> signOut());
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
