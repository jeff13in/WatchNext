package cosc.brocku.ca.watchnext;

public class ListEntry {
    private String title;
    private String type; // "Movie" or "TV Show"
    private String status; // "Watching" or "Finished"
    private int episode; // episode number for TV shows, -1 for movies
    private String playlist; // "Watching", "Finished", or custom name
    private String movieId;
    private int supabaseId;

    public ListEntry(String title, String type, String status, int episode, String playlist) {
        this.title = title;
        this.type = type;
        this.status = status;
        this.episode = episode;
        this.playlist = playlist;
        this.movieId = "";
        this.supabaseId = -1;
    }

    public String getTitle() { return title; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getEpisode() { return episode; }
    public void setEpisode(int episode) { this.episode = episode; }
    public String getPlaylist() { return playlist; }
    public void setPlaylist(String playlist) { this.playlist = playlist; }
    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }
    public int getSupabaseId() { return supabaseId; }
    public void setSupabaseId(int supabaseId) { this.supabaseId = supabaseId; }

    public String getStatusDisplay() {
        if (type.equals("Movie")) {
            return status;
        }
        return "S01E" + String.format("%02d", episode);
    }
}
