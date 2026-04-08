package cosc.brocku.ca.watchnext;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private MovieAdapter adapter;
    private ProgressBar progressBar;

    private static final String[] GENRE_NAMES = {
            "Action", "Drama", "Sci-Fi", "Fantasy", "Crime", "Comedy", "Horror", "Romance"
    };
    private static final int[] MOVIE_GENRE_IDS = {28, 18, 878, 14, 80, 35, 27, 10749};
    private static final int[] TV_GENRE_IDS    = {10759, 18, 10765, 10765, 80, 35, 27, 10749};
    private static final String[] PREF_KEYS = {
            "action", "drama", "scifi", "fantasy", "crime", "comedy", "horror", "romance"
    };

    private boolean moviesTabSelected = true;
    private boolean categorySelected  = false;
    private boolean popularSelected   = true;
    private List<TmdbMovie> allRecommendations = null;

    // Active genre (-1 = none)
    private int activeGenreId = -1;

    // Year dropdown options: label → {dateGte, dateLte} or exact year
    // Index 0 = All, 1-6 = exact years 2025..2020, 7-10 = decade ranges
    private static final String[] YEAR_OPTIONS = {
            "All Years", "2025", "2024", "2023", "2022", "2021", "2020",
            "2015 – 2019", "2010 – 2014", "2000 – 2009", "1990 – 1999"
    };
    private int   selectedYearIndex = 0;   // 0 = All
    private float selectedRating    = -1f; // -1 = All, else minimum rating

    private Spinner spin;
    private Spinner categorySpinner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progress_home);

        RecyclerView rv = view.findViewById(R.id.rv_movies);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new MovieAdapter(new ArrayList<>(), movie -> {
            Intent intent = new Intent(requireContext(), MovieDetailActivity.class);
            intent.putExtra("movie_id", movie.getId());
            String mediaType = movie.getMediaType();
            intent.putExtra("media_type", (mediaType == null || mediaType.isEmpty()) ? "movie" : mediaType);
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        // ── Tabs ──────────────────────────────────────────────────────────────
        TabLayout tabs = view.findViewById(R.id.tab_home);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                spin.setSelection(0);
                categorySpinner.setSelection(0);
                allRecommendations = null;
                activeGenreId = -1;
                moviesTabSelected = tab.getPosition() == 0;
                refresh();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // ── Category spinner ──────────────────────────────────────────────────
        String[] categoryOptions = {"Categories", "Action", "Drama", "Sci-Fi",
                "Fantasy", "Crime", "Comedy", "Horror", "Romance"};
        categorySpinner = view.findViewById(R.id.categorySpinner);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryOptions);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(catAdapter);
        categorySpinner.setOnTouchListener((v, event) -> {
            categorySelected = true;
            return false;
        });
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!categorySelected) return;
                if (position == 0) {
                    activeGenreId = -1;
                    refresh();
                } else {
                    activeGenreId = moviesTabSelected
                            ? MOVIE_GENRE_IDS[position - 1]
                            : TV_GENRE_IDS[position - 1];
                    refresh();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ── Popular / Recommended spinner ─────────────────────────────────────
        spin = view.findViewById(R.id.spinner);
        String[] options = {"Popular", "Recommended"};
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, options);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(spinAdapter);
        spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                popularSelected = (position == 0);
                categorySelected = false;
                categorySpinner.setSelection(0);
                activeGenreId = -1;
                allRecommendations = null;
                refresh();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ── Year spinner ──────────────────────────────────────────────────────
        Spinner yearSpinner = view.findViewById(R.id.yearSpinner);
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, YEAR_OPTIONS);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);
        yearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == selectedYearIndex) return;
                selectedYearIndex = position;
                allRecommendations = null;
                refresh();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ── Rating filter chips ───────────────────────────────────────────────
        ChipGroup ratingGroup = view.findViewById(R.id.chipgroup_rating);
        ratingGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if      (id == R.id.chip_rating_all) selectedRating = -1f;
            else if (id == R.id.chip_rating_6)   selectedRating = 6f;
            else if (id == R.id.chip_rating_7)   selectedRating = 7f;
            else if (id == R.id.chip_rating_8)   selectedRating = 8f;
            else if (id == R.id.chip_rating_9)   selectedRating = 9f;
            allRecommendations = null;
            refresh();
        });

        refresh();
    }

    // ── Central refresh dispatcher ─────────────────────────────────────────────

    private void refresh() {
        if (activeGenreId != -1) {
            loadDiscover(activeGenreId);
        } else if (!popularSelected) {
            loadRecommendations();
        } else {
            loadDiscover(null);
        }
    }

    private boolean hasFilters() {
        return selectedYearIndex != 0 || selectedRating > 0;
    }

    // ── Core load methods ──────────────────────────────────────────────────────

    /**
     * Single discover call that respects active genre, year, and rating filters.
     * When genreId is null and no filters are active, falls back to popularity.desc
     * so the results match "popular" behaviour via the discover endpoint.
     */
    private void loadDiscover(@Nullable Integer genreId) {
        showLoading(true);
        String type = moviesTabSelected ? "movie" : "tv";

        // Parse year dropdown selection into API params
        Integer yearParam = null;
        String  dateGte   = null;
        String  dateLte   = null;
        switch (selectedYearIndex) {
            case 0:  break; // All Years — no filter
            case 1:  yearParam = 2025; break;
            case 2:  yearParam = 2024; break;
            case 3:  yearParam = 2023; break;
            case 4:  yearParam = 2022; break;
            case 5:  yearParam = 2021; break;
            case 6:  yearParam = 2020; break;
            case 7:  dateGte = "2015-01-01"; dateLte = "2019-12-31"; break;
            case 8:  dateGte = "2010-01-01"; dateLte = "2014-12-31"; break;
            case 9:  dateGte = "2000-01-01"; dateLte = "2009-12-31"; break;
            case 10: dateGte = "1990-01-01"; dateLte = "1999-12-31"; break;
        }

        Float minRating = selectedRating > 0 ? selectedRating : null;

        Call<TmdbResponse> call = moviesTabSelected
                ? TmdbClient.getService().discoverMoviesFull(
                        genreId, yearParam, dateGte, dateLte, minRating, "popularity.desc")
                : TmdbClient.getService().discoverTvFull(
                        genreId, yearParam, dateGte, dateLte, minRating, "popularity.desc");

        call.enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<TmdbMovie> results = response.body().getResults();
                    stampMediaType(results, type);
                    adapter.updateMovies(results != null ? results : new ArrayList<>());
                } else {
                    Toast.makeText(requireContext(), "Failed to load content", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(requireContext(), "Failed to load content", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRecommendations() {
        if (allRecommendations != null && !hasFilters()) {
            adapter.updateMovies(allRecommendations);
            return;
        }

        showLoading(true);
        String type = moviesTabSelected ? "movie" : "tv";
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        List<TmdbMovie> recs = new ArrayList<>();

        for (int i = 0; i < PREF_KEYS.length; i++) {
            int score = prefs.getInt("genre_" + PREF_KEYS[i], 50);
            if (score < 60) continue;

            int genreId = moviesTabSelected ? MOVIE_GENRE_IDS[i] : TV_GENRE_IDS[i];

            // Parse year dropdown selection into API params
            Integer yearParam = null;
            String  dateGte   = null;
            String  dateLte   = null;
            switch (selectedYearIndex) {
                case 0:  break;
                case 1:  yearParam = 2025; break;
                case 2:  yearParam = 2024; break;
                case 3:  yearParam = 2023; break;
                case 4:  yearParam = 2022; break;
                case 5:  yearParam = 2021; break;
                case 6:  yearParam = 2020; break;
                case 7:  dateGte = "2015-01-01"; dateLte = "2019-12-31"; break;
                case 8:  dateGte = "2010-01-01"; dateLte = "2014-12-31"; break;
                case 9:  dateGte = "2000-01-01"; dateLte = "2009-12-31"; break;
                case 10: dateGte = "1990-01-01"; dateLte = "1999-12-31"; break;
            }
            Float minRating = selectedRating > 0 ? selectedRating : null;

            Call<TmdbResponse> call = moviesTabSelected
                    ? TmdbClient.getService().discoverMoviesFull(
                            genreId, yearParam, dateGte, dateLte, minRating, "popularity.desc")
                    : TmdbClient.getService().discoverTvFull(
                            genreId, yearParam, dateGte, dateLte, minRating, "popularity.desc");

            call.enqueue(new Callback<TmdbResponse>() {
                @Override
                public void onResponse(@NonNull Call<TmdbResponse> call,
                                       @NonNull Response<TmdbResponse> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null) {
                        List<TmdbMovie> batch = response.body().getResults();
                        stampMediaType(batch, type);
                        recs.addAll(batch);
                        allRecommendations = recs;
                        showLoading(false);
                        adapter.updateMovies(allRecommendations);
                    }
                }
                @Override
                public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                    if (!isAdded()) return;
                    showLoading(false);
                    Toast.makeText(requireContext(),
                            "Failed to load recommendations", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void stampMediaType(List<TmdbMovie> items, String type) {
        if (items == null) return;
        for (TmdbMovie item : items) {
            if (item.getMediaType() == null || item.getMediaType().isEmpty()) {
                item.setMediaType(type);
            }
        }
    }
}
