package cosc.brocku.ca.watchnext;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
//Class that implements the XML file for Recommendation Fragment
public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.RecommendationViewHolder> {

    public interface OnWatchlistClickListener {
        void onAddToWatchlist(ProfileMovieShow item);
    }

    private Context context;
    private List<ScoredRec> recommendations;
    private double maxScore = 1.0; // used to normalise scores to 0-100%
    private OnWatchlistClickListener listener;

    public RecommendationAdapter(Context context, OnWatchlistClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.recommendations = new ArrayList<>();
    }

    public void setRecommendations(List<ScoredRec> recs) {
        this.recommendations = recs != null ? recs : new ArrayList<>();
        // Find the highest raw score so everything else is relative to it
        maxScore = 1.0;
        for (ScoredRec r : this.recommendations) {
            if (r.getScore() > maxScore) maxScore = r.getScore();
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecommendationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.media_recommendation, parent, false);
        return new RecommendationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendationViewHolder holder, int position) {
        ScoredRec rec = recommendations.get(position);
        ProfileMovieShow item = rec.getItem();

        holder.titleText.setText(item.getTitle());
        int matchPct = (int) Math.round((rec.getScore() / maxScore) * 100);
        holder.scoreText.setText(matchPct + "% Match");
        holder.explanationText.setText(rec.getExplanation());

        if (item.getSourceMovie() != null && item.getSourceMovie().getPosterUrl() != null) {
            Glide.with(context)
                    .load(item.getSourceMovie().getPosterUrl())
                    .into(holder.posterImage);
        } else {
            holder.posterImage.setImageDrawable(null);
        }

        holder.watchlistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onAddToWatchlist(item);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return recommendations.size();
    }

    static class RecommendationViewHolder extends RecyclerView.ViewHolder {
        ImageView posterImage;
        TextView titleText;
        TextView scoreText;
        TextView explanationText;
        Button watchlistButton;

        public RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImage = itemView.findViewById(R.id.iv_recommendation_poster);
            titleText = itemView.findViewById(R.id.tv_recommendation_title);
            scoreText = itemView.findViewById(R.id.tv_recommendation_score);
            explanationText = itemView.findViewById(R.id.tv_recommendation_explanation);
            watchlistButton = itemView.findViewById(R.id.btn_add_to_watchlist);
        }
    }
}