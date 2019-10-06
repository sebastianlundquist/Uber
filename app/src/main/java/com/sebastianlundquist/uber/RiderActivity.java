package com.sebastianlundquist.uber;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.List;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

	private GoogleMap mMap;

	LocationManager locationManager;
	LocationListener locationListener;
	Button requestUberButton;
	Boolean requestActive = false;

	public void updateMap(Location location) {
		LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
		mMap.clear();
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
		mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
	}

	public void requestUber(View view) {

		if (requestActive) {
			ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
			query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
			query.findInBackground(new FindCallback<ParseObject>() {
				@Override
				public void done(List<ParseObject> objects, ParseException e) {
					if (e == null) {
						if (objects.size() > 0) {
							for (ParseObject object : objects) {
								object.deleteInBackground();
							}
							requestActive = false;
							requestUberButton.setText("Request \u040F\u0432\u0454\u0433");
						}
					}
				}
			});
		}
		else {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, locationListener);
				Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if (lastKnownLocation != null) {
					ParseObject request = new ParseObject("Request");
					request.put("username", ParseUser.getCurrentUser().getUsername());
					ParseGeoPoint parseGeoPoint = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
					request.put("location", parseGeoPoint);
					request.saveInBackground(new SaveCallback() {
						@Override
						public void done(ParseException e) {
							if (e == null) {
								requestActive = true;
								requestUberButton.setText("Cancel \u040F\u0432\u0454\u0433");
							}
						}
					});
				}
				else {
					Toast.makeText(this, "Could not find location. Please try again later.", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rider);
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);
		requestUberButton = findViewById(R.id.requestUberButton);
		requestUberButton.setText("Request \u040F\u0432\u0454\u0433");
		ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
		query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
		query.findInBackground(new FindCallback<ParseObject>() {
			@Override
			public void done(List<ParseObject> objects, ParseException e) {
				if (e == null) {
					if (objects.size() > 0) {
						requestActive = true;
						requestUberButton.setText("Cancel \u040F\u0432\u0454\u0433");
					}
				}
			}
		});
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;
		locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
		locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				updateMap(location);
			}

			@Override
			public void onStatusChanged(String s, int i, Bundle bundle) {

			}

			@Override
			public void onProviderEnabled(String s) {

			}

			@Override
			public void onProviderDisabled(String s) {

			}
		};

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, 1);
		}
		else {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, locationListener);
			Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (lastKnownLocation != null) {
				updateMap(lastKnownLocation);
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 1) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, locationListener);
					Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					updateMap(lastKnownLocation);
				}
			}
		}
	}
}
