package cosc.brocku.ca.watchnext;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "For You" page — personalised recommendations driven by the user's Supabase watchlist.
 *
 * Flow:
 *  1. Load user's watchlist from Supabase.
 *  2. Resolve each saved title to a TmdbMovie via searchMulti.
 *  3. Fetch base candidate pool (popular movies + TV).
 *  4. If a mood is selected, fetch mood-based movie + TV candidates from TMDb discover.
 *  5. Merge pools with mood candidates dominating when a mood is selected.
 *  6. Run Recommender with user's profile + selected mood.
 */
public class RecommendationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecommendationAdapter adapter;
    private Spinner moodSpinner;
    private TextView emptyText;

    private final List<TmdbMovie> likedMovies = new ArrayList<>();
    private final List<TmdbMovie> watchedMovies = new ArrayList<>();
    private final List<TmdbMovie> dislikedMovies = new ArrayList<>();

    private final List<TmdbMovie> baseCandidates = new ArrayList<>();
    private final List<TmdbMovie> moodCandidates = new ArrayList<>();

    private String selectedMood = "";
    private int sessionRetries = 0;

    private boolean baseCandidatesLoaded = false;
    private int moviePagesLoaded = 0;

    private static final int MOVIE_PAGES = 3; // 60 popular movies
    private static final int MOOD_PAGES_PER_GENRE = 2;
    private static final int BASE_EXTRAS_WHEN_MOOD_SELECTED = 20;

    private static final Map<String, List<Integer>> MOOD_GENRE_ID_MAP = new LinkedHashMap<>();
    static {
        MOOD_GENRE_ID_MAP.put("happy", Arrays.asList(35, 16, 10751));        // Comedy, Animation, Family
        MOOD_GENRE_ID_MAP.put("excited", Arrays.asList(28, 12, 878));        // Action, Adventure, Sci-Fi
        MOOD_GENRE_ID_MAP.put("relaxed", Arrays.asList(99, 10749));          // Documentary, Romance
        MOOD_GENRE_ID_MAP.put("scared", Arrays.asList(27, 53));              // Horror, Thriller
        MOOD_GENRE_ID_MAP.put("emotional", Arrays.asList(18, 10749, 10402)); // Drama, Romance, Music
    }

    public RecommendationsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recommendations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_recommendations);
        moodSpinner = view.findViewById(R.id.spinner_mood);
        emptyText = view.findViewById(R.id.tv_empty_recommendations);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new RecommendationAdapter(requireContext(), item -> {
            UserSession session = UserSession.get();
            if (!session.isLoaded()) {
                Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show();
                return;
            }

            TmdbMovie source = item.getSourceMovie();
            String movieId = String.valueOf(source != null ? source.getId() : 0);
            String mediaType = source != null ? source.getMediaType() : "movie";
            if (mediaType == null || mediaType.isEmpty()) {
                mediaType = "movie";
            }
            String posterUrl = source != null ? source.getPosterUrl() : null;

            SupabaseClient.addToWatchlist(
                    session.getSupabaseUserId(),
                    movieId,
                    item.getTitle(),
                    posterUrl,
                    mediaType,
                    new SupabaseClient.Callback() {
                        @Override
                        public void onSuccess() {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                Toast.makeText(
                                        requireContext(),
                                        item.getTitle() + " added to watchlist!",
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                        }

                        @Override
                        public void onFailure(String error) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                Toast.makeText(
                                        requireContext(),
                                        "Failed to add: " + error,
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                        }
                    }
            );
        });

        recyclerView.setAdapter(adapter);
        setupMoodSpinner();
        loadFromSupabase();
    }

    private void setupMoodSpinner() {
        String[] moods = {"Any Mood", "Happy", "Excited", "Relaxed", "Scared", "Emotional"};

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                moods
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        moodSpinner.setAdapter(spinnerAdapter);

        moodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                selectedMood = "Any Mood".equalsIgnoreCase(selected) ? "" : selected.toLowerCase();

                if (!baseCandidatesLoaded) {
                    return;
                }

                if (selectedMood.isEmpty()) {
                    moodCandidates.clear();
                    generateRecommendations();
                } else {
                    fetchMoodCandidates(selectedMood);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedMood = "";
            }
        });
    }

    private void loadFromSupabase() {
        if (!UserSession.get().isLoaded()) {
            if (sessionRetries < 5) {
                sessionRetries++;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isAdded()) {
                        loadFromSupabase();
                    }
                }, 2000);
            } else {
                fetchBaseCandidateMovies();
            }
            return;
        }

        sessionRetries = 0;

        likedMovies.clear();
        watchedMovies.clear();
        dislikedMovies.clear();
        baseCandidates.clear();
        moodCandidates.clear();
        baseCandidatesLoaded = false;

        SupabaseClient.getWatchlist(
                UserSession.get().getSupabaseUserId(),
                new SupabaseClient.ListCallback() {
                    @Override
                    public void onSuccess(JSONArray data) {
                        List<ListEntry> entries = new ArrayList<>();

                        for (int i = 0; i < data.length(); i++) {
                            try {
                                JSONObject obj = data.getJSONObject(i);
                                String title = obj.optString("title", "");
                                String type = obj.optString("media_type", "movie")
                                        .equalsIgnoreCase("tv") ? "TV Show" : "Movie";
                                String status = obj.optString("status", "Watching");
                                String movieId = obj.optString("movie_id", "");
                                int sid = obj.optInt("id", -1);

                                ListEntry e = new ListEntry(title, type, status, 0);
                                e.setMovieId(movieId);
                                e.setSupabaseId(sid);
                                entries.add(e);
                            } catch (Exception ignored) {
                            }
                        }

                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;

                            if (entries.isEmpty()) {
                                fetchBaseCandidateMovies();
                            } else {
                                resolveUserEntries(entries, 0);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (isAdded()) {
                                fetchBaseCandidateMovies();
                            }
                        });
                    }
                }
        );
    }

    private void resolveUserEntries(List<ListEntry> entries, int index) {
        if (index >= entries.size()) {
            fetchBaseCandidateMovies();
            return;
        }

        ListEntry entry = entries.get(index);
        TmdbClient.getService().searchMulti(entry.getTitle()).enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getResults() != null) {
                    TmdbMovie best = findBestMatch(entry, response.body().getResults());
                    if (best != null) {
                        mapEntryToUserLists(entry, best);
                    }
                }

                resolveUserEntries(entries, index + 1);
            }

            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                resolveUserEntries(entries, index + 1);
            }
        });
    }

    private TmdbMovie findBestMatch(ListEntry entry, List<TmdbMovie> results) {
        if (results == null || results.isEmpty()) {
            return null;
        }

        String desiredType = entry.getType();

        for (TmdbMovie m : results) {
            if (m == null || m.getDisplayTitle() == null) continue;
            if (typeMatches(m, desiredType)
                    && m.getDisplayTitle().equalsIgnoreCase(entry.getTitle())) {
                return m;
            }
        }

        for (TmdbMovie m : results) {
            if (m != null && typeMatches(m, desiredType)) {
                return m;
            }
        }

        return results.get(0);
    }

    private boolean typeMatches(TmdbMovie movie, String desiredType) {
        String mediaType = movie.getMediaType();
        if ("Movie".equalsIgnoreCase(desiredType)) {
            return "movie".equalsIgnoreCase(mediaType);
        }
        if ("TV Show".equalsIgnoreCase(desiredType)) {
            return "tv".equalsIgnoreCase(mediaType);
        }
        return false;
    }

    private void mapEntryToUserLists(ListEntry entry, TmdbMovie movie) {
        String status = entry.getStatus();

        if ("Liked".equalsIgnoreCase(status)) {
            addUnique(likedMovies, movie);
        } else if ("Disliked".equalsIgnoreCase(status)) {
            addUnique(dislikedMovies, movie);
        } else if ("Finished".equalsIgnoreCase(status)) {
            addUnique(watchedMovies, movie);
            addUnique(likedMovies, movie);
        } else {
            addUnique(watchedMovies, movie);
        }
    }

    private void addUnique(List<TmdbMovie> list, TmdbMovie movie) {
        if (movie == null) {
            return;
        }

        for (TmdbMovie existing : list) {
            if (existing != null
                    && existing.getId() == movie.getId()
                    && safeType(existing).equalsIgnoreCase(safeType(movie))) {
                return;
            }
        }

        list.add(movie);
    }

    private String safeType(TmdbMovie movie) {
        if (movie == null || movie.getMediaType() == null) {
            return "movie";
        }
        return movie.getMediaType();
    }

    private void fetchBaseCandidateMovies() {
        baseCandidates.clear();
        moodCandidates.clear();
        baseCandidatesLoaded = false;
        moviePagesLoaded = 0;
        fetchMoviePage(1);
    }

    private void fetchMoviePage(int page) {
        TmdbClient.getService().getPopularMoviesPage(page).enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getResults() != null) {
                    for (TmdbMovie movie : response.body().getResults()) {
                        if (movie != null) {
                            movie.setMediaType("movie");
                            addUnique(baseCandidates, movie);
                        }
                    }
                }

                moviePagesLoaded++;
                if (moviePagesLoaded < MOVIE_PAGES) {
                    fetchMoviePage(page + 1);
                } else {
                    fetchCandidateTv();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;

                moviePagesLoaded++;
                if (moviePagesLoaded < MOVIE_PAGES) {
                    fetchMoviePage(page + 1);
                } else {
                    fetchCandidateTv();
                }
            }
        });
    }

    private void fetchCandidateTv() {
        TmdbClient.getService().getPopularTvShows().enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getResults() != null) {
                    for (TmdbMovie movie : response.body().getResults()) {
                        if (movie != null) {
                            movie.setMediaType("tv");
                            addUnique(baseCandidates, movie);
                        }
                    }
                }

                baseCandidatesLoaded = true;

                if (selectedMood.isEmpty()) {
                    generateRecommendations();
                } else {
                    fetchMoodCandidates(selectedMood);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;

                baseCandidatesLoaded = true;

                if (selectedMood.isEmpty()) {
                    generateRecommendations();
                } else {
                    fetchMoodCandidates(selectedMood);
                }
            }
        });
    }

    private void fetchMoodCandidates(String mood) {
        List<Integer> genreIds = MOOD_GENRE_ID_MAP.get(mood);
        if (genreIds == null || genreIds.isEmpty()) {
            moodCandidates.clear();
            generateRecommendations();
            return;
        }

        moodCandidates.clear();

        // movie + tv requests
        final int[] totalRequests = {genreIds.size() * MOOD_PAGES_PER_GENRE * 2};
        final int[] completedRequests = {0};

        for (int genreId : genreIds) {
            for (int page = 1; page <= MOOD_PAGES_PER_GENRE; page++) {
                fetchMoodMoviePage(mood, genreId, page, totalRequests, completedRequests);
                fetchMoodTvPage(mood, genreId, page, totalRequests, completedRequests);
            }
        }
    }

    private void fetchMoodMoviePage(String requestedMood,
                                    int genreId,
                                    int page,
                                    int[] totalRequests,
                                    int[] completedRequests) {
        TmdbClient.getService().discoverMoviesByGenre(genreId, page)
                .enqueue(new Callback<TmdbResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbResponse> call,
                                           @NonNull Response<TmdbResponse> response) {
                        if (!isAdded()) return;

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getResults() != null) {
                            for (TmdbMovie movie : response.body().getResults()) {
                                if (movie != null) {
                                    movie.setMediaType("movie");
                                    addUnique(moodCandidates, movie);
                                }
                            }
                        }

                        onMoodRequestFinished(requestedMood, totalRequests, completedRequests);
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbResponse> call,
                                          @NonNull Throwable t) {
                        if (!isAdded()) return;
                        onMoodRequestFinished(requestedMood, totalRequests, completedRequests);
                    }
                });
    }

    private void fetchMoodTvPage(String requestedMood,
                                 int genreId,
                                 int page,
                                 int[] totalRequests,
                                 int[] completedRequests) {
        TmdbClient.getService().discoverTvByGenre(genreId, page)
                .enqueue(new Callback<TmdbResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbResponse> call,
                                           @NonNull Response<TmdbResponse> response) {
                        if (!isAdded()) return;

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getResults() != null) {
                            for (TmdbMovie tv : response.body().getResults()) {
                                if (tv != null) {
                                    tv.setMediaType("tv");
                                    addUnique(moodCandidates, tv);
                                }
                            }
                        }

                        onMoodRequestFinished(requestedMood, totalRequests, completedRequests);
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbResponse> call,
                                          @NonNull Throwable t) {
                        if (!isAdded()) return;
                        onMoodRequestFinished(requestedMood, totalRequests, completedRequests);
                    }
                });
    }

    private synchronized void onMoodRequestFinished(String requestedMood,
                                                    int[] totalRequests,
                                                    int[] completedRequests) {
        completedRequests[0]++;

        if (completedRequests[0] >= totalRequests[0]) {
            if (!requestedMood.equals(selectedMood)) {
                return;
            }
            generateRecommendations();
        }
    }

    private void generateRecommendations() {
        final String mood = selectedMood;
        final List<TmdbMovie> liked = new ArrayList<>(likedMovies);
        final List<TmdbMovie> disliked = new ArrayList<>(dislikedMovies);
        final List<TmdbMovie> watched = new ArrayList<>(watchedMovies);
        final List<TmdbMovie> mergedCandidates = mergeCandidatePools();

        new Thread(() -> {
            List<ScoredRec> results = RecommendationsBuilder.generateTopRecommendations(
                    liked,
                    disliked,
                    watched,
                    mergedCandidates,
                    mood,
                    20
            );

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) return;

                adapter.setRecommendations(results);

                if (results == null || results.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    private List<TmdbMovie> mergeCandidatePools() {
        Map<String, TmdbMovie> merged = new LinkedHashMap<>();

        if (selectedMood != null
                && !selectedMood.trim().isEmpty()
                && !moodCandidates.isEmpty()) {

            for (TmdbMovie movie : moodCandidates) {
                if (movie == null) continue;
                merged.put(buildCandidateKey(movie), movie);
            }

            int extras = 0;
            for (TmdbMovie movie : baseCandidates) {
                if (movie == null) continue;

                String key = buildCandidateKey(movie);
                if (!merged.containsKey(key)) {
                    merged.put(key, movie);
                    extras++;
                }

                if (extras >= BASE_EXTRAS_WHEN_MOOD_SELECTED) {
                    break;
                }
            }

        } else {
            for (TmdbMovie movie : baseCandidates) {
                if (movie == null) continue;
                merged.put(buildCandidateKey(movie), movie);
            }
        }

        return new ArrayList<>(merged.values());
    }

    private String buildCandidateKey(TmdbMovie movie) {
        return safeType(movie) + "_" + movie.getId();
    }
}