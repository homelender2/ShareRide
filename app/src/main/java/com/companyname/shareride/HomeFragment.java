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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.companyname.shareride.database.RideDAO;
import com.companyname.shareride.utils.LocationSearchHelper;
import com.companyname.shareride.utils.LocationUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import android.widget.ArrayAdapter;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private MaterialAutoCompleteTextView spnWhen;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final double DEFAULT_SEARCH_RADIUS_KM = 10.0;

    // UI Components - Search Form
    private AutoCompleteTextView etFrom, etTo;
    private Button btnFindRides, btnCurrentLocation;

    // UI Components - User Profile
    private TextView tvUserName, tvRating, tvUserProfileAlphabet;

    // UI Components - Search Results
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvNoRidesFound, tvSearchInfo;
    private View layoutSearchForm, layoutSearchResults;
    private Button btnBackToSearch;

    // Data and Managers
    private UserDataManager userDataManager;
    private RideDAO rideDAO;
    private RideAdapter adapter;
    private List<Ride> rideList;

    // Location Services
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

        // Initialize managers
        userDataManager = UserDataManager.getInstance();
        rideDAO = new RideDAO(getContext());

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        setupLocationCallback();

        initViews(view);
        setupWhenSpinner();
        setupLocationSearch();
        setupClickListeners();
        setupRecyclerView();

        // Load user data
        loadUserData();

        return view;
    }

    private void initViews(View view) {
        // Search form components
        // Search form components
        etFrom = view.findViewById(R.id.et_from);
        etTo = view.findViewById(R.id.et_to);
        spnWhen = view.findViewById(R.id.spn_when); // This will get the MaterialAutoCompleteTextView
        btnFindRides = view.findViewById(R.id.btn_find_rides);
        btnCurrentLocation = view.findViewById(R.id.btn_current_location);

        // User profile components
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvRating = view.findViewById(R.id.tv_rating);
        tvUserProfileAlphabet = view.findViewById(R.id.tv_user_profile_alphabet);

        // Search results components (add these to your layout if not present)
        layoutSearchForm = view.findViewById(R.id.layout_search_form);
        layoutSearchResults = view.findViewById(R.id.layout_search_results);
        recyclerView = view.findViewById(R.id.recycler_rides);
        progressBar = view.findViewById(R.id.progress_bar);
        tvNoRidesFound = view.findViewById(R.id.tv_no_rides_found);
        tvSearchInfo = view.findViewById(R.id.tv_search_info);
        btnBackToSearch = view.findViewById(R.id.btn_back_to_search);

        // Initially show search form
        showSearchForm();
    }

    private void setupLocationSearch() {
        // Setup autocomplete for destination field
        if (etTo != null) {
            LocationSearchHelper.setupLocationAutoComplete(etTo, getContext(),
                    new LocationSearchHelper.OnLocationSelectedListener() {
                        @Override
                        public void onLocationSelected(LocationSearchHelper.LocationSuggestion location, AutoCompleteTextView view) {
                            toLatitude = location.latitude;
                            toLongitude = location.longitude;
                            view.setTag("coords:" + location.latitude + "," + location.longitude);
                        }

                        @Override
                        public void onLocationCleared(AutoCompleteTextView view) {
                            toLatitude = 0.0;
                            toLongitude = 0.0;
                            view.setTag(null);
                        }
                    });
        }
    }

    private void setupRecyclerView() {
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            rideList = new ArrayList<>();
            adapter = new RideAdapter(rideList, this::onJoinRide);
            recyclerView.setAdapter(adapter);
        }
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    stopLocationUpdates();
                    getAddressFromLocation(location);
                } else {
                    stopLocationUpdates();
                    Toast.makeText(requireContext(),
                            "Unable to get current location. Please try again.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private void loadUserData() {
        if (userDataManager.isDataLoaded()) {
            updateUI();
        } else {
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
                        getActivity().runOnUiThread(() -> updateUI());
                    }
                }
            });
        }
    }

    private void updateUI() {
        String userName = userDataManager.getUserName();
        tvUserProfileAlphabet.setText("" + userName.charAt(0));
        tvUserName.setText("Welcome back, " + userName.split("\\s+")[0] + "!");
        tvRating.setText(userDataManager.getShortRating());
    }

    private void setupClickListeners() {
        btnFindRides.setOnClickListener(v -> performRideSearch());
        btnCurrentLocation.setOnClickListener(v -> getCurrentLocation());

        if (btnBackToSearch != null) {
            btnBackToSearch.setOnClickListener(v -> showSearchForm());
        }
    }

    private void performRideSearch() {
        // Validate input
        if (etFrom.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Please enter pickup location", Toast.LENGTH_SHORT).show();
            return;
        }

        if (etTo.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Please enter destination", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if we have coordinates for both locations
        double[] fromCoords = getFromCoordinates();
        double[] toCoords = getToCoordinates();

        if (fromCoords[0] == 0.0 && fromCoords[1] == 0.0) {
            Toast.makeText(getContext(), "Please select a valid pickup location", Toast.LENGTH_SHORT).show();
            return;
        }

        if (toCoords[0] == 0.0 && toCoords[1] == 0.0) {
            Toast.makeText(getContext(), "Please select a valid destination", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show search results view
        showSearchResults();
        showLoading(true);

        // Perform search in background
        new Thread(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                long endTime = currentTime + (24 * 60 * 60 * 1000); // 24 hours from now

                List<Ride> searchResults = rideDAO.searchRides(
                        fromCoords[0], fromCoords[1],
                        toCoords[0], toCoords[1],
                        DEFAULT_SEARCH_RADIUS_KM,
                        currentTime,
                        endTime,
                        1 // minimum 1 seat available
                );

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        rideList.clear();
                        rideList.addAll(searchResults);
                        adapter.notifyDataSetChanged();
                        showLoading(false);
                        updateSearchResultsUI();

                        String distance = LocationUtils.formatDistance(
                                LocationUtils.calculateDistance(fromCoords[0], fromCoords[1], toCoords[0], toCoords[1])
                        );
                        updateSearchInfo("Found " + searchResults.size() + " rides • Distance: " + distance);
                    });
                }

            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), "Search error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();

                        // Fallback to sample data
                        loadSampleRides();
                    });
                }
            }
        }).start();
    }

    private void loadSampleRides() {
        rideList.clear();

        // Create sample rides
        long currentTime = System.currentTimeMillis();
        Ride ride1 = new Ride("Koramangala → Electronic City", "₹40 each", "2/3 passengers", "Leaving in 10 mins", "Priya, Amit");
        Ride ride2 = new Ride("Koramangala → Electronic City", "₹50 each", "1/3 passengers", "Leaving in 25 mins", "Neha");
        Ride ride3 = new Ride("BTM Layout → Whitefield", "₹60 each", "2/4 passengers", "Leaving in 15 mins", "Arjun, Kavya");

        rideList.add(ride1);
        rideList.add(ride2);
        rideList.add(ride3);

        adapter.notifyDataSetChanged();
        updateSearchResultsUI();
        updateSearchInfo("Showing sample rides (database unavailable)");
    }

    private void onJoinRide(Ride ride) {
        // Check if ride has available seats (for database rides)
        if (ride.getId() > 0 && ride.getAvailableSeats() <= 0) {
            Toast.makeText(getContext(), "Sorry, this ride is full!", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "🎉 Ride joined! You will be notified when other passengers confirm.",
                Toast.LENGTH_LONG).show();

        // If it's a database ride, update the seat count
        if (ride.getId() > 0) {
            new Thread(() -> {
                try {
                    int newSeats = ride.getAvailableSeats() - 1;
                    rideDAO.updateAvailableSeats(ride.getId(), newSeats);

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // Refresh the search results
                            performRideSearch();
                        });
                    }
                } catch (Exception e) {
                    // Handle error silently
                }
            }).start();
        }

        // Navigate to MyRidesFragment
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new MyRidesFragment())
                .addToBackStack(null)
                .commit();
    }

    private void showSearchForm() {
        if (layoutSearchForm != null) layoutSearchForm.setVisibility(View.VISIBLE);
        if (layoutSearchResults != null) layoutSearchResults.setVisibility(View.GONE);
    }

    private void showSearchResults() {
        if (layoutSearchForm != null) layoutSearchForm.setVisibility(View.GONE);
        if (layoutSearchResults != null) layoutSearchResults.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void updateSearchResultsUI() {
        if (tvNoRidesFound != null && recyclerView != null) {
            if (rideList.isEmpty()) {
                tvNoRidesFound.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                tvNoRidesFound.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateSearchInfo(String message) {
        if (tvSearchInfo != null) {
            tvSearchInfo.setText(message);
            tvSearchInfo.setVisibility(View.VISIBLE);
        }
    }

    // Keep all your existing location methods unchanged
    private void getCurrentLocation() {
        if (isRequestingLocation) {
            Toast.makeText(requireContext(), "Location request already in progress...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isLocationEnabled()) {
            showLocationSettingsDialog();
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        Toast.makeText(requireContext(), "Getting your location...", Toast.LENGTH_SHORT).show();
        isRequestingLocation = true;

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            isRequestingLocation = false;
                            getAddressFromLocation(location);
                        } else {
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
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
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
            fromLatitude = location.getLatitude();
            fromLongitude = location.getLongitude();

            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = buildDetailedAddress(address);
                etFrom.setText(addressText);
                etFrom.setTag("coords:" + fromLatitude + "," + fromLongitude);
                Toast.makeText(requireContext(), "Current location set", Toast.LENGTH_SHORT).show();
            } else {
                String coordsText = "Lat: " + String.format("%.6f", fromLatitude) +
                        ", Long: " + String.format("%.6f", fromLongitude);
                etFrom.setText(coordsText);
                etFrom.setTag("coords:" + fromLatitude + "," + fromLongitude);
                Toast.makeText(requireContext(), "Location set using coordinates", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            fromLatitude = location.getLatitude();
            fromLongitude = location.getLongitude();

            String coordsText = "Lat: " + String.format("%.6f", fromLatitude) +
                    ", Long: " + String.format("%.6f", fromLongitude);
            etFrom.setText(coordsText);
            etFrom.setTag("coords:" + fromLatitude + "," + fromLongitude);
            Toast.makeText(requireContext(), "Location set using coordinates", Toast.LENGTH_SHORT).show();
        }
    }

    private String buildDetailedAddress(Address address) {
        StringBuilder addressBuilder = new StringBuilder();

        if (address.getSubThoroughfare() != null) {
            addressBuilder.append(address.getSubThoroughfare()).append(" ");
        }
        if (address.getThoroughfare() != null) {
            addressBuilder.append(address.getThoroughfare()).append(", ");
        }
        if (address.getSubLocality() != null) {
            addressBuilder.append(address.getSubLocality()).append(", ");
        }
        if (address.getLocality() != null) {
            addressBuilder.append(address.getLocality()).append(", ");
        }
        if (address.getAdminArea() != null) {
            addressBuilder.append(address.getAdminArea());
        }
        if (address.getPostalCode() != null) {
            addressBuilder.append(" ").append(address.getPostalCode());
        }

        String result = addressBuilder.toString();
        result = result.replaceAll(",\\s*$", "").trim();

        return result.isEmpty() ? "Current Location" : result;
    }

    public double[] getFromCoordinates() {
        String tag = (String) etFrom.getTag();
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
        stopLocationUpdates();
        if (rideDAO != null) {
            rideDAO.close();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userDataManager.isDataLoaded()) {
            updateUI();
        }
    }

    private void setupWhenSpinner() {
        // Create dropdown options
        String[] timeOptions = {
                "Now",
                "In 15 minutes",
                "In 30 minutes",
                "In 1 hour",
                "In 2 hours",
                "Today evening",
                "Tomorrow morning",
                "Tomorrow evening"
        };

        // Create adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                timeOptions
        );

        // Set adapter
        spnWhen.setAdapter(adapter);

        // Set default selection
        spnWhen.setText(timeOptions[0], false);

        // Handle selection
        spnWhen.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTime = timeOptions[position];
            Toast.makeText(getContext(), "Selected: " + selectedTime, Toast.LENGTH_SHORT).show();
        });
    }


}
