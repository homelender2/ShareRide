package com.companyname.shareride.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.companyname.shareride.Ride;
import java.util.ArrayList;
import java.util.List;

public class RideDAO {

    private DatabaseHelper dbHelper;

    public RideDAO(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }

    // Create a new ride
    public long createRide(Ride ride) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(DatabaseHelper.KEY_DRIVER_ID, ride.getDriverId());
        values.put(DatabaseHelper.KEY_FROM_ADDRESS, ride.getFromAddress());
        values.put(DatabaseHelper.KEY_FROM_LATITUDE, ride.getFromLatitude());
        values.put(DatabaseHelper.KEY_FROM_LONGITUDE, ride.getFromLongitude());
        values.put(DatabaseHelper.KEY_TO_ADDRESS, ride.getToAddress());
        values.put(DatabaseHelper.KEY_TO_LATITUDE, ride.getToLatitude());
        values.put(DatabaseHelper.KEY_TO_LONGITUDE, ride.getToLongitude());
        values.put(DatabaseHelper.KEY_DEPARTURE_TIME, ride.getDepartureTime());
        values.put(DatabaseHelper.KEY_AVAILABLE_SEATS, ride.getAvailableSeats());
        values.put(DatabaseHelper.KEY_PRICE, ride.getPrice());
        values.put(DatabaseHelper.KEY_RIDE_STATUS, ride.getStatus());
        values.put(DatabaseHelper.KEY_NOTES, ride.getNotes());
        values.put(DatabaseHelper.KEY_CREATED_AT, DatabaseHelper.getCurrentTimestamp());
        values.put(DatabaseHelper.KEY_UPDATED_AT, DatabaseHelper.getCurrentTimestamp());

