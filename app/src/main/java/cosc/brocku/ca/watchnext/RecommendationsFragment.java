package cosc.brocku.ca.watchnext;

import android.os.Bundle;
import android.text.TextUtils;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Class that displays personalized recommendations in a RecyclerView
// It uses the user's saved list entries, resolves them to TMDb items,
// fetches real candidate movies/shows from TMDb, then scores them.
public class RecommendationsFragment extends Fragment {
    private RecyclerView recyclerView;
    private RecommendationAdapter adapter;

    //Spinner showing currently selected mood
    private Spinner moodSpinner;
    private TextView emptyText;

    // Lists used by the recommender
    private final List<TmdbMovie> likedMovies = new ArrayList<>();
    private final List<TmdbMovie> watchedMovies = new ArrayList<>();
    private final List<TmdbMovie> dislikedMovies = new ArrayList<>();
    private final List<TmdbMovie> candidateMovies = new ArrayList<>();

    //Optional mood selected by the user
    private String selectedMood = "";

    public RecommendationsFragment() {
        //Required empty public constructor
    }

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

        // Find views from the layout
        recyclerView = view.findViewById(R.id.recycler_recommendations);
        moodSpinner = view.findViewById(R.id.spinner_mood);
        emptyText = view.findViewById(R.id.tv_empty_recommendations);

        // Set RecyclerView layout manager
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Create adapter and define watchlist button behavior
        adapter = new RecommendationAdapter(requireContext(), item -> {
            UserSession session = UserSession.get();
            if (!session.isLoaded()) {
                Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show();
                return;
            }

            TmdbMovie source = item.getSourceMovie();
            String movieId   = String.valueOf(source != null ? source.getId() : 0);
            String mediaType = source != null ? source.getMediaType() : "movie";
            if (mediaType == null || mediaType.isEmpty()) mediaType = "movie";
            String posterUrl = source != null ? source.getPosterUrl() : null;
            String finalMediaType = mediaType;

            SupabaseClient.addToWatchlist(
                    session.getSupabaseUserId(),
                    movieId,
                    item.getTitle(),
                    posterUrl,
                    finalMediaType,
                    new SupabaseClient.Callback() {
                        @Override
                        public void onSuccess() {
                            // Also log to watch history so it appears on Watch Days
                            SupabaseClient.addToWatchHistory(
                                    session.getSupabaseUserId(),
                                    movieId,
                                    item.getTitle(),
                                    finalMediaType,
                                    posterUrl,
                                    new SupabaseClient.Callback() {
                                        @Override public void onSuccess() {}
                                        @Override public void onFailure(String e) {}
                                    });
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(),
                                        item.getTitle() + " added to watchlist!",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onFailure(String error) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(),
                                        "Failed to add: " + error,
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });

        recyclerView.setAdapter(adapter);
        setupMoodSpinner();

        // Start loading recommendations
        loadRecommendations();
    }
    private void setupMoodSpinner() {
        List<String> moods = new ArrayList<>();
        moods.add("None");
        moods.add("Happy");
        moods.add("Excited");
        moods.add("Relaxed");
        moods.add("Scared");
        moods.add("Emotional");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                moods
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        moodSpinner.setAdapter(spinnerAdapter);

        moodSpinner.setSelection(0);

        moodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();

                if ("None".equalsIgnoreCase(selected)) {
                    selectedMood = "";
                } else {
                    selectedMood = selected.toLowerCase();
                }

                if (!candidateMovies.isEmpty()) {
                    generateRecommendations();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedMood = "";
            }
        });
    }

    // Main entry point for recommendation loading
    private void loadRecommendations() {
        // Clear old data before rebuilding
        likedMovies.clear();
        watchedMovies.clear();
        dislikedMovies.clear();
        candidateMovies.clear();

        // Get saved user list entries from shared repository
        List<ListEntry> entries = ListRepository.getAllEntries();

        // If there are no saved entries, show empty state
        if (entries == null || entries.isEmpty()) {
            showEmpty();
            return;
        }

        // Resolve each ListEntry title into a real TMDb item
        resolveUserEntries(entries, 0);
    }

    // Recursively resolves user list entries to TMDb search results
    // This uses searchMulti(title) so we can turn saved titles into real TmdbMovie objects
    private void resolveUserEntries(List<ListEntry> entries, int index) {
        // Base case: once all entries are processed, fetch candidate recommendation items
        if (index >= entries.size()) {
            fetchCandidateMovies();
            return;
        }

        // Current entry being resolved
        ListEntry entry = entries.get(index);

        // Search TMDb using the saved title
        TmdbClient.getService().searchMulti(entry.getTitle()).enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call, @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;

                // If search succeeded, try to find the best matching TMDb result
                if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                    TmdbMovie bestMatch = findBestMatch(entry, response.body().getResults());

                    // Add matched item to liked/watched lists based on list status
                    if (bestMatch != null) {
                        mapEntryToUserLists(entry, bestMatch);
                    }
                }

