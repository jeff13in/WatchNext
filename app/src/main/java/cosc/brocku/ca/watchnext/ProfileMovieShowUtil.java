package cosc.brocku.ca.watchnext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//This class helps the ProfileMovieShow object to obtain a model for recommendation
//Based on the attributes; keywords, cast, director and mood based on the genre and tags
public class ProfileMovieShowUtil {
    //creating the ProfileMovieShow object
    public static ProfileMovieShow fromTmdbMovie(
            TmdbMovie movie,
            List<String> keywords,
            List<String> cast,
            String director
    ) {
        Set<String> tags = new HashSet<>();
        //adding genre tags
        List<Integer> genreIds = movie.getGenreIds();
        if (genreIds != null) {
            for (Integer id : genreIds) {
                String genreName = TmdbMovie.getGenreName(id);
                if (genreName != null) {
                    tags.add(genreName.toLowerCase());
                }
            }
        }

        //adding keyword tags
        if (keywords != null) {
            for (String keyword : keywords) {
                if (keyword != null && !keyword.trim().isEmpty()) {
                    tags.add(keyword.trim().toLowerCase());
                }
            }
        }
        //adding tags for the cast involved
        if (cast != null) {
            for (String actor : cast) {
                if (actor != null && !actor.trim().isEmpty()) {
                    tags.add("actor:" + actor.trim().toLowerCase());
                }
            }
        }

        //adding tags for the director involved
        if (director != null && !director.trim().isEmpty()) {
            tags.add("director:" + director.trim().toLowerCase());
        }

        //adding tags for the inferred mod
        tags.addAll(inferMoodTags(tags));

        return new ProfileMovieShow(
                movie.getId(),
                movie.getDisplayTitle(),
                tags,
                movie.getPopularity(),
                movie.getVoteAverage(),
                movie
        );
    }
//setting up the logic for the mood inferred by the different tags involved
    private static Set<String> inferMoodTags(Set<String> currentTags) {
        Set<String> moodTags = new HashSet<>();
        // Happy / light / comfort
        if (currentTags.contains("comedy") || currentTags.contains("family") || currentTags.contains("animation")) {
            moodTags.add("mood:happy");
            moodTags.add("mood:light");
            moodTags.add("mood:comfort");
        }

        // Excited / intense
        if (currentTags.contains("action") || currentTags.contains("adventure")) {
            moodTags.add("mood:excited");
        }

        if (currentTags.contains("thriller")) {
            moodTags.add("mood:excited");
            moodTags.add("mood:intense");
            moodTags.add("mood:tense");
        }

        // Scared should be much stricter: horror only
        if (currentTags.contains("horror")) {
            moodTags.add("mood:scared");
            moodTags.add("mood:dark");
            moodTags.add("mood:intense");
        }

        // Mystery is curious / tense / dark, but not necessarily scared
        if (currentTags.contains("mystery")) {
            moodTags.add("mood:curious");
            moodTags.add("mood:tense");
            moodTags.add("mood:dark");
        }

        // Emotional / relaxed
        if (currentTags.contains("drama") || currentTags.contains("romance")) {
            moodTags.add("mood:emotional");
        }

        if (currentTags.contains("romance") || currentTags.contains("family")) {
            moodTags.add("mood:relaxed");
        }

        // Sci-fi / fantasy
        if (currentTags.contains("sci-fi") || currentTags.contains("sci-fi & fantasy")) {
            moodTags.add("mood:curious");
        }

        // Keyword-based boosts
        if (currentTags.contains("space") || currentTags.contains("future") || currentTags.contains("technology")) {
            moodTags.add("mood:curious");
        }

        if (currentTags.contains("friendship") || currentTags.contains("love")) {
            moodTags.add("mood:warm");
        }

        if (currentTags.contains("murder") || currentTags.contains("crime")) {
            moodTags.add("mood:tense");
            moodTags.add("mood:dark");
        }
        return moodTags;
    }
}