package cosc.brocku.ca.watchnext;

import java.util.List;

public class WatchDay {
    private final String date;
    private final List<String> titlesWatched;
    private final String recommendation;

    public WatchDay(String date, List<String> titlesWatched, String recommendation) {
        this.date = date;
        this.titlesWatched = titlesWatched;
        this.recommendation = recommendation;
    }

    public String getDate() { return date; }
    public List<String> getTitlesWatched() { return titlesWatched; }
    public String getRecommendation() { return recommendation; }
}