                // Move to next saved entry
                resolveUserEntries(entries, index + 1);
            }

            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;

                // Even if one search fails, continue resolving the rest
                resolveUserEntries(entries, index + 1);
            }
        });
    }

    // Picks the best search result for a given saved list entry
    // Tries exact title + media type first, then type match, then falls back to first result
    private TmdbMovie findBestMatch(ListEntry entry, List<TmdbMovie> results) {
        if (results == null || results.isEmpty()) {
            return null;
        }

        String desiredType = entry.getType();

        // First pass: exact title and correct media type
        for (TmdbMovie movie : results) {
            if (movie == null || movie.getDisplayTitle() == null) {
                continue;
            }

            String mediaType = movie.getMediaType();
            boolean typeMatches = false;

            if ("Movie".equalsIgnoreCase(desiredType) && "movie".equalsIgnoreCase(mediaType)) {
                typeMatches = true;
            } else if ("TV Show".equalsIgnoreCase(desiredType) && "tv".equalsIgnoreCase(mediaType)) {
                typeMatches = true;
            }

            if (typeMatches && movie.getDisplayTitle().equalsIgnoreCase(entry.getTitle())) {
                return movie;
            }
        }

        // Second pass: correct media type only
        for (TmdbMovie movie : results) {
            if (movie == null) {
                continue;
            }

            String mediaType = movie.getMediaType();

            if ("Movie".equalsIgnoreCase(desiredType) && "movie".equalsIgnoreCase(mediaType)) {
                return movie;
            } else if ("TV Show".equalsIgnoreCase(desiredType) && "tv".equalsIgnoreCase(mediaType)) {
                return movie;
            }
        }

        // Final fallback: just use the first result
        return results.get(0);
    }

    // Converts a saved ListEntry into profile data for the recommender
    // Current simple rule:
    // - Finished = watched + liked
    // - Watching = liked
    // - Disliked list is empty for now because your current list model doesn't store dislikes
    private void mapEntryToUserLists(ListEntry entry, TmdbMovie movie) {
        String status = entry.getStatus();
        String playlist = entry.getPlaylist();

        if ("Finished".equalsIgnoreCase(status)) {
            if (!containsMovieId(watchedMovies, movie.getId())) {
                watchedMovies.add(movie);
            }
            if (!containsMovieId(likedMovies, movie.getId())) {
                likedMovies.add(movie);
            }
        } else if ("Watching".equalsIgnoreCase(status)) {
            if (!containsMovieId(likedMovies, movie.getId())) {
                likedMovies.add(movie);
            }
        }

        if ("Finished".equalsIgnoreCase(playlist)) {
            if (!containsMovieId(watchedMovies, movie.getId())) {
                watchedMovies.add(movie);
            }
        }
    }

    // Fetches popular movies from TMDb to use as candidate recommendation pool
    private void fetchCandidateMovies() {
        TmdbClient.getService().getPopularMovies().enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call, @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                    candidateMovies.addAll(response.body().getResults());
                }

                // After movies load, fetch TV shows too
                fetchCandidateTv();
            }

            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;

                // Still continue to fetch TV even if movie call fails
                fetchCandidateTv();
            }
        });
    }

    // Fetches popular TV shows and adds them to the candidate pool
    private void fetchCandidateTv() {
        TmdbClient.getService().getPopularTvShows().enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call, @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                    candidateMovies.addAll(response.body().getResults());
                }

                // Once candidates are ready, generate final recommendations
                generateRecommendations();
            }

            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;

                // Even if TV fetch fails, try generating with whatever we have
                generateRecommendations();
            }
        });
    }

    // Runs the recommendation engine and updates the RecyclerView
    private void generateRecommendations() {
        List<ScoredRec> results = RecommendationsBuilder.generateTopRecommendations(
                likedMovies,
                dislikedMovies,
                watchedMovies,
                candidateMovies,
                selectedMood,
                20
        );

        adapter.setRecommendations(results);

        if (results == null || results.isEmpty()) {
            showEmpty();
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // Shows the empty-state message and hides the list
    private void showEmpty() {
        emptyText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    // Utility method to avoid duplicate items by TMDb id
    private boolean containsMovieId(List<TmdbMovie> list, int id) {
        for (TmdbMovie movie : list) {
            if (movie != null && movie.getId() == id) {
                return true;
            }
        }
        return false;
    }
}