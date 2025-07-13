package com.companyname.shareride;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

public class CreateRouteFragment extends Fragment {

    private EditText etRouteName, etFrom, etTo, etFare;
    private Spinner spnMaxPassengers;
    private Button btnCreateRoute;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_route, container, false);

        initViews(view);
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        etRouteName = view.findViewById(R.id.et_route_name);
        etFrom = view.findViewById(R.id.et_from);
        etTo = view.findViewById(R.id.et_to);
        etFare = view.findViewById(R.id.et_fare);
        spnMaxPassengers = view.findViewById(R.id.spn_max_passengers);
        btnCreateRoute = view.findViewById(R.id.btn_create_route);
    }

    private void setupClickListeners() {
        btnCreateRoute.setOnClickListener(v -> {
            Toast.makeText(getContext(), "✅ Route created! You will be notified when someone wants to join.", Toast.LENGTH_LONG).show();

            // Navigate to MyRidesFragment
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MyRidesFragment())
                    .commit();
        });
    }
}