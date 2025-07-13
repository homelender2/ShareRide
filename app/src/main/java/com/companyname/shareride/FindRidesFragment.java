package com.companyname.shareride;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FindRidesFragment extends Fragment {

    private RecyclerView recyclerView;
    private RideAdapter adapter;
    private List<Ride> rideList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_find_rides, container, false);

        recyclerView = view.findViewById(R.id.recycler_rides);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        setupRideList();
        adapter = new RideAdapter(rideList, this::onJoinRide);
        recyclerView.setAdapter(adapter);

        return view;
    }

    private void setupRideList() {
        rideList = new ArrayList<>();
        rideList.add(new Ride("Koramangala → Electronic City", "₹40 each", "2/3 passengers", "Leaving in 10 mins", "Priya, Amit"));
        rideList.add(new Ride("Koramangala → Electronic City", "₹50 each", "1/3 passengers", "Leaving in 25 mins", "Neha"));
        rideList.add(new Ride("BTM Layout → Whitefield", "₹60 each", "2/4 passengers", "Leaving in 15 mins", "Arjun, Kavya"));
    }

    private void onJoinRide(Ride ride) {
        Toast.makeText(getContext(), "🎉 Ride joined! You will be notified when other passengers confirm.", Toast.LENGTH_LONG).show();

        // Navigate to MyRidesFragment
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new MyRidesFragment())
                .commit();
    }
}