        long rideId = db.insert(DatabaseHelper.TABLE_RIDES, null, values);
        return rideId;
    }

    // Get ride by ID
    public Ride getRideById(long rideId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_RIDES,
                null,
                DatabaseHelper.KEY_ID + "=?",
                new String[]{String.valueOf(rideId)},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            Ride ride = cursorToRide(cursor);
            cursor.close();
            return ride;
        }

        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    // Get all active rides
    public List<Ride> getAllActiveRides() {
        List<Ride> rides = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + DatabaseHelper.TABLE_RIDES +
                " WHERE " + DatabaseHelper.KEY_RIDE_STATUS + " = 'active'" +
                " ORDER BY " + DatabaseHelper.KEY_DEPARTURE_TIME + " ASC";

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Ride ride = cursorToRide(cursor);
                rides.add(ride);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return rides;
    }

    // Get rides by driver ID
    public List<Ride> getRidesByDriverId(long driverId) {
        List<Ride> rides = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + DatabaseHelper.TABLE_RIDES +
                " WHERE " + DatabaseHelper.KEY_DRIVER_ID + " = ?" +
                " ORDER BY " + DatabaseHelper.KEY_DEPARTURE_TIME + " DESC";

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(driverId)});

        if (cursor.moveToFirst()) {
            do {
                Ride ride = cursorToRide(cursor);
                rides.add(ride);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return rides;
    }

    // Search rides by location proximity - FIXED HAVING ERROR
    public List<Ride> searchRidesByLocation(double fromLat, double fromLng,
                                            double toLat, double toLng,
                                            double radiusKm) {
        List<Ride> rides = new ArrayList<>();

        // Using Haversine formula for distance calculation - FIXED
        String selectQuery = "SELECT *, " +
                "(6371 * acos(cos(radians(?)) * cos(radians(" + DatabaseHelper.KEY_FROM_LATITUDE + ")) * " +
                "cos(radians(" + DatabaseHelper.KEY_FROM_LONGITUDE + ") - radians(?)) + " +
                "sin(radians(?)) * sin(radians(" + DatabaseHelper.KEY_FROM_LATITUDE + ")))) AS from_distance, " +

                "(6371 * acos(cos(radians(?)) * cos(radians(" + DatabaseHelper.KEY_TO_LATITUDE + ")) * " +
                "cos(radians(" + DatabaseHelper.KEY_TO_LONGITUDE + ") - radians(?)) + " +
                "sin(radians(?)) * sin(radians(" + DatabaseHelper.KEY_TO_LATITUDE + ")))) AS to_distance " +

                "FROM " + DatabaseHelper.TABLE_RIDES + " " +
                "WHERE " + DatabaseHelper.KEY_RIDE_STATUS + " = 'active' " +
                "AND (6371 * acos(cos(radians(?)) * cos(radians(" + DatabaseHelper.KEY_FROM_LATITUDE + ")) * " +
                "cos(radians(" + DatabaseHelper.KEY_FROM_LONGITUDE + ") - radians(?)) + " +
                "sin(radians(?)) * sin(radians(" + DatabaseHelper.KEY_FROM_LATITUDE + ")))) < ? " +
                "AND (6371 * acos(cos(radians(?)) * cos(radians(" + DatabaseHelper.KEY_TO_LATITUDE + ")) * " +
                "cos(radians(" + DatabaseHelper.KEY_TO_LONGITUDE + ") - radians(?)) + " +
                "sin(radians(?)) * sin(radians(" + DatabaseHelper.KEY_TO_LATITUDE + ")))) < ? " +
                "ORDER BY from_distance ASC";

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{
                String.valueOf(fromLat), String.valueOf(fromLng), String.valueOf(fromLat),
                String.valueOf(toLat), String.valueOf(toLng), String.valueOf(toLat),
                String.valueOf(fromLat), String.valueOf(fromLng), String.valueOf(fromLat), String.valueOf(radiusKm),
                String.valueOf(toLat), String.valueOf(toLng), String.valueOf(toLat), String.valueOf(radiusKm)
        });

        if (cursor.moveToFirst()) {
            do {
                Ride ride = cursorToRide(cursor);
                rides.add(ride);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return rides;
    }

    // Search rides by departure time range
    public List<Ride> searchRidesByTimeRange(long startTime, long endTime) {
        List<Ride> rides = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + DatabaseHelper.TABLE_RIDES +
                " WHERE " + DatabaseHelper.KEY_RIDE_STATUS + " = 'active'" +
                " AND " + DatabaseHelper.KEY_DEPARTURE_TIME + " BETWEEN ? AND ?" +
                " ORDER BY " + DatabaseHelper.KEY_DEPARTURE_TIME + " ASC";

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{
                String.valueOf(startTime), String.valueOf(endTime)
        });

        if (cursor.moveToFirst()) {
            do {
                Ride ride = cursorToRide(cursor);
                rides.add(ride);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return rides;
    }

    // Combined search: location + time + available seats - FIXED HAVING ERROR
    public List<Ride> searchRides(double fromLat, double fromLng,
                                  double toLat, double toLng,
                                  double radiusKm,
                                  long startTime, long endTime,
                                  int minSeats) {
        List<Ride> rides = new ArrayList<>();

        String selectQuery = "SELECT *, " +
                "(6371 * acos(cos(radians(?)) * cos(radians(" + DatabaseHelper.KEY_FROM_LATITUDE + ")) * " +
                "cos(radians(" + DatabaseHelper.KEY_FROM_LONGITUDE + ") - radians(?)) + " +
                "sin(radians(?)) * sin(radians(" + DatabaseHelper.KEY_FROM_LATITUDE + ")))) AS from_distance, " +

                "(6371 * acos(cos(radians(?)) * cos(radians(" + DatabaseHelper.KEY_TO_LATITUDE + ")) * " +
                "cos(radians(" + DatabaseHelper.KEY_TO_LONGITUDE + ") - radians(?)) + " +
                "sin(radians(?)) * sin(radians(" + DatabaseHelper.KEY_TO_LATITUDE + ")))) AS to_distance " +

                "FROM " + DatabaseHelper.TABLE_RIDES + " " +
                "WHERE " + DatabaseHelper.KEY_RIDE_STATUS + " = 'active' " +
                "AND " + DatabaseHelper.KEY_DEPARTURE_TIME + " BETWEEN ? AND ? " +
                "AND " + DatabaseHelper.KEY_AVAILABLE_SEATS + " >= ? " +
                "AND (6371 * acos(cos(radians(?)) * cos(radians(" + DatabaseHelper.KEY_FROM_LATITUDE + ")) * " +
                "cos(radians(" + DatabaseHelper.KEY_FROM_LONGITUDE + ") - radians(?)) + " +
                "sin(radians(?)) * sin(radians(" + DatabaseHelper.KEY_FROM_LATITUDE + ")))) < ? " +
                "AND (6371 * acos(cos(radians(?)) * cos(radians(" + DatabaseHelper.KEY_TO_LATITUDE + ")) * " +
                "cos(radians(" + DatabaseHelper.KEY_TO_LONGITUDE + ") - radians(?)) + " +
                "sin(radians(?)) * sin(radians(" + DatabaseHelper.KEY_TO_LATITUDE + ")))) < ? " +
                "ORDER BY from_distance ASC, " + DatabaseHelper.KEY_DEPARTURE_TIME + " ASC";

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{
                String.valueOf(fromLat), String.valueOf(fromLng), String.valueOf(fromLat),
                String.valueOf(toLat), String.valueOf(toLng), String.valueOf(toLat),
                String.valueOf(startTime), String.valueOf(endTime), String.valueOf(minSeats),
                String.valueOf(fromLat), String.valueOf(fromLng), String.valueOf(fromLat), String.valueOf(radiusKm),
                String.valueOf(toLat), String.valueOf(toLng), String.valueOf(toLat), String.valueOf(radiusKm)
        });

        if (cursor.moveToFirst()) {
            do {
                Ride ride = cursorToRide(cursor);
                rides.add(ride);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return rides;
    }

    // Update ride
    public int updateRide(Ride ride) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(DatabaseHelper.KEY_FROM_ADDRESS, ride.getFromAddress());
        values.put(DatabaseHelper.KEY_FROM_LATITUDE, ride.getFromLatitude());
        values.put(DatabaseHelper.KEY_FROM_LONGITUDE, ride.getFromLongitude());
        values.put(DatabaseHelper.KEY_TO_ADDRESS, ride.getToAddress());
        values.put(DatabaseHelper.KEY_TO_LATITUDE, ride.getToLatitude());
        values.put(DatabaseHelper.KEY_TO_LONGITUDE, ride.getToLongitude());
        values.put(DatabaseHelper.KEY_DEPARTURE_TIME, ride.getDepartureTime());
        values.put(DatabaseHelper.KEY_AVAILABLE_SEATS, ride.getAvailableSeats());
        values.put(DatabaseHelper.KEY_PRICE, ride.getPrice());
        values.put(DatabaseHelper.KEY_RIDE_STATUS, ride.getStatus());
        values.put(DatabaseHelper.KEY_NOTES, ride.getNotes());
        values.put(DatabaseHelper.KEY_UPDATED_AT, DatabaseHelper.getCurrentTimestamp());

        return db.update(DatabaseHelper.TABLE_RIDES, values,
                DatabaseHelper.KEY_ID + " = ?",
                new String[]{String.valueOf(ride.getId())});
    }

    // Update ride status
    public int updateRideStatus(long rideId, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KEY_RIDE_STATUS, status);
        values.put(DatabaseHelper.KEY_UPDATED_AT, DatabaseHelper.getCurrentTimestamp());

        return db.update(DatabaseHelper.TABLE_RIDES, values,
                DatabaseHelper.KEY_ID + " = ?",
                new String[]{String.valueOf(rideId)});
    }

    // Update available seats
    public int updateAvailableSeats(long rideId, int seats) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KEY_AVAILABLE_SEATS, seats);
        values.put(DatabaseHelper.KEY_UPDATED_AT, DatabaseHelper.getCurrentTimestamp());

        return db.update(DatabaseHelper.TABLE_RIDES, values,
                DatabaseHelper.KEY_ID + " = ?",
                new String[]{String.valueOf(rideId)});
    }

    // Delete ride
    public void deleteRide(long rideId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_RIDES,
                DatabaseHelper.KEY_ID + " = ?",
                new String[]{String.valueOf(rideId)});
    }

    // Get ride count for a driver
    public int getRideCountForDriver(long driverId) {
        String countQuery = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_RIDES +
                " WHERE " + DatabaseHelper.KEY_DRIVER_ID + " = ?";

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, new String[]{String.valueOf(driverId)});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // Helper method to convert cursor to Ride object
    private Ride cursorToRide(Cursor cursor) {
        Ride ride = new Ride();

        ride.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ID)));
        ride.setDriverId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_DRIVER_ID)));
        ride.setFromAddress(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_FROM_ADDRESS)));
        ride.setFromLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_FROM_LATITUDE)));
        ride.setFromLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_FROM_LONGITUDE)));
        ride.setToAddress(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_TO_ADDRESS)));
        ride.setToLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_TO_LATITUDE)));
        ride.setToLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_TO_LONGITUDE)));
        ride.setDepartureTime(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_DEPARTURE_TIME)));
        ride.setAvailableSeats(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_AVAILABLE_SEATS)));
        ride.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PRICE)));
        ride.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_RIDE_STATUS)));
        ride.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_NOTES)));
        ride.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_CREATED_AT)));
        ride.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_UPDATED_AT)));

        return ride;
    }

    // Close database connection
    public void close() {
        dbHelper.closeDB();
    }
}
