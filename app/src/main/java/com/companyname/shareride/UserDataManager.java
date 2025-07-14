package com.companyname.shareride;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserDataManager {
    private static UserDataManager instance;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // User data fields
    private String userName;
    private String rating;
    private String savings;
    private String ridesShared;
    private String co2Reduced;
    private boolean isDataLoaded = false;

    // Interface for callbacks
    public interface UserDataCallback {
        void onUserDataLoaded();
        void onUserDataError(String error);
    }

    private UserDataManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized UserDataManager getInstance() {
        if (instance == null) {
            instance = new UserDataManager();
        }
        return instance;
    }

    public void loadUserData(UserDataCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Load user data from Firestore
                            userName = documentSnapshot.getString("name");
                            if (userName == null || userName.isEmpty()) {
                                userName = "Unknown User";
                            }

                            // Load other stats
                            rating = documentSnapshot.getString("rating");
                            savings = documentSnapshot.getString("savings");
                            ridesShared = documentSnapshot.getString("ridesShared");
                            co2Reduced = documentSnapshot.getString("co2Reduced");

                            // Set defaults if null
                            setDefaultsIfNull();

                            isDataLoaded = true;
                            if (callback != null) {
                                callback.onUserDataLoaded();
                            }
                        } else {
                            // Set default values if no document exists
                            setDefaultValues();
                            isDataLoaded = true;
                            if (callback != null) {
                                callback.onUserDataLoaded();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle error - set default values
                        setDefaultValues();
                        isDataLoaded = true;
                        if (callback != null) {
                            callback.onUserDataError("Error loading user data: " + e.getMessage());
                        }
                    });
        } else {
            // No user logged in
            setDefaultValues();
            if (callback != null) {
                callback.onUserDataError("No user logged in");
            }
        }
    }

    private void setDefaultsIfNull() {
        if (rating == null) rating = "⭐⭐⭐⭐⭐ 4.8 (24 rides)";
        if (savings == null) savings = "💰 Total Saved: ₹840";
        if (ridesShared == null) ridesShared = "🚗 Rides Shared: 12";
        if (co2Reduced == null) co2Reduced = "🌱 CO₂ Reduced: 2.4 kg";
    }

    private void setDefaultValues() {
        userName = "Unknown User";
        rating = "⭐⭐⭐⭐⭐ 4.8 (24 rides)";
        savings = "💰 Total Saved: ₹840";
        ridesShared = "🚗 Rides Shared: 12";
        co2Reduced = "🌱 CO₂ Reduced: 2.4 kg";
    }

    // Getters
    public String getUserName() {
        return userName != null ? userName : "Unknown User";
    }

    public String getRating() {
        return rating != null ? rating : "⭐⭐⭐⭐⭐ 4.8 (24 rides)";
    }

    public String getSavings() {
        return savings != null ? savings : "💰 Total Saved: ₹840";
    }

    public String getRidesShared() {
        return ridesShared != null ? ridesShared : "🚗 Rides Shared: 12";
    }

    public String getCo2Reduced() {
        return co2Reduced != null ? co2Reduced : "🌱 CO₂ Reduced: 2.4 kg";
    }

    public boolean isDataLoaded() {
        return isDataLoaded;
    }

    // Method to get just the numeric rating value
    public double getNumericRating() {
        if (rating != null && rating.contains("⭐")) {
            String[] parts = rating.split(" ");
            if (parts.length >= 2) {
                try {
                    return Double.parseDouble(parts[1]);
                } catch (NumberFormatException e) {
                    return 4.8; // Default rating
                }
            }
        }
        return 4.8; // Default rating
    }

    // Method to get short rating for home fragment
    public String getShortRating() {
        if (rating != null && rating.contains("⭐")) {
            // Extract the numeric rating from the string
            String[] parts = rating.split(" ");
            if (parts.length >= 2) {
                try {
                    double numericRating = Double.parseDouble(parts[1]);
                    String stars = generateStars(numericRating);
                    return stars + " " + parts[1]; // e.g., "⭐⭐⭐⭐⭐ 4.8"
                } catch (NumberFormatException e) {
                    return "⭐⭐⭐⭐⭐ 4.8";
                }
            }
        }
        return "⭐⭐⭐⭐⭐ 4.8";
    }

    // Helper method to generate stars based on rating
    private String generateStars(double rating) {
        int fullStars = (int) Math.floor(rating);
        boolean hasHalfStar = (rating - fullStars) >= 0.5;

        StringBuilder stars = new StringBuilder();

        // Add full stars
        for (int i = 0; i < fullStars && i < 5; i++) {
            stars.append("⭐");
        }

        // Add half star if needed
        if (hasHalfStar && fullStars < 5) {
            stars.append("⭐"); // You can use a different emoji like "🌟" for half star if preferred
        }

        // Fill remaining with empty stars if you want to show 5 total
        // Uncomment the lines below if you want to show empty stars
        /*
        int totalStars = fullStars + (hasHalfStar ? 1 : 0);
        for (int i = totalStars; i < 5; i++) {
            stars.append("☆"); // Empty star
        }
        */

        return stars.toString();
    }

    // Method to clear data on logout
    public void clearUserData() {
        userName = null;
        rating = null;
        savings = null;
        ridesShared = null;
        co2Reduced = null;
        isDataLoaded = false;
    }

    // Method to refresh data
    public void refreshUserData(UserDataCallback callback) {
        isDataLoaded = false;
        loadUserData(callback);
    }
}