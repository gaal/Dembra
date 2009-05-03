package org.forum2.gaal.dembra;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Dembra extends Activity {
	public static final String APP_NAME = "Dembra";

	protected static final String APP_VERSION = "0.1";

	public final Button.OnClickListener clearScans_ = new Button.OnClickListener() {
		public void onClick(View v) {
			clearScansTop();
		}
	};

	private DatabaseHelper dbHelper_;

	private boolean firstScan = true;

	public final int scanActivity = 0;

	public final Button.OnClickListener scanMode_ = new Button.OnClickListener() {
		public void onClick(View v) {
			enterScanMode();
		}
	};

	public final Button.OnClickListener sendDb_ = new Button.OnClickListener() {
		public void onClick(View v) {
			sendDb();
		}
	};

	private void clearScansBottom() {
		dbHelper_.clear();
		Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show();
		updateDbDump();
	}

	private void clearScansTop() {
		showConfirmClearDialog(R.string.confirmTitle, getString(R.string.confirmHelp));
	}

	private void enterScanMode() {
		if (firstScan) {
			Toast.makeText(this, getString(R.string.stop_scan_help), Toast.LENGTH_LONG).show();
			firstScan = false;
		}
		try {
			Intent intent = new Intent("com.google.zxing.client.android.SCAN");
			intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
			startActivityForResult(intent, scanActivity);
		} catch (ActivityNotFoundException e) {
			showDialog(R.string.result_failed, e.getMessage());
	
		}
	}

	private View getScanModeButton() {
		return findViewById(R.id.enterScanMode);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == scanActivity) {
			if (resultCode == RESULT_OK) {
				String contents = intent.getStringExtra("SCAN_RESULT");
				String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
				if (format.equals("EAN_13")) {
					Toast.makeText(this, contents, Toast.LENGTH_SHORT).show();

					// Insert it into the database.
					Long now = Long.valueOf(System.currentTimeMillis());
					ContentValues values = new ContentValues();
					values.put("created", now);
					values.put("modified", now);
					values.put("isbn", contents);
					values.put("resattempts", 0);
					long rowId = dbHelper_.insertToScansTable(values);
					if (rowId > 0) {
						// TODO: notify the resolution service that it has work to do.
					} else {
						throw new SQLException("Failed to insert row");
					}
				} else {
					showDialog(R.string.result_failed, "Not an ISBN: " + format);
				}

				// Keep scanning.
				updateDbDump();
				enterScanMode();
			}
			// Else, we've canceled scan mode, so there's nothing to do and
			// we'll just return to the main activity.
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		View scan = getScanModeButton();
		scan.setOnClickListener(scanMode_);

		View clear = findViewById(R.id.clearScans);
		clear.setOnClickListener(clearScans_);

		View send = findViewById(R.id.sendDb);
		send.setOnClickListener(sendDb_);

		dbHelper_ = new DatabaseHelper(this);

		updateDbDump();
	}

	private void sendDb() {
		// Prepare the email.
		int scanned = 0;
		StringBuilder sb = new StringBuilder(
				getString(R.string.send_db_preamble) + "\n\n");
		for (String isbn : dbHelper_.getAllISBNs()) {
			sb.append(isbn + "\n");
			++scanned;
		}
		sb.append("\n-- \n" + getString(R.string.send_db_postamble) + " "
				+ APP_NAME + " v" + APP_VERSION + "\n");
		if (scanned == 0) {
			Toast.makeText(this, getText(R.string.db_empty), Toast.LENGTH_SHORT).show();
			return;  // Don't bother with empty reports.
		}

		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
		sendIntent.putExtra(Intent.EXTRA_SUBJECT,
				getString(R.string.send_db_subject));
		sendIntent.setType("message/rfc822");
		try {
			startActivity(Intent.createChooser(sendIntent,
					getText(R.string.send_this_list)));
			Toast.makeText(this, "Choose recipient", Toast.LENGTH_SHORT).show();
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(this, "Can't send", Toast.LENGTH_SHORT).show();
		}
	}

	private void showConfirmClearDialog(int title, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						clearScansBottom();
					}
				});
		builder.setNeutralButton(getString(R.string.cancel), null);
		builder.show();
	}

	private void showDialog(int title, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setPositiveButton(getString(R.string.ok), null);
		builder.show();
	}

	private void updateDbDump() {
		TextView dbDumpView = (TextView) findViewById(R.id.dbDump);
		StringBuilder sb = new StringBuilder("Current scans: ");

		List<String> isbns = dbHelper_.getAllISBNs();
		for (String isbn : isbns) {
			sb.append(isbn + ", ");
		}
		dbDumpView.setText(sb);

		// When we get a nice listview (and background resolution and all that)...
		// scans.setText(SimpleCursorAdapter.convertToString(all));
		// (and worry about this not being a cursor any more)
	}
}