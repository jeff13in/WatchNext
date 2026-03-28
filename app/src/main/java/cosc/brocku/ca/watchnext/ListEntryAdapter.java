package cosc.brocku.ca.watchnext;

import android.app.AlertDialog;
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

    public interface OnEntryChangedListener {
        void onEntryChanged();
    }

    public ListEntryAdapter(List<ListEntry> entries, OnEntryChangedListener listener) {
        this.entries = entries;
        this.listener = listener;
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

        holder.btnUpdate.setOnClickListener(v -> {
            if (entry.getType().equals("TV Show")) {
                entry.setEpisode(entry.getEpisode() + 1);
            } else {
                String newStatus = entry.getStatus().equals("Watching") ? "Finished" : "Watching";
                entry.setStatus(newStatus);
                entry.setPlaylist(newStatus);
            }
            holder.status.setText(entry.getStatusDisplay());
            listener.onEntryChanged();
        });

        holder.btnEdit.setOnClickListener(v -> {
            String[] options = {"Remove from list", "Move to Watching", "Move to Finished"};
            new AlertDialog.Builder(v.getContext())
                    .setTitle(entry.getTitle())
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                entries.remove(position);
                                notifyItemRemoved(position);
                                listener.onEntryChanged();
                                break;
                            case 1:
                                entry.setStatus("Watching");
                                entry.setPlaylist("Watching");
                                listener.onEntryChanged();
                                break;
                            case 2:
                                entry.setStatus("Finished");
                                entry.setPlaylist("Finished");
                                listener.onEntryChanged();
                                break;
                        }
                    }).show();
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

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
