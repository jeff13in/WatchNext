package cosc.brocku.ca.watchnext;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class WatchNextApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
