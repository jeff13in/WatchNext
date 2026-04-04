package cosc.brocku.ca.watchnext;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetailActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "watchnext_prefs";

    private int movieId;
    private String mediaType;

    private ImageView ivBackdrop, ivPoster;
    private TextView tvTitle, tvGenre, tvDate, tvRating, tvOverview;
    private Button btnWatchlist, btnLike, btnDislike, btnTrailer;

    private TmdbMovieDetail currentDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        ivBackdrop = findViewById(R.id.iv_backdrop);
        ivPoster = findViewById(R.id.iv_poster);
        tvTitle = findViewById(R.id.tv_detail_title);
        tvGenre = findViewById(R.id.tv_detail_genre);
        tvDate = findViewById(R.id.tv_detail_date);
        tvRating = findViewById(R.id.tv_detail_rating);
        tvOverview = findViewById(R.id.tv_detail_overview);
        btnWatchlist = findViewById(R.id.btn_add_watchlist);
        btnLike = findViewById(R.id.btn_like);
        btnDislike = findViewById(R.id.btn_dislike);
        btnTrailer = findViewById(R.id.btn_trailer);

        movieId = getIntent().getIntExtra("movie_id", -1);
        mediaType = getIntent().getStringExtra("media_type");
        if (mediaType == null || mediaType.isEmpty()) {
            mediaType = "movie";
        }

        loadDetails();

        btnWatchlist.setOnClickListener(v -> addToWatchlist());
        btnLike.setOnClickListener(v -> saveFeedback("liked"));
        btnDislike.setOnClickListener(v -> saveFeedback("disliked"));
        btnTrailer.setOnClickListener(v -> openTrailer());
    }

    private void loadDetails() {
        Callback<TmdbMovieDetail> callback = new Callback<TmdbMovieDetail>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieDetail> call,
                                   @NonNull Response<TmdbMovieDetail> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentDetail = response.body();
                    bindData(currentDetail);
                } else {
                    Toast.makeText(MovieDetailActivity.this, "Failed to load details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbMovieDetail> call, @NonNull Throwable t) {
                Toast.makeText(MovieDetailActivity.this, "Failed to load details", Toast.LENGTH_SHORT).show();
            }
        };

        if ("tv".equals(mediaType)) {
            TmdbClient.getService().getTvDetails(movieId, "videos").enqueue(callback);
        } else {
            TmdbClient.getService().getMovieDetails(movieId, "videos").enqueue(callback);
        }
    }

    private void bindData(TmdbMovieDetail detail) {
        tvTitle.setText(detail.getDisplayTitle());
        tvGenre.setText(detail.getGenreText());
        tvDate.setText(detail.getDisplayDate());
        tvRating.setText(String.format("★ %.1f", detail.getVoteAverage()));
        tvOverview.setText(detail.getOverview());

        loadImage(detail.getPosterUrl(), ivPoster);
        loadImage(detail.getBackdropUrl(), ivBackdrop);
    }

    private void loadImage(String url, ImageView imageView) {
        if (url == null) return;

        new Thread(() -> {
            try {
                InputStream input = new URL(url).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                runOnUiThread(() -> imageView.setImageBitmap(bitmap));
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void addToWatchlist() {
        if (currentDetail == null) return;

        UserSession session = UserSession.get();
        if (!session.isLoaded()) {
            Toast.makeText(this, "Please wait, loading user session...", Toast.LENGTH_SHORT).show();
            return;
        }

        btnWatchlist.setEnabled(false);
        SupabaseClient.addToWatchlist(
                session.getSupabaseUserId(),
                String.valueOf(currentDetail.getId()),
                currentDetail.getDisplayTitle(),
                currentDetail.getPosterUrl(),
                mediaType,
                new SupabaseClient.Callback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            btnWatchlist.setEnabled(true);
                            Toast.makeText(MovieDetailActivity.this, "Added to watchlist", Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> {
                            btnWatchlist.setEnabled(true);
                            Toast.makeText(MovieDetailActivity.this, "Failed to add: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
        );
    }

    private void saveFeedback(String value) {
        if (currentDetail == null) {
            Toast.makeText(this, "Movie details not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        prefs.edit()
                .putString("feedback_" + currentDetail.getId(), value)
                .putString("feedback_title_" + currentDetail.getId(), currentDetail.getDisplayTitle())
                .apply();

        Toast.makeText(this, currentDetail.getDisplayTitle() + " marked as " + value, Toast.LENGTH_SHORT).show();
    }

    private void openTrailer() {
        if (currentDetail == null || currentDetail.getVideos() == null) {
            Toast.makeText(this, "No trailer found", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = currentDetail.getVideos().getFirstTrailerUrl();
        if (url == null) {
            Toast.makeText(this, "No trailer found", Toast.LENGTH_SHORT).show();
            return;
        }

        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}