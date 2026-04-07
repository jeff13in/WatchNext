package cosc.brocku.ca.watchnext;
/**
 * Class that sets up the actual recommendation
 * Deletes seen titles, scores all the unseen ones,
 * does a content match, does a mood match
 * adds a rating&/popularity bonus
 * then returns the results
 * score = tagOverlapScore + moodBonus + (rating * 0.15) + (popularity * 0.01)
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Recommender {

    private static final double MOOD_BONUS = 2.0;
    private static final double RATING_WEIGHT = 0.15;
    private static final double POPULARITY_WEIGHT = 0.01;

    public static List<ScoredRec> recommend(
            ProfileUser profile,
            List<ProfileMovieShow> candidates,
            String selectedMood
    ) {
        List<ScoredRec> results = new ArrayList<>();

        if (profile == null || candidates == null) {
            return results;
        }

        for (ProfileMovieShow item : candidates) {
            if (item == null) {
                continue;
            }

            if (profile.hasSeen(item.getId())) {
                continue;
            }

            double score = calculateScore(profile, item, selectedMood);
            String explanation = buildExplanation(profile, item, selectedMood);

            results.add(new ScoredRec(item, score, explanation));
        }

        Collections.sort(results, new Comparator<ScoredRec>() {
            @Override
            public int compare(ScoredRec a, ScoredRec b) {
                return Double.compare(b.getScore(), a.getScore());
            }
        });

        return results;
    }

    private static double calculateScore(
            ProfileUser profile,
            ProfileMovieShow item,
            String selectedMood
    ) {
        double score = 0.0;
        Map<String, Double> userWeights = profile.getTagWeights();

        if (item.getTags() != null) {
            for (String tag : item.getTags()) {
                Double value = userWeights.get(tag);
                if (value == null) {
                    value = 0.0;
                }
                score += value;
            }
        }

        if (selectedMood != null && !selectedMood.trim().isEmpty() && item.getTags() != null) {
            String moodTag = "mood:" + selectedMood.trim().toLowerCase();
            if (item.getTags().contains(moodTag)) {
                score += MOOD_BONUS;
            }
        }

        score += item.getRating() * RATING_WEIGHT;
        score += item.getPopularity() * POPULARITY_WEIGHT;

        return score;
    }

    private static String buildExplanation(
            ProfileUser profile,
            ProfileMovieShow item,
            String selectedMood
    ) {
        List<String> matchedTags = new ArrayList<>();

        if (item.getTags() != null) {
            for (String tag : item.getTags()) {
                Double value = profile.getTagWeights().get(tag);

                // only show positive NON-mood tags in explanation
                if (value != null && value > 0 && !tag.startsWith("mood:")) {
                    matchedTags.add(tag);
                }
            }
        }

        StringBuilder explanation = new StringBuilder("Recommended because it matches ");
        boolean addedSomething = false;

        if (!matchedTags.isEmpty()) {
            explanation.append("your interests in ");
            int limit = Math.min(3, matchedTags.size());

            for (int i = 0; i < limit; i++) {
                explanation.append(matchedTags.get(i));
                if (i < limit - 1) {
                    explanation.append(", ");
                }
            }

            addedSomething = true;
        }

        if (selectedMood != null && !selectedMood.trim().isEmpty() && item.getTags() != null) {
            String moodTag = "mood:" + selectedMood.trim().toLowerCase();
            if (item.getTags().contains(moodTag)) {
                if (addedSomething) {
                    explanation.append(" and ");
                }
                explanation.append("your current mood (")
                        .append(selectedMood)
                        .append(")");
                addedSomething = true;
            }
        }

        if (!addedSomething) {
            explanation.append("its rating and popularity");
        }

        return explanation.toString();
    }
}