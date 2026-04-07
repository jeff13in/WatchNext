package cosc.brocku.ca.watchnext;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ListEntryAdapter extends RecyclerView.Adapter<ListEntryAdapter.ViewHolder> {

    private List<ListEntry> entries;
    private final OnEntryChangedListener listener;
    private final OnEntryClickListener clickListener;

    public interface OnEntryChangedListener {
        void onEntryChanged();
    }

    public interface OnEntryClickListener {
        void onEntryClick(ListEntry entry);
    }

    public ListEntryAdapter(List<ListEntry> entries, OnEntryChangedListener listener) {
        this(entries, listener, null);
    }

    public ListEntryAdapter(List<ListEntry> entries, OnEntryChangedListener listener,
                            OnEntryClickListener clickListener) {
        this.entries = entries;
        this.listener = listener;
        this.clickListener = clickListener;
    }

    public void updateEntries(List<ListEntry> newEntries) {
        this.entries = newEntries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ListEntry entry = entries.get(position);
        holder.title.setText(entry.getTitle());
        holder.type.setText(entry.getType());
        holder.status.setText(entry.getStatusDisplay());

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onEntryClick(entry);
        });

        // Update button: for movies toggles Watching↔Finished; for TV shows advances episode
        holder.btnUpdate.setOnClickListener(v -> {
            if (entry.getType().equals("TV Show")) {
                entry.setEpisode(entry.getEpisode() + 1);
                holder.status.setText(entry.getStatusDisplay());
                // Episode progress not stored in Supabase schema yet — local only
            } else {
                String newStatus = entry.getStatus().equals("Watching") ? "Finished" : "Watching";
                entry.setStatus(newStatus);
                entry.setPlaylist(newStatus);
                holder.status.setText(entry.getStatusDisplay());
                syncStatusToSupabase(v, entry, newStatus);
            }
            listener.onEntryChanged();
        });

        // Edit button: remove or move between tabs
        holder.btnEdit.setOnClickListener(v -> {
            String[] options = {"Remove from list", "Move to Watching", "Move to Finished"};
            new AlertDialog.Builder(v.getContext())
                    .setTitle(entry.getTitle())
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0: // Remove from list
                                removeEntry(v, holder, entry);
                                break;
                            case 1: // Move to Watching
                                entry.setStatus("Watching");
                                entry.setPlaylist("Watching");
                                listener.onEntryChanged();
                                syncStatusToSupabase(v, entry, "Watching");
                                break;
                            case 2: // Move to Finished
                                entry.setStatus("Finished");
                                entry.setPlaylist("Finished");
                                listener.onEntryChanged();
                                syncStatusToSupabase(v, entry, "Finished");
                                addToWatchHistory(entry);
                                break;
                        }
                    }).show();
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    // ── Supabase sync helpers ─────────────────────────────────────────────────

    private void syncStatusToSupabase(View v, ListEntry entry, String newStatus) {
        if (entry.getSupabaseId() == -1) return;

        SupabaseClient.updateWatchlistStatus(entry.getSupabaseId(), newStatus,
                new SupabaseClient.Callback() {
                    @Override public void onSuccess() {}

                    @Override
                    public void onFailure(String error) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                Toast.makeText(v.getContext(),
                                        "Sync failed: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void removeEntry(View v, ViewHolder holder, ListEntry entry) {
        if (entry.getSupabaseId() == -1) {
            // Not from Supabase — just remove locally
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) {
                entries.remove(pos);
                notifyItemRemoved(pos);
            }
            listener.onEntryChanged();
            return;
        }

        SupabaseClient.removeFromWatchlist(entry.getSupabaseId(),
                new SupabaseClient.Callback() {
                    @Override
                    public void onSuccess() {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            int pos = holder.getAdapterPosition();
                            if (pos != RecyclerView.NO_ID) {
                                entries.remove(pos);
                                notifyItemRemoved(pos);
                            }
                            listener.onEntryChanged();
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                Toast.makeText(v.getContext(),
                                        "Failed to remove: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void addToWatchHistory(ListEntry entry) {
        UserSession session = UserSession.get();
        if (!session.isLoaded()) return;
        if (entry.getMovieId() == null || entry.getMovieId().isEmpty()) return;

        String mediaType = entry.getType().equals("TV Show") ? "tv" : "movie";

        SupabaseClient.addToWatchHistory(
                session.getSupabaseUserId(),
                entry.getMovieId(),
                entry.getTitle(),
                mediaType,
                "",
                new SupabaseClient.Callback() {
                    @Override public void onSuccess() {}
                    @Override public void onFailure(String error) {}
                });
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView type;
        TextView status;
        ImageButton btnUpdate;
        ImageButton btnEdit;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_entry_title);
            type = itemView.findViewById(R.id.tv_entry_type);
            status = itemView.findViewById(R.id.tv_entry_status);
            btnUpdate = itemView.findViewById(R.id.btn_update);
            btnEdit = itemView.findViewById(R.id.btn_edit);
        }
    }
}
