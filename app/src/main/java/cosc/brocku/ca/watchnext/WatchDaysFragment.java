package cosc.brocku.ca.watchnext;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class WatchDaysFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_watch_days, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rv_watch_days);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new WatchDayAdapter(new ArrayList<>()));

        TextView emptyView = view.findViewById(R.id.tv_watch_history_empty);

        loadWatchHistory(rv, emptyView);
    }

    private int sessionRetries = 0;

    private void loadWatchHistory(RecyclerView rv, TextView emptyView) {
        UserSession session = UserSession.get();
        if (!session.isLoaded()) {
            if (sessionRetries < 5) {
                sessionRetries++;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isAdded()) loadWatchHistory(rv, emptyView);
                }, 2000);
            } else if (isAdded()) {
                Toast.makeText(requireContext(),
                        "Could not load session. Please sign out and back in.",
                        Toast.LENGTH_LONG).show();
            }
            return;
        }
        sessionRetries = 0;

        SupabaseClient.getWatchHistory(session.getSupabaseUserId(), new SupabaseClient.ListCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                // Group titles by date, skipping duplicate movie_ids
                Map<String, List<String>> byDate = new LinkedHashMap<>();
                Set<String> seenMovieIds = new HashSet<>();
                SimpleDateFormat inputFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat displayFmt = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());

                for (int i = 0; i < data.length(); i++) {
                    try {
                        JSONObject item = data.getJSONObject(i);
                        String movieId = item.optString("movie_id", "");
                        if (!movieId.isEmpty() && !seenMovieIds.add(movieId)) continue; // skip duplicate
                        String title = item.optString("title", "Unknown");
                        String watchedAt = item.optString("watched_at", "");

                        String dateLabel;
                        try {
                            Date date = inputFmt.parse(watchedAt.replace("Z", "").split("\\.")[0]);
                            dateLabel = date != null ? displayFmt.format(date) : watchedAt;
                        } catch (Exception e) {
                            dateLabel = watchedAt.isEmpty() ? "Unknown Date" : watchedAt.substring(0, 10);
                        }

                        if (!byDate.containsKey(dateLabel)) {
                            byDate.put(dateLabel, new ArrayList<>());
                        }
                        byDate.get(dateLabel).add(title);
                    } catch (Exception ignored) {}
                }

                List<WatchDay> watchDays = new ArrayList<>();
                for (Map.Entry<String, List<String>> entry : byDate.entrySet()) {
                    watchDays.add(new WatchDay(entry.getKey(), entry.getValue(), ""));
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        rv.setAdapter(new WatchDayAdapter(watchDays));
                        if (emptyView != null) {
                            emptyView.setVisibility(watchDays.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Failed to load watch history", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
}
