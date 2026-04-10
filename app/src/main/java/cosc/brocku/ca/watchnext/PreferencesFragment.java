package cosc.brocku.ca.watchnext;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.LinkedHashMap;
import java.util.Map;

public class PreferencesFragment extends Fragment {

    private static final String[] GENRES = {
            "Action", "Drama", "Sci-Fi", "Fantasy", "Crime", "Comedy", "Horror", "Romance"
    };

    private SharedPreferences prefs;
    private final Map<String, TextInputEditText> inputFields = new LinkedHashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_preferences, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);

        View scrollCurrent = view.findViewById(R.id.scroll_current);
        View scrollSetNew = view.findViewById(R.id.scroll_set_new);
        LinearLayout layoutCurrent = view.findViewById(R.id.layout_current_prefs);
        LinearLayout layoutSetNew = view.findViewById(R.id.layout_set_new_prefs);

        buildCurrentView(layoutCurrent);
        buildSetNewView(layoutSetNew);

        TabLayout tabs = view.findViewById(R.id.tab_preferences);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    scrollCurrent.setVisibility(View.VISIBLE);
                    scrollSetNew.setVisibility(View.GONE);
                    // Refresh current view
                    layoutCurrent.removeAllViews();
                    buildCurrentView(layoutCurrent);
                } else {
                    scrollCurrent.setVisibility(View.GONE);
                    scrollSetNew.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void buildCurrentView(LinearLayout layout) {
        layout.removeAllViews();
        for (String genre : GENRES) {
            int pct = prefs.getInt("genre_" + genre.toLowerCase().replace("-", ""), 50);
            TextView tv = new TextView(requireContext());
            tv.setText(genre + ": " + pct + "%");
            tv.setTextSize(16f);
            tv.setPadding(0, 16, 0, 16);
            layout.addView(tv);
        }
    }

    private void buildSetNewView(LinearLayout layout) {
        layout.removeAllViews();
        inputFields.clear();

        for (String genre : GENRES) {
            int pct = prefs.getInt("genre_" + genre.toLowerCase().replace("-", ""), 50);

            TextInputLayout til = new TextInputLayout(requireContext(),
                    null, com.google.android.material.R.attr.textInputOutlinedStyle);
            til.setHint(genre + " (0-100)");
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = 24;
            til.setLayoutParams(params);

            TextInputEditText et = new TextInputEditText(til.getContext());
            et.setText(String.valueOf(pct));
            et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            til.addView(et);
            layout.addView(til);
            inputFields.put(genre, et);
        }

        Button saveBtn = new Button(requireContext());
        saveBtn.setText(getString(R.string.save));
        saveBtn.setOnClickListener(v -> savePreferences());
        layout.addView(saveBtn);
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, TextInputEditText> entry : inputFields.entrySet()) {
            String text = entry.getValue().getText() != null
                    ? entry.getValue().getText().toString().trim() : "50";
            int value = 50;
            try {
                value = Math.min(100, Math.max(0, Integer.parseInt(text)));
            } catch (NumberFormatException ignored) {}
            editor.putInt("genre_" + entry.getKey().toLowerCase().replace("-", ""), value);
        }
        editor.apply();
        Toast.makeText(requireContext(), "Preferences saved!", Toast.LENGTH_SHORT).show();
    }
}
