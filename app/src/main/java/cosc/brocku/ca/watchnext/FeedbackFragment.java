package cosc.brocku.ca.watchnext;

import android.content.Context;
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

        Map<String, ?> all = prefs.getAll();

        likedContainer.removeAllViews();
        dislikedContainer.removeAllViews();

        int likedCount = 0;
        int dislikedCount = 0;

        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();

            if (key.startsWith("feedback_") && !key.startsWith("feedback_title_")) {
                String movieId = key.replace("feedback_", "");
                String value = String.valueOf(entry.getValue());
                String title = prefs.getString("feedback_title_" + movieId, null);
                if (title == null || title.isEmpty()) continue;  // ADD THIS

                TextView tv = new TextView(requireContext());
                tv.setText(title);
                tv.setTextSize(16f);
                tv.setPadding(0, 12, 0, 12);

                if ("liked".equals(value)) {
                    likedContainer.addView(tv);
                    likedCount++;
                } else if ("disliked".equals(value)) {
                    dislikedContainer.addView(tv);
                    dislikedCount++;
                }
            }
        }

        emptyLiked.setVisibility(likedCount == 0 ? View.VISIBLE : View.GONE);
        emptyDisliked.setVisibility(dislikedCount == 0 ? View.VISIBLE : View.GONE);

        }
    }
