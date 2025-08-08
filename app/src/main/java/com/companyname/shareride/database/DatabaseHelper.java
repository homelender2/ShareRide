package com.companyname.shareride.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database Info
    private static final String DATABASE_NAME = "ShareRideDB";
    private static final int DATABASE_VERSION = 2; // Incremented for schema change

    // Table Names
    public static final String TABLE_RIDES = "rides";
    public static final String TABLE_USERS = "users";
    public static final String TABLE_RIDE_REQUESTS = "ride_requests";
    public static final String TABLE_CHAT_MESSAGES = "chat_messages";

    // Common column names
    public static final String KEY_ID = "id";
    public static final String KEY_CREATED_AT = "created_at";
    public static final String KEY_UPDATED_AT = "updated_at";

    // RIDES Table - Column names (Updated to match RideDAO constants)
    public static final String KEY_DRIVER_ID = "driver_id";
    public static final String KEY_FROM_ADDRESS = "from_address";
    public static final String KEY_FROM_LATITUDE = "from_latitude";
    public static final String KEY_FROM_LONGITUDE = "from_longitude";
    public static final String KEY_TO_ADDRESS = "to_address";
    public static final String KEY_TO_LATITUDE = "to_latitude";
    public static final String KEY_TO_LONGITUDE = "to_longitude";
    public static final String KEY_DEPARTURE_TIME = "departure_time";
    public static final String KEY_AVAILABLE_SEATS = "available_seats";
    public static final String KEY_PRICE = "price";
    public static final String KEY_RIDE_STATUS = "status"; // Changed from "ride_status" to "status"
    public static final String KEY_DESCRIPTION = "description"; // Added description field
    public static final String KEY_NOTES = "notes"; // Keep for backward compatibility

    // Alternative column constants for RideDAO compatibility
    public static final String COLUMN_ID = KEY_ID;
    public static final String COLUMN_DRIVER_ID = KEY_DRIVER_ID;
    public static final String COLUMN_FROM_ADDRESS = KEY_FROM_ADDRESS;
    public static final String COLUMN_FROM_LATITUDE = KEY_FROM_LATITUDE;
    public static final String COLUMN_FROM_LONGITUDE = KEY_FROM_LONGITUDE;
    public static final String COLUMN_TO_ADDRESS = KEY_TO_ADDRESS;
    public static final String COLUMN_TO_LATITUDE = KEY_TO_LATITUDE;
    public static final String COLUMN_TO_LONGITUDE = KEY_TO_LONGITUDE;
    public static final String COLUMN_DEPARTURE_TIME = KEY_DEPARTURE_TIME;
    public static final String COLUMN_AVAILABLE_SEATS = KEY_AVAILABLE_SEATS;
    public static final String COLUMN_PRICE = KEY_PRICE;
    public static final String COLUMN_STATUS = KEY_RIDE_STATUS;
    public static final String COLUMN_DESCRIPTION = KEY_DESCRIPTION;
    public static final String COLUMN_CREATED_AT = KEY_CREATED_AT;
    public static final String COLUMN_UPDATED_AT = KEY_UPDATED_AT;

    // USERS Table - Column names
    public static final String KEY_USERNAME = "username";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_PROFILE_IMAGE = "profile_image";
    public static final String KEY_RATING = "rating";
    public static final String KEY_TOTAL_RIDES = "total_rides";

    // RIDE_REQUESTS Table - Column names
    public static final String KEY_RIDE_ID = "ride_id";
    public static final String KEY_PASSENGER_ID = "passenger_id";
    public static final String KEY_REQUEST_STATUS = "request_status"; // pending, accepted, rejected
    public static final String KEY_REQUEST_MESSAGE = "request_message";

    // CHAT_MESSAGES Table - Column names
    public static final String KEY_SENDER_ID = "sender_id";
    public static final String KEY_RECEIVER_ID = "receiver_id";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_MESSAGE_TYPE = "message_type"; // text, location, etc.
    public static final String KEY_IS_READ = "is_read";

    // Table Create Statements

    // RIDES table create statement (Updated with description field)
    private static final String CREATE_TABLE_RIDES = "CREATE TABLE " + TABLE_RIDES + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_DRIVER_ID + " INTEGER NOT NULL,"
            + KEY_FROM_ADDRESS + " TEXT NOT NULL,"
            + KEY_FROM_LATITUDE + " REAL NOT NULL,"
            + KEY_FROM_LONGITUDE + " REAL NOT NULL,"
            + KEY_TO_ADDRESS + " TEXT NOT NULL,"
            + KEY_TO_LATITUDE + " REAL NOT NULL,"
            + KEY_TO_LONGITUDE + " REAL NOT NULL,"
            + KEY_DEPARTURE_TIME + " INTEGER NOT NULL,"
            + KEY_AVAILABLE_SEATS + " INTEGER NOT NULL DEFAULT 1,"
            + KEY_PRICE + " REAL NOT NULL DEFAULT 0.0,"
            + KEY_RIDE_STATUS + " TEXT NOT NULL DEFAULT 'active',"
            + KEY_DESCRIPTION + " TEXT DEFAULT ''," // Added description field
            + KEY_NOTES + " TEXT,"
            + KEY_CREATED_AT + " INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),"
            + KEY_UPDATED_AT + " INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)"
            + ")";

    // USERS table create statement
    private static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_USERNAME + " TEXT NOT NULL UNIQUE,"
            + KEY_EMAIL + " TEXT NOT NULL UNIQUE,"
            + KEY_PHONE + " TEXT,"
            + KEY_PROFILE_IMAGE + " TEXT,"
            + KEY_RATING + " REAL DEFAULT 4.8,"
            + KEY_TOTAL_RIDES + " INTEGER DEFAULT 0,"
            + KEY_CREATED_AT + " INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),"
            + KEY_UPDATED_AT + " INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)"
            + ")";

    // RIDE_REQUESTS table create statement
    private static final String CREATE_TABLE_RIDE_REQUESTS = "CREATE TABLE " + TABLE_RIDE_REQUESTS + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_RIDE_ID + " INTEGER NOT NULL,"
            + KEY_PASSENGER_ID + " INTEGER NOT NULL,"
            + KEY_REQUEST_STATUS + " TEXT NOT NULL DEFAULT 'pending',"
            + KEY_REQUEST_MESSAGE + " TEXT,"
            + KEY_CREATED_AT + " INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),"
            + KEY_UPDATED_AT + " INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),"
            + "FOREIGN KEY(" + KEY_RIDE_ID + ") REFERENCES " + TABLE_RIDES + "(" + KEY_ID + ") ON DELETE CASCADE,"
            + "FOREIGN KEY(" + KEY_PASSENGER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + ") ON DELETE CASCADE"
            + ")";

    // CHAT_MESSAGES table create statement
    private static final String CREATE_TABLE_CHAT_MESSAGES = "CREATE TABLE " + TABLE_CHAT_MESSAGES + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_RIDE_ID + " INTEGER NOT NULL,"
            + KEY_SENDER_ID + " INTEGER NOT NULL,"
            + KEY_RECEIVER_ID + " INTEGER NOT NULL,"
            + KEY_MESSAGE + " TEXT NOT NULL,"
            + KEY_MESSAGE_TYPE + " TEXT NOT NULL DEFAULT 'text',"
            + KEY_IS_READ + " INTEGER NOT NULL DEFAULT 0,"
            + KEY_CREATED_AT + " INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),"
            + "FOREIGN KEY(" + KEY_RIDE_ID + ") REFERENCES " + TABLE_RIDES + "(" + KEY_ID + ") ON DELETE CASCADE,"
            + "FOREIGN KEY(" + KEY_SENDER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + ") ON DELETE CASCADE,"
            + "FOREIGN KEY(" + KEY_RECEIVER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + ") ON DELETE CASCADE"
            + ")";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create all tables
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_RIDES);
        db.execSQL(CREATE_TABLE_RIDE_REQUESTS);
        db.execSQL(CREATE_TABLE_CHAT_MESSAGES);

        // Create indexes for better performance
        createIndexes(db);

        // Insert sample data for testing
        insertSampleData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add description column to existing rides table
            try {
                db.execSQL("ALTER TABLE " + TABLE_RIDES + " ADD COLUMN " + KEY_DESCRIPTION + " TEXT DEFAULT ''");
            } catch (Exception e) {
                // If alter fails, recreate table
                recreateTables(db);
            }
        }

        if (oldVersion < newVersion) {
            // For any other future upgrades, recreate tables
            recreateTables(db);
        }
    }

    private void recreateTables(SQLiteDatabase db) {
        // Drop older tables if existed (in reverse order due to foreign keys)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RIDE_REQUESTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RIDES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);

        // Create tables again
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Enable foreign key constraints
        db.setForeignKeyConstraintsEnabled(true);
    }

    private void createIndexes(SQLiteDatabase db) {
        try {
            // Index for location-based searches on rides (composite index for better performance)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_rides_from_location ON " + TABLE_RIDES +
                    "(" + KEY_FROM_LATITUDE + ", " + KEY_FROM_LONGITUDE + ")");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_rides_to_location ON " + TABLE_RIDES +
                    "(" + KEY_TO_LATITUDE + ", " + KEY_TO_LONGITUDE + ")");

            // Index for departure time searches
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_rides_departure_time ON " + TABLE_RIDES +
                    "(" + KEY_DEPARTURE_TIME + ")");

            // Index for ride status and available seats
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_rides_status_seats ON " + TABLE_RIDES +
                    "(" + KEY_RIDE_STATUS + ", " + KEY_AVAILABLE_SEATS + ")");

            // Index for driver searches
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_rides_driver_id ON " + TABLE_RIDES +
                    "(" + KEY_DRIVER_ID + ")");

            // Index for user email (login purposes)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_users_email ON " + TABLE_USERS +
                    "(" + KEY_EMAIL + ")");

            // Index for user username
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_users_username ON " + TABLE_USERS +
                    "(" + KEY_USERNAME + ")");

            // Index for ride requests
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_ride_requests_ride_id ON " + TABLE_RIDE_REQUESTS +
                    "(" + KEY_RIDE_ID + ")");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_ride_requests_passenger_id ON " + TABLE_RIDE_REQUESTS +
                    "(" + KEY_PASSENGER_ID + ")");

            // Composite index for ride request queries
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_ride_requests_composite ON " + TABLE_RIDE_REQUESTS +
                    "(" + KEY_RIDE_ID + ", " + KEY_PASSENGER_ID + ", " + KEY_REQUEST_STATUS + ")");

            // Index for chat messages
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_chat_ride_id ON " + TABLE_CHAT_MESSAGES +
                    "(" + KEY_RIDE_ID + ")");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_chat_sender_receiver ON " + TABLE_CHAT_MESSAGES +
                    "(" + KEY_SENDER_ID + ", " + KEY_RECEIVER_ID + ")");

            // Index for unread messages
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_chat_unread ON " + TABLE_CHAT_MESSAGES +
                    "(" + KEY_IS_READ + ", " + KEY_RECEIVER_ID + ")");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertSampleData(SQLiteDatabase db) {
        try {
            // Insert sample users
            long currentTime = System.currentTimeMillis();

            db.execSQL("INSERT INTO " + TABLE_USERS + " (" +
                    KEY_ID + ", " + KEY_USERNAME + ", " + KEY_EMAIL + ", " + KEY_PHONE + ", " +
                    KEY_RATING + ", " + KEY_TOTAL_RIDES + ", " + KEY_CREATED_AT + ", " + KEY_UPDATED_AT +
                    ") VALUES (1, 'john_doe', 'john@example.com', '+919876543210', 4.8, 25, " + currentTime + ", " + currentTime + ")");

            db.execSQL("INSERT INTO " + TABLE_USERS + " (" +
                    KEY_ID + ", " + KEY_USERNAME + ", " + KEY_EMAIL + ", " + KEY_PHONE + ", " +
                    KEY_RATING + ", " + KEY_TOTAL_RIDES + ", " + KEY_CREATED_AT + ", " + KEY_UPDATED_AT +
                    ") VALUES (2, 'priya_sharma', 'priya@example.com', '+919876543211', 4.9, 30, " + currentTime + ", " + currentTime + ")");

            db.execSQL("INSERT INTO " + TABLE_USERS + " (" +
                    KEY_ID + ", " + KEY_USERNAME + ", " + KEY_EMAIL + ", " + KEY_PHONE + ", " +
                    KEY_RATING + ", " + KEY_TOTAL_RIDES + ", " + KEY_CREATED_AT + ", " + KEY_UPDATED_AT +
                    ") VALUES (3, 'amit_kumar', 'amit@example.com', '+919876543212', 4.7, 20, " + currentTime + ", " + currentTime + ")");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper methods for getting current timestamp
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    // Method to get table creation status
    public boolean isTableExists(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        android.database.Cursor cursor = db.rawQuery(query, new String[]{tableName});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Method to get database version
    public int getCurrentDatabaseVersion() {
        return this.getReadableDatabase().getVersion();
    }

    // Method to check if description column exists
    public boolean hasDescriptionColumn() {
        SQLiteDatabase db = null;
        android.database.Cursor cursor = null;
        boolean hasDescription = false;

        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery("PRAGMA table_info(" + TABLE_RIDES + ")", null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // Check if 'name' column exists before accessing it
                    int nameIndex = cursor.getColumnIndex("name");
                    if (nameIndex >= 0) {
                        String columnName = cursor.getString(nameIndex);
                        if (KEY_DESCRIPTION.equals(columnName)) {
                            hasDescription = true;
                            break;
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            hasDescription = false;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return hasDescription;
    }


    // Method to manually add description column if needed
    public void addDescriptionColumnIfNotExists() {
        if (!hasDescriptionColumn()) {
            SQLiteDatabase db = this.getWritableDatabase();
            try {
                db.execSQL("ALTER TABLE " + TABLE_RIDES + " ADD COLUMN " + KEY_DESCRIPTION + " TEXT DEFAULT ''");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Method to close database properly
    public synchronized void closeDB() {
        if (instance != null) {
            SQLiteDatabase db = instance.getReadableDatabase();
            if (db != null && db.isOpen()) {
                db.close();
            }
            instance = null;
        }
    }

    // Method to clear all data (useful for testing)
    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.execSQL("DELETE FROM " + TABLE_CHAT_MESSAGES);
            db.execSQL("DELETE FROM " + TABLE_RIDE_REQUESTS);
            db.execSQL("DELETE FROM " + TABLE_RIDES);
            db.execSQL("DELETE FROM " + TABLE_USERS);

            // Reset auto-increment counters
            db.execSQL("DELETE FROM sqlite_sequence WHERE name IN (?, ?, ?, ?)",
                    new String[]{TABLE_USERS, TABLE_RIDES, TABLE_RIDE_REQUESTS, TABLE_CHAT_MESSAGES});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
