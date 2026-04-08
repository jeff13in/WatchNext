package cosc.brocku.ca.watchnext;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.datepicker.MaterialDatePicker;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ListEntryAdapter extends RecyclerView.Adapter<ListEntryAdapter.ViewHolder> {

    private List<ListEntry> entries;
    private final OnEntryChangedListener listener;
    private final OnEntryClickListener clickListener;
    private final FragmentManager fragmentManager;

    public interface OnEntryChangedListener {
        void onEntryChanged();
    }

    public interface OnEntryClickListener {
        void onEntryClick(ListEntry entry);
    }

    public ListEntryAdapter(List<ListEntry> entries, OnEntryChangedListener listener,
                            OnEntryClickListener clickListener, FragmentManager fragmentManager) {
        this.entries = entries;
        this.listener = listener;
        this.fragmentManager = fragmentManager;
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

        // Date
        String date = entry.getDisplayDate();
        if (date != null && !date.isEmpty()) {
            holder.tvDate.setText(date);
            holder.tvDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvDate.setVisibility(View.GONE);
        }

        // ── Status indicator color ────────────────────────────────────────────
        int indicatorColor;
        switch (entry.getStatus()) {
            case "Finished":  indicatorColor = R.color.btn_watched;   break;
            case "Liked":     indicatorColor = R.color.btn_like;      break;
            case "Disliked":  indicatorColor = R.color.btn_dislike;   break;
            default:          indicatorColor = R.color.btn_watchlist; break; // Watching
        }
        holder.statusIndicator.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.getContext(), indicatorColor));

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onEntryClick(entry);
        });

        boolean isTvShow = entry.getType().equals("TV Show");
        boolean isFinished = entry.getStatus().equals("Finished");
        boolean isWatching = entry.getStatus().equals("Watching");

        // ── Load persisted episode count ─────────────────────────────────────
        if (isTvShow && entry.getSupabaseId() != -1) {
            SharedPreferences ep = holder.itemView.getContext()
                    .getSharedPreferences("ep_progress", Context.MODE_PRIVATE);
            int saved = ep.getInt("ep_" + entry.getSupabaseId(), 0);
            if (saved != entry.getEpisode()) {
                entry.setEpisode(saved);
                holder.status.setText(entry.getStatusDisplay());
            }
        }

        // ── Episode button (TV shows only, disabled when Finished) ──────────
        if (isTvShow && isWatching) {
            holder.btnEpisode.setVisibility(View.VISIBLE);
            holder.btnEpisode.setEnabled(true);
            holder.btnEpisode.setAlpha(1f);
            holder.btnEpisode.setOnClickListener(v -> {
                int next = entry.getEpisode() + 1;
                if (entry.getTotalEpisodes() > 0 && next > entry.getTotalEpisodes()) {
                    Toast.makeText(v.getContext(), "No more episodes!", Toast.LENGTH_SHORT).show();
                    return;
                }
                entry.setEpisode(next);
                holder.status.setText(entry.getStatusDisplay());
                // Persist episode progress
                if (entry.getSupabaseId() != -1) {
                    v.getContext().getSharedPreferences("ep_progress", Context.MODE_PRIVATE)
                            .edit().putInt("ep_" + entry.getSupabaseId(), next).apply();
                }
            });
        } else if (isTvShow && isFinished) {
            holder.btnEpisode.setVisibility(View.VISIBLE);
            holder.btnEpisode.setEnabled(false);
            holder.btnEpisode.setAlpha(0.3f);
        } else {
            // Movie — hide episode button
            holder.btnEpisode.setVisibility(View.GONE);
        }

        // ── Finish / Watching toggle ─────────────────────────────────────────
        if (isFinished) {
            // Show "undo" icon — move back to Watching
            holder.btnFinish.setImageResource(android.R.drawable.ic_menu_revert);
            holder.btnFinish.setContentDescription("Move to Watching");
        } else {
            // Show checkmark — mark as Finished
            holder.btnFinish.setImageResource(android.R.drawable.checkbox_on_background);
            holder.btnFinish.setContentDescription("Mark Finished");
        }

        // Hide finish button for Liked/Disliked tabs — only relevant for Watching/Finished
        boolean isRatingTab = entry.getStatus().equals("Liked") || entry.getStatus().equals("Disliked");
        holder.btnFinish.setVisibility(isRatingTab ? View.GONE : View.VISIBLE);

        holder.btnFinish.setOnClickListener(v -> {
            if (isFinished) {
                entry.setStatus("Watching");
                syncStatusToSupabase(v, entry, "Watching");
                listener.onEntryChanged();
            } else {
                showDatePickerForFinish(v, entry);
            }
        });

        // ── Remove button ────────────────────────────────────────────────────
        holder.btnRemove.setOnClickListener(v -> removeEntry(v, holder, entry));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    // ── Supabase sync helpers ─────────────────────────────────────────────────

    private void syncStatusToSupabaseWithDate(View v, ListEntry entry, String newStatus, String watchedAt) {
        if (entry.getSupabaseId() == -1) return;
        SupabaseClient.updateWatchlistStatus(entry.getSupabaseId(), newStatus, watchedAt,
                new SupabaseClient.Callback() {
                    @Override public void onSuccess() {}
                    @Override public void onFailure(String error) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                Toast.makeText(v.getContext(),
                                        "Sync failed: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void syncStatusToSupabase(View v, ListEntry entry, String newStatus) {
        if (entry.getSupabaseId() == -1) return;
        SupabaseClient.updateWatchlistStatus(entry.getSupabaseId(), newStatus,
                new SupabaseClient.Callback() {
                    @Override public void onSuccess() {}
                    @Override public void onFailure(String error) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                Toast.makeText(v.getContext(),
                                        "Sync failed: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void removeEntry(View v, ViewHolder holder, ListEntry entry) {
        if (entry.getSupabaseId() == -1) {
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
                        // Clear persisted episode progress
                        v.getContext().getSharedPreferences("ep_progress", Context.MODE_PRIVATE)
                                .edit().remove("ep_" + entry.getSupabaseId()).apply();
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

    private void showDatePickerForFinish(View v, ListEntry entry) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("When did you finish watching?")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        picker.show(fragmentManager, "finish_date_picker");
        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String dateStr   = sdf.format(new Date(selection));
            String watchedAt = dateStr + "T00:00:00";

            // Format for display: "Finished Apr 7, 2026"
            try {
                Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr);
                String formatted = new SimpleDateFormat("MMM d, yyyy", Locale.US).format(d);
                entry.setDisplayDate("Finished " + formatted);
            } catch (Exception ignored) {}

            entry.setStatus("Finished");
            syncStatusToSupabaseWithDate(v, entry, "Finished", watchedAt);
            addToWatchHistory(entry, watchedAt);
            listener.onEntryChanged();
        });
    }

    private void addToWatchHistory(ListEntry entry, String watchedAt) {
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
                watchedAt,
                new SupabaseClient.Callback() {
                    @Override public void onSuccess() {}
                    @Override public void onFailure(String error) {}
                });
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        View statusIndicator;
        TextView title;
        TextView type;
        TextView status;
        TextView tvDate;
        ImageButton btnEpisode;
        ImageButton btnFinish;
        ImageButton btnRemove;

        ViewHolder(View itemView) {
            super(itemView);
            statusIndicator = itemView.findViewById(R.id.view_status_indicator);
            title      = itemView.findViewById(R.id.tv_entry_title);
            type       = itemView.findViewById(R.id.tv_entry_type);
            status     = itemView.findViewById(R.id.tv_entry_status);
            tvDate     = itemView.findViewById(R.id.tv_entry_date);
            btnEpisode = itemView.findViewById(R.id.btn_episode);
            btnFinish  = itemView.findViewById(R.id.btn_finish);
            btnRemove  = itemView.findViewById(R.id.btn_remove);
        }
    }
}
