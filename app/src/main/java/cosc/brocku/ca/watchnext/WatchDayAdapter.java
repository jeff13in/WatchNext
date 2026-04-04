package cosc.brocku.ca.watchnext;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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
        StringBuilder sb = new StringBuilder("Watched: ");
        List<String> titles = day.getTitlesWatched();
        for (int i = 0; i < titles.size(); i++) {
            sb.append(titles.get(i));
            if (i < titles.size() - 1) sb.append(", ");
        }
        holder.watched.setText(sb.toString());
        holder.recommendation.setText(day.getRecommendation());
    }

    @Override
    public int getItemCount() {
        return watchDays.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView date;
        TextView watched;
        TextView recommendation;

        ViewHolder(View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.tv_date);
            watched = itemView.findViewById(R.id.tv_watched);
            recommendation = itemView.findViewById(R.id.tv_recommendation);
        }
    }
}
