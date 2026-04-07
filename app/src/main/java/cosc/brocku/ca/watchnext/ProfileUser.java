package cosc.brocku.ca.watchnext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class that implements an object to keep data for a User and create a profile
 * With tag weights for their likes and dislikes and an attribute to save their seen movie/shows
 **/
public class ProfileUser {

    private Map<String, Double> tagWeights;
    private Set<Integer> seenItemIds;

    public ProfileUser() {
        tagWeights = new HashMap<>();
        seenItemIds = new HashSet<>();
    }
//positive weights for liked
    public void addPositiveTag(String tag, double weight) {
        Double current = tagWeights.get(tag);
        if (current == null) {
            current = 0.0;
        }
        tagWeights.put(tag, current + weight);
    }
    //negative weights for disliked
    public void addNegativeTag(String tag, double weight) {
        Double current = tagWeights.get(tag);
        if (current == null) {
            current = 0.0;
        }
        tagWeights.put(tag, current - weight);
    }

    public void markSeen(int itemId) {
        seenItemIds.add(itemId);
    }

    public boolean hasSeen(int itemId) {
        return seenItemIds.contains(itemId);
    }

    public Map<String, Double> getTagWeights() {
        return tagWeights;
    }

    public Set<Integer> getSeenItemIds() {
        return seenItemIds;
    }
}