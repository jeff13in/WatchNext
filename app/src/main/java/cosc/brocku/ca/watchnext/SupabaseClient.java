package cosc.brocku.ca.watchnext;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseClient {

    private static final String BASE_URL = BuildConfig.SUPABASE_URL + "/rest/v1/";
    private static final String API_KEY  = BuildConfig.SUPABASE_KEY;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface Callback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface DataCallback {
        void onSuccess(JSONObject data);
        void onFailure(String error);
    }

    public interface ListCallback {
        void onSuccess(JSONArray data);
        void onFailure(String error);
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    public static void registerUser(String firebaseUid, String email, Callback callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("firebase_uid", firebaseUid);
                body.put("username", email.split("@")[0]);
                body.put("email", email);
                post("users", body, callback);
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        });
    }

    public static void getUser(String firebaseUid, DataCallback callback) {
        executor.execute(() -> {
            try {
                String endpoint = "users?firebase_uid=eq." + firebaseUid + "&limit=1";
                HttpURLConnection conn = openConnection(BASE_URL + endpoint, "GET");
                String response = readResponse(conn);
                JSONArray arr = new JSONArray(response);
                if (arr.length() > 0) callback.onSuccess(arr.getJSONObject(0));
                else callback.onFailure("User not found");
                conn.disconnect();
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        });
    }

    public static void updateMood(String firebaseUid, String mood, Callback callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("mood", mood);
                patch("users?firebase_uid=eq." + firebaseUid, body, callback);
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        });
    }

    // ── Watchlist ─────────────────────────────────────────────────────────────

    public static void getWatchlist(int userId, ListCallback callback) {
        executor.execute(() -> getList("watchlist?user_id=eq." + userId + "&order=added_at.desc", callback));
    }

    public static void addToWatchlist(int userId, String movieId, String title,
                                      String posterPath, String mediaType, Callback callback) {
        addToWatchlist(userId, movieId, title, posterPath, mediaType, "Watching", callback);
    }

    public static void addToWatchlist(int userId, String movieId, String title,
                                      String posterPath, String mediaType, String status,
                                      Callback callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("user_id", userId);
                body.put("movie_id", movieId);
                body.put("title", title);
                body.put("poster_path", posterPath != null ? posterPath : "");
                body.put("media_type", mediaType);
                body.put("status", status);
                post("watchlist", body, callback);
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        });
    }

    public static void updateWatchlistStatus(int id, String status, Callback callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("status", status);
                patch("watchlist?id=eq." + id, body, callback);
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        });
    }

    public static void removeFromWatchlist(int id, Callback callback) {
        executor.execute(() -> delete("watchlist?id=eq." + id, callback));
    }

    // ── Ratings ───────────────────────────────────────────────────────────────

    public static void getRatings(String movieId, ListCallback callback) {
        executor.execute(() -> getList("ratings?movie_id=eq." + movieId + "&order=created_at.desc", callback));
    }

    public static void submitRating(int userId, String movieId, String title,
                                    int rating, String review, Callback callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("user_id", userId);
                body.put("movie_id", movieId);
                body.put("title", title);
                body.put("rating", rating);
                body.put("review", review);
                // upsert — updates if already rated
                postUpsert("ratings", body, "user_id,movie_id", callback);
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        });
    }

    // ── Watch History ─────────────────────────────────────────────────────────

    public static void addToWatchHistory(int userId, String movieId, String title,
                                         String mediaType, String posterPath, Callback callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("user_id", userId);
                body.put("movie_id", movieId);
                body.put("title", title);
                body.put("media_type", mediaType);
                body.put("poster_path", posterPath != null ? posterPath : "");
                post("watch_history", body, callback);
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        });
    }

    public static void getWatchHistory(int userId, ListCallback callback) {
        executor.execute(() -> getList(
                "watch_history?user_id=eq." + userId + "&order=watched_at.desc",
                callback
        ));
    }

    // ── Followers ─────────────────────────────────────────────────────────────

    public static void getFollowers(int userId, ListCallback callback) {
        executor.execute(() -> getList("followers?following_id=eq." + userId, callback));
    }

    public static void getFollowing(int userId, ListCallback callback) {
        executor.execute(() -> getList("followers?follower_id=eq." + userId, callback));
    }

    public static void follow(int followerUserId, int followingUserId, Callback callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("follower_id", followerUserId);
                body.put("following_id", followingUserId);
                post("followers", body, callback);
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        });
    }

    public static void unfollow(int followerUserId, int followingUserId, Callback callback) {
        executor.execute(() -> delete(
            "followers?follower_id=eq." + followerUserId + "&following_id=eq." + followingUserId,
            callback
        ));
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private static void post(String endpoint, JSONObject body, Callback callback) {
        try {
            HttpURLConnection conn = openConnection(BASE_URL + endpoint, "POST");
            conn.setRequestProperty("Prefer", "return=minimal");
            writeBody(conn, body);
            int code = conn.getResponseCode();
            if (code == 200 || code == 201) callback.onSuccess();
            else callback.onFailure("Error: " + code);
            conn.disconnect();
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    private static void postUpsert(String endpoint, JSONObject body, String onConflict, Callback callback) {
        try {
            HttpURLConnection conn = openConnection(BASE_URL + endpoint, "POST");
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal");
            conn.setRequestProperty("on_conflict", onConflict);
            writeBody(conn, body);
            int code = conn.getResponseCode();
            if (code == 200 || code == 201) callback.onSuccess();
            else callback.onFailure("Error: " + code);
            conn.disconnect();
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    private static void patch(String endpoint, JSONObject body, Callback callback) {
        try {
            HttpURLConnection conn = openConnection(BASE_URL + endpoint, "PATCH");
            conn.setRequestProperty("Prefer", "return=minimal");
            writeBody(conn, body);
            int code = conn.getResponseCode();
            if (code == 200 || code == 204) callback.onSuccess();
            else callback.onFailure("Error: " + code);
            conn.disconnect();
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    private static void delete(String endpoint, Callback callback) {
        try {
            HttpURLConnection conn = openConnection(BASE_URL + endpoint, "DELETE");
            int code = conn.getResponseCode();
            if (code == 200 || code == 204) callback.onSuccess();
            else callback.onFailure("Error: " + code);
            conn.disconnect();
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    private static void getList(String endpoint, ListCallback callback) {
        try {
            HttpURLConnection conn = openConnection(BASE_URL + endpoint, "GET");
            String response = readResponse(conn);
            callback.onSuccess(new JSONArray(response));
            conn.disconnect();
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    private static HttpURLConnection openConnection(String url, String method) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("apikey", API_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        return conn;
    }

    private static void writeBody(HttpURLConnection conn, JSONObject body) throws Exception {
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(body.toString().getBytes("UTF-8"));
        os.close();
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        java.io.InputStream stream = (code >= 200 && code < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();
        if (stream == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        android.util.Log.d("SupabaseClient", "HTTP " + code + " → " + sb);
        return sb.toString();
    }
}
