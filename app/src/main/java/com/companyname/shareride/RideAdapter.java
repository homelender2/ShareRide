package com.companyname.shareride;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.companyname.shareride.utils.LocationUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    private List<Ride> rides;
    private OnRideActionListener listener;

    public interface OnRideActionListener {
        void onJoinRide(Ride ride);
    }

    // Constructor with backward compatibility
    public RideAdapter(List<Ride> rides, OnRideActionListener listener) {
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
        return rides != null ? rides.size() : 0;
    }

    class RideViewHolder extends RecyclerView.ViewHolder {
        // UI Components
        TextView tvRoute, tvFare, tvPassengers, tvNames, tvTime, tvStatus, tvDistance;
        Button btnJoin;
        ImageButton btnMore;
        View statusIndicator;

        public RideViewHolder(View itemView) {
            super(itemView);

            // Find views with null checks
            tvRoute = itemView.findViewById(R.id.tv_route);
            tvFare = itemView.findViewById(R.id.tv_fare);
            tvPassengers = itemView.findViewById(R.id.tv_passengers);
            tvNames = itemView.findViewById(R.id.tv_names);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            btnJoin = itemView.findViewById(R.id.btn_join);
            btnMore = itemView.findViewById(R.id.btn_more);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }

        public void bind(Ride ride) {
            if (ride == null) return;

            try {
                // Handle both legacy and database rides
                if (ride.getId() > 0) {
                    // Database ride - use enhanced fields
                    bindDatabaseRide(ride);
                } else {
                    // Legacy ride - use old fields
                    bindLegacyRide(ride);
                }

                // Set click listeners
                setupClickListeners(ride);

            } catch (Exception e) {
                e.printStackTrace();
                // Fallback to basic display
                bindBasicRideInfo(ride);
            }
        }

        private void bindDatabaseRide(Ride ride) {
            // Route information
            if (tvRoute != null) {
                String route = ride.getFromAddress() + " → " + ride.getToAddress();
                if (route.length() > 40) {
                    route = ride.getShortRoute();
                }
                tvRoute.setText(route);
            }

            // Fare information
            if (tvFare != null) {
                tvFare.setText(ride.getFormattedPrice());
            }

            // Passengers/Seats information
            if (tvPassengers != null) {
                String seatsInfo = ride.getAvailableSeats() + " seats available";
                tvPassengers.setText(seatsInfo);
            }

            // Departure time
            if (tvTime != null) {
                tvTime.setText(ride.getFormattedDepartureTime());
            }

            // Status information
            if (tvStatus != null) {
                tvStatus.setText(ride.getStatus().toUpperCase());
                updateStatusColor(ride.getStatus());
            }

            // Names (if available)
            if (tvNames != null) {
                if (ride.getNames() != null && !ride.getNames().isEmpty()) {
                    tvNames.setText("👤 " + ride.getNames());
                    tvNames.setVisibility(View.VISIBLE);
                } else {
                    tvNames.setVisibility(View.GONE);
                }
            }

            // Distance (if coordinates are available)
            if (tvDistance != null && ride.hasValidCoordinates()) {
                double distance = LocationUtils.calculateDistance(
                        ride.getFromLatitude(), ride.getFromLongitude(),
                        ride.getToLatitude(), ride.getToLongitude()
                );
                tvDistance.setText(LocationUtils.formatDistance(distance));
                tvDistance.setVisibility(View.VISIBLE);
            } else if (tvDistance != null) {
                tvDistance.setVisibility(View.GONE);
            }

            // Update button text based on ride status and available seats
            updateJoinButton(ride);
        }

        private void bindLegacyRide(Ride ride) {
            // Use legacy getters for backward compatibility
            if (tvRoute != null) {
                tvRoute.setText(ride.getRoute());
            }

            if (tvFare != null) {
                tvFare.setText(ride.getFare());
            }

            if (tvPassengers != null) {
                tvPassengers.setText(ride.getPassengers());
            }

            if (tvTime != null) {
                tvTime.setText(ride.getTime());
            }

            if (tvNames != null && ride.getNames() != null) {
                tvNames.setText("👤 " + ride.getNames());
                tvNames.setVisibility(View.VISIBLE);
            }

            // Hide database-specific views
            if (tvStatus != null) tvStatus.setVisibility(View.GONE);
            if (tvDistance != null) tvDistance.setVisibility(View.GONE);
            if (statusIndicator != null) statusIndicator.setVisibility(View.GONE);

            // Standard join button for legacy rides
            if (btnJoin != null) {
                btnJoin.setText("Join Ride");
                btnJoin.setVisibility(View.VISIBLE);
            }
        }

        private void bindBasicRideInfo(Ride ride) {
            // Fallback binding in case of errors
            if (tvRoute != null) {
                String route = ride.getRoute() != null ? ride.getRoute() : "Route unavailable";
                tvRoute.setText(route);
            }

            if (tvFare != null) {
                String fare = ride.getFare() != null ? ride.getFare() : "Fare: N/A";
                tvFare.setText(fare);
            }

            if (btnJoin != null) {
                btnJoin.setText("View Details");
                btnJoin.setVisibility(View.VISIBLE);
            }
        }

        private void updateJoinButton(Ride ride) {
            if (btnJoin == null) return;

            if (!ride.isActive()) {
                if (ride.isCompleted()) {
                    btnJoin.setText("Completed");
                    btnJoin.setEnabled(false);
                } else if (ride.isCancelled()) {
                    btnJoin.setText("Cancelled");
                    btnJoin.setEnabled(false);
                } else {
                    btnJoin.setText("Unavailable");
                    btnJoin.setEnabled(false);
                }
            } else if (!ride.hasAvailableSeats()) {
                btnJoin.setText("Full");
                btnJoin.setEnabled(false);
            } else if (!ride.isDepartureInFuture()) {
                btnJoin.setText("Departed");
                btnJoin.setEnabled(false);
            } else {
                btnJoin.setText("Join Ride");
                btnJoin.setEnabled(true);
            }
        }

        private void updateStatusColor(String status) {
            if (statusIndicator == null) return;

            int color;
            switch (status.toLowerCase()) {
                case "active":
                    color = 0xFF4CAF50; // Green
                    break;
                case "completed":
                    color = 0xFF2196F3; // Blue
                    break;
                case "cancelled":
                    color = 0xFFF44336; // Red
                    break;
                default:
                    color = 0xFF9E9E9E; // Gray
                    break;
            }
            statusIndicator.setBackgroundColor(color);
        }

        private void setupClickListeners(Ride ride) {
            // Main join button click
            if (btnJoin != null) {
                btnJoin.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onJoinRide(ride);
                    }
                });
            }

            // More options button (if available)
            if (btnMore != null) {
                btnMore.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onJoinRide(ride); // For now, same as join
                    }
                });
            }

            // Whole item click for details
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onJoinRide(ride);
                }
            });
        }
    }

    // Utility methods for adapter management
    public void updateRides(List<Ride> newRides) {
        if (newRides != null) {
            this.rides = newRides;
            notifyDataSetChanged();
        }
    }

    public void addRide(Ride ride) {
        if (ride != null && rides != null) {
            rides.add(ride);
            notifyItemInserted(rides.size() - 1);
        }
    }

    public void removeRide(int position) {
        if (rides != null && position >= 0 && position < rides.size()) {
            rides.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void updateRide(int position, Ride ride) {
        if (rides != null && ride != null && position >= 0 && position < rides.size()) {
            rides.set(position, ride);
            notifyItemChanged(position);
        }
    }

    // Get ride at position safely
    public Ride getRideAtPosition(int position) {
        if (rides != null && position >= 0 && position < rides.size()) {
            return rides.get(position);
        }
        return null;
    }
}
