package com.companyname.shareride;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.companyname.shareride.database.RideDAO;
import com.companyname.shareride.utils.LocationSearchHelper;
import com.companyname.shareride.utils.LocationUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CreateRideFragment extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2000;

    // UI Components
    private AutoCompleteTextView etFromLocation, etToLocation;
    private Button btnCurrentLocation, btnSelectDateTime, btnCreateRide, btnCancel;
    private TextInputEditText etSeats, etPrice, etDescription;
    private TextView tvSelectedDateTime, tvDistanceInfo, tvEstimatedDuration;
    private ProgressBar progressBar;

    // Data and Managers
    private RideDAO rideDAO;
    private UserDataManager userDataManager;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // Location coordinates
    private double fromLatitude = 0.0;
    private double fromLongitude = 0.0;
    private double toLatitude = 0.0;
    private double toLongitude = 0.0;

    // Date and time
    private Calendar selectedDateTime;
    private SimpleDateFormat dateTimeFormatter;

    // State management
    private boolean isRequestingLocation = false;
    private boolean isCreatingRide = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_ride, container, false);

        // Initialize managers and services
        initializeServices();

        // Initialize views
        initViews(view);

        // Setup functionality
        setupLocationSearch();
        setupClickListeners();
        setupLocationCallback();

        // Initialize date/time
        selectedDateTime = Calendar.getInstance();
        selectedDateTime.add(Calendar.HOUR_OF_DAY, 1); // Default to 1 hour from now
        dateTimeFormatter = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
        updateDateTimeDisplay();

        return view;
    }

    private void initializeServices() {
        try {
            rideDAO = new RideDAO(getContext());
            userDataManager = UserDataManager.getInstance();
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error initializing services", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews(View view) {
        // Location inputs
        etFromLocation = view.findViewById(R.id.et_from_location);
        etToLocation = view.findViewById(R.id.et_to_location);
        btnCurrentLocation = view.findViewById(R.id.btn_current_location);

        // Date/time selection
        btnSelectDateTime = view.findViewById(R.id.btn_select_datetime);
        tvSelectedDateTime = view.findViewById(R.id.tv_selected_datetime);

        // Ride details
        etSeats = view.findViewById(R.id.et_seats);
        etPrice = view.findViewById(R.id.et_price);
        etDescription = view.findViewById(R.id.et_description);

        // Info displays
        tvDistanceInfo = view.findViewById(R.id.tv_distance_info);
        tvEstimatedDuration = view.findViewById(R.id.tv_estimated_duration);

        // Action buttons
        btnCreateRide = view.findViewById(R.id.btn_create_ride);
        btnCancel = view.findViewById(R.id.btn_cancel);

        // Progress indicator
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupLocationSearch() {
        // Setup FROM location autocomplete
        if (etFromLocation != null) {
            LocationSearchHelper.setupLocationAutoComplete(etFromLocation, getContext(),
                    new LocationSearchHelper.OnLocationSelectedListener() {
                        @Override
                        public void onLocationSelected(LocationSearchHelper.LocationSuggestion location, AutoCompleteTextView view) {
                            fromLatitude = location.latitude;
                            fromLongitude = location.longitude;
                            view.setTag("coords:" + location.latitude + "," + location.longitude);
                            calculateRouteInfo();
                        }

                        @Override
                        public void onLocationCleared(AutoCompleteTextView view) {
                            fromLatitude = 0.0;
                            fromLongitude = 0.0;
                            view.setTag(null);
                            clearRouteInfo();
                        }
                    });
        }

        // Setup TO location autocomplete
        if (etToLocation != null) {
            LocationSearchHelper.setupLocationAutoComplete(etToLocation, getContext(),
                    new LocationSearchHelper.OnLocationSelectedListener() {
                        @Override
                        public void onLocationSelected(LocationSearchHelper.LocationSuggestion location, AutoCompleteTextView view) {
                            toLatitude = location.latitude;
                            toLongitude = location.longitude;
                            view.setTag("coords:" + location.latitude + "," + location.longitude);
                            calculateRouteInfo();
                        }

                        @Override
                        public void onLocationCleared(AutoCompleteTextView view) {
                            toLatitude = 0.0;
                            toLongitude = 0.0;
                            view.setTag(null);
                            clearRouteInfo();
                        }
                    });
        }
    }

    private void setupClickListeners() {
        // Current location button
        if (btnCurrentLocation != null) {
            btnCurrentLocation.setOnClickListener(v -> getCurrentLocation());
        }

        // Date/time selection button
        if (btnSelectDateTime != null) {
            btnSelectDateTime.setOnClickListener(v -> showDateTimePicker());
        }

        // Create ride button
        if (btnCreateRide != null) {
            btnCreateRide.setOnClickListener(v -> createRide());
        }

        // Cancel button
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
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
                    setCurrentLocationAsFrom(location);
                } else {
                    stopLocationUpdates();
                    Toast.makeText(requireContext(),
                            "Unable to get current location. Please try again.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private void getCurrentLocation() {
        if (isRequestingLocation) {
            Toast.makeText(requireContext(), "Location request already in progress...", Toast.LENGTH_SHORT).show();
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

        try {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(5000)
                    .setMaxUpdateDelayMillis(15000)
                    .setMaxUpdates(1)
                    .build();

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

            // Timeout after 15 seconds
            new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isRequestingLocation) {
                    stopLocationUpdates();
                    Toast.makeText(requireContext(),
                            "Location request timed out. Please try again.",
                            Toast.LENGTH_SHORT).show();
                }
            }, 15000);

        } catch (SecurityException e) {
            isRequestingLocation = false;
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        isRequestingLocation = false;
    }

    private void setCurrentLocationAsFrom(Location location) {
        try {
            fromLatitude = location.getLatitude();
            fromLongitude = location.getLongitude();

            // Use LocationUtils to get address name
            String locationName = LocationUtils.getAddressFromCoordinates(getContext(),
                    fromLatitude, fromLongitude);

            if (locationName != null && !locationName.isEmpty()) {
                etFromLocation.setText(locationName);
                etFromLocation.setTag("coords:" + fromLatitude + "," + fromLongitude);
                Toast.makeText(requireContext(), "Current location set", Toast.LENGTH_SHORT).show();
                calculateRouteInfo();
            } else {
                String coordsText = "Current Location (" + String.format("%.6f", fromLatitude) +
                        ", " + String.format("%.6f", fromLongitude) + ")";
                etFromLocation.setText(coordsText);
                etFromLocation.setTag("coords:" + fromLatitude + "," + fromLongitude);
                Toast.makeText(requireContext(), "Location set using coordinates", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error setting current location", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDateTimePicker() {
        Calendar currentDateTime = Calendar.getInstance();

        // Date picker first
        DatePickerDialog dateDialog = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // Time picker after date selection
                    TimePickerDialog timeDialog = new TimePickerDialog(getContext(),
                            (view1, hourOfDay, minute) -> {
                                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selectedDateTime.set(Calendar.MINUTE, minute);
                                updateDateTimeDisplay();
                            },
                            selectedDateTime.get(Calendar.HOUR_OF_DAY),
                            selectedDateTime.get(Calendar.MINUTE),
                            false);

                    timeDialog.show();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH));

        // Set minimum date to current date
        dateDialog.getDatePicker().setMinDate(currentDateTime.getTimeInMillis());
        dateDialog.show();
    }

    private void updateDateTimeDisplay() {
        if (tvSelectedDateTime != null && selectedDateTime != null) {
            tvSelectedDateTime.setText(dateTimeFormatter.format(selectedDateTime.getTime()));
        }
    }

    private void calculateRouteInfo() {
        if (fromLatitude != 0.0 && fromLongitude != 0.0 &&
                toLatitude != 0.0 && toLongitude != 0.0) {

            // Calculate distance
            double distance = LocationUtils.calculateDistance(fromLatitude, fromLongitude,
                    toLatitude, toLongitude);

            // Update UI with route information
            if (tvDistanceInfo != null) {
                tvDistanceInfo.setText("Distance: " + LocationUtils.formatDistance(distance));
                tvDistanceInfo.setVisibility(View.VISIBLE);
            }

            // Estimate duration (assuming average city speed of 25 km/h)
            if (tvEstimatedDuration != null) {
                int durationMinutes = (int) ((distance / 25.0) * 60);
                String duration = durationMinutes < 60 ?
                        durationMinutes + " mins" :
                        (durationMinutes / 60) + "h " + (durationMinutes % 60) + "m";
                tvEstimatedDuration.setText("Est. duration: " + duration);
                tvEstimatedDuration.setVisibility(View.VISIBLE);
            }
        } else {
            clearRouteInfo();
        }
    }

    private void clearRouteInfo() {
        if (tvDistanceInfo != null) {
            tvDistanceInfo.setVisibility(View.GONE);
        }
        if (tvEstimatedDuration != null) {
            tvEstimatedDuration.setVisibility(View.GONE);
        }
    }

    private void createRide() {
        if (isCreatingRide) {
            return; // Prevent multiple submissions
        }

        // Validate inputs
        String validationError = validateInputs();
        if (validationError != null) {
            Toast.makeText(getContext(), validationError, Toast.LENGTH_SHORT).show();
            return;
        }

        isCreatingRide = true;
        showLoading(true);

        // Create ride in background thread
        new Thread(() -> {
            try {
                // Create ride object
                Ride newRide = createRideObject();

                // Insert into database
                long rideId = rideDAO.createRide(newRide);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isCreatingRide = false;
                        showLoading(false);

                        if (rideId > 0) {
                            // Success
                            Toast.makeText(getContext(), "🎉 Ride created successfully!", Toast.LENGTH_LONG).show();

                            // Navigate to MyRidesFragment
                            getParentFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, new MyRidesFragment())
                                    .commit();
                        } else {
                            Toast.makeText(getContext(), "Failed to create ride. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isCreatingRide = false;
                        showLoading(false);
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private String validateInputs() {
        // Check from location
        if (etFromLocation.getText().toString().trim().isEmpty()) {
            return "Please enter pickup location";
        }
        if (fromLatitude == 0.0 && fromLongitude == 0.0) {
            return "Please select pickup location from suggestions";
        }

        // Check to location
        if (etToLocation.getText().toString().trim().isEmpty()) {
            return "Please enter destination";
        }
        if (toLatitude == 0.0 && toLongitude == 0.0) {
            return "Please select destination from suggestions";
        }

        // Check seats
        String seatsText = etSeats.getText().toString().trim();
        if (seatsText.isEmpty()) {
            return "Please enter number of available seats";
        }
        try {
            int seats = Integer.parseInt(seatsText);
            if (seats < 1 || seats > 8) {
                return "Available seats must be between 1 and 8";
            }
        } catch (NumberFormatException e) {
            return "Please enter a valid number of seats";
        }

        // Check price
        String priceText = etPrice.getText().toString().trim();
        if (priceText.isEmpty()) {
            return "Please enter price per passenger";
        }
        try {
            double price = Double.parseDouble(priceText);
            if (price < 0 || price > 1000) {
                return "Price must be between ₹0 and ₹1000";
            }
        } catch (NumberFormatException e) {
            return "Please enter a valid price";
        }

        // Check datetime
        if (selectedDateTime.getTimeInMillis() <= System.currentTimeMillis()) {
            return "Departure time must be in the future";
        }

        // Check user login
        if (!userDataManager.isLoggedIn()) {
            return "Please log in to create a ride";
        }

        return null; // All validations passed
    }

    private Ride createRideObject() {
        Ride ride = new Ride();

        // Location details
        ride.setFromAddress(etFromLocation.getText().toString().trim());
        ride.setFromLatitude(fromLatitude);
        ride.setFromLongitude(fromLongitude);
        ride.setToAddress(etToLocation.getText().toString().trim());
        ride.setToLatitude(toLatitude);
        ride.setToLongitude(toLongitude);

        // Ride details
        ride.setAvailableSeats(Integer.parseInt(etSeats.getText().toString().trim()));
        ride.setPrice(Double.parseDouble(etPrice.getText().toString().trim()));
        ride.setDepartureTime(selectedDateTime.getTimeInMillis());

        // Optional description
        String description = etDescription.getText().toString().trim();
        if (!description.isEmpty()) {
            ride.setDescription(description);
        }

        // Driver details
        ride.setDriverId(userDataManager.getUserIdAsLong());
        ride.setStatus("active");

        // Timestamps
        ride.setCreatedAt(System.currentTimeMillis());
        ride.setUpdatedAt(System.currentTimeMillis());

        return ride;
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnCreateRide != null) {
            btnCreateRide.setEnabled(!show);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
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
}
