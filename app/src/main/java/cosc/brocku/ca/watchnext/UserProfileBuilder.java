package cosc.brocku.ca.watchnext;

import java.util.List;
/**
 * This class builds the ProfileUser object with the things that are liked, disliked and watched items by a user.
 **/
public class UserProfileBuilder {
    public static ProfileUser buildProfile(
            List<ProfileMovieShow> likedItems,
            List<ProfileMovieShow> dislikedItems,
            List<ProfileMovieShow> watchedItems
    ) {
        ProfileUser profile = new ProfileUser();
        if (likedItems != null) {
            for (ProfileMovieShow item:likedItems) {
                profile.markSeen(item.getId());
                for (String tag:item.getTags()) {
                    profile.addPositiveTag(tag, 2.0);
                }
            }
        }
        if (watchedItems != null) {
            for (ProfileMovieShow item:watchedItems) {
                profile.markSeen(item.getId());
                for (String tag:item.getTags()) {
                    profile.addPositiveTag(tag, 1.0);
                }
            }
        }
        if (dislikedItems != null) {
            for (ProfileMovieShow item:dislikedItems) {
                profile.markSeen(item.getId());

                for (String tag:item.getTags()) {
                    profile.addNegativeTag(tag, 2.0);
                }
            }
        }
        return profile;
    }
}