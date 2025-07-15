package com.companyname.shareride;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;
    private FirebaseAuth firebaseAuth;
    private UserDataManager userDataManager;

    // Loading screen components
    private View loadingScreen;
    private ProgressBar loadingProgressBar;
    private TextView loadingText;
    private View mainContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize UserDataManager
        userDataManager = UserDataManager.getInstance();

        // Check if user is logged in
        checkUserAuthentication();

        initViews();
        setupBottomNavigation();

        // Show loading screen initially
        showLoadingScreen();

        // Load user data after authentication check
        loadUserDataOnStart();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkUserAuthentication();
    }

    private void checkUserAuthentication() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
        } else if (!currentUser.isEmailVerified()) {
            // Optional: You can also check for email verification
            // firebaseAuth.signOut();
            // redirectToLogin();
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();

        // Initialize loading screen components
        loadingScreen = findViewById(R.id.loading_screen);
        loadingProgressBar = findViewById(R.id.loading_progress_bar);
        loadingText = findViewById(R.id.loading_text);
        mainContent = findViewById(R.id.main_content);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    fragment = new HomeFragment();
                } else if (itemId == R.id.nav_find) {
                    fragment = new FindRidesFragment();
                } else if (itemId == R.id.nav_rides) {
                    fragment = new MyRidesFragment();
                } else if (itemId == R.id.nav_profile) {
                    fragment = new ProfileFragment();
                }

                if (fragment != null) {
                    loadFragment(fragment);
                    return true;
                }
                return false;
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void showLoadingScreen() {
        if (loadingScreen != null) {
            loadingScreen.setVisibility(View.VISIBLE);
            loadingText.setText("Loading your data...");
        }
        if (mainContent != null) {
            mainContent.setVisibility(View.GONE);
        }
    }

    private void hideLoadingScreen() {
        if (loadingScreen != null) {
            loadingScreen.setVisibility(View.GONE);
        }
        if (mainContent != null) {
            mainContent.setVisibility(View.VISIBLE);
        }
    }

    private void loadUserDataOnStart() {
        // Load user data when app starts (after authentication check)
        userDataManager.loadUserData(new UserDataManager.UserDataCallback() {
            @Override
            public void onUserDataLoaded() {
                // Data is now available for all fragments
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoadingScreen();
                        // Load default fragment after data is loaded
                        loadFragment(new HomeFragment());
                    }
                });
            }

            @Override
            public void onUserDataError(String error) {
                // Handle error if needed
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoadingScreen();
                        // Load default fragment even on error (with default values)
                        loadFragment(new HomeFragment());

                        // Optionally show error message
                        Toast.makeText(MainActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Optionally refresh user data when app resumes
        if (userDataManager != null && userDataManager.isDataLoaded()) {
            userDataManager.refreshUserData(null);
        }
    }
}