package com.companyname.shareride;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private EditText etFrom, etTo;
    private Spinner spnWhen;
    private Button btnFindRides, btnCreateRoute;
    private TextView tvUserName, tvRating;
    private UserDataManager userDataManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize UserDataManager
        userDataManager = UserDataManager.getInstance();

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
        btnCreateRoute = view.findViewById(R.id.btn_create_route);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvRating = view.findViewById(R.id.tv_rating);
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
        // Set welcome message with user name
        String userName = userDataManager.getUserName();
        tvUserName.setText("Welcome back, " + userName + "!");

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

        btnCreateRoute.setOnClickListener(v -> {
            // Navigate to CreateRouteFragment
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new CreateRouteFragment())
                    .addToBackStack(null)
                    .commit();
        });
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