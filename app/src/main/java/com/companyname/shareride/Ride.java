package com.companyname.shareride;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Ride {
    // Database fields
    private long id;
    private long driverId;
    private String fromAddress;
    private double fromLatitude;
    private double fromLongitude;
    private String toAddress;
    private double toLatitude;
    private double toLongitude;
    private long departureTime;
    private int availableSeats;
    private double price;
    private String status; // active, completed, cancelled
    private String notes;
    private long createdAt;
    private long updatedAt;

    // Legacy fields (for backward compatibility)
    private String route;
    private String fare;
    private String passengers;
    private String time;
    private String names;

    // Status constants
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";

    // Constructors
    public Ride() {
        this.status = STATUS_ACTIVE;
        this.availableSeats = 1;
        this.price = 0.0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Legacy constructor (for backward compatibility)
    public Ride(String route, String fare, String passengers, String time, String names) {
        this();
        this.route = route;
        this.fare = fare;
        this.passengers = passengers;
        this.time = time;
        this.names = names;

        // Try to parse legacy data into new format
        parseRouteToAddresses(route);
        parseFareToPrice(fare);
        parsePassengersToSeats(passengers);
        parseTimeToTimestamp(time);
    }

    // New constructor for database
    public Ride(long driverId, String fromAddress, double fromLat, double fromLng,
                String toAddress, double toLat, double toLng, long departureTime) {
        this();
        this.driverId = driverId;
        this.fromAddress = fromAddress;
        this.fromLatitude = fromLat;
        this.fromLongitude = fromLng;
        this.toAddress = toAddress;
        this.toLatitude = toLat;
        this.toLongitude = toLng;
        this.departureTime = departureTime;

        // Update legacy fields for compatibility
        updateLegacyFields();
    }

    // Constructor with price and seats
    public Ride(long driverId, String fromAddress, double fromLat, double fromLng,
                String toAddress, double toLat, double toLng, long departureTime,
                int availableSeats, double price) {
        this(driverId, fromAddress, fromLat, fromLng, toAddress, toLat, toLng, departureTime);
        this.availableSeats = availableSeats;
        this.price = price;
        updateLegacyFields();
    }

    // Database Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getDriverId() { return driverId; }
    public void setDriverId(long driverId) { this.driverId = driverId; }

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
        this.updatedAt = System.currentTimeMillis();
        updateLegacyFields();
    }

    public double getFromLatitude() { return fromLatitude; }
    public void setFromLatitude(double fromLatitude) {
        this.fromLatitude = fromLatitude;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getFromLongitude() { return fromLongitude; }
    public void setFromLongitude(double fromLongitude) {
        this.fromLongitude = fromLongitude;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getToAddress() { return toAddress; }
    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
        this.updatedAt = System.currentTimeMillis();
        updateLegacyFields();
    }

    public double getToLatitude() { return toLatitude; }
    public void setToLatitude(double toLatitude) {
        this.toLatitude = toLatitude;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getToLongitude() { return toLongitude; }
    public void setToLongitude(double toLongitude) {
        this.toLongitude = toLongitude;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getDepartureTime() { return departureTime; }
    public void setDepartureTime(long departureTime) {
        this.departureTime = departureTime;
        this.updatedAt = System.currentTimeMillis();
        updateLegacyFields();
    }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
        this.updatedAt = System.currentTimeMillis();
        updateLegacyFields();
    }

    public double getPrice() { return price; }
    public void setPrice(double price) {
        this.price = price;
        this.updatedAt = System.currentTimeMillis();
        updateLegacyFields();
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // Legacy Getters (for backward compatibility)
    public String getRoute() {
        if (route == null && fromAddress != null && toAddress != null) {
            route = fromAddress + " → " + toAddress;
        }
        return route;
    }

    public String getFare() {
        if (fare == null && price > 0) {
            fare = "₹" + String.format("%.0f", price);
        }
        return fare;
    }

    public String getPassengers() {
        if (passengers == null) {
            passengers = availableSeats + " seats available";
        }
        return passengers;
    }

    public String getTime() {
        if (time == null && departureTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            time = sdf.format(new Date(departureTime));
        }
        return time;
    }

    public String getNames() { return names; }
    public void setNames(String names) { this.names = names; }

    // Helper methods to parse legacy data into new format
    private void parseRouteToAddresses(String route) {
        if (route != null && route.contains("→")) {
            String[] parts = route.split("→");
            if (parts.length == 2) {
                this.fromAddress = parts[0].trim();
                this.toAddress = parts[1].trim();
            }
        } else if (route != null) {
            this.fromAddress = route;
            this.toAddress = "";
        }
    }

    private void parseFareToPrice(String fare) {
        if (fare != null) {
            try {
                // Remove currency symbols and parse
                String numericFare = fare.replaceAll("[^0-9.]", "");
                if (!numericFare.isEmpty()) {
                    this.price = Double.parseDouble(numericFare);
                }
            } catch (NumberFormatException e) {
                this.price = 0.0;
            }
        }
    }

    private void parsePassengersToSeats(String passengers) {
        if (passengers != null) {
            try {
                // Extract number from strings like "3 seats available"
                String numericSeats = passengers.replaceAll("[^0-9]", "");
                if (!numericSeats.isEmpty()) {
                    this.availableSeats = Integer.parseInt(numericSeats);
                }
            } catch (NumberFormatException e) {
                this.availableSeats = 1;
            }
        }
    }

    private void parseTimeToTimestamp(String time) {
        if (time != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date timeDate = sdf.parse(time);
                if (timeDate != null) {
                    // Set to today's date with the given time
                    Date today = new Date();
                    today.setHours(timeDate.getHours());
                    today.setMinutes(timeDate.getMinutes());
                    this.departureTime = today.getTime();
                }
            } catch (Exception e) {
                this.departureTime = System.currentTimeMillis();
            }
        }
    }

    // Update legacy fields when database fields change
    private void updateLegacyFields() {
        // Update route
        if (fromAddress != null && toAddress != null) {
            this.route = fromAddress + " → " + toAddress;
        }

        // Update fare
        if (price > 0) {
            this.fare = "₹" + String.format("%.0f", price);
        }

        // Update passengers
        this.passengers = availableSeats + " seats available";

        // Update time
        if (departureTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            this.time = sdf.format(new Date(departureTime));
        }
    }

    // Enhanced Utility methods
    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    public boolean isCancelled() {
        return STATUS_CANCELLED.equals(status);
    }

    public boolean hasAvailableSeats() {
        return availableSeats > 0;
    }

    public boolean hasValidCoordinates() {
        return fromLatitude != 0.0 && fromLongitude != 0.0 &&
                toLatitude != 0.0 && toLongitude != 0.0;
    }

    public boolean isDepartureInFuture() {
        return departureTime > System.currentTimeMillis();
    }

    public String getFormattedDepartureTime() {
        if (departureTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            return sdf.format(new Date(departureTime));
        }
        return time; // fallback to legacy time
    }

    public String getFormattedDepartureDate() {
        if (departureTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(departureTime));
        }
        return "";
    }

    public String getFormattedPrice() {
        if (price > 0) {
            return "₹" + String.format("%.0f", price);
        }
        return fare; // fallback to legacy fare
    }

    public String getShortRoute() {
        if (fromAddress != null && toAddress != null) {
            String from = fromAddress.length() > 20 ? fromAddress.substring(0, 17) + "..." : fromAddress;
            String to = toAddress.length() > 20 ? toAddress.substring(0, 17) + "..." : toAddress;
            return from + " → " + to;
        }
        return getRoute();
    }

    // Data validation methods
    public boolean isValidForDatabase() {
        return driverId > 0 &&
                fromAddress != null && !fromAddress.trim().isEmpty() &&
                toAddress != null && !toAddress.trim().isEmpty() &&
                hasValidCoordinates() &&
                departureTime > 0 &&
                availableSeats > 0 &&
                price >= 0;
    }

    @Override
    public String toString() {
        return "Ride{" +
                "id=" + id +
                ", from='" + fromAddress + '\'' +
                ", to='" + toAddress + '\'' +
                ", departureTime=" + getFormattedDepartureTime() +
                ", availableSeats=" + availableSeats +
                ", price=" + price +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Ride ride = (Ride) obj;
        return id == ride.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
