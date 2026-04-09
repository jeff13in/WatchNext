package cosc.brocku.ca.watchnext;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Handler;
import android.os.Looper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListsFragment extends Fragment implements ListEntryAdapter.OnEntryChangedListener {

    private List<ListEntry> allEntries;
    private ListEntryAdapter adapter;
    private String currentTab = "Watchlist";
    private String searchQuery = "";
    private final List<String> customPlaylists = new ArrayList<>();
    private TabLayout tabLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        allEntries = new ArrayList<>();

        tabLayout = view.findViewById(R.id.tab_lists);
        tabLayout.addTab(tabLayout.newTab().setText("Watchlist"));
        tabLayout.addTab(tabLayout.newTab().setText("Finished"));

        RecyclerView rv = view.findViewById(R.id.rv_lists);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ListEntryAdapter(getFiltered(), this, entry -> {
            if (entry.getMovieId() != null && !entry.getMovieId().isEmpty()) {
                Intent intent = new Intent(requireContext(), MovieDetailActivity.class);
                intent.putExtra("movie_id", Integer.parseInt(entry.getMovieId()));
                intent.putExtra("media_type", entry.getType().equals("TV Show") ? "tv" : "movie");
                startActivity(intent);
            }
        }, getChildFragmentManager());
        rv.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getText() != null ? tab.getText().toString() : "Watchlist";
                refreshList();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        SearchView searchView = view.findViewById(R.id.search_lists);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText;
                refreshList();
                return true;
            }
        });

        FloatingActionButton fab = view.findViewById(R.id.fab_add_playlist);
        fab.setOnClickListener(v -> showAddPlaylistDialog());

        loadWatchlistFromSupabase(view);
    }

    private int sessionRetries = 0;

    private void loadWatchlistFromSupabase(View view) {
        if (!UserSession.get().isLoaded()) {
            if (sessionRetries < 5) {
                sessionRetries++;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isAdded()) loadWatchlistFromSupabase(view);
                }, 2000);
            } else if (isAdded()) {
                Toast.makeText(requireContext(),
                        "Could not load session. Please sign out and back in.",
                        Toast.LENGTH_LONG).show();
            }
            return;
        }
        sessionRetries = 0;

        ProgressBar progressBar = view.findViewById(R.id.progress_lists);
        TextView emptyView = view.findViewById(R.id.tv_lists_empty);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (emptyView != null) emptyView.setVisibility(View.GONE);

        int userId = UserSession.get().getSupabaseUserId();
        SupabaseClient.getWatchlist(userId, new SupabaseClient.ListCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                List<ListEntry> loaded = new ArrayList<>();
                for (int i = 0; i < data.length(); i++) {
                    try {
                        JSONObject item = data.getJSONObject(i);
                        String title      = item.optString("title", "Unknown");
                        String mediaType  = item.optString("media_type", "movie");
                        String status     = item.optString("status", "Watching");
                        String movieId    = item.optString("movie_id", "");
                        int supabaseId    = item.optInt("id", -1);
                        String addedAt    = item.optString("added_at", "");
                        String watchedAt  = item.optString("watched_at", "");

                        // Show watched_at for Finished, added_at for Watching
                        String rawDate = (status.equals("Finished") && !watchedAt.isEmpty())
                                ? watchedAt : addedAt;
                        String label   = status.equals("Finished") ? "Finished" : "Added";
                        String displayDate = rawDate.isEmpty() ? "" : label + " " + DateUtils.formatDate(rawDate);

                        String typeLabel = mediaType.equalsIgnoreCase("tv") ? "TV Show" : "Movie";
                        ListEntry entry = new ListEntry(title, typeLabel, status, -1);
                        entry.setMovieId(movieId);
                        entry.setSupabaseId(supabaseId);
                        entry.setDisplayDate(displayDate);
                        loaded.add(entry);
                    } catch (Exception ignored) {}
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        allEntries = loaded;
                        ListRepository.setEntries(loaded);
                        refreshList();
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (emptyView != null) {
                            emptyView.setVisibility(allEntries.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                        fetchEpisodeCounts(loaded);
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(),
                                "Failed to load watchlist: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /** Fetches total episode count from TMDB for each TV show entry. */
    private void fetchEpisodeCounts(List<ListEntry> entries) {
        for (ListEntry entry : entries) {
            if (!entry.getType().equals("TV Show")) continue;
            if (entry.getMovieId() == null || entry.getMovieId().isEmpty()) continue;
            int tvId;
            try { tvId = Integer.parseInt(entry.getMovieId()); } catch (NumberFormatException e) { continue; }

            TmdbClient.getService().getTvDetails(tvId, "").enqueue(new Callback<TmdbMovieDetail>() {
                @Override
                public void onResponse(@androidx.annotation.NonNull Call<TmdbMovieDetail> call,
                                       @androidx.annotation.NonNull Response<TmdbMovieDetail> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null) {
                        int total = response.body().getNumberOfEpisodes();
                        if (total > 0) {
                            entry.setTotalEpisodes(total);
                            getActivity().runOnUiThread(this::refresh);
                        }
                    }
                }
                @Override
                public void onFailure(@androidx.annotation.NonNull Call<TmdbMovieDetail> call,
                                      @androidx.annotation.NonNull Throwable t) {}

                private void refresh() {
                    if (isAdded()) refreshList();
                }
            });
        }
    }

    private void showAddPlaylistDialog() {
        EditText input = new EditText(requireContext());
        input.setHint(getString(R.string.playlist_name_hint));
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.add_playlist))
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        customPlaylists.add(name);
                        tabLayout.addTab(tabLayout.newTab().setText(name));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private List<ListEntry> getFiltered() {
        String statusFilter = currentTab.equals("Watchlist") ? "Watching" : currentTab;
        List<ListEntry> result = new ArrayList<>();
        for (ListEntry e : allEntries) {
            boolean matchesTab = e.getStatus().equals(statusFilter);
            boolean matchesSearch = searchQuery.isEmpty() ||
                    e.getTitle().toLowerCase().contains(searchQuery.toLowerCase());
            if (matchesTab && matchesSearch) result.add(e);
        }
        return result;
    }

    private void refreshList() {
        adapter.updateEntries(getFiltered());
    }


    @Override
    public void onEntryChanged() {
        refreshList();
    }
}
