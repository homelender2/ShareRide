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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView tvUserName, tvRating, tvSavings, tvRidesShared, tvCO2Reduced;
    private Button logoutButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews(view);

        // Set logout button click listener
        logoutButton.setOnClickListener(v -> logout());

        // Load user data from Firebase
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
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Load user data from Firestore
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            if (name != null) {
                                tvUserName.setText(name);
                            } else {
                                tvUserName.setText("Unknown User");
                            }

                            // Load profile stats from Firestore or set defaults
                            setupUserStats(documentSnapshot);
                        } else {
                            // If no user document exists, set default values
                            tvUserName.setText("Unknown User");
                            setupDefaultStats();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle error - set default values
                        tvUserName.setText("Error loading name");
                        setupDefaultStats();
                    });
        }
    }

    private void setupUserStats(DocumentSnapshot documentSnapshot) {
        // Get stats from Firestore document or use defaults
        String rating = documentSnapshot.getString("rating");
        String savings = documentSnapshot.getString("savings");
        String ridesShared = documentSnapshot.getString("ridesShared");
        String co2Reduced = documentSnapshot.getString("co2Reduced");

        // Set values or defaults
        tvRating.setText(rating != null ? rating : "⭐⭐⭐⭐⭐ 4.8 (24 rides)");
        tvSavings.setText(savings != null ? savings : "💰 Total Saved: ₹840");
        tvRidesShared.setText(ridesShared != null ? ridesShared : "🚗 Rides Shared: 12");
        tvCO2Reduced.setText(co2Reduced != null ? co2Reduced : "🌱 CO₂ Reduced: 2.4 kg");
    }

    private void setupDefaultStats() {
        tvRating.setText("⭐⭐⭐⭐⭐ 4.8 (24 rides)");
        tvSavings.setText("💰 Total Saved: ₹840");
        tvRidesShared.setText("🚗 Rides Shared: 12");
        tvCO2Reduced.setText("🌱 CO₂ Reduced: 2.4 kg");
    }

    private void logout() {
        auth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}