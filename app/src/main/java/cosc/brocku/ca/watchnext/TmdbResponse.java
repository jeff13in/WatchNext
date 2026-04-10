package cosc.brocku.ca.watchnext;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TmdbResponse {

    @SerializedName("results")
    private List<TmdbMovie> results;

    public List<TmdbMovie> getResults() { return results; }
}
