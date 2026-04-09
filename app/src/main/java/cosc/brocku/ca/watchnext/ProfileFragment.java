package cosc.brocku.ca.watchnext;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import org.json.JSONArray;
import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView tvUsername, tvEmail, tvFollowers, tvFollowing;
    private TextView tvStatWatchlist, tvStatFinished, tvStatLiked;

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

        tvUsername      = view.findViewById(R.id.tv_profile_username);
        tvEmail         = view.findViewById(R.id.tv_profile_email);
        tvFollowers     = view.findViewById(R.id.tv_followers_count);
        tvFollowing     = view.findViewById(R.id.tv_following_count);
        tvStatWatchlist = view.findViewById(R.id.tv_stat_watchlist);
        tvStatFinished  = view.findViewById(R.id.tv_stat_finished);
        tvStatLiked     = view.findViewById(R.id.tv_stat_liked);

        // Populate email / username
        UserSession session = UserSession.get();
        String email;
        if (session.isLoaded()) {
            email = session.getEmail();
        } else {
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            email = prefs.getString("email", "user@example.com");
        }
        tvEmail.setText(email);
        tvUsername.setText(email.contains("@") ? email.split("@")[0] : email);

        // Populate watchlist stats from cached entries
        populateStats();

        // Fetch followers / following from Supabase
        if (session.isLoaded()) {
            int userId = session.getSupabaseUserId();
            SupabaseClient.getFollowers(userId, new SupabaseClient.ListCallback() {
                @Override public void onSuccess(JSONArray data) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (isAdded()) tvFollowers.setText(String.valueOf(data.length()));
                    });
                }
                @Override public void onFailure(String error) {}
            });
            SupabaseClient.getFollowing(userId, new SupabaseClient.ListCallback() {
                @Override public void onSuccess(JSONArray data) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (isAdded()) tvFollowing.setText(String.valueOf(data.length()));
                    });
                }
                @Override public void onFailure(String error) {}
            });
        }

        Button btnSignOut = view.findViewById(R.id.btn_sign_out);
        btnSignOut.setOnClickListener(v -> signOut());
    }

    private void populateStats() {
        List<ListEntry> entries = ListRepository.getAllEntries();
        int watchlist = 0, finished = 0, liked = 0;
        for (ListEntry e : entries) {
            switch (e.getStatus()) {
                case "Watching":  watchlist++; break;
                case "Finished":  finished++;  break;
                case "Liked":     liked++;     break;
            }
        }
        tvStatWatchlist.setText(String.valueOf(watchlist));
        tvStatFinished.setText(String.valueOf(finished));
        tvStatLiked.setText(String.valueOf(liked));
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
