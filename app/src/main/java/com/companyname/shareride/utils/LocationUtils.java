package com.companyname.shareride.utils;

import android.location.Location;
import java.text.DecimalFormat;

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
     * Format distance for display (e.g., "2.5 km", "150 m")
     */
    public static String formatDistance(double distanceKm) {
        if (distanceKm < 1.0) {
            // Show in meters if less than 1km
            int meters = (int) (distanceKm * 1000);
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
        DecimalFormat df = new DecimalFormat("#.#");
        if (useKilometers) {
            if (distance < 1.0) {
                int meters = (int) (distance * 1000);
                return meters + " m";
            } else {
                return df.format(distance) + " km";
            }
        } else {
            // Miles
            if (distance < 0.1) {
                int feet = (int) (distance * 5280);
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
        double distance = calculateDistance(lat1, lon1, lat2, lon2);
        return distance <= radiusKm;
    }

    /**
     * Validate latitude value
     */
    public static boolean isValidLatitude(double latitude) {
        return latitude >= -90.0 && latitude <= 90.0;
    }

    /**
     * Validate longitude value
     */
    public static boolean isValidLongitude(double longitude) {
        return longitude >= -180.0 && longitude <= 180.0;
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
        if (averageSpeedKmh <= 0) {
            averageSpeedKmh = 50; // Default city driving speed
        }
        double timeHours = distanceKm / averageSpeedKmh;
        return (int) Math.ceil(timeHours * 60); // Convert to minutes and round up
    }

    /**
     * Calculate estimated travel time with default speed (50 km/h)
     */
    public static int calculateEstimatedTravelTime(double distanceKm) {
        return calculateEstimatedTravelTime(distanceKm, 50.0);
    }

    /**
     * Format travel time for display
     */
    public static String formatTravelTime(int minutes) {
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
}
