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
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private static final String PREFS_SEARCH = "search_prefs";
    private static final String KEY_RECENT = "recent_searches";

    private SearchView searchView;
    private TextView tvSearchHint;
    private ProgressBar progressBar;
    private RecyclerView rvResults;
    private ChipGroup chipGroupRecent;
    private View layoutRecent;

    private MovieAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchView = view.findViewById(R.id.search_view);
        tvSearchHint = view.findViewById(R.id.tv_search_hint);
        progressBar = view.findViewById(R.id.progress_search);
        rvResults = view.findViewById(R.id.rv_search_results);
        chipGroupRecent = view.findViewById(R.id.chipgroup_recent_searches);
        layoutRecent = view.findViewById(R.id.layout_recent_searches);

        rvResults.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new MovieAdapter(new ArrayList<>(), movie -> {
            Intent intent = new Intent(requireContext(), MovieDetailActivity.class);
            intent.putExtra("movie_id", movie.getId());
            intent.putExtra("media_type", movie.getMediaType() == null ? "movie" : movie.getMediaType());
            startActivity(intent);
        });
        rvResults.setAdapter(adapter);

        loadRecentSearches();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String q = query == null ? "" : query.trim();

                performSearch(q);

                if (q.length() >= 3) {
                    saveRecentSearch(q);
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                String query = newText == null ? "" : newText.trim();

                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }

                if (query.isEmpty()) {
                    showEmptyState();
                    return true;
                }

                searchRunnable = () -> performSearch(query);
                handler.postDelayed(searchRunnable, 350);
                return true;
            }
        });

        showEmptyState();
    }

    private void performSearch(String query) {
        if (!isAdded()) return;

        if (query.isEmpty()) {
            showEmptyState();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvSearchHint.setVisibility(View.GONE);
        layoutRecent.setVisibility(View.GONE);
        rvResults.setVisibility(View.GONE);

        TmdbClient.getService().searchMulti(query).enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;

                progressBar.setVisibility(View.GONE);

                List<TmdbMovie> results = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                    for (TmdbMovie item : response.body().getResults()) {
                        if (item == null) continue;

                        String type = item.getMediaType();
                        if (!"movie".equalsIgnoreCase(type) && !"tv".equalsIgnoreCase(type)) {
                            continue;
                        }
                        results.add(item);
                    }
                }

                adapter.updateMovies(results);
                rvResults.setVisibility(View.VISIBLE);

                if (results.isEmpty()) {
                    tvSearchHint.setText("No results found");
                    tvSearchHint.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;

                progressBar.setVisibility(View.GONE);
                rvResults.setVisibility(View.GONE);
                tvSearchHint.setText("Search failed. Please try again.");
                tvSearchHint.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showEmptyState() {
        adapter.updateMovies(new ArrayList<>());
        rvResults.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        tvSearchHint.setText("Start typing to search for movies or TV shows");
        tvSearchHint.setVisibility(View.VISIBLE);
        layoutRecent.setVisibility(View.VISIBLE);
        loadRecentSearches();
    }

    private void loadRecentSearches() {
        if (!isAdded()) return;

        chipGroupRecent.removeAllViews();

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_RECENT, "");
        if (saved == null || saved.trim().isEmpty()) {
            layoutRecent.setVisibility(View.GONE);
            return;
        }

        layoutRecent.setVisibility(View.VISIBLE);

        String[] parts = saved.split("\\|\\|");
        for (String q : parts) {
            String query = q.trim();
            if (query.isEmpty()) continue;

            Chip chip = new Chip(requireContext());
            chip.setText(query);
            chip.setClickable(true);
            chip.setCheckable(false);
            chip.setOnClickListener(v -> searchView.setQuery(query, true));
            chipGroupRecent.addView(chip);
        }
    }

    private void saveRecentSearch(String query) {
        if (!isAdded() || query == null || query.trim().isEmpty()) return;

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_RECENT, "");

        Set<String> ordered = new LinkedHashSet<>();
        ordered.add(query.trim());

        if (saved != null && !saved.trim().isEmpty()) {
            String[] parts = saved.split("\\|\\|");
            for (String part : parts) {
                String p = part.trim();
                if (!p.isEmpty() && !p.equalsIgnoreCase(query.trim())) {
                    ordered.add(p);
                }
            }
        }

        List<String> limited = new ArrayList<>(ordered);
        if (limited.size() > 6) {
            limited = limited.subList(0, 6);
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < limited.size(); i++) {
            builder.append(limited.get(i));
            if (i < limited.size() - 1) {
                builder.append("||");
            }
        }

        prefs.edit().putString(KEY_RECENT, builder.toString()).apply();
    }
}