package cosc.brocku.ca.watchnext;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "watchnext_prefs";
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Hamburger toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Update drawer header with user email
        NavigationView navView = findViewById(R.id.nav_view);
        TextView headerEmail = navView.getHeaderView(0).findViewById(R.id.header_email);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        headerEmail.setText(prefs.getString("email", "user@example.com"));

        // Drawer navigation
        navView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawers();
            int id = item.getItemId();
            if (id == R.id.drawer_profile) {
                loadFragment(new ProfileFragment(), "Profile");
            } else if (id == R.id.drawer_playlists || id == R.id.drawer_share_lists) {
                loadFragment(new ListsFragment(), "Lists");
            } else if (id == R.id.drawer_feedback) {
                loadFragment(new FeedbackFragment(), "Feedback");
            } else if (id == R.id.drawer_preferences) {
                loadFragment(new PreferencesFragment(), "Preferences");
            }
            return true;
        });

        // Bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment(), "Home");
            } else if (id == R.id.nav_watch_days) {
                loadFragment(new WatchDaysFragment(), "Watch Days");
            } else if (id == R.id.nav_lists) {
                loadFragment(new ListsFragment(), "Lists");
            } else if (id == R.id.nav_search) {
                loadFragment(new SearchFragment(), "Search");
            } else if (id == R.id.nav_feedback || id == R.id.nav_feedback) {
                loadFragment(new FeedbackFragment(), "Feedback");
                }
            return true;
        });

        // Load Home by default
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "Home");
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void loadFragment(Fragment fragment, String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
