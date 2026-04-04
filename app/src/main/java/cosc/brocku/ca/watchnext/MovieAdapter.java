package cosc.brocku.ca.watchnext;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.ViewHolder> {

    private final List<TmdbMovie> movies;

    // OkHttpClient with trust-all SSL — needed for API 23 emulator's outdated cert store
    private static final OkHttpClient imageClient = buildImageClient();

    private static OkHttpClient buildImageClient() {
        try {
            X509TrustManager trustAll = new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{trustAll}, new SecureRandom());
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sc.getSocketFactory(), trustAll)
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(8, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MovieAdapter(List<TmdbMovie> movies) {
        this.movies = movies;
    }

    public void updateMovies(List<TmdbMovie> newMovies) {
        movies.clear();
        movies.addAll(newMovies);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movie_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TmdbMovie movie = movies.get(position);

        holder.title.setText(movie.getDisplayTitle());
        holder.genre.setText(movie.getDisplayGenre());
        holder.rating.setText(String.format("★ %.1f", movie.getVoteAverage()));

        holder.btnAddWatchlist.setOnClickListener(v -> {
            int userId = UserSession.get().getSupabaseUserId();
            if (userId == -1) {
                Toast.makeText(v.getContext(), "Please wait, loading profile...", Toast.LENGTH_SHORT).show();
                return;
            }
            Handler main = new Handler(Looper.getMainLooper());
            holder.btnAddWatchlist.setEnabled(false);
            SupabaseClient.addToWatchlist(
                userId,
                String.valueOf(movie.getId()),
                movie.getDisplayTitle(),
                movie.getPosterUrl(),
                movie.getMediaType() != null ? movie.getMediaType() : "movie",
                new SupabaseClient.Callback() {
                    @Override public void onSuccess() {
                        main.post(() -> {
                            Toast.makeText(v.getContext(), "Added to watchlist!", Toast.LENGTH_SHORT).show();
                            holder.btnAddWatchlist.setText("✓ Added");
                        });
                    }
                    @Override public void onFailure(String error) {
                        Log.e("Supabase", "addToWatchlist failed: " + error);
                        main.post(() -> {
                            holder.btnAddWatchlist.setEnabled(true);
                            Toast.makeText(v.getContext(), "Failed: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            );
        });

        String posterUrl = movie.getPosterUrl();
        holder.poster.setImageBitmap(null);
        holder.poster.setBackgroundColor(
                holder.itemView.getContext().getColor(R.color.genre_default));

        if (posterUrl == null) return;

        new Thread(() -> {
            try {
                Request request = new Request.Builder().url(posterUrl).build();
                Response response = imageClient.newCall(request).execute();
                if (response.body() != null) {
                    byte[] bytes = response.body().bytes();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    holder.itemView.post(() -> {
                        if (bitmap != null) holder.poster.setImageBitmap(bitmap);
                    });
                }
                response.close();
            } catch (Exception e) {
                Log.e("MovieAdapter", "Load failed: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView poster;
        TextView title;
        TextView genre;
        TextView rating;
        Button btnAddWatchlist;

        ViewHolder(View itemView) {
            super(itemView);
            poster           = itemView.findViewById(R.id.view_poster);
            title            = itemView.findViewById(R.id.tv_title);
            genre            = itemView.findViewById(R.id.tv_genre);
            rating           = itemView.findViewById(R.id.tv_rating);
            btnAddWatchlist  = itemView.findViewById(R.id.btn_add_watchlist);
        }
    }
}
