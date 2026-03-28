package cosc.brocku.ca.watchnext;

public class Movie {
    private final String title;
    private final String genre;
    private final String type; // "Movie" or "TV Show"
    private final float rating;
    private final int posterColor; // color resource id for placeholder

    public Movie(String title, String genre, String type, float rating, int posterColor) {
        this.title = title;
        this.genre = genre;
        this.type = type;
        this.rating = rating;
        this.posterColor = posterColor;
    }

    public String getTitle() { return title; }
    public String getGenre() { return genre; }
    public String getType() { return type; }
    public float getRating() { return rating; }
    public int getPosterColor() { return posterColor; }
}
