package com.companyname.shareride.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

public class LocationUtils {

    // Earth's radius in kilometers
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double EARTH_RADIUS_MILES = 3959.0;

    /**
     * Calculate distance between two points using Haversine formula
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in kilometers
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitude and longitude from degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Calculate differences
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        // Apply Haversine formula
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculate distance in miles
     */
    public static double calculateDistanceInMiles(double lat1, double lon1, double lat2, double lon2) {
        // Same formula but with miles radius
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_MILES * c;
    }

    /**
     * Calculate distance using Android's Location class (alternative method)
     */
    public static float calculateDistanceAndroid(double lat1, double lon1, double lat2, double lon2) {
        Location startPoint = new Location("start");
        startPoint.setLatitude(lat1);
        startPoint.setLongitude(lon1);

        Location endPoint = new Location("end");
        endPoint.setLatitude(lat2);
        endPoint.setLongitude(lon2);

        return startPoint.distanceTo(endPoint) / 1000; // Convert meters to kilometers
    }

    /**
     * Get address string from coordinates using reverse geocoding
     *
     * @param context The Android context
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @return Address string or null if not found
     */
    public static String getAddressFromCoordinates(Context context, double latitude, double longitude) {
        if (context == null || !isValidCoordinate(latitude, longitude)) {
            return null;
        }

        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());

            // Check if Geocoder is present on device
            if (!Geocoder.isPresent()) {
                return "Geocoder not available";
            }

            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Build a detailed address string
                StringBuilder addressBuilder = new StringBuilder();

                // Add street number and name
                if (address.getSubThoroughfare() != null) {
                    addressBuilder.append(address.getSubThoroughfare()).append(" ");
                }
                if (address.getThoroughfare() != null) {
                    addressBuilder.append(address.getThoroughfare()).append(", ");
                }

                // Add locality/area
                if (address.getSubLocality() != null) {
                    addressBuilder.append(address.getSubLocality()).append(", ");
                }

                // Add city
                if (address.getLocality() != null) {
                    addressBuilder.append(address.getLocality());
                }

                String result = addressBuilder.toString();
                // Clean up trailing comma and spaces
                result = result.replaceAll(",\\s*$", "").trim();

