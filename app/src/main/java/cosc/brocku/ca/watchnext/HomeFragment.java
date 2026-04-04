package cosc.brocku.ca.watchnext;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
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

    private static final String[] GENRE_NAMES = {
            "Action", "Drama", "Sci-Fi", "Fantasy", "Crime", "Comedy", "Horror", "Romance"
    };
    private static final int[] MOVIE_GENRE_IDS = {28, 18, 878, 14, 80, 35, 27, 10749};
    private static final int[] TV_GENRE_IDS    = {10759, 18, 10765, 10765, 80, 35, 27, 10749};

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

        TabLayout tabs = view.findViewById(R.id.tab_home);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos == 0) {
                    loadPopularMovies();
                } else if (pos == 1) {
                    loadPopularTvShows();
                } else {
                    showCategoryDialog();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 2) showCategoryDialog();
            }
        });
    }

    private void loadPopularMovies() {
        TmdbClient.getService().getPopularMovies().enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.updateMovies(response.body().getResults());
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load movies", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load movies", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadPopularTvShows() {
        TmdbClient.getService().getPopularTvShows().enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.updateMovies(response.body().getResults());
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load TV shows", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load TV shows", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showCategoryDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Genre")
                .setItems(GENRE_NAMES, (dialog, which) -> {
                    int movieGenreId = MOVIE_GENRE_IDS[which];
                    int tvGenreId = TV_GENRE_IDS[which];
                    loadByGenre(movieGenreId, tvGenreId);
                })
                .show();
    }

    private void loadByGenre(int movieGenreId, int tvGenreId) {
        final List<TmdbMovie> combined = new ArrayList<>();

        TmdbClient.getService().discoverMovies(movieGenreId).enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    combined.addAll(response.body().getResults());
                }

                TmdbClient.getService().discoverTv(tvGenreId).enqueue(new Callback<TmdbResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbResponse> call2,
                                           @NonNull Response<TmdbResponse> response2) {
                        if (response2.isSuccessful() && response2.body() != null) {
                            combined.addAll(response2.body().getResults());
                        }
                        if (isAdded()) {
                            adapter.updateMovies(combined);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbResponse> call2, @NonNull Throwable t) {
                        if (isAdded()) {
                            adapter.updateMovies(combined);
                        }
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load genre", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}