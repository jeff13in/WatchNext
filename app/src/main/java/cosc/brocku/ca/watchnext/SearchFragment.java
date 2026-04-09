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
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private MovieAdapter adapter;
    private View layoutEmptyState;
    private RecyclerView rv;
    private TextView tvEmptyHint;

    // "movie", "tv", or "person"
    private String selectedCategory = "movie";
    private String currentQuery = "";

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

        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        tvEmptyHint      = view.findViewById(R.id.tv_empty_hint);
        rv               = view.findViewById(R.id.rv_search_results);

        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new MovieAdapter(new ArrayList<>(), movie -> {
            if ("person".equals(movie.getMediaType())) {
                Toast.makeText(requireContext(), movie.getDisplayTitle(), Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(requireContext(), MovieDetailActivity.class);
            intent.putExtra("movie_id", movie.getId());
            String mediaType = movie.getMediaType();
            intent.putExtra("media_type", (mediaType == null || mediaType.isEmpty()) ? "movie" : mediaType);
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        // Category chips
        ChipGroup chipGroup = view.findViewById(R.id.chipgroup_category);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if      (id == R.id.chip_movies) { selectedCategory = "movie";  updateEmptyHint(); }
            else if (id == R.id.chip_tv)     { selectedCategory = "tv";     updateEmptyHint(); }
            else if (id == R.id.chip_people) { selectedCategory = "person"; updateEmptyHint(); }
            if (!currentQuery.isEmpty()) search(currentQuery);
        });

        // Search view
        SearchView searchView = view.findViewById(R.id.search_all);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query.trim();
                if (!currentQuery.isEmpty()) search(currentQuery);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText.trim();
                if (currentQuery.isEmpty()) {
                    showEmptyState();
                } else if (currentQuery.length() >= 2) {
                    search(currentQuery);
                }
                return true;
            }
        });

        showEmptyState();
    }

    private void search(String query) {
        TmdbClient.getService().searchMulti(query).enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<TmdbMovie> filtered = new ArrayList<>();
                    for (TmdbMovie item : response.body().getResults()) {
                        if (selectedCategory.equals(item.getMediaType())) {
                            filtered.add(item);
                        }
                    }
                    if (filtered.isEmpty()) {
                        tvEmptyHint.setText("No results found for \"" + query + "\"");
                        showEmptyState();
                    } else {
                        showResults(filtered);
                    }
                } else {
                    Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {
                if (isAdded()) Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmptyState() {
        layoutEmptyState.setVisibility(View.VISIBLE);
        rv.setVisibility(View.GONE);
        updateEmptyHint();
    }

    private void showResults(List<TmdbMovie> results) {
        layoutEmptyState.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);
        adapter.updateMovies(results);
    }

    private void updateEmptyHint() {
        if (currentQuery.isEmpty()) {
            switch (selectedCategory) {
                case "tv":     tvEmptyHint.setText("Search for a TV show…");  break;
                case "person": tvEmptyHint.setText("Search for a person…");   break;
                default:       tvEmptyHint.setText("Search for a movie or show…"); break;
            }
        }
    }
}
