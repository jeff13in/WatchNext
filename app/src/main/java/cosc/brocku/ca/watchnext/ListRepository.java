package cosc.brocku.ca.watchnext;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared entry list accessible by both ListsFragment and RecommendationsFragment.
 * ListsFragment updates this after loading from Supabase.
 * Falls back to empty list if Supabase hasn't loaded yet.
 */
public class ListRepository {

    private static List<ListEntry> allEntries = new ArrayList<>();

    public static List<ListEntry> getAllEntries() {
        return allEntries;
    }

    public static void setEntries(List<ListEntry> entries) {
        allEntries = entries != null ? entries : new ArrayList<>();
    }
}
