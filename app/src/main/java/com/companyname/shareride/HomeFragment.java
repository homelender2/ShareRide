package com.companyname.shareride;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private EditText etFrom, etTo;
    private Spinner spnWhen;
    private Button btnFindRides, btnCurrentLocation;
    private TextView tvUserName, tvRating, tvUserProfileAlphabet;
    private UserDataManager userDataManager;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isRequestingLocation = false;

    // Store coordinates for precise location matching
    private double fromLatitude = 0.0;
    private double fromLongitude = 0.0;
    private double toLatitude = 0.0;
    private double toLongitude = 0.0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize UserDataManager
        userDataManager = UserDataManager.getInstance();

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    // Stop location updates
                    stopLocationUpdates();
                    // Get address from coordinates
                    getAddressFromLocation(location);
                } else {
                    stopLocationUpdates();
                    Toast.makeText(requireContext(),
                            "Unable to get current location. Please try again.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        };

        initViews(view);
        setupClickListeners();

        // Load user data
        loadUserData();

        return view;
    }

    private void initViews(View view) {
        etFrom = view.findViewById(R.id.et_from);
        etTo = view.findViewById(R.id.et_to);
        spnWhen = view.findViewById(R.id.spn_when);
        btnFindRides = view.findViewById(R.id.btn_find_rides);
        btnCurrentLocation = view.findViewById(R.id.btn_current_location);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvRating = view.findViewById(R.id.tv_rating);
        tvUserProfileAlphabet = view.findViewById(R.id.tv_user_profile_alphabet);
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
        // Get Username
        String userName = userDataManager.getUserName();

        // Set Profile Alphabet
        tvUserProfileAlphabet.setText("" + userName.charAt(0));

        // Set welcome message with user name
        tvUserName.setText("Welcome back, " + userName.split("\\s+")[0] + "!");

        // Set short rating
        tvRating.setText(userDataManager.getShortRating());
    }

    private void setupClickListeners() {
        btnFindRides.setOnClickListener(v -> {
            // Navigate to FindRidesFragment
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new FindRidesFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnCurrentLocation.setOnClickListener(v -> {
            getCurrentLocation();
        });
    }

    private void getCurrentLocation() {
        // Prevent multiple simultaneous requests
        if (isRequestingLocation) {
            Toast.makeText(requireContext(), "Location request already in progress...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if location services are enabled
        if (!isLocationEnabled()) {
            showLocationSettingsDialog();
            return;
        }

        // Check if location permission is granted
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Show loading message
        Toast.makeText(requireContext(), "Getting your location...", Toast.LENGTH_SHORT).show();
        isRequestingLocation = true;

        // Try to get last known location first
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // We have a recent location, use it
                            isRequestingLocation = false;
                            getAddressFromLocation(location);
                        } else {
                            // No recent location, request fresh location
                            requestNewLocation();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    isRequestingLocation = false;
                    Toast.makeText(requireContext(),
                            "Error getting location: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void showLocationSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Location Services Disabled")
                .setMessage("Please enable location services to use this feature.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    // Open location settings
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void requestNewLocation() {
        try {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(5000)
                    .setMaxUpdateDelayMillis(15000)
                    .setMaxUpdates(1)
                    .build();

            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

                // Set a timeout to stop location updates after 15 seconds
                new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isRequestingLocation) {
                        stopLocationUpdates();
                        Toast.makeText(requireContext(),
                                "Location request timed out. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                }, 15000);
            }
        } catch (Exception e) {
            isRequestingLocation = false;
            Toast.makeText(requireContext(),
                    "Error requesting location: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        isRequestingLocation = false;
    }

    private void getAddressFromLocation(Location location) {
        try {
            // Store the coordinates for precise searching
            fromLatitude = location.getLatitude();
            fromLongitude = location.getLongitude();

            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Build a more detailed address string for better searching
                String addressText = buildDetailedAddress(address);

                // Set the address in the EditText
                etFrom.setText(addressText);

                // Store coordinates as tag for later use
                etFrom.setTag("coords:" + fromLatitude + "," + fromLongitude);

                Toast.makeText(requireContext(),
                        "Current location set",
                        Toast.LENGTH_SHORT).show();
            } else {
                // Fallback to coordinates if geocoding fails
                String coordsText = "Lat: " + String.format("%.6f", fromLatitude) +
                        ", Long: " + String.format("%.6f", fromLongitude);
                etFrom.setText(coordsText);
                etFrom.setTag("coords:" + fromLatitude + "," + fromLongitude);

                Toast.makeText(requireContext(),
                        "Location set using coordinates",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            // Fallback to coordinates if geocoding fails
            fromLatitude = location.getLatitude();
            fromLongitude = location.getLongitude();

            String coordsText = "Lat: " + String.format("%.6f", fromLatitude) +
                    ", Long: " + String.format("%.6f", fromLongitude);
            etFrom.setText(coordsText);
            etFrom.setTag("coords:" + fromLatitude + "," + fromLongitude);

            Toast.makeText(requireContext(),
                    "Location set using coordinates",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private String buildDetailedAddress(Address address) {
        StringBuilder addressBuilder = new StringBuilder();

        // Add street address
        if (address.getSubThoroughfare() != null) {
            addressBuilder.append(address.getSubThoroughfare()).append(" ");
        }
        if (address.getThoroughfare() != null) {
            addressBuilder.append(address.getThoroughfare()).append(", ");
        }

        // Add locality/area
        if (address.getSubLocality() != null) {
            addressBuilder.append(address.getSubLocality()).append(", ");
        }

        // Add city
        if (address.getLocality() != null) {
            addressBuilder.append(address.getLocality()).append(", ");
        }

        // Add state/province
        if (address.getAdminArea() != null) {
            addressBuilder.append(address.getAdminArea());
        }

        // Add postal code
        if (address.getPostalCode() != null) {
            addressBuilder.append(" ").append(address.getPostalCode());
        }

        String result = addressBuilder.toString();
        // Clean up trailing comma and spaces
        result = result.replaceAll(",\\s*$", "").trim();

        return result.isEmpty() ? "Current Location" : result;
    }

    // Method to get coordinates from EditText
    public double[] getFromCoordinates() {
        String tag = (String) etFrom.getTag();
        if (tag != null && tag.startsWith("coords:")) {
            String coords = tag.substring(7); // Remove "coords:" prefix
            String[] parts = coords.split(",");
            if (parts.length == 2) {
                try {
                    return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
                } catch (NumberFormatException e) {
                    return new double[]{0.0, 0.0};
                }
            }
        }
        return new double[]{0.0, 0.0};
    }

    // Method to get coordinates from destination (you can implement similar for "To" field)
    public double[] getToCoordinates() {
        String tag = (String) etTo.getTag();
        if (tag != null && tag.startsWith("coords:")) {
            String coords = tag.substring(7);
            String[] parts = coords.split(",");
            if (parts.length == 2) {
                try {
                    return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
                } catch (NumberFormatException e) {
                    return new double[]{0.0, 0.0};
                }
            }
        }
        return new double[]{0.0, 0.0};
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, check if location services are enabled
                if (isLocationEnabled()) {
                    getCurrentLocation();
                } else {
                    showLocationSettingsDialog();
                }
            } else {
                Toast.makeText(requireContext(),
                        "Location permission denied. Cannot get current location.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up location updates
        stopLocationUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user data when fragment resumes
        if (userDataManager.isDataLoaded()) {
            updateUI();
        }
    }
}