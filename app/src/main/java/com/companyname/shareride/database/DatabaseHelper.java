package com.companyname.shareride.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database Info
    private static final String DATABASE_NAME = "ShareRideDB";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    public static final String TABLE_RIDES = "rides";
    public static final String TABLE_USERS = "users";
    public static final String TABLE_RIDE_REQUESTS = "ride_requests";
    public static final String TABLE_CHAT_MESSAGES = "chat_messages";

    // Common column names
    public static final String KEY_ID = "id";
    public static final String KEY_CREATED_AT = "created_at";
    public static final String KEY_UPDATED_AT = "updated_at";

    // RIDES Table - Column names
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
    public static final String KEY_RIDE_STATUS = "ride_status"; // active, completed, cancelled
    public static final String KEY_NOTES = "notes";

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

    // RIDES table create statement
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
            + KEY_NOTES + " TEXT,"
            + KEY_CREATED_AT + " INTEGER NOT NULL,"
            + KEY_UPDATED_AT + " INTEGER NOT NULL"
            + ")";

    // USERS table create statement
    private static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_USERNAME + " TEXT NOT NULL UNIQUE,"
            + KEY_EMAIL + " TEXT NOT NULL UNIQUE,"
            + KEY_PHONE + " TEXT,"
            + KEY_PROFILE_IMAGE + " TEXT,"
            + KEY_RATING + " REAL DEFAULT 0.0,"
            + KEY_TOTAL_RIDES + " INTEGER DEFAULT 0,"
            + KEY_CREATED_AT + " INTEGER NOT NULL,"
            + KEY_UPDATED_AT + " INTEGER NOT NULL"
            + ")";

    // RIDE_REQUESTS table create statement
    private static final String CREATE_TABLE_RIDE_REQUESTS = "CREATE TABLE " + TABLE_RIDE_REQUESTS + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_RIDE_ID + " INTEGER NOT NULL,"
            + KEY_PASSENGER_ID + " INTEGER NOT NULL,"
            + KEY_REQUEST_STATUS + " TEXT NOT NULL DEFAULT 'pending',"
            + KEY_REQUEST_MESSAGE + " TEXT,"
            + KEY_CREATED_AT + " INTEGER NOT NULL,"
            + KEY_UPDATED_AT + " INTEGER NOT NULL,"
            + "FOREIGN KEY(" + KEY_RIDE_ID + ") REFERENCES " + TABLE_RIDES + "(" + KEY_ID + "),"
            + "FOREIGN KEY(" + KEY_PASSENGER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + ")"
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
            + KEY_CREATED_AT + " INTEGER NOT NULL,"
            + "FOREIGN KEY(" + KEY_RIDE_ID + ") REFERENCES " + TABLE_RIDES + "(" + KEY_ID + "),"
            + "FOREIGN KEY(" + KEY_SENDER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + "),"
            + "FOREIGN KEY(" + KEY_RECEIVER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + ")"
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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if existed
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
        // Index for location-based searches on rides
        db.execSQL("CREATE INDEX idx_rides_from_location ON " + TABLE_RIDES +
                "(" + KEY_FROM_LATITUDE + ", " + KEY_FROM_LONGITUDE + ")");
        db.execSQL("CREATE INDEX idx_rides_to_location ON " + TABLE_RIDES +
                "(" + KEY_TO_LATITUDE + ", " + KEY_TO_LONGITUDE + ")");

        // Index for departure time searches
        db.execSQL("CREATE INDEX idx_rides_departure_time ON " + TABLE_RIDES +
                "(" + KEY_DEPARTURE_TIME + ")");

        // Index for ride status
        db.execSQL("CREATE INDEX idx_rides_status ON " + TABLE_RIDES +
                "(" + KEY_RIDE_STATUS + ")");

        // Index for user email (login purposes)
        db.execSQL("CREATE INDEX idx_users_email ON " + TABLE_USERS +
                "(" + KEY_EMAIL + ")");

        // Index for ride requests
        db.execSQL("CREATE INDEX idx_ride_requests_ride_id ON " + TABLE_RIDE_REQUESTS +
                "(" + KEY_RIDE_ID + ")");
        db.execSQL("CREATE INDEX idx_ride_requests_passenger_id ON " + TABLE_RIDE_REQUESTS +
                "(" + KEY_PASSENGER_ID + ")");

        // Index for chat messages
        db.execSQL("CREATE INDEX idx_chat_ride_id ON " + TABLE_CHAT_MESSAGES +
                "(" + KEY_RIDE_ID + ")");
    }

    // Helper methods for getting current timestamp
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    // Method to close database properly
    public synchronized void closeDB() {
        if (instance != null) {
            SQLiteDatabase db = instance.getReadableDatabase();
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    
}