package com.companyname.shareride;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.companyname.shareride.RideAdapter;
import com.companyname.shareride.database.RideDAO;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyRidesFragment extends Fragment {

    // UI Components for Rides Display
    private RecyclerView recyclerMyRides;
    private TextView tvNoRides, tvRideStats;
    private ProgressBar progressBar;

    // UI Components for Chat (when ride is selected)
    private LinearLayout layoutRidesList, layoutChatSection;
    private LinearLayout chatContainer;
    private EditText etMessage;
    private ImageButton btnSend, btnBackToRides, btnRefresh;
    private TextView tvChatTitle;

    // Data and Adapters
    private RideDAO rideDAO;
    private RideAdapter adapter;
    private List<Ride> myRidesList;
    private List<ChatMessage> chatMessages;
    private UserDataManager userDataManager;

    // Current selected ride for chat
    private Ride selectedRide;
    private long currentUserId = 1; // This should come from authentication system

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_rides, container, false);

        try {
            // Initialize managers safely
            initializeManagers();

            // Initialize views with null checks
            initViews(view);

            // Setup components only if views exist
            setupRecyclerView();
            setupChat();
            setupClickListeners();

            // Load data
            loadUserRides();

        } catch (Exception e) {
            // Log error and show user-friendly message
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error loading My Rides", Toast.LENGTH_SHORT).show();
            }
        }

        return view;
    }

    private void initializeManagers() {
        try {
            if (getContext() != null) {
                rideDAO = new RideDAO(getContext());
                userDataManager = UserDataManager.getInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initViews(View view) {
        try {
            // Rides list components
            recyclerMyRides = view.findViewById(R.id.recycler_my_rides);
            tvNoRides = view.findViewById(R.id.tv_no_rides);
            tvRideStats = view.findViewById(R.id.tv_ride_stats);
            progressBar = view.findViewById(R.id.progress_bar);
            btnRefresh = view.findViewById(R.id.btn_refresh);

            // Layout containers
            layoutRidesList = view.findViewById(R.id.layout_rides_list);
            layoutChatSection = view.findViewById(R.id.layout_chat_section);

            // Chat components
            chatContainer = view.findViewById(R.id.chat_container);
            etMessage = view.findViewById(R.id.et_message);
            btnSend = view.findViewById(R.id.btn_send);
            btnBackToRides = view.findViewById(R.id.btn_back_to_rides);
            tvChatTitle = view.findViewById(R.id.tv_chat_title);

            // Initially show rides list
            showRidesList();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupRecyclerView() {
        try {
            if (recyclerMyRides != null && getContext() != null) {
                recyclerMyRides.setLayoutManager(new LinearLayoutManager(getContext()));
                myRidesList = new ArrayList<>();
                adapter = new RideAdapter(myRidesList, this::onRideAction);
                recyclerMyRides.setAdapter(adapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupClickListeners() {
        try {
            if (btnRefresh != null) {
                btnRefresh.setOnClickListener(v -> loadUserRides());
            }

            if (btnBackToRides != null) {
                btnBackToRides.setOnClickListener(v -> showRidesList());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadUserRides() {
        showLoading(true);

        // Always load sample data for now to avoid database issues
        loadSampleRides();

        // Uncomment this when database is ready
        /*
        new Thread(() -> {
            try {
                if (rideDAO != null) {
                    List<Ride> driverRides = rideDAO.getRidesByDriverId(currentUserId);

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (myRidesList != null) {
                                myRidesList.clear();
                                myRidesList.addAll(driverRides);
                                if (adapter != null) {
                                    adapter.notifyDataSetChanged();
                                }
                            }
                            showLoading(false);
                            updateUI();
                            updateRideStats();
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error loading rides", Toast.LENGTH_SHORT).show();
                        }
                        loadSampleRides();
                    });
                }
            }
        }).start();
        */
    }

    private void loadSampleRides() {
        try {
            if (myRidesList == null) {
                myRidesList = new ArrayList<>();
            }

            myRidesList.clear();

            // Create sample rides using legacy constructor for compatibility
            Ride ride1 = new Ride("Koramangala → Electronic City", "₹40 each", "2/3 passengers",
                    "Today 6:30 PM", "Priya, Amit");
            Ride ride2 = new Ride("BTM Layout → Whitefield", "₹60 each", "1/4 passengers",
                    "Tomorrow 8:00 AM", "Kavya");
            Ride ride3 = new Ride("Indiranagar → Hebbal", "₹45 each", "3/4 passengers",
                    "Today 7:00 PM", "Raj, Neha, Suresh");

            myRidesList.add(ride1);
            myRidesList.add(ride2);
            myRidesList.add(ride3);

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

            showLoading(false);
            updateUI();
            updateRideStats();

        } catch (Exception e) {
            e.printStackTrace();
            showLoading(false);
        }
    }

    private void onRideAction(Ride ride) {
        try {
            if (getContext() == null || ride == null) return;

            // Simple action for now - just show chat
            openRideChat(ride);

        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error opening ride", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openRideChat(Ride ride) {
        try {
            selectedRide = ride;
            setupChatForRide(ride);
            showChatSection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupChat() {
        try {
            if (chatMessages == null) {
                chatMessages = new ArrayList<>();
            }

            if (btnSend != null && etMessage != null) {
                btnSend.setOnClickListener(v -> {
                    try {
                        String message = etMessage.getText().toString().trim();
                        if (!message.isEmpty()) {
                            sendMessage(message);
                            etMessage.setText("");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupChatForRide(Ride ride) {
        try {
            if (tvChatTitle != null && ride != null) {
                String chatTitle = "Chat - " + ride.getRoute();
                if (chatTitle.length() > 50) {
                    chatTitle = chatTitle.substring(0, 47) + "...";
                }
                tvChatTitle.setText(chatTitle);
            }

            loadChatMessages(ride);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadChatMessages(Ride ride) {
        try {
            if (chatMessages == null) {
                chatMessages = new ArrayList<>();
            }

            chatMessages.clear();

            if (ride != null && ride.getNames() != null && !ride.getNames().isEmpty()) {
                chatMessages.add(new ChatMessage("System", "Ride participants: " + ride.getNames(), false));
            }

            chatMessages.add(new ChatMessage("Priya", "I'm at the bus stop, wearing blue jacket", false));
            chatMessages.add(new ChatMessage("You", "On my way, 2 mins!", true));
            chatMessages.add(new ChatMessage("Amit", "Auto driver is here, white auto", false));

            displayChatMessages();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) {
        try {
            if (chatMessages == null) {
                chatMessages = new ArrayList<>();
            }

            chatMessages.add(new ChatMessage("You", message, true));
            displayChatMessages();

            // Scroll to bottom
            if (chatContainer != null && chatContainer.getChildCount() > 0) {
                chatContainer.getChildAt(chatContainer.getChildCount() - 1).requestFocus();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayChatMessages() {
        try {
            if (chatContainer == null || chatMessages == null || getContext() == null) {
                return;
            }

            chatContainer.removeAllViews();

            for (ChatMessage message : chatMessages) {
                View messageView = LayoutInflater.from(getContext()).inflate(R.layout.item_chat_message, chatContainer, false);
                TextView tvMessage = messageView.findViewById(R.id.tv_message);

                if (tvMessage != null && message != null) {
                    tvMessage.setText(message.getSender() + ": " + message.getMessage());

                    if (message.isOwnMessage()) {
                        tvMessage.setBackgroundResource(R.drawable.bg_chat_own);
                    } else {
                        tvMessage.setBackgroundResource(R.drawable.bg_chat_other);
                    }

                    chatContainer.addView(messageView);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showRidesList() {
        try {
            if (layoutRidesList != null) {
                layoutRidesList.setVisibility(View.VISIBLE);
            }
            if (layoutChatSection != null) {
                layoutChatSection.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showChatSection() {
        try {
            if (layoutRidesList != null) {
                layoutRidesList.setVisibility(View.GONE);
            }
            if (layoutChatSection != null) {
                layoutChatSection.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showLoading(boolean show) {
        try {
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (recyclerMyRides != null) {
                recyclerMyRides.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUI() {
        try {
            if (myRidesList == null) return;

            if (myRidesList.isEmpty()) {
                if (tvNoRides != null) {
                    tvNoRides.setVisibility(View.VISIBLE);
                    tvNoRides.setText("No rides found.\nStart by offering a ride or joining one!");
                }
                if (recyclerMyRides != null) {
                    recyclerMyRides.setVisibility(View.GONE);
                }
            } else {
                if (tvNoRides != null) {
                    tvNoRides.setVisibility(View.GONE);
                }
                if (recyclerMyRides != null) {
                    recyclerMyRides.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateRideStats() {
        try {
            if (tvRideStats != null && myRidesList != null) {
                int totalRides = myRidesList.size();
                String stats = String.format("Total rides: %d", totalRides);
                tvRideStats.setText(stats);
                tvRideStats.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (rideDAO != null) {
                rideDAO.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Don't auto-refresh to avoid crashes
    }

    // Handle back button press
    public boolean onBackPressed() {
        try {
            if (layoutChatSection != null && layoutChatSection.getVisibility() == View.VISIBLE) {
                showRidesList();
                return true; // Consumed the back press
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; // Let the activity handle it
    }
}
