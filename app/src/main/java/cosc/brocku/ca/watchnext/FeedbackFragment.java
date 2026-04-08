package cosc.brocku.ca.watchnext;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.Map;

public class FeedbackFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feedback, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isAdded()) return;
        View view = getView();
        if (view == null) return;

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);

        LinearLayout layoutMovieLiked    = view.findViewById(R.id.layout_movie_liked);
        LinearLayout layoutMovieDisliked = view.findViewById(R.id.layout_movie_disliked);
        LinearLayout layoutTvLiked       = view.findViewById(R.id.layout_tv_liked);
        LinearLayout layoutTvDisliked    = view.findViewById(R.id.layout_tv_disliked);

        TextView emptyMovieLiked    = view.findViewById(R.id.tv_empty_movie_liked);
        TextView emptyMovieDisliked = view.findViewById(R.id.tv_empty_movie_disliked);
        TextView emptyTvLiked       = view.findViewById(R.id.tv_empty_tv_liked);
        TextView emptyTvDisliked    = view.findViewById(R.id.tv_empty_tv_disliked);

        TextView chipMovieLiked    = view.findViewById(R.id.chip_movie_liked);
        TextView chipMovieDisliked = view.findViewById(R.id.chip_movie_disliked);
        TextView chipTvLiked       = view.findViewById(R.id.chip_tv_liked);
        TextView chipTvDisliked    = view.findViewById(R.id.chip_tv_disliked);

        layoutMovieLiked.removeAllViews();
        layoutMovieDisliked.removeAllViews();
        layoutTvLiked.removeAllViews();
        layoutTvDisliked.removeAllViews();

        String uid = UserSession.get().getFirebaseUid();
        boolean hasUid = uid != null && !uid.isEmpty();
        String feedbackPrefix = hasUid ? "feedback_" + uid + "_" : "feedback_";
        String titlePrefix    = hasUid ? "feedback_title_" + uid + "_" : "feedback_title_";
        String typePrefix     = hasUid ? "feedback_type_" + uid + "_" : "feedback_type_";

        int[] counts = {0, 0, 0, 0}; // [movieLiked, movieDisliked, tvLiked, tvDisliked]

        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(feedbackPrefix)) continue;
            if (key.startsWith(titlePrefix) || key.startsWith(typePrefix)) continue;

            String movieId = key.substring(feedbackPrefix.length());
            if (movieId.startsWith("title_") || movieId.startsWith("type_")) continue;

            String value  = String.valueOf(entry.getValue());
            String title  = prefs.getString(titlePrefix + movieId, null);
            String type   = prefs.getString(typePrefix + movieId, "movie"); // default to movie for old entries

            if (title == null || title.isEmpty()) continue;
            if (!"liked".equals(value) && !"disliked".equals(value)) continue;

            boolean isTv      = "tv".equals(type);
            boolean isLiked   = "liked".equals(value);
            String  mediaType = isTv ? "tv" : "movie";

            LinearLayout container = isTv
                    ? (isLiked ? layoutTvLiked    : layoutTvDisliked)
                    : (isLiked ? layoutMovieLiked  : layoutMovieDisliked);

            if (isTv && isLiked)        counts[2]++;
            else if (isTv)              counts[3]++;
            else if (isLiked)           counts[0]++;
            else                        counts[1]++;

            container.addView(buildItemRow(movieId, title, mediaType));
        }

        // Update chips
        chipMovieLiked.setText(String.valueOf(counts[0]));
        chipMovieDisliked.setText(String.valueOf(counts[1]));
        chipTvLiked.setText(String.valueOf(counts[2]));
        chipTvDisliked.setText(String.valueOf(counts[3]));

        // Show/hide empty states
        emptyMovieLiked.setVisibility(counts[0] == 0 ? View.VISIBLE : View.GONE);
        emptyMovieDisliked.setVisibility(counts[1] == 0 ? View.VISIBLE : View.GONE);
        emptyTvLiked.setVisibility(counts[2] == 0 ? View.VISIBLE : View.GONE);
        emptyTvDisliked.setVisibility(counts[3] == 0 ? View.VISIBLE : View.GONE);
    }

    private View buildItemRow(String movieId, String title, String mediaType) {
        // Outer wrapper for the ripple + padding
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dpToPx(10), 0, dpToPx(10));
        row.setClickable(true);
        row.setFocusable(true);

        int[] attrs = new int[]{android.R.attr.selectableItemBackground};
        android.content.res.TypedArray ta = requireContext().obtainStyledAttributes(attrs);
        row.setBackground(ta.getDrawable(0));
        ta.recycle();

        // Bullet indicator
        TextView bullet = new TextView(requireContext());
        bullet.setText("•");
        bullet.setTextSize(18f);
        bullet.setTextColor(requireContext().getColor(android.R.color.darker_gray));
        bullet.setPadding(0, 0, dpToPx(10), 0);

        // Title
        TextView tv = new TextView(requireContext());
        tv.setText(title);
        tv.setTextSize(14f);
        tv.setTextColor(requireContext().obtainStyledAttributes(
                new int[]{android.R.attr.textColorPrimary})
                .getColor(0, android.graphics.Color.BLACK));
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(bullet);
        row.addView(tv);

        row.setOnClickListener(v -> {
            if (!isAdded()) return;
            try {
                int id = Integer.parseInt(movieId);
                Intent intent = new Intent(requireContext(), MovieDetailActivity.class);
                intent.putExtra("movie_id", id);
                intent.putExtra("media_type", mediaType);
                startActivity(intent);
            } catch (NumberFormatException ignored) {}
        });

        return row;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
