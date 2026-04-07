package cosc.brocku.ca.watchnext;

//Class that creates a profile object for a movie or a show
//with the attributes being; id, title, tags, popularity, overallRating

import java.util.Set;

public class ProfileMovieShow {
    private int id;
    private String title;
    private Set<String> tags;
    private double popularity;
    private double overallRating;
    private TmdbMovie sourceMovie;
    public ProfileMovieShow(int id, String title, Set<String> tags, double popularity, double overalRating, TmdbMovie sourceMovie) {
        this.id = id;
        this.title = title;
        this.tags = tags;
        this.popularity = popularity;
        this.overallRating = overalRating;
        this.sourceMovie = sourceMovie;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Set<String> getTags() {
        return tags;
    }

    public double getPopularity() {
        return popularity;
    }

    public double getRating() {
        return overallRating;
    }
    public TmdbMovie getSourceMovie() {
        return sourceMovie;
    }
}
