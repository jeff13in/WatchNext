package cosc.brocku.ca.watchnext;

public class ListEntry {
    private int id;          // Supabase watchlist row id
    private String movieId;  // TMDB movie id
    private String title;
    private String type;     // "movie" or "tv"
    private String status;   // "plan_to_watch", "watching", "completed"
    private int episode;
    private String playlist;

    public ListEntry(int id, String movieId, String title, String type, String status) {
        this.id      = id;
        this.movieId = movieId;
        this.title   = title;
        this.type    = type;
        this.status  = status;
        this.episode = -1;
        this.playlist = statusToTab(status);
    }

    private static String statusToTab(String status) {
        if ("watching".equals(status))   return "Watching";
        if ("completed".equals(status))  return "Finished";
        return "Plan to Watch";
    }

    public int getId()          { return id; }
    public String getMovieId()  { return movieId; }
    public String getTitle()    { return title; }
    public String getType()     { return type; }
    public String getStatus()   { return status; }
    public void setStatus(String status) { this.status = status; this.playlist = statusToTab(status); }
    public int getEpisode()     { return episode; }
    public void setEpisode(int episode) { this.episode = episode; }
    public String getPlaylist() { return playlist; }
    public void setPlaylist(String playlist) { this.playlist = playlist; }

    public String getStatusDisplay() {
        if ("movie".equals(type)) return statusToTab(status);
        return episode > 0 ? "S01E" + String.format("%02d", episode) : statusToTab(status);
    }
}
