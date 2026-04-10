package cosc.brocku.ca.watchnext;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    /** Parses a Supabase/TMDB ISO-8601 timestamp and returns "MMM d, yyyy". */
    public static String formatDate(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            String normalized = iso.replaceAll("\\.\\d+", "").replaceAll("[+-]\\d{2}:\\d{2}$", "").replace("Z", "");
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = parser.parse(normalized);
            return new SimpleDateFormat("MMM d, yyyy", Locale.US).format(date);
        } catch (Exception e) {
            return iso;
        }
    }
}
