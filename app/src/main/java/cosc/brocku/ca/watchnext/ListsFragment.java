package cosc.brocku.ca.watchnext;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ListsFragment extends Fragment implements ListEntryAdapter.OnEntryChangedListener {

    private List<ListEntry> allEntries = new ArrayList<>();
    private ListEntryAdapter adapter;
    private String currentTab = "Watching";
    private String searchQuery = "";

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

        TabLayout tabLayout = view.findViewById(R.id.tab_lists);
        tabLayout.addTab(tabLayout.newTab().setText("Watching"));
        tabLayout.addTab(tabLayout.newTab().setText("Finished"));
        tabLayout.addTab(tabLayout.newTab().setText("Plan to Watch"));

        RecyclerView rv = view.findViewById(R.id.rv_lists);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ListEntryAdapter(new ArrayList<>(), this);
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
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText;
                refreshList();
                return true;
            }
        });

        loadWatchlist();
    }

    private void loadWatchlist() {
        int userId = UserSession.get().getSupabaseUserId();
        if (userId == -1) {
            Toast.makeText(requireContext(), "Loading profile...", Toast.LENGTH_SHORT).show();
            return;
        }

        SupabaseClient.getWatchlist(userId, new SupabaseClient.ListCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                allEntries.clear();
                for (int i = 0; i < data.length(); i++) {
                    try {
                        JSONObject obj = data.getJSONObject(i);
                        allEntries.add(new ListEntry(
                            obj.getInt("id"),
                            obj.getString("movie_id"),
                            obj.getString("title"),
                            obj.getString("media_type"),
                            obj.getString("status")
                        ));
                    } catch (Exception ignored) {}
                }
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> refreshList());
                }
            }
            @Override
            public void onFailure(String error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Failed to load watchlist", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private List<ListEntry> getFiltered() {
        List<ListEntry> result = new ArrayList<>();
        for (ListEntry e : allEntries) {
            boolean matchesTab    = e.getPlaylist().equals(currentTab);
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
