package cosc.brocku.ca.watchnext;

import com.google.gson.annotations.SerializedName;

public class TmdbVideo {

    @SerializedName("key")
    private String key;

    @SerializedName("site")
    private String site;

    @SerializedName("type")
    private String type;

    public String getKey() {
        return key;
    }

    public String getSite() {
        return site;
    }

    public String getType() {
        return type;
    }

    public boolean isYouTubeTrailer() {
        return "YouTube".equalsIgnoreCase(site) && "Trailer".equalsIgnoreCase(type);
    }
}