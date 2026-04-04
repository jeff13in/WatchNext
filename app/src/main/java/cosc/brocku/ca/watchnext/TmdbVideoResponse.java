package cosc.brocku.ca.watchnext;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TmdbVideoResponse {

    @SerializedName("results")
    private List<TmdbVideo> results;

    public List<TmdbVideo> getResults() {
        return results;
    }

    public String getFirstTrailerUrl() {
        if (results == null) return null;

        for (TmdbVideo video : results) {
            if (video.isYouTubeTrailer()) {
                return "https://www.youtube.com/watch?v=" + video.getKey();
            }
        }
        return null;
    }
}