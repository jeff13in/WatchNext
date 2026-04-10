package cosc.brocku.ca.watchnext;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class WatchDayAdapter extends RecyclerView.Adapter<WatchDayAdapter.ViewHolder> {

    private final List<WatchDay> watchDays;

    public WatchDayAdapter(List<WatchDay> watchDays) {
        this.watchDays = watchDays;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_watch_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WatchDay day = watchDays.get(position);
        holder.date.setText(day.getDate());

        holder.chipGroupWatched.removeAllViews();

        List<String> titles = day.getTitlesWatched();
        if (titles != null) {
            for (String title : titles) {
                if (title == null || title.trim().isEmpty()) continue;

                Chip chip = new Chip(holder.itemView.getContext());
                chip.setText(title.trim());
                chip.setClickable(false);
                chip.setCheckable(false);
                chip.setCloseIconVisible(false);
                chip.setEnsureMinTouchTargetSize(false);

                holder.chipGroupWatched.addView(chip);
            }
        }
    }

    @Override
    public int getItemCount() {
        return watchDays.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView date;
        ChipGroup chipGroupWatched;

        ViewHolder(View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.tv_date);
            chipGroupWatched = itemView.findViewById(R.id.chip_group_watched);
        }
    }
}