package cosc.brocku.ca.watchnext;

import java.util.ArrayList;
import java.util.List;
//Helper class that will map TMDb movies into profile items and generates recommendations

public class RecommendationsBuilder {

    public static List<ProfileMovieShow> mapTmdbMoviesToProfiles(List<TmdbMovie> tmdbMovies) {
        List<ProfileMovieShow> profileItems = new ArrayList<ProfileMovieShow>();

        if (tmdbMovies == null) {
            return profileItems;
        }

        for (TmdbMovie movie : tmdbMovies) {
            if (movie == null) {
                continue;
            }

            if ("person".equals(movie.getMediaType())) {
                continue;
            }

            ProfileMovieShow profileItem = ProfileMovieShowUtil.fromTmdbMovie(
                    movie,
                    null,
                    null,
                    null
            );

            profileItems.add(profileItem);
        }

        return profileItems;
    }

    public static List<ScoredRec> generateRecommendations(
            List<TmdbMovie> likedTmdbMovies,
            List<TmdbMovie> dislikedTmdbMovies,
            List<TmdbMovie> watchedTmdbMovies,
            List<TmdbMovie> candidateTmdbMovies,
            String selectedMood
    ) {
        List<ProfileMovieShow> likedItems = mapTmdbMoviesToProfiles(likedTmdbMovies);
        List<ProfileMovieShow> dislikedItems = mapTmdbMoviesToProfiles(dislikedTmdbMovies);
        List<ProfileMovieShow> watchedItems = mapTmdbMoviesToProfiles(watchedTmdbMovies);
        List<ProfileMovieShow> candidateItems = mapTmdbMoviesToProfiles(candidateTmdbMovies);

        ProfileUser profile = UserProfileBuilder.buildProfile(
                likedItems,
                dislikedItems,
                watchedItems
        );

        return Recommender.recommend(profile, candidateItems, selectedMood);
    }

    public static List<ScoredRec> generateTopRecommendations(
            List<TmdbMovie> likedTmdbMovies,
            List<TmdbMovie> dislikedTmdbMovies,
            List<TmdbMovie> watchedTmdbMovies,
            List<TmdbMovie> candidateTmdbMovies,
            String selectedMood,
            int limit
    ) {
        List<ScoredRec> all = generateRecommendations(
                likedTmdbMovies,
                dislikedTmdbMovies,
                watchedTmdbMovies,
                candidateTmdbMovies,
                selectedMood
        );

        if (all.size() <= limit) {
            return all;
        }

        return new ArrayList<ScoredRec>(all.subList(0, limit));
    }
}