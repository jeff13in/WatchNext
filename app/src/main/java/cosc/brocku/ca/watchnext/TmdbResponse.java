package cosc.brocku.ca.watchnext;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TmdbResponse {

    @SerializedName("results")
    private List<TmdbMovie> results;

    @SerializedName("page")
    private int page;

    @SerializedName("total_pages")
    private int totalPages;

    public List<TmdbMovie> getResults() { return results; }
    public int getPage() { return page; }
    public int getTotalPages() { return totalPages; }
}
