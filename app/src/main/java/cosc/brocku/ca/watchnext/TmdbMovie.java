package cosc.brocku.ca.watchnext;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TmdbMovie {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title; // movies

    @SerializedName("name")
    private String name; // TV shows

    @SerializedName("poster_path")
    private String posterPath;

    @SerializedName("backdrop_path")
    private String backdropPath;

    @SerializedName("genre_ids")
    private List<Integer> genreIds;

    @SerializedName("vote_average")
    private double voteAverage;

    @SerializedName("popularity")
    private double popularity;

    @SerializedName("media_type")
    private String mediaType; // "movie", "tv", "person" (multi-search only)

    // Combined genre ID → name map (movie + TV genres)
    private static final Map<Integer, String> GENRE_MAP = new HashMap<>();
    static {
        GENRE_MAP.put(28, "Action");
        GENRE_MAP.put(12, "Adventure");
        GENRE_MAP.put(35, "Comedy");
        GENRE_MAP.put(80, "Crime");
        GENRE_MAP.put(18, "Drama");
        GENRE_MAP.put(14, "Fantasy");
        GENRE_MAP.put(27, "Horror");
        GENRE_MAP.put(10749, "Romance");
        GENRE_MAP.put(878, "Sci-Fi");
        GENRE_MAP.put(53, "Thriller");
        GENRE_MAP.put(9648, "Mystery");
        GENRE_MAP.put(10759, "Action & Adventure");
        GENRE_MAP.put(10765, "Sci-Fi & Fantasy");
        GENRE_MAP.put(16, "Animation");
        GENRE_MAP.put(99, "Documentary");
        GENRE_MAP.put(10751, "Family");
        GENRE_MAP.put(36, "History");
    }

    public String getDisplayTitle() {
        return (title != null && !title.isEmpty()) ? title : name;
    }

    public String getDisplayGenre() {
        if (genreIds == null || genreIds.isEmpty()) return "Unknown";
        String genre = GENRE_MAP.get(genreIds.get(0));
        return genre != null ? genre : "Unknown";
    }

    public String getPosterUrl() {
        String path = (posterPath != null && !posterPath.isEmpty()) ? posterPath : backdropPath;
        if (path == null || path.isEmpty()) return null;
        return "https://image.tmdb.org/t/p/w500" + path;
    }

    public static String getGenreName(int id) {
        return GENRE_MAP.get(id);
    }

    public List<Integer> getGenreIds() { return genreIds; }
    public double getVoteAverage() { return voteAverage; }
    public double getPopularity() { return popularity; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public int getId() { return id; }
}
