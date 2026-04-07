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
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private MovieAdapter adapter;
    private ProgressBar progressBar;

    private static final String PREFS_NAME = "watchnext_prefs";

    private static final String[] GENRE_NAMES = {
            "Action", "Drama", "Sci-Fi", "Fantasy", "Crime", "Comedy", "Horror", "Romance"
    };
    private static final int[] MOVIE_GENRE_IDS = {28, 18, 878, 14, 80, 35, 27, 10749};
    private static final int[] TV_GENRE_IDS    = {10759, 18, 10765, 10765, 80, 35, 27, 10749};
    private static final String[] PREF_KEYS = {
            "action", "drama", "scifi", "fantasy", "crime", "comedy", "horror", "romance"
    };

    private boolean moviesTabSelected = true;
    private boolean categorySelected = false;
    private boolean popularSelected = true;
    private List<TmdbMovie> allRecommendations = null;
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
            if (mediaType == null || mediaType.isEmpty()) {
                mediaType = "movie";
            }
            intent.putExtra("media_type", mediaType);
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        loadPopularMovies();

        // Dark mode toggle button
        ImageButton btnToggleTheme = view.findViewById(R.id.btn_toggle_theme);
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", true);
        btnToggleTheme.setImageResource(isDark ? R.drawable.ic_mode_light : R.drawable.ic_mode_night);
        btnToggleTheme.setOnClickListener(v -> {
            boolean currentlyDark = prefs.getBoolean("dark_mode", true);
            boolean newDark = !currentlyDark;
            prefs.edit().putBoolean("dark_mode", newDark).apply();
            AppCompatDelegate.setDefaultNightMode(
                    newDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            requireActivity().recreate();
        });

        TabLayout tabs = view.findViewById(R.id.tab_home);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Reset spinners to default when switching tabs
                spin.setSelection(0);
                categorySpinner.setSelection(0);

                if (tab.getPosition() == 0) {
                    allRecommendations = null;
                    moviesTabSelected = true;
                    loadPopularMovies();
                } else {
                    allRecommendations = null;
                    moviesTabSelected = false;
                    loadPopularTvShows();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Category spinner
        String[] categoryOptions = {"Categories", "Action", "Drama", "Sci-Fi", "Fantasy", "Crime", "Comedy", "Horror", "Romance"};
        categorySpinner = view.findViewById(R.id.categorySpinner);
        ArrayAdapter<String> adapterCategory = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryOptions);
        adapterCategory.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapterCategory);
        categorySpinner.setOnTouchListener((v, event) -> {
            categorySelected = true;
            return false;
        });
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!categorySelected) return;
                if (position == 0) {
                    if (popularSelected) {
                        if (moviesTabSelected) loadPopularMovies();
                        else loadPopularTvShows();
                    } else {
                        loadRecommendations();
                    }
                    return;
                }
                loadGenre(moviesTabSelected
                        ? MOVIE_GENRE_IDS[position - 1]
                        : TV_GENRE_IDS[position - 1]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Popular / Recommended spinner
        spin = view.findViewById(R.id.spinner);
        String[] options = {"Popular", "Recommended"};
        ArrayAdapter<String> adapterS = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, options);
        adapterS.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(adapterS);
        spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                popularSelected = (position == 0);
                categorySelected = false;
                categorySpinner.setSelection(0);

                if (popularSelected) {
                    if (moviesTabSelected) loadPopularMovies();
                    else loadPopularTvShows();
                } else {
                    loadRecommendations();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void loadGenre(int genreId) {
        showLoading(true);
        String type = moviesTabSelected ? "movie" : "tv";
        Call<TmdbResponse> call = moviesTabSelected
                ? TmdbClient.getService().discoverMovies(genreId)
                : TmdbClient.getService().discoverTv(genreId);

        call.enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<TmdbMovie> results = response.body().getResults();
                    stampMediaType(results, type);
                    adapter.updateMovies(results);
                } else {
                    Toast.makeText(requireContext(), "Failed to load genre", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(requireContext(), "Failed to load genre", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPopularMovies() {
        showLoading(true);
        TmdbClient.getService().getPopularMovies().enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<TmdbMovie> results = response.body().getResults();
                    stampMediaType(results, "movie");
                    adapter.updateMovies(results);
                } else {
                    Toast.makeText(requireContext(), "Failed to load movies", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(requireContext(), "Failed to load movies", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPopularTvShows() {
        showLoading(true);
        TmdbClient.getService().getPopularTvShows().enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<TmdbMovie> results = response.body().getResults();
                    stampMediaType(results, "tv");
                    adapter.updateMovies(results);
                } else {
                    Toast.makeText(requireContext(), "Failed to load TV shows", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(requireContext(), "Failed to load TV shows", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRecommendations() {
        if (allRecommendations != null) {
            adapter.updateMovies(allRecommendations);
            return;
        }

        showLoading(true);
        String type = moviesTabSelected ? "movie" : "tv";
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        List<TmdbMovie> recs = new ArrayList<>();

        for (int i = 0; i < PREF_KEYS.length; i++) {
            int score = prefs.getInt("genre_" + PREF_KEYS[i], 50);
            if (score >= 60) {
                int genreId = moviesTabSelected ? MOVIE_GENRE_IDS[i] : TV_GENRE_IDS[i];
                Call<TmdbResponse> call = moviesTabSelected
                        ? TmdbClient.getService().discoverMovies(genreId)
                        : TmdbClient.getService().discoverTv(genreId);

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
                        Toast.makeText(requireContext(), "Failed to load recommendations", Toast.LENGTH_SHORT).show();
                    }
                });
            }
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
