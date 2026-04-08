package cosc.brocku.ca.watchnext;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.TimeZone;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetailActivity extends AppCompatActivity {

    private int movieId;
    private String mediaType;

    private ImageView ivBackdrop, ivPoster;
    private TextView tvTitle, tvGenre, tvDate, tvRating, tvOverview;
    private MaterialButton btnWatchlist, btnLike, btnDislike, btnTrailer, btnAlreadyWatched;
    private TextInputEditText etReview;
    private TextView tvReviewDisplay, tvReviewUpdated;
    private MaterialButton btnSaveReview, btnEditReview;
    private TextInputLayout tilReview;
    private TmdbMovieDetail currentDetail;
    private MovieAdapter similarAdapter;

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
        btnWatchlist     = findViewById(R.id.btn_add_watchlist);
        btnLike          = findViewById(R.id.btn_like);
        btnDislike       = findViewById(R.id.btn_dislike);
        btnTrailer       = findViewById(R.id.btn_trailer);
        btnAlreadyWatched = findViewById(R.id.btn_already_watched);

        movieId = getIntent().getIntExtra("movie_id", -1);
        mediaType = getIntent().getStringExtra("media_type");
        if (mediaType == null || mediaType.isEmpty()) {
            mediaType = "movie";
        }

        // Similar movies RecyclerView
        RecyclerView rvSimilar = findViewById(R.id.rv_similar);
        TextView tvSimilarHeader = findViewById(R.id.tv_similar_header);
        rvSimilar.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        similarAdapter = new MovieAdapter(new ArrayList<>(), movie -> {
            Intent intent = new Intent(this, MovieDetailActivity.class);
            intent.putExtra("movie_id", movie.getId());
            String mt = movie.getMediaType();
            intent.putExtra("media_type", (mt == null || mt.isEmpty()) ? mediaType : mt);
            startActivity(intent);
        });
        rvSimilar.setAdapter(similarAdapter);

        loadDetails();
        loadSimilar(rvSimilar, tvSimilarHeader);

        // Restore saved like/dislike state
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String uid = UserSession.get().getFirebaseUid();
        boolean hasUid = uid != null && !uid.isEmpty();
        String feedbackKey = hasUid
                ? "feedback_" + uid + "_" + movieId
                : "feedback_" + movieId;
        applyFeedbackState(prefs.getString(feedbackKey, null));

        btnWatchlist.setOnClickListener(v -> addToWatchlist());
        btnLike.setOnClickListener(v -> saveFeedback("liked"));
        btnDislike.setOnClickListener(v -> saveFeedback("disliked"));
        btnTrailer.setOnClickListener(v -> openTrailer());
        btnAlreadyWatched.setOnClickListener(v -> markAsWatched());

        // Review
        etReview        = findViewById(R.id.et_review);
        tvReviewDisplay = findViewById(R.id.tv_review_display);
        tvReviewUpdated = findViewById(R.id.tv_review_updated);
        btnSaveReview   = findViewById(R.id.btn_save_review);
        btnEditReview   = findViewById(R.id.btn_edit_review);
        tilReview       = findViewById(R.id.til_review);

        btnSaveReview.setOnClickListener(v -> saveReview());
        btnEditReview.setOnClickListener(v -> showEditMode());
        loadReview();
    }

    // ── Review ────────────────────────────────────────────────────────────────

    private void loadReview() {
        UserSession session = UserSession.get();
        if (!session.isLoaded()) return;

        SupabaseClient.getReview(session.getSupabaseUserId(), String.valueOf(movieId),
                new SupabaseClient.ListCallback() {
                    @Override
                    public void onSuccess(JSONArray data) {
                        if (data.length() == 0) return;
                        try {
                            JSONObject row    = data.getJSONObject(0);
                            String text       = row.optString("review_text", "");
                            String updatedAt  = row.optString("updated_at", "");
                            if (!text.isEmpty()) {
                                runOnUiThread(() -> {
                                    etReview.setText(text);
                                    showReadMode(text, updatedAt);
                                });
                            }
                        } catch (Exception ignored) {}
                    }
                    @Override public void onFailure(String error) {}
                });
    }

    private void saveReview() {
        UserSession session = UserSession.get();
        if (!session.isLoaded()) {
            Toast.makeText(this, "Please wait, loading session…", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentDetail == null) {
            Toast.makeText(this, "Details not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = etReview.getText() != null ? etReview.getText().toString().trim() : "";
        if (text.isEmpty()) {
            Toast.makeText(this, "Review cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveReview.setEnabled(false);

        SupabaseClient.upsertReview(
                session.getSupabaseUserId(),
                String.valueOf(currentDetail.getId()),
                currentDetail.getDisplayTitle(),
                mediaType,
                text,
                new SupabaseClient.Callback() {
                    @Override
                    public void onSuccess() {
                        String nowIso = new SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());
                        runOnUiThread(() -> {
                            btnSaveReview.setEnabled(true);
                            showReadMode(text, nowIso);
                            Toast.makeText(MovieDetailActivity.this,
                                    "Review saved!", Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> {
                            btnSaveReview.setEnabled(true);
                            Toast.makeText(MovieDetailActivity.this,
                                    "Failed to save: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    /** Switch to read-only mode: show styled text, hide input. */
    private void showReadMode(String text, String updatedAt) {
        tvReviewDisplay.setText(text);
        tvReviewDisplay.setVisibility(View.VISIBLE);
        tilReview.setVisibility(View.GONE);
        btnSaveReview.setVisibility(View.GONE);
        btnEditReview.setVisibility(View.VISIBLE);
        if (!updatedAt.isEmpty()) {
            tvReviewUpdated.setText("Edited " + DateUtils.formatDate(updatedAt));
            tvReviewUpdated.setVisibility(View.VISIBLE);
        }
    }

    /** Switch to edit mode: show input, hide text block. */
    private void showEditMode() {
        tvReviewDisplay.setVisibility(View.GONE);
        tilReview.setVisibility(View.VISIBLE);
        btnSaveReview.setVisibility(View.VISIBLE);
        btnEditReview.setVisibility(View.GONE);
        etReview.requestFocus();
    }


    /** Updates Like/Dislike button visuals based on current feedback value. */
    private void applyFeedbackState(String value) {
        int green       = getResources().getColor(R.color.btn_like,    getTheme());
        int red         = getResources().getColor(R.color.btn_dislike, getTheme());
        int transparent = Color.TRANSPARENT;
        int white       = Color.WHITE;

        if ("liked".equals(value)) {
            // Like → filled green
            btnLike.setBackgroundTintList(ColorStateList.valueOf(green));
            btnLike.setTextColor(white);
            btnLike.setIconTint(ColorStateList.valueOf(white));
            btnLike.setStrokeColor(ColorStateList.valueOf(green));
            // Dislike → outlined red (reset)
            btnDislike.setBackgroundTintList(ColorStateList.valueOf(transparent));
            btnDislike.setTextColor(red);
            btnDislike.setIconTint(ColorStateList.valueOf(red));
            btnDislike.setStrokeColor(ColorStateList.valueOf(red));

        } else if ("disliked".equals(value)) {
            // Dislike → filled red
            btnDislike.setBackgroundTintList(ColorStateList.valueOf(red));
            btnDislike.setTextColor(white);
            btnDislike.setIconTint(ColorStateList.valueOf(white));
            btnDislike.setStrokeColor(ColorStateList.valueOf(red));
            // Like → outlined green (reset)
            btnLike.setBackgroundTintList(ColorStateList.valueOf(transparent));
            btnLike.setTextColor(green);
            btnLike.setIconTint(ColorStateList.valueOf(green));
            btnLike.setStrokeColor(ColorStateList.valueOf(green));

        } else {
            // Neither — both outlined (default state)
            btnLike.setBackgroundTintList(ColorStateList.valueOf(transparent));
            btnLike.setTextColor(green);
            btnLike.setIconTint(ColorStateList.valueOf(green));
            btnLike.setStrokeColor(ColorStateList.valueOf(green));

            btnDislike.setBackgroundTintList(ColorStateList.valueOf(transparent));
            btnDislike.setTextColor(red);
            btnDislike.setIconTint(ColorStateList.valueOf(red));
            btnDislike.setStrokeColor(ColorStateList.valueOf(red));
        }
    }

    private void loadSimilar(RecyclerView rv, TextView header) {
        Call<TmdbResponse> call = "tv".equals(mediaType)
                ? TmdbClient.getService().getSimilarTv(movieId)
                : TmdbClient.getService().getSimilarMovies(movieId);

        call.enqueue(new Callback<TmdbResponse>() {
            @Override
            public void onResponse(@NonNull Call<TmdbResponse> call,
                                   @NonNull Response<TmdbResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TmdbMovie> results = response.body().getResults();
                    if (results != null && !results.isEmpty()) {
                        for (TmdbMovie m : results) {
                            if (m.getMediaType() == null || m.getMediaType().isEmpty()) {
                                m.setMediaType(mediaType);
                            }
                        }
                        runOnUiThread(() -> {
                            similarAdapter.updateMovies(results);
                            rv.setVisibility(View.VISIBLE);
                            header.setVisibility(View.VISIBLE);
                        });
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<TmdbResponse> call, @NonNull Throwable t) {}
        });
    }

    private void markAsWatched() {
        if (currentDetail == null) return;

        UserSession session = UserSession.get();
        if (!session.isLoaded()) {
            Toast.makeText(this, "Please wait, loading user session...", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("When did you watch this?")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        picker.show(getSupportFragmentManager(), "watched_date_picker");

        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String watchedAt = sdf.format(new Date(selection)) + "T00:00:00";
            saveWatched(session, watchedAt);
        });
    }

    private void saveWatched(UserSession session, String watchedAt) {
        btnAlreadyWatched.setEnabled(false);

        // Add to watchlist as Finished
        SupabaseClient.addToWatchlist(
                session.getSupabaseUserId(),
                String.valueOf(currentDetail.getId()),
                currentDetail.getDisplayTitle(),
                currentDetail.getPosterUrl(),
                mediaType,
                "Finished",
                new SupabaseClient.Callback() {
                    @Override public void onSuccess() {}
                    @Override public void onFailure(String error) {}
                }
        );

        // Add to watch history with the selected date
        SupabaseClient.addToWatchHistory(
                session.getSupabaseUserId(),
                String.valueOf(currentDetail.getId()),
                currentDetail.getDisplayTitle(),
                mediaType,
                currentDetail.getPosterUrl(),
                watchedAt,
                new SupabaseClient.Callback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            btnAlreadyWatched.setEnabled(true);
                            Toast.makeText(MovieDetailActivity.this,
                                    currentDetail.getDisplayTitle() + " marked as watched!",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> {
                            btnAlreadyWatched.setEnabled(true);
                            Toast.makeText(MovieDetailActivity.this,
                                    "Failed: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
        );
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

        // Save locally for FeedbackFragment display
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        String uid = UserSession.get().getFirebaseUid();
        boolean hasUid = uid != null && !uid.isEmpty();
        String feedbackKey = hasUid
                ? "feedback_" + uid + "_" + currentDetail.getId()
                : "feedback_" + currentDetail.getId();
        String titleKey = hasUid
                ? "feedback_title_" + uid + "_" + currentDetail.getId()
                : "feedback_title_" + currentDetail.getId();
        String typeKey = hasUid
                ? "feedback_type_" + uid + "_" + currentDetail.getId()
                : "feedback_type_" + currentDetail.getId();
        prefs.edit()
                .putString(feedbackKey, value)
                .putString(titleKey, currentDetail.getDisplayTitle())
                .putString(typeKey, mediaType)
                .apply();

        // Save to Supabase ratings table
        UserSession session = UserSession.get();
        if (session.isLoaded()) {
            int rating = "liked".equals(value) ? 5 : 1;
            SupabaseClient.submitRating(
                    session.getSupabaseUserId(),
                    String.valueOf(currentDetail.getId()),
                    currentDetail.getDisplayTitle(),
                    rating,
                    value,
                    new SupabaseClient.Callback() {
                        @Override public void onSuccess() {}
                        @Override public void onFailure(String error) {}
                    }
            );

            // If liked/disliked, add to watchlist so it shows in Lists page
            String watchlistStatus = "liked".equals(value) ? "Liked" : "Disliked";
            SupabaseClient.addToWatchlist(
                    session.getSupabaseUserId(),
                    String.valueOf(currentDetail.getId()),
                    currentDetail.getDisplayTitle(),
                    currentDetail.getPosterUrl(),
                    mediaType,
                    watchlistStatus,
                    new SupabaseClient.Callback() {
                        @Override public void onSuccess() {}
                        @Override public void onFailure(String error) {}
                    }
            );
        }

        applyFeedbackState(value);
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