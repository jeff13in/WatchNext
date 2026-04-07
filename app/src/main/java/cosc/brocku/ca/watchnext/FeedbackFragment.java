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

    private static final String PREFS_NAME = "watchnext_prefs";

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

        LinearLayout likedContainer = view.findViewById(R.id.layout_liked);
        LinearLayout dislikedContainer = view.findViewById(R.id.layout_disliked);
        TextView emptyLiked = view.findViewById(R.id.tv_empty_liked);
        TextView emptyDisliked = view.findViewById(R.id.tv_empty_disliked);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        loadFeedback(prefs, likedContainer, dislikedContainer, emptyLiked, emptyDisliked);
    }

    private void loadFeedback(SharedPreferences prefs,
                              LinearLayout likedContainer,
                              LinearLayout dislikedContainer,
                              TextView emptyLiked,
                              TextView emptyDisliked) {

        String uid = UserSession.get().getFirebaseUid();
        boolean hasUid = uid != null && !uid.isEmpty();

        String feedbackPrefix = hasUid ? "feedback_" + uid + "_" : "feedback_";
        String titlePrefix    = hasUid ? "feedback_title_" + uid + "_" : "feedback_title_";

        Map<String, ?> all = prefs.getAll();

        likedContainer.removeAllViews();
        dislikedContainer.removeAllViews();

        int likedCount = 0;
        int dislikedCount = 0;

        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();

            if (!key.startsWith(feedbackPrefix) || key.startsWith(titlePrefix)) continue;

            String movieId = key.substring(feedbackPrefix.length());
            if (movieId.startsWith("title_")) continue;

            String value = String.valueOf(entry.getValue());
            String title = prefs.getString(titlePrefix + movieId, null);
            if (title == null || title.isEmpty()) continue;

            final String finalMovieId = movieId;

            TextView tv = new TextView(requireContext());
            tv.setText(title);
            tv.setTextSize(16f);
            tv.setPadding(0, 12, 0, 12);
            tv.setClickable(true);
            tv.setFocusable(true);

            int[] attrs = new int[]{android.R.attr.selectableItemBackground};
            android.content.res.TypedArray ta = requireContext().obtainStyledAttributes(attrs);
            tv.setBackground(ta.getDrawable(0));
            ta.recycle();

            tv.setOnClickListener(v -> {
                if (!isAdded()) return;
                try {
                    int id = Integer.parseInt(finalMovieId);
                    Intent intent = new Intent(requireContext(), MovieDetailActivity.class);
                    intent.putExtra("movie_id", id);
                    intent.putExtra("media_type", "movie");
                    startActivity(intent);
                } catch (NumberFormatException ignored) {}
            });

            if ("liked".equals(value)) {
                likedContainer.addView(tv);
                likedCount++;
            } else if ("disliked".equals(value)) {
                dislikedContainer.addView(tv);
                dislikedCount++;
            }
        }

        emptyLiked.setVisibility(likedCount == 0 ? View.VISIBLE : View.GONE);
        emptyDisliked.setVisibility(dislikedCount == 0 ? View.VISIBLE : View.GONE);
    }
}