                // If we couldn't build a detailed address, use the first address line
                return result.isEmpty() ? address.getAddressLine(0) : result;
            }

        } catch (IOException e) {
            // Network error or service unavailable
            return "Address lookup failed";
        } catch (IllegalArgumentException e) {
            // Invalid coordinates
            return "Invalid coordinates";
        } catch (Exception e) {
            // Other errors
            e.printStackTrace();
            return "Error getting address";
        }

        return "Address not found";
    }

    /**
     * Alternative simpler version that returns just the main address line
     */
    public static String getSimpleAddressFromCoordinates(Context context, double latitude, double longitude) {
        try {
            if (context == null || !isValidCoordinate(latitude, longitude)) {
                return null;
            }

            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            if (!Geocoder.isPresent()) {
                return "Location unavailable";
            }

            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }

        } catch (IOException e) {
            return "Network error";
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Unknown location";
    }

    /**
     * Get coordinates from address string using forward geocoding
     *
     * @param context The Android context
     * @param addressString The address to geocode
     * @return Coordinates array [latitude, longitude] or null if not found
     */
    public static double[] getCoordinatesFromAddress(Context context, String addressString) {
        if (context == null || addressString == null || addressString.trim().isEmpty()) {
            return null;
        }

        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());

            if (!Geocoder.isPresent()) {
                return null;
            }

            List<Address> addresses = geocoder.getFromLocationName(addressString, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return new double[]{address.getLatitude(), address.getLongitude()};
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Format distance for display (e.g., "2.5 km", "150 m")
     */
    public static String formatDistance(double distanceKm) {
        if (distanceKm < 0) {
            return "0 m";
        }

        if (distanceKm < 1.0) {
            // Show in meters if less than 1km
            int meters = (int) Math.round(distanceKm * 1000);
            return meters + " m";
        } else {
            // Show in kilometers with one decimal place
            DecimalFormat df = new DecimalFormat("#.#");
            return df.format(distanceKm) + " km";
        }
    }

    /**
     * Format distance for display with custom unit
     */
    public static String formatDistance(double distance, boolean useKilometers) {
        if (distance < 0) {
            return useKilometers ? "0 m" : "0 ft";
        }

        DecimalFormat df = new DecimalFormat("#.#");
        if (useKilometers) {
            if (distance < 1.0) {
                int meters = (int) Math.round(distance * 1000);
                return meters + " m";
            } else {
                return df.format(distance) + " km";
            }
        } else {
            // Miles
            if (distance < 0.1) {
                int feet = (int) Math.round(distance * 5280);
                return feet + " ft";
            } else {
                return df.format(distance) + " mi";
            }
        }
    }

    /**
     * Check if two locations are within a specified radius
     */
    public static boolean isWithinRadius(double lat1, double lon1, double lat2, double lon2, double radiusKm) {
        if (!isValidCoordinate(lat1, lon1) || !isValidCoordinate(lat2, lon2) || radiusKm < 0) {
            return false;
        }
        double distance = calculateDistance(lat1, lon1, lat2, lon2);
        return distance <= radiusKm;
    }

    /**
     * Validate latitude value
     */
    public static boolean isValidLatitude(double latitude) {
        return !Double.isNaN(latitude) && !Double.isInfinite(latitude) &&
                latitude >= -90.0 && latitude <= 90.0 && latitude != 0.0;
    }

    /**
     * Validate longitude value
     */
    public static boolean isValidLongitude(double longitude) {
        return !Double.isNaN(longitude) && !Double.isInfinite(longitude) &&
                longitude >= -180.0 && longitude <= 180.0 && longitude != 0.0;
    }

    /**
     * Validate coordinate pair
     */
    public static boolean isValidCoordinate(double latitude, double longitude) {
        return isValidLatitude(latitude) && isValidLongitude(longitude);
    }

    /**
     * Calculate bearing between two points (direction from point A to point B)
     * @return Bearing in degrees (0-360)
     */
    public static double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        if (!isValidCoordinate(lat1, lon1) || !isValidCoordinate(lat2, lon2)) {
            return 0.0;
        }

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLonRad = Math.toRadians(lon2 - lon1);

        double x = Math.sin(deltaLonRad) * Math.cos(lat2Rad);
        double y = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad);

        double bearingRad = Math.atan2(x, y);
        double bearingDeg = Math.toDegrees(bearingRad);

        // Normalize to 0-360 degrees
        return (bearingDeg + 360) % 360;
    }

    /**
     * Get compass direction from bearing
     */
    public static String getCompassDirection(double bearing) {
        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};

        int index = (int) Math.round(bearing / 22.5) % 16;
        return directions[index];
    }

    /**
     * Calculate estimated travel time based on distance and average speed
     * @param distanceKm Distance in kilometers
     * @param averageSpeedKmh Average speed in km/h (default: 50 km/h for city driving)
     * @return Estimated time in minutes
     */
    public static int calculateEstimatedTravelTime(double distanceKm, double averageSpeedKmh) {
        if (distanceKm < 0 || averageSpeedKmh <= 0) {
            averageSpeedKmh = 25; // Default city driving speed with traffic
        }
        double timeHours = distanceKm / averageSpeedKmh;
        return Math.max(1, (int) Math.ceil(timeHours * 60)); // Minimum 1 minute
    }

    /**
     * Calculate estimated travel time with default speed (25 km/h for city with traffic)
     */
    public static int calculateEstimatedTravelTime(double distanceKm) {
        return calculateEstimatedTravelTime(distanceKm, 25.0);
    }

    /**
     * Format travel time for display
     */
    public static String formatTravelTime(int minutes) {
        if (minutes <= 0) {
            return "0 min";
        }

        if (minutes < 60) {
            return minutes + " min";
        } else {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return hours + " hr";
            } else {
                return hours + " hr " + remainingMinutes + " min";
            }
        }
    }

    /**
     * Get location bounds for a given center point and radius
     * Useful for database queries to limit search area
     */
    public static LocationBounds getLocationBounds(double centerLat, double centerLon, double radiusKm) {
        if (!isValidCoordinate(centerLat, centerLon) || radiusKm <= 0) {
            return null;
        }

        // Approximate degrees per kilometer
        double latDegreePerKm = 1.0 / 111.0; // 1 degree latitude ≈ 111 km
        double lonDegreePerKm = 1.0 / (111.0 * Math.cos(Math.toRadians(centerLat)));

        double latRadius = radiusKm * latDegreePerKm;
        double lonRadius = radiusKm * lonDegreePerKm;

        return new LocationBounds(
                centerLat - latRadius,  // minLat
                centerLat + latRadius,  // maxLat
                centerLon - lonRadius,  // minLon
                centerLon + lonRadius   // maxLon
        );
    }

    /**
     * Helper class for location bounds
     */
    public static class LocationBounds {
        public final double minLatitude;
        public final double maxLatitude;
        public final double minLongitude;
        public final double maxLongitude;

        public LocationBounds(double minLat, double maxLat, double minLon, double maxLon) {
            this.minLatitude = minLat;
            this.maxLatitude = maxLat;
            this.minLongitude = minLon;
            this.maxLongitude = maxLon;
        }

        public boolean contains(double latitude, double longitude) {
            return latitude >= minLatitude && latitude <= maxLatitude &&
                    longitude >= minLongitude && longitude <= maxLongitude;
        }

        @Override
        public String toString() {
            return String.format("Bounds[lat: %.4f to %.4f, lon: %.4f to %.4f]",
                    minLatitude, maxLatitude, minLongitude, maxLongitude);
        }
    }

    /**
     * Convert kilometers to miles
     */
    public static double kmToMiles(double kilometers) {
        return kilometers * 0.621371;
    }

    /**
     * Convert miles to kilometers
     */
    public static double milesToKm(double miles) {
        return miles * 1.609344;
    }

    /**
     * Calculate CO2 reduction from ride sharing
     * @param distanceKm Distance in kilometers
     * @param passengers Number of passengers sharing the ride
     * @return CO2 reduction in kg
     */
    public static double calculateCO2Reduction(double distanceKm, int passengers) {
        if (distanceKm <= 0 || passengers <= 0) {
            return 0.0;
        }

        // Average car emits about 120g CO2 per km
        double totalEmission = distanceKm * 0.12; // kg CO2

        // Reduction = (passengers * individual emission) - shared emission
        // Assuming ride sharing reduces per-person emission by 60-80%
        double reductionFactor = 0.7; // 70% reduction per person
        return totalEmission * passengers * reductionFactor;
    }

    /**
     * Check if a coordinate is likely within a specific city/region
     * (Can be extended with more specific bounds for different cities)
     */
    public static boolean isWithinCity(double latitude, double longitude, String cityName) {
        if (!isValidCoordinate(latitude, longitude) || cityName == null) {
            return false;
        }

        // Example bounds for Bangalore (can be extended for other cities)
        if (cityName.toLowerCase().contains("bangalore") || cityName.toLowerCase().contains("bengaluru")) {
            return latitude >= 12.7 && latitude <= 13.2 && longitude >= 77.3 && longitude <= 77.9;
        }

        // Add more cities as needed
        return true; // Default to true for unknown cities
    }

    /**
     * Get area/locality from detailed address
     */
    public static String getLocalityFromAddress(Context context, double latitude, double longitude) {
        try {
            if (context == null || !isValidCoordinate(latitude, longitude)) {
                return "Unknown area";
            }

            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            if (!Geocoder.isPresent()) {
                return "Area unavailable";
            }

            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Try to get the most specific locality
                if (address.getSubLocality() != null) {
                    return address.getSubLocality();
                } else if (address.getLocality() != null) {
                    return address.getLocality();
                } else if (address.getAdminArea() != null) {
                    return address.getAdminArea();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Unknown area";
    }
}
