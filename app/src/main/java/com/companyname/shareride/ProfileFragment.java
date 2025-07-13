package com.companyname.shareride;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private TextView tvUserName, tvRating, tvSavings, tvRidesShared, tvCO2Reduced;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initViews(view);
        setupUserData();

        return view;
    }

    private void initViews(View view) {
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvRating = view.findViewById(R.id.tv_rating);
        tvSavings = view.findViewById(R.id.tv_savings);
        tvRidesShared = view.findViewById(R.id.tv_rides_shared);
        tvCO2Reduced = view.findViewById(R.id.tv_co2_reduced);
    }

    private void setupUserData() {
        tvUserName.setText("Raj Kumar");
        tvRating.setText("⭐⭐⭐⭐⭐ 4.8 (24 rides)");
        tvSavings.setText("💰 Total Saved: ₹840");
        tvRidesShared.setText("🚗 Rides Shared: 12");
        tvCO2Reduced.setText("🌱 CO₂ Reduced: 2.4 kg");
    }
}