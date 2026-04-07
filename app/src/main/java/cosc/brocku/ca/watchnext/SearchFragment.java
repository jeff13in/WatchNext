package cosc.brocku.ca.watchnext;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private MovieAdapter adapter;
    private TextView tvSearchLabel;

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

        tvSearchLabel = view.findViewById(R.id.tv_search_label);

        RecyclerView rv = view.findViewById(R.id.rv_search_results);
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

        loadDefault();

        SearchView searchView = view.findViewById(R.id.search_all);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (tvSearchLabel != null) tvSearchLabel.setText("Results");
                search(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    loadDefault();
                } else if (newText.length() >= 2) {
                    if (tvSearchLabel != null) tvSearchLabel.setText("Results");
                    search(newText);
                }
                return true;
            }
        });
    }

    private void loadDefault() {
        if (tvSearchLabel != null) tvSearchLabel.setText("Popular");
        TmdbClient.getService().getPopularMovies().enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<TmdbMovie> results = response.body().getResults();
                    for (TmdbMovie item : results) {
                        if (item.getMediaType() == null || item.getMediaType().isEmpty()) {
                            item.setMediaType("movie");
                        }
                    }
                    adapter.updateMovies(results);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                // silent fail for default content
            }
        });
    }

    private void search(String query) {
        TmdbClient.getService().searchMulti(query).enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<TmdbMovie> results = new ArrayList<>();
                    for (TmdbMovie item : response.body().getResults()) {
                        String type = item.getMediaType();
                        if ("movie".equals(type) || "tv".equals(type)) {
                            results.add(item);
                        }
                    }
                    adapter.updateMovies(results);
                } else {
                    Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
