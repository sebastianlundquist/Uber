package com.sebastianlundquist.uber;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getSupportActionBar().hide();

		TextView logoTextView = findViewById(R.id.logoTextView);
		logoTextView.setText("\u040F\u0432\u0454\u0433");
		Switch userSwitch = findViewById(R.id.userSwitch);
		userSwitch.getTrackDrawable().setColorFilter(ContextCompat.getColor(this, R.color.switch_track_color), PorterDuff.Mode.SRC_IN);

		// Login anonymously if not already logged in
		if (ParseUser.getCurrentUser() == null) {
			ParseAnonymousUtils.logIn(new LogInCallback() {
				@Override
				public void done(ParseUser user, ParseException e) {
					if (e != null)
						Toast.makeText(MainActivity.this, "Anonymous login failed.", Toast.LENGTH_SHORT).show();
				}
			});
		}
		// Redirect to driver or rider activity if already logged in
		else {
			if (ParseUser.getCurrentUser().get("isRiderOrDriver") != null)
				redirect();
		}
	}

	// Save driver/rider setting and redirect appropriately when finished
	public void getStarted(View view) {
		Switch userSwitch = findViewById(R.id.userSwitch);
		String userType = "rider";
		if (userSwitch.isChecked()) {
			userType = "driver";
		}
		ParseUser.getCurrentUser().put("isRiderOrDriver", userType);
		ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
			@Override
			public void done(ParseException e) {
				redirect();
			}
		});
	}

	// Start appropriate activity
	public void redirect() {
		if (ParseUser.getCurrentUser().get("isRiderOrDriver").equals("rider"))
			startActivity(new Intent(getApplicationContext(), RiderActivity.class));
		else
			startActivity(new Intent(getApplicationContext(), ViewRequestsActivity.class));
	}
}
