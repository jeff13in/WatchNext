package cosc.brocku.ca.watchnext;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;

public class WatchDaysFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_watch_days, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<WatchDay> watchDays = Arrays.asList(
                new WatchDay("March 25, 2026",
                        Arrays.asList("Inception", "The Matrix"),
                        "Interstellar"),
                new WatchDay("March 23, 2026",
                        Arrays.asList("Breaking Bad S01E01"),
                        "Ozark"),
                new WatchDay("March 20, 2026",
                        Arrays.asList("Avengers: Endgame"),
                        "The Dark Knight"),
                new WatchDay("March 18, 2026",
                        Arrays.asList("Stranger Things S03E01", "Stranger Things S03E02"),
                        "Dark"),
                new WatchDay("March 15, 2026",
                        Arrays.asList("The Godfather"),
                        "Goodfellas")
        );

        RecyclerView rv = view.findViewById(R.id.rv_watch_days);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new WatchDayAdapter(watchDays));
    }
}
