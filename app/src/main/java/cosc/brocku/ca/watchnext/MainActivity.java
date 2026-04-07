package cosc.brocku.ca.watchnext;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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

        // Update drawer header with user email + wire theme toggle
        NavigationView navView = findViewById(R.id.nav_view);
        android.view.View headerView = navView.getHeaderView(0);
        TextView headerEmail = headerView.findViewById(R.id.header_email);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        headerEmail.setText(prefs.getString("email", "user@example.com"));

        ImageButton btnToggleTheme = headerView.findViewById(R.id.btn_toggle_theme);
        boolean isDark = prefs.getBoolean("dark_mode", true);
        btnToggleTheme.setImageResource(isDark ? R.drawable.ic_mode_light : R.drawable.ic_mode_night);
        btnToggleTheme.setOnClickListener(v -> {
            boolean currentlyDark = prefs.getBoolean("dark_mode", true);
            boolean newDark = !currentlyDark;
            prefs.edit().putBoolean("dark_mode", newDark).apply();
            AppCompatDelegate.setDefaultNightMode(
                    newDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            recreate();
        });

        // Load UserSession from Supabase if not already loaded
        if (!UserSession.get().isLoaded()) {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                SupabaseClient.getUser(firebaseUser.getUid(), new SupabaseClient.DataCallback() {
                    @Override
                    public void onSuccess(org.json.JSONObject data) {
                        try {
                            int supabaseId = data.getInt("id");
                            String email = data.getString("email");
                            UserSession.get().set(supabaseId, firebaseUser.getUid(), email);
                        } catch (Exception ignored) {}
                    }
                    @Override
                    public void onFailure(String error) {}
                });
            }
        }

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
            } else if (id == R.id.nav_recommendations) {
                loadFragment(new RecommendationsFragment(), "For You");
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
