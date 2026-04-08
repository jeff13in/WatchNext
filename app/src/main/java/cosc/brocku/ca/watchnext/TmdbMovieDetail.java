package cosc.brocku.ca.watchnext;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TmdbMovieDetail {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("name")
    private String name;

    @SerializedName("overview")
    private String overview;

    @SerializedName("release_date")
    private String releaseDate;

    @SerializedName("first_air_date")
    private String firstAirDate;

    @SerializedName("vote_average")
    private double voteAverage;

    @SerializedName("poster_path")
    private String posterPath;

    @SerializedName("backdrop_path")
    private String backdropPath;

    @SerializedName("genres")
    private List<Genre> genres;

    @SerializedName("videos")
    private TmdbVideoResponse videos;

    @SerializedName("number_of_episodes")
    private int numberOfEpisodes;

    public static class Genre {
        @SerializedName("name")
        private String name;

        public String getName() {
            return name;
        }
    }

    public int getId() {
        return id;
    }

    public String getDisplayTitle() {
        return (title != null && !title.isEmpty()) ? title : name;
    }

    public String getDisplayDate() {
        return (releaseDate != null && !releaseDate.isEmpty()) ? releaseDate : firstAirDate;
    }

    public String getOverview() {
        return overview != null ? overview : "";
    }

    public double getVoteAverage() {
        return voteAverage;
    }

    public String getPosterUrl() {
        return posterPath == null ? null : "https://image.tmdb.org/t/p/w500" + posterPath;
    }

    public String getBackdropUrl() {
        return backdropPath == null ? null : "https://image.tmdb.org/t/p/w780" + backdropPath;
    }

    public String getGenreText() {
        if (genres == null || genres.isEmpty()) return "Unknown";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < genres.size(); i++) {
            sb.append(genres.get(i).getName());
            if (i < genres.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    public TmdbVideoResponse getVideos() {
        return videos;
    }

    public int getNumberOfEpisodes() {
        return numberOfEpisodes;
    }
}