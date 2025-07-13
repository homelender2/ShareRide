package com.companyname.shareride;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    private List<Ride> rides;
    private OnRideClickListener listener;

    public interface OnRideClickListener {
        void onJoinRide(Ride ride);
    }

    public RideAdapter(List<Ride> rides, OnRideClickListener listener) {
        this.rides = rides;
        this.listener = listener;
    }

    @Override
    public RideViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ride, parent, false);
        return new RideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RideViewHolder holder, int position) {
        Ride ride = rides.get(position);
        holder.bind(ride);
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    class RideViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoute, tvFare, tvPassengers, tvNames;
        Button btnJoin;

        public RideViewHolder(View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tv_route);
            tvFare = itemView.findViewById(R.id.tv_fare);
            tvPassengers = itemView.findViewById(R.id.tv_passengers);
            tvNames = itemView.findViewById(R.id.tv_names);
            btnJoin = itemView.findViewById(R.id.btn_join);
        }

        public void bind(Ride ride) {
            tvRoute.setText(ride.getRoute());
            tvFare.setText(ride.getFare());
            tvPassengers.setText(ride.getPassengers());
            tvNames.setText("👤 " + ride.getNames());

            btnJoin.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onJoinRide(ride);
                }
            });
        }
    }
}