package com.sebastianlundquist.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class ViewRequestsActivity extends AppCompatActivity {

	ListView requestsListView;
	ArrayList<String> requests = new ArrayList<>();
	ArrayAdapter arrayAdapter;
	LocationManager locationManager;
	LocationListener locationListener;

	ArrayList<Double> requestLatitudes = new ArrayList<>();
	ArrayList<Double> requestLongitudes = new ArrayList<>();
	ArrayList<String> usernames = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_requests);
		setTitle("\u040F\u0432\u0454\u0433: Nearby Requests");

		requestsListView = findViewById(R.id.requestsListView);
		arrayAdapter = new ArrayAdapter(this, R.layout.custom_textview, requests);
		requestsListView.setAdapter(arrayAdapter);

		requests.clear();
		requests.add("Getting nearby requests...");

		requestsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				if (ContextCompat.checkSelfPermission(ViewRequestsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
					Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					if (requestLatitudes.size() > i && requestLongitudes.size() > i && lastKnownLocation != null && usernames.size() > i) {
						Intent intent = new Intent(getApplicationContext(), DriverLocationActivity.class);
						intent.putExtra("requestLatitude", requestLatitudes.get(i));
						intent.putExtra("requestLongitude", requestLongitudes.get(i));
						intent.putExtra("driverLatitude", lastKnownLocation.getLatitude());
						intent.putExtra("driverLongitude", lastKnownLocation.getLongitude());
						intent.putExtra("username", usernames.get(i));
						startActivity(intent);
					}
				}
			}
		});

		// Update driver's location on server when it changes
		locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
		locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				updateList(location);
				ParseUser.getCurrentUser().put("location", new ParseGeoPoint(location.getLatitude(), location.getLongitude()));
				ParseUser.getCurrentUser().saveInBackground();
			}

			@Override
			public void onStatusChanged(String s, int i, Bundle bundle) { }

			@Override
			public void onProviderEnabled(String s) { }

			@Override
			public void onProviderDisabled(String s) { }
		};

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, locationListener);
			Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (lastKnownLocation != null) {
				updateList(lastKnownLocation);
			}
		}
		else {
			ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, 1);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		// Update list of nearby requests when fine location permission is granted
		if (requestCode == 1) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, locationListener);
					Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					updateList(lastKnownLocation);
				}
			}
		}
	}

	public void updateList(Location location) {
		if (location != null) {
			requests.clear();
			// Get max 10 nearby requests that have not yet been accepted
			ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
			final ParseGeoPoint geoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
			query.whereNear("location", geoPoint);
			query.whereDoesNotExist("driverUsername");
			query.setLimit(10);
			query.findInBackground(new FindCallback<ParseObject>() {
				@Override
				public void done(List<ParseObject> objects, ParseException e) {
					if (e == null) {
						requests.clear();
						requestLatitudes.clear();
						requestLongitudes.clear();
						// Add info for every nearby request
						if (objects.size() > 0) {
							for (ParseObject object : objects) {
								ParseGeoPoint requestLocation = (ParseGeoPoint)object.get("location");
								if (requestLocation != null) {
									double distance = geoPoint.distanceInKilometersTo(requestLocation);
									double distanceRounded = (double)Math.round((distance * 10) / 10);
									requests.add(distanceRounded + " km");
									requestLatitudes.add(requestLocation.getLatitude());
									requestLongitudes.add(requestLocation.getLongitude());
									usernames.add(object.getString("username"));
								}
							}
						}
						else {
							requests.add("No nearby requests.");
						}
						arrayAdapter.notifyDataSetChanged();
					}
				}
			});
		}
	}
}
