package cosc.brocku.ca.watchnext;

import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class MainActivity extends AppCompatActivity {

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

        // Update drawer header with Firebase user email
        NavigationView navView = findViewById(R.id.nav_view);
        TextView headerEmail = navView.getHeaderView(0).findViewById(R.id.header_email);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            headerEmail.setText(currentUser.getEmail());
        }

        // Drawer navigation
        navView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawers();
            int id = item.getItemId();
            if (id == R.id.drawer_profile) {
                loadFragment(new ProfileFragment(), "Profile");
            } else if (id == R.id.drawer_playlists || id == R.id.drawer_share_lists) {
                loadFragment(new ListsFragment(), "Lists");
            } else if (id == R.id.drawer_preferences) {
                loadFragment(new PreferencesFragment(), "Preferences");
            } else if (id == R.id.drawer_sign_out) {
                UserSession.get().clear();
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
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
