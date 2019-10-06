package com.sebastianlundquist.uber;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class MainActivity extends AppCompatActivity {

	public void redirect() {
		if (ParseUser.getCurrentUser().get("isRiderOrDriver").equals("rider")) {
			Intent intent = new Intent(getApplicationContext(), RiderActivity.class);
			startActivity(intent);
		}
		else {
			Intent intent = new Intent(getApplicationContext(), ViewRequestsActivity.class);
			startActivity(intent);
		}
	}

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
		Log.i("Info", "Redirecting as " + ParseUser.getCurrentUser().get("isRiderOrDriver"));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getSupportActionBar().hide();
		TextView logoTextView = findViewById(R.id.logoTextView);
		logoTextView.setText("\u040F\u0432\u0454\u0433");
		Switch userSwitch = findViewById(R.id.userSwitch);
		userSwitch.getTrackDrawable().setColorFilter(ContextCompat.getColor(this, R.color.switch_track_color), PorterDuff.Mode.SRC_IN);

		if (ParseUser.getCurrentUser() == null) {
			ParseAnonymousUtils.logIn(new LogInCallback() {
				@Override
				public void done(ParseUser user, ParseException e) {
					if (e == null) {
						Log.i("Info", "Login successful");
					}
					else {
						Log.i("Info", "Login successful");
					}
				}
			});
		}
		else {
			if (ParseUser.getCurrentUser().get("isRiderOrDriver") != null) {
				Log.i("Info", "Redirecting as " + ParseUser.getCurrentUser().get("isRiderOrDriver"));
				redirect();
			}
		}
	}
}
