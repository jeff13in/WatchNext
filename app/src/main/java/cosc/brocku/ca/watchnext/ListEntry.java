package cosc.brocku.ca.watchnext;

public class ListEntry {
    private String title;
    private String type; // "Movie" or "TV Show"
    private String status; // "Watching" or "Finished"
    private int episode; // episode number for TV shows, -1 for movies
    private String movieId;
    private int supabaseId;
    private int totalEpisodes = 0; // 0 = unknown
    private String displayDate = ""; // formatted date shown in the list item

    public ListEntry(String title, String type, String status, int episode) {
        this.title = title;
        this.type = type;
        this.status = status;
        this.episode = episode;
        this.movieId = "";
        this.supabaseId = -1;
    }

    public String getTitle() { return title; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getEpisode() { return episode; }
    public void setEpisode(int episode) { this.episode = episode; }
    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }
    public int getSupabaseId() { return supabaseId; }
    public void setSupabaseId(int supabaseId) { this.supabaseId = supabaseId; }
    public int getTotalEpisodes() { return totalEpisodes; }
    public void setTotalEpisodes(int totalEpisodes) { this.totalEpisodes = totalEpisodes; }
    public String getDisplayDate() { return displayDate; }
    public void setDisplayDate(String displayDate) { this.displayDate = displayDate != null ? displayDate : ""; }

    public String getStatusDisplay() {
        if (type.equals("Movie")) {
            return status;
        }
        String epStr = "Ep " + episode;
        if (totalEpisodes > 0) epStr += " / " + totalEpisodes;
        return epStr;
    }
}
