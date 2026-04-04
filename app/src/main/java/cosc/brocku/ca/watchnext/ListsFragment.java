package cosc.brocku.ca.watchnext;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ListsFragment extends Fragment implements ListEntryAdapter.OnEntryChangedListener {

    private List<ListEntry> allEntries;
    private ListEntryAdapter adapter;
    private String currentTab = "Watching";
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
        buildEntries();

        tabLayout = view.findViewById(R.id.tab_lists);
        tabLayout.addTab(tabLayout.newTab().setText("Watching"));
        tabLayout.addTab(tabLayout.newTab().setText("Finished"));

        RecyclerView rv = view.findViewById(R.id.rv_lists);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ListEntryAdapter(getFiltered(), this);
        rv.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getText() != null ? tab.getText().toString() : "Watching";
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

        loadWatchlistFromSupabase();
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
        List<ListEntry> result = new ArrayList<>();
        for (ListEntry e : allEntries) {
            boolean matchesTab = e.getPlaylist().equals(currentTab) || e.getStatus().equals(currentTab);
            boolean matchesSearch = searchQuery.isEmpty() ||
                    e.getTitle().toLowerCase().contains(searchQuery.toLowerCase());

            if (matchesTab && matchesSearch) {
                result.add(e);
            }
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

    private void buildEntries() {
        allEntries = new ArrayList<>();
    }

    private void loadWatchlistFromSupabase() {
        UserSession session = UserSession.get();
        if (!session.isLoaded()) {
            Toast.makeText(requireContext(), "Loading user session...", Toast.LENGTH_SHORT).show();
            return;
        }

        SupabaseClient.getWatchlist(session.getSupabaseUserId(), new SupabaseClient.ListCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                List<ListEntry> loaded = new ArrayList<>();
                for (int i = 0; i < data.length(); i++) {
                    try {
                        JSONObject item = data.getJSONObject(i);
                        String title = item.optString("title", "Unknown");
                        String mediaType = item.optString("media_type", "Movie");
                        String status = item.optString("status", "Watching");
                        int id = item.optInt("id", -1);
                        loaded.add(new ListEntry(title, mediaType, status, -1, status));
                    } catch (Exception ignored) {}
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allEntries.clear();
                        allEntries.addAll(loaded);
                        refreshList();
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Failed to load watchlist: " + error, Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
}