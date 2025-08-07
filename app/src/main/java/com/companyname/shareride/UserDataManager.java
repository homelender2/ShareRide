package com.companyname.shareride;

import android.content.Context;
import android.util.Log;
import com.companyname.shareride.database.RideDAO;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDataManager {
    private static final String TAG = "UserDataManager";
    private static UserDataManager instance;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Context context;

    // User data fields
    private String userId;
    private String userName;
    private String email;
    private String phoneNumber;
    private String profilePictureUrl;
    private double rating;
    private int totalRides;
    private double totalSavings;
    private int ridesShared;
    private double co2Reduced;
    private String userRole; // "driver", "passenger", "both"
    private boolean isDataLoaded = false;
    private boolean isEmailVerified = false;

    // Legacy fields for backward compatibility
    private String ratingString;
    private String savingsString;
    private String ridesSharedString;
    private String co2ReducedString;

    // Interface for callbacks
    public interface UserDataCallback {
        void onUserDataLoaded();
        void onUserDataError(String error);
    }

    public interface UserUpdateCallback {
        void onUserDataUpdated();
        void onUserUpdateError(String error);
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

    // Initialize with context for database operations
    public void initialize(Context context) {
        this.context = context.getApplicationContext();
    }

    public void loadUserData(UserDataCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            email = user.getEmail();
            isEmailVerified = user.isEmailVerified();

            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        try {
                            if (documentSnapshot.exists()) {
                                loadFirestoreData(documentSnapshot);
                            } else {
                                createDefaultUserProfile();
                            }

                            // Calculate stats from database if context is available
                            if (context != null) {
                                calculateStatsFromDatabase();
                            }

                            updateLegacyFields();
                            isDataLoaded = true;

                            if (callback != null) {
                                callback.onUserDataLoaded();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing user data", e);
                            handleDataLoadError(callback, e.getMessage());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load user data from Firestore", e);
                        handleDataLoadError(callback, e.getMessage());
                    });
        } else {
            Log.w(TAG, "No user logged in");
            setGuestUserDefaults();
            if (callback != null) {
                callback.onUserDataError("No user logged in");
            }
        }
    }

    private void loadFirestoreData(DocumentSnapshot doc) {
        // Basic profile information
        userName = doc.getString("name");
        phoneNumber = doc.getString("phoneNumber");
        profilePictureUrl = doc.getString("profilePictureUrl");
        userRole = doc.getString("userRole");

        // Stats (try both numeric and string formats for compatibility)
        rating = getDoubleValue(doc, "rating", 4.8);
        totalRides = getIntValue(doc, "totalRides", 0);
        totalSavings = getDoubleValue(doc, "totalSavings", 0.0);
        ridesShared = getIntValue(doc, "ridesShared", 0);
        co2Reduced = getDoubleValue(doc, "co2Reduced", 0.0);

        // Set defaults if null
        if (userName == null || userName.trim().isEmpty()) {
            userName = email != null ? email.split("@")[0] : "User";
        }
        if (userRole == null) {
            userRole = "passenger";
        }
    }

    private double getDoubleValue(DocumentSnapshot doc, String field, double defaultValue) {
        try {
            Object value = doc.get(field);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error parsing double value for field: " + field);
        }
        return defaultValue;
    }

    private int getIntValue(DocumentSnapshot doc, String field, int defaultValue) {
        try {
            Object value = doc.get(field);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error parsing int value for field: " + field);
        }
        return defaultValue;
    }

    private void createDefaultUserProfile() {
        Log.i(TAG, "Creating default user profile");
        userName = email != null ? email.split("@")[0] : "User";
        rating = 4.8;
        totalRides = 0;
        totalSavings = 0.0;
        ridesShared = 0;
        co2Reduced = 0.0;
        userRole = "passenger";

        // Save default profile to Firestore
        saveUserProfileToFirestore(null);
    }

    private void calculateStatsFromDatabase() {
        try {
            RideDAO rideDAO = new RideDAO(context);

            // Calculate stats in background thread
            new Thread(() -> {
                try {
                    long userIdLong = getUserIdAsLong();

                    // Get user's rides as driver
                    List<Ride> driverRides = rideDAO.getRidesByDriverId(userIdLong);

                    // Calculate statistics
                    int completedRides = 0;
                    double totalEarnings = 0.0;
                    double totalDistance = 0.0;

                    for (Ride ride : driverRides) {
                        if (ride.isCompleted()) {
                            completedRides++;
                            totalEarnings += ride.getPrice();

                            if (ride.hasValidCoordinates()) {
                                double distance = com.companyname.shareride.utils.LocationUtils.calculateDistance(
                                        ride.getFromLatitude(), ride.getFromLongitude(),
                                        ride.getToLatitude(), ride.getToLongitude()
                                );
                                totalDistance += distance;
                            }
                        }
                    }

                    // Update statistics
                    ridesShared = completedRides;
                    totalSavings = totalEarnings;
                    co2Reduced = calculateCO2Reduction(totalDistance);
                    totalRides = completedRides; // For now, same as ridesShared

                    // Update legacy fields
                    updateLegacyFields();

                    rideDAO.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error calculating stats from database", e);
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing database for stats calculation", e);
        }
    }

    private double calculateCO2Reduction(double totalDistanceKm) {
        // Average car emits about 120g CO2 per km
        // Ride sharing reduces this by approximately 50%
        return (totalDistanceKm * 0.12 * 0.5); // Result in kg
    }

    private void handleDataLoadError(UserDataCallback callback, String error) {
        setGuestUserDefaults();
        isDataLoaded = true;
        if (callback != null) {
            callback.onUserDataError("Error loading user data: " + error);
        }
    }

    private void setGuestUserDefaults() {
        userName = "Guest User";
        rating = 4.8;
        totalRides = 24;
        totalSavings = 840.0;
        ridesShared = 12;
        co2Reduced = 2.4;
        userRole = "passenger";
        updateLegacyFields();
    }

    private void updateLegacyFields() {
        // Update legacy string fields for backward compatibility
        ratingString = String.format("⭐⭐⭐⭐⭐ %.1f (%d rides)", rating, totalRides);
        savingsString = String.format("💰 Total Saved: ₹%.0f", totalSavings);
        ridesSharedString = String.format("🚗 Rides Shared: %d", ridesShared);
        co2ReducedString = String.format("🌱 CO₂ Reduced: %.1f kg", co2Reduced);
    }

    // Enhanced user profile update methods
    public void updateUserProfile(String name, String phone, UserUpdateCallback callback) {
        if (!isLoggedIn()) {
            if (callback != null) {
                callback.onUserUpdateError("User not logged in");
            }
            return;
        }

        this.userName = name;
        this.phoneNumber = phone;

        saveUserProfileToFirestore(callback);
    }

    public void updateUserRating(double newRating, UserUpdateCallback callback) {
        if (!isLoggedIn()) {
            if (callback != null) {
                callback.onUserUpdateError("User not logged in");
            }
            return;
        }

        this.rating = newRating;
        updateLegacyFields();

        Map<String, Object> updates = new HashMap<>();
        updates.put("rating", rating);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onUserDataUpdated();
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onUserUpdateError(e.getMessage());
                    }
                });
    }

    private void saveUserProfileToFirestore(UserUpdateCallback callback) {
        if (!isLoggedIn()) return;

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("name", userName);
        userProfile.put("email", email);
        userProfile.put("phoneNumber", phoneNumber);
        userProfile.put("profilePictureUrl", profilePictureUrl);
        userProfile.put("rating", rating);
        userProfile.put("totalRides", totalRides);
        userProfile.put("totalSavings", totalSavings);
        userProfile.put("ridesShared", ridesShared);
        userProfile.put("co2Reduced", co2Reduced);
        userProfile.put("userRole", userRole);
        userProfile.put("lastUpdated", System.currentTimeMillis());

        db.collection("users").document(userId)
                .set(userProfile)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile updated successfully");
                    if (callback != null) {
                        callback.onUserDataUpdated();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user profile", e);
                    if (callback != null) {
                        callback.onUserUpdateError(e.getMessage());
                    }
                });
    }

    // Enhanced getters
    public String getUserId() {
        return userId != null ? userId : "";
    }

    public long getUserIdAsLong() {
        // Convert Firebase UID to a numeric ID for database operations
        // This is a simple hash-based approach - you might want to use a proper mapping
        return userId != null ? Math.abs(userId.hashCode()) : 1;
    }

    public String getUserName() {
        return userName != null ? userName : "User";
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public String getPhoneNumber() {
        return phoneNumber != null ? phoneNumber : "";
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public double getNumericRating() {
        return rating;
    }

    public int getTotalRides() {
        return totalRides;
    }

    public double getTotalSavings() {
        return totalSavings;
    }

    public int getRidesShared() {
        return ridesShared;
    }

    public double getCo2Reduced() {
        return co2Reduced;
    }

    public String getUserRole() {
        return userRole != null ? userRole : "passenger";
    }

    public boolean isDriver() {
        return "driver".equals(userRole) || "both".equals(userRole);
    }

    public boolean isPassenger() {
        return "passenger".equals(userRole) || "both".equals(userRole);
    }

    public boolean isEmailVerified() {
        return isEmailVerified;
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    // Legacy getters for backward compatibility
    public String getRating() {
        return ratingString != null ? ratingString : "⭐⭐⭐⭐⭐ 4.8 (0 rides)";
    }

    public String getSavings() {
        return savingsString != null ? savingsString : "💰 Total Saved: ₹0";
    }

    public String getRidesSharedString() {
        return ridesSharedString != null ? ridesSharedString : "🚗 Rides Shared: 0";
    }

    public String getCo2ReducedString() {
        return co2ReducedString != null ? co2ReducedString : "🌱 CO₂ Reduced: 0.0 kg";
    }

    public boolean isDataLoaded() {
        return isDataLoaded;
    }

    public String getShortRating() {
        String stars = generateStars(rating);
        return String.format("%s %.1f", stars, rating);
    }

    private String generateStars(double rating) {
        int fullStars = (int) Math.floor(rating);
        StringBuilder stars = new StringBuilder();

        for (int i = 0; i < fullStars && i < 5; i++) {
            stars.append("⭐");
        }

        return stars.toString();
    }

    // User state management
    public void signOut() {
        auth.signOut();
        clearUserData();
    }

    public void clearUserData() {
        userId = null;
        userName = null;
        email = null;
        phoneNumber = null;
        profilePictureUrl = null;
        rating = 0.0;
        totalRides = 0;
        totalSavings = 0.0;
        ridesShared = 0;
        co2Reduced = 0.0;
        userRole = null;
        isDataLoaded = false;
        isEmailVerified = false;

        // Clear legacy fields
        ratingString = null;
        savingsString = null;
        ridesSharedString = null;
        co2ReducedString = null;
    }

    public void refreshUserData(UserDataCallback callback) {
        isDataLoaded = false;
        loadUserData(callback);
    }

    // Utility methods for ride operations
    public void incrementRideCount() {
        ridesShared++;
        totalRides++;
        updateLegacyFields();
    }

    public void addEarnings(double amount) {
        totalSavings += amount;
        updateLegacyFields();
    }

    public void addCO2Reduction(double kg) {
        co2Reduced += kg;
        updateLegacyFields();
    }

    // Sync local changes to Firestore
    public void syncToFirestore(UserUpdateCallback callback) {
        if (isLoggedIn()) {
            saveUserProfileToFirestore(callback);
        }
    }
}
