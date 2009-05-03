/**
 * 
 */
package org.forum2.gaal.dembra;

import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DatabaseHelper extends SQLiteOpenHelper {
	public static final String DATABASE_NAME = "DembraDB";

	public static final int DATABASE_VERSION = 1;

	public static final String SCANS_TABLE_NAME = "Scans";

	public void clear() {
		getWritableDatabase().delete(SCANS_TABLE_NAME, "", new String[] {});
	}

	public long insertToScansTable(ContentValues values) {
		return getWritableDatabase().insert(SCANS_TABLE_NAME, "", values);
	}

	public List<String> getAllISBNs() {
		List<String> isbns = new LinkedList<String>();

		Cursor all = getReadableDatabase().query(SCANS_TABLE_NAME,
				new String[] { "isbn" }, null, null, null, null, null);

		if (all.moveToFirst()) {
			do {
				isbns.add(all.getString(0)); // TODO: eww positional!
			} while (all.moveToNext());
		}

		all.close();

		return isbns;
	}

	DatabaseHelper(Context context) {
		super(context, DatabaseHelper.DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + SCANS_TABLE_NAME + " ("
				+ "_id INTEGER PRIMARY KEY," + "isbn TEXT," + "title TEXT,"
				+ "fulltitle TEXT," + "author TEXT,"
				+ "resattempts INTEGER," + "created INTEGER,"
				+ "modified INTEGER" + ");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(Dembra.APP_NAME, "Upgrading database from version "
						+ oldVersion + " to " + newVersion
						+ ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS notes");
		onCreate(db);
	}
}