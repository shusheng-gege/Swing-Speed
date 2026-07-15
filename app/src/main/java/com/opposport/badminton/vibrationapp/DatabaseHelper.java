package com.opposport.badminton.vibrationapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "SwingRecords.db";
    private static final int DATABASE_VERSION = 1;

    // table and columns
    private static final String TABLE_TRAINING = "training";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_COUNT = "count";
    private static final String COLUMN_AVG_SPEED = "avg_speed";
    private static final String COLUMN_MAX_SPEED = "max_speed";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TRAINING_TABLE = "CREATE TABLE " + TABLE_TRAINING + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_DATE + " INTEGER,"
                + COLUMN_COUNT + " INTEGER,"
                + COLUMN_AVG_SPEED + " REAL,"
                + COLUMN_MAX_SPEED + " REAL"
                + ")";
        db.execSQL(CREATE_TRAINING_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRAINING);
        onCreate(db);
    }

    // insert a training record
    public long insertTraining(int count, float avgSpeed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, System.currentTimeMillis());
        values.put(COLUMN_COUNT, count);
        values.put(COLUMN_AVG_SPEED, avgSpeed);
        values.put(COLUMN_MAX_SPEED, avgSpeed);  // temporary max speed
        long id = db.insert(TABLE_TRAINING, null, values);
        db.close();
        return id;
    }

    // update max speed for the latest record
    public void updateMaxSpeed(long recordId, float maxSpeed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MAX_SPEED, maxSpeed);
        db.update(TABLE_TRAINING, values, COLUMN_ID + " = ?", new String[]{String.valueOf(recordId)});
        db.close();
    }

    // get all training records
    public List<TrainingRecord> getAllRecords() {
        List<TrainingRecord> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TRAINING, null, null, null, null, null, COLUMN_DATE + " DESC");

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE));
                int count = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COUNT));
                float avgSpeed = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_AVG_SPEED));
                float maxSpeed = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_MAX_SPEED));
                records.add(new TrainingRecord(id, date, count, avgSpeed, maxSpeed));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return records;
    }

    // get recent records (limit N)
    public List<TrainingRecord> getRecentRecords(int limit) {
        List<TrainingRecord> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TRAINING, null, null, null, null, COLUMN_DATE + " DESC", "0," + limit);

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE));
                int count = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COUNT));
                float avgSpeed = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_AVG_SPEED));
                float maxSpeed = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_MAX_SPEED));
                records.add(new TrainingRecord(id, date, count, avgSpeed, maxSpeed));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return records;
    }

    // delete a record
    public boolean deleteRecord(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_TRAINING, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rowsDeleted > 0;
    }

    // get total count
    public int getTotalRecords() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TRAINING, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }
}
