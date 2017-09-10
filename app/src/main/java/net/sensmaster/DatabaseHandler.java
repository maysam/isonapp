package net.sensmaster;

import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHandler extends SQLiteOpenHelper {

  private static final String TAG = "DatabaseHandler";
  private static final String DATABASE_NAME = "roof_monitor.db";
  private static final int DATABASE_VERSION = 7;

  private static final String TABLE_TAGS = "tags";
  private static final String TABLE_LOGS = "logs";

  private static final String COLUMN_ID = "id";
  private static final String COLUMN_UID = "uid";
  private static final String COLUMN_RF_INTERVAL = "rf_interval";
  private static final String COLUMN_COLLECTED = "collected";

  private static final String COLUMN_DATA = "data";
  private static final String COLUMN_DATETIME = "datetime";
  private static final String COLUMN_LOG_INTERVAL = "log_interval";
  private static final String COLUMN_UPLOADED = "uploaded";

  private TagAdapter tagAdapter;

  public DatabaseHandler(Context context, TagAdapter _tagAdapter) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
    tagAdapter = _tagAdapter;
    readAllTags();
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    String CREATE_TAGS_TABLE = "CREATE TABLE IF NOT EXISTS " +
        TABLE_TAGS + "("
        + COLUMN_ID + " INTEGER PRIMARY KEY," + COLUMN_UID 
        + " STRING," + COLUMN_RF_INTERVAL + " INTEGER," + COLUMN_COLLECTED + " INTEGER)";
    Log.i(TAG, CREATE_TAGS_TABLE);
    db.execSQL(CREATE_TAGS_TABLE);
    String CREATE_LOGS_TABLE = "CREATE TABLE IF NOT EXISTS " +
        TABLE_LOGS + "("
        + COLUMN_ID + " INTEGER PRIMARY KEY," + COLUMN_UID
        + " STRING," + COLUMN_LOG_INTERVAL + " INTEGER ," + COLUMN_DATETIME + " INTEGER ," + COLUMN_DATA + " BLOB," + COLUMN_UPLOADED + " BOOLEAN)";
    Log.i(TAG, CREATE_LOGS_TABLE);
    db.execSQL(CREATE_LOGS_TABLE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_TAGS);
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);
    Log.i(TAG, "onupgrade");
    onCreate(db);
  }

  public void addTag(Tag tag) {
    if(hasTag(tag)) {
      updateTag(tag);
    } else {
      Log.i(TAG, "adding tag");
      ContentValues values = new ContentValues();
      values.put(COLUMN_UID, tag.getUid());
      values.put(COLUMN_RF_INTERVAL, tag.getRFInterval());
      values.put(COLUMN_COLLECTED, tag.getCollected());
      SQLiteDatabase db = this.getWritableDatabase();
      db.insert(TABLE_TAGS, null, values);
      db.close();
    }

    byte[] data = tag.hasData().toBytes();
    if(data.length > 0) {
      long datetime = tag.getDatetime();
      int log_interval = tag.getLogInterval();
      if(!hasLog(tag.getUid(), datetime, log_interval, data)) {
        Log.i(TAG, "adding log");
        ContentValues values = new ContentValues();
        values.put(COLUMN_UID, tag.getUid());
        values.put(COLUMN_LOG_INTERVAL, log_interval);
        values.put(COLUMN_DATETIME, datetime);
        values.put(COLUMN_DATA, data);
        SQLiteDatabase db = this.getWritableDatabase();
        db.insert(TABLE_LOGS, null, values);
        db.close();
      }
    }
  }

  private void updateTag(Tag tag) {
    SQLiteDatabase db = this.getWritableDatabase();
    String whereClause = COLUMN_UID + " = ?";
    ContentValues values = new ContentValues();
    values.put(COLUMN_COLLECTED, tag.getCollected());
    values.put(COLUMN_RF_INTERVAL, tag.getRFInterval());
    int rows = db.update(TABLE_TAGS, values , whereClause, new String[] { tag.getUid() });
    Log.i(TABLE_TAGS, String.format("logging for %s collected = %d in %d rows", tag.getUid(), tag.getCollected(), rows));
    db.close();  
  }

  public boolean hasTag(Tag tag) {
    String query = "Select * FROM " + TABLE_TAGS + " WHERE " + COLUMN_UID + " =  \"" + tag.getUid() + "\"";
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery(query, null);
    boolean found = false;
    if (cursor.moveToFirst()) {
      cursor.moveToFirst();
      found = true;
      cursor.close();
    }
    db.close();
    return found;
  }

  private boolean hasLog(String uid, long datetime, int log_interval, byte[] data) {
    String query = String.format(Locale.ENGLISH, "Select * FROM " + TABLE_LOGS + " WHERE " + COLUMN_UID + " = \"%s\" AND " + COLUMN_LOG_INTERVAL + " = %d AND " + COLUMN_DATETIME + " = %d AND " + COLUMN_DATA + " =  \"%s\"", uid, log_interval, datetime, data);
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery(query, null);
    boolean found = false;
    if (cursor.moveToFirst()) {
      found = true;
    }
    db.close();
    return found;
  }

  public void readAllTags() {
    String query = "Select * FROM " + TABLE_TAGS;
    SQLiteDatabase db = this.getWritableDatabase();
    Cursor cursor = db.rawQuery(query, null);
    if (cursor.moveToFirst()) {
      do {
        String uid = cursor.getString(1);
        ByteArray _uid = ByteArray.parse(uid);
        Tag tag = tagAdapter.rebuild(_uid);
        int collected = cursor.getInt(3);
        tag.setCollected(collected );        
      } while(cursor.moveToNext());
    }
  }

}
