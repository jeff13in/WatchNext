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
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "For You" page — personalised recommendations driven by the user's Supabase watchlist.
 *
 * Flow:
 *  1. Load user's watchlist from Supabase (Liked → liked, Disliked → disliked,
 *     Watching/Finished → watched).
 *  2. Resolve each saved title to a TmdbMovie via searchMulti.
 *  3. Fetch popular movies + TV as the candidate pool.
 *  4. Run Recommender with the user's profile + selected mood.
 *  5. Mood spinner re-runs step 4 instantly (candidates already in memory).
 */
public class RecommendationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecommendationAdapter adapter;
    private Spinner moodSpinner;
    private TextView emptyText;

    private final List<TmdbMovie> likedMovies    = new ArrayList<>();
    private final List<TmdbMovie> watchedMovies  = new ArrayList<>();
    private final List<TmdbMovie> dislikedMovies = new ArrayList<>();
    private final List<TmdbMovie> candidateMovies = new ArrayList<>();

    private String selectedMood = "";
    private int sessionRetries  = 0;

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
        moodSpinner  = view.findViewById(R.id.spinner_mood);
        emptyText    = view.findViewById(R.id.tv_empty_recommendations);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new RecommendationAdapter(requireContext(), item -> {
            UserSession session = UserSession.get();
            if (!session.isLoaded()) {
                Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show();
                return;
            }
            TmdbMovie source  = item.getSourceMovie();
            String movieId    = String.valueOf(source != null ? source.getId() : 0);
            String mediaType  = source != null ? source.getMediaType() : "movie";
            if (mediaType == null || mediaType.isEmpty()) mediaType = "movie";
            String posterUrl  = source != null ? source.getPosterUrl() : null;
            String finalType  = mediaType;

            SupabaseClient.addToWatchlist(
                    session.getSupabaseUserId(), movieId, item.getTitle(),
                    posterUrl, finalType,
                    new SupabaseClient.Callback() {
                        @Override public void onSuccess() {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(),
                                        item.getTitle() + " added to watchlist!", Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override public void onFailure(String error) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(),
                                        "Failed to add: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });

        recyclerView.setAdapter(adapter);
        setupMoodSpinner();
        loadFromSupabase();
    }

    // ── Mood spinner ──────────────────────────────────────────────────────────

    private void setupMoodSpinner() {
        String[] moods = {"Any Mood", "Happy", "Excited", "Relaxed", "Scared", "Emotional"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, moods);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        moodSpinner.setAdapter(spinnerAdapter);

        moodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                selectedMood = "Any Mood".equalsIgnoreCase(selected) ? "" : selected.toLowerCase();

                // Re-score with new mood if candidates are already loaded
                if (!candidateMovies.isEmpty()) {
                    generateRecommendations();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                selectedMood = "";
            }
        });
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    /**
     * Step 1: load user watchlist from Supabase.
     * Retries up to 5 times with 2 s delay while UserSession is not ready.
     */
    private void loadFromSupabase() {
        if (!UserSession.get().isLoaded()) {
            if (sessionRetries < 5) {
                sessionRetries++;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isAdded()) loadFromSupabase();
                }, 2000);
            } else {
                // Session never loaded — still show popular recommendations with mood support
                fetchCandidateMovies();
            }
            return;
        }
        sessionRetries = 0;

        likedMovies.clear();
        watchedMovies.clear();
        dislikedMovies.clear();
        candidateMovies.clear();

        SupabaseClient.getWatchlist(UserSession.get().getSupabaseUserId(),
                new SupabaseClient.ListCallback() {
                    @Override
                    public void onSuccess(JSONArray data) {
                        List<ListEntry> entries = new ArrayList<>();
                        for (int i = 0; i < data.length(); i++) {
                            try {
                                JSONObject obj  = data.getJSONObject(i);
                                String title    = obj.optString("title", "");
                                String type     = obj.optString("media_type", "movie")
                                                     .equalsIgnoreCase("tv") ? "TV Show" : "Movie";
                                String status   = obj.optString("status", "Watching");
                                String movieId  = obj.optString("movie_id", "");
                                int sid         = obj.optInt("id", -1);

                                ListEntry e = new ListEntry(title, type, status, 0);
                                e.setMovieId(movieId);
                                e.setSupabaseId(sid);
                                entries.add(e);
                            } catch (Exception ignored) {}
                        }

                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            if (entries.isEmpty()) {
                                // No watchlist — still show mood-filtered popular content
                                fetchCandidateMovies();
                            } else {
                                resolveUserEntries(entries, 0);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            if (isAdded()) fetchCandidateMovies();
                        });
                    }
                });
    }

    // ── Entry resolution ──────────────────────────────────────────────────────

    /**
     * Step 2: resolve each ListEntry title to a TmdbMovie via searchMulti,
     * then bucket into liked/watched/disliked based on status.
     */
    private void resolveUserEntries(List<ListEntry> entries, int index) {
        if (index >= entries.size()) {
            fetchCandidateMovies();
            return;
        }

        ListEntry entry = entries.get(index);
        TmdbClient.getService().searchMulti(entry.getTitle()).enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResults() != null) {
                    TmdbMovie best = findBestMatch(entry, response.body().getResults());
                    if (best != null) mapEntryToUserLists(entry, best);
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
        if (results == null || results.isEmpty()) return null;
        String desiredType = entry.getType(); // "Movie" or "TV Show"

        // Exact title + correct type
        for (TmdbMovie m : results) {
            if (m == null || m.getDisplayTitle() == null) continue;
            if (typeMatches(m, desiredType) && m.getDisplayTitle().equalsIgnoreCase(entry.getTitle()))
                return m;
        }
        // Correct type only
        for (TmdbMovie m : results) {
            if (m != null && typeMatches(m, desiredType)) return m;
        }
        return results.get(0);
    }

    private boolean typeMatches(TmdbMovie m, String desiredType) {
        String mt = m.getMediaType();
        if ("Movie".equalsIgnoreCase(desiredType)) return "movie".equalsIgnoreCase(mt);
        if ("TV Show".equalsIgnoreCase(desiredType)) return "tv".equalsIgnoreCase(mt);
        return false;
    }

    /**
     * Maps a resolved TmdbMovie into the correct user list based on watchlist status.
     *  Liked    → likedMovies (positive signal)
     *  Disliked → dislikedMovies (negative signal)
     *  Finished → watchedMovies + likedMovies
     *  Watching → watchedMovies
     */
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
            // Watching
            addUnique(watchedMovies, movie);
        }
    }

    private void addUnique(List<TmdbMovie> list, TmdbMovie movie) {
        for (TmdbMovie m : list) {
            if (m != null && m.getId() == movie.getId()) return;
        }
        list.add(movie);
    }

    // ── Candidate fetching ────────────────────────────────────────────────────

    /**
     * Step 3: fetch 3 pages of popular movies (60 results) + 1 page of TV shows.
     * Pages are fetched sequentially; once all are done, generateRecommendations() is called.
     * Target: ~50 movies in the candidate pool before scoring.
     */
    private int moviePagesLoaded = 0;
    private static final int MOVIE_PAGES = 3; // 3 × 20 = 60 movies

    private void fetchCandidateMovies() {
        moviePagesLoaded = 0;
        fetchMoviePage(1);
    }

    private void fetchMoviePage(int page) {
        TmdbClient.getService().getPopularMoviesPage(page).enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResults() != null) {
                    for (TmdbMovie m : response.body().getResults()) {
                        if (m != null) { m.setMediaType("movie"); candidateMovies.add(m); }
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

    /** Step 3b: fetch popular TV shows and add to candidate pool. */
    private void fetchCandidateTv() {
        TmdbClient.getService().getPopularTvShows().enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getResults() != null) {
                    for (TmdbMovie m : response.body().getResults()) {
                        if (m != null) { m.setMediaType("tv"); candidateMovies.add(m); }
                    }
                }
                generateRecommendations();
            }
            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (isAdded()) generateRecommendations();
            }
        });
    }

    // ── Scoring ───────────────────────────────────────────────────────────────

    /** Step 4: score candidates off the main thread, then update UI. */
    private void generateRecommendations() {
        // Snapshot current mood so background thread uses the right value
        final String mood = selectedMood;
        final List<TmdbMovie> liked    = new ArrayList<>(likedMovies);
        final List<TmdbMovie> disliked = new ArrayList<>(dislikedMovies);
        final List<TmdbMovie> watched  = new ArrayList<>(watchedMovies);
        final List<TmdbMovie> cands    = new ArrayList<>(candidateMovies);

        new Thread(() -> {
            List<ScoredRec> results = RecommendationsBuilder.generateTopRecommendations(
                    liked, disliked, watched, cands, mood, 20);

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
}
