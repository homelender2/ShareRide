package com.companyname.shareride;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

    private TextView tvUserName, tvRating, tvSavings, tvRidesShared, tvCO2Reduced;
    private Button logoutButton;
    private FirebaseAuth auth;
    private UserDataManager userDataManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        userDataManager = UserDataManager.getInstance();

        // Initialize views
        initViews(view);

        // Set logout button click listener
        logoutButton.setOnClickListener(v -> logout());

        // Load user data using UserDataManager
        loadUserData();

        return view;
    }

    private void initViews(View view) {
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvRating = view.findViewById(R.id.tv_rating);
        tvSavings = view.findViewById(R.id.tv_savings);
        tvRidesShared = view.findViewById(R.id.tv_rides_shared);
        tvCO2Reduced = view.findViewById(R.id.tv_co2_reduced);
        logoutButton = view.findViewById(R.id.logout_button);
    }

    private void loadUserData() {
        // Check if data is already loaded
        if (userDataManager.isDataLoaded()) {
            updateUI();
        } else {
            // Load data from Firebase
            userDataManager.loadUserData(new UserDataManager.UserDataCallback() {
                @Override
                public void onUserDataLoaded() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> updateUI());
                    }
                }

                @Override
                public void onUserDataError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // Still update UI with default values
                            updateUI();
                        });
                    }
                }
            });
        }
    }

    private void updateUI() {
        tvUserName.setText(userDataManager.getUserName());
        tvRating.setText(userDataManager.getRating());
        tvSavings.setText(userDataManager.getSavings());
        tvRidesShared.setText(userDataManager.getRidesSharedString()); // Also fixed this one
        tvCO2Reduced.setText(userDataManager.getCo2ReducedString()); // Fixed method name
    }

    private void logout() {
        // Clear user data from manager
        userDataManager.clearUserData();

        // Sign out from Firebase
        auth.signOut();

        // Navigate to login
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}