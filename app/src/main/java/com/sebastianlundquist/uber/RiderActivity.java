package com.sebastianlundquist.uber;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

	private GoogleMap mMap;

	Button requestUberButton;
	TextView infoTextView;
	LocationManager locationManager;
	LocationListener locationListener;
	Handler handler = new Handler();
	Boolean requestActive = false;
	Boolean driverActive = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rider);
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

		mapFragment.getMapAsync(this);
		requestUberButton = findViewById(R.id.requestUberButton);
		requestUberButton.setText("Request \u040F\u0432\u0454\u0433");
		infoTextView = findViewById(R.id.infoTextView);

		// Check if user already has a pending request
		ParseQuery<ParseObject> query = new ParseQuery<>("Request");
		query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
		query.findInBackground(new FindCallback<ParseObject>() {
			@Override
			public void done(List<ParseObject> objects, ParseException e) {
				if (e == null) {
					if (objects.size() > 0) {
						requestActive = true;
						requestUberButton.setText("Cancel \u040F\u0432\u0454\u0433");
						checkForUpdates();
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
				updateMap(lastKnownLocation);
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
					updateMap(lastKnownLocation);
				}
			}
		}
	}

	public void updateMap(Location location) {
		if (!driverActive) {
			LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
			mMap.clear();
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
			mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
		}
	}

	public void checkForUpdates() {
		// Get the current request that has been accepted
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
		query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
		query.whereExists("driverUsername");
		query.findInBackground(new FindCallback<ParseObject>() {
			@Override
			public void done(List<ParseObject> objects, ParseException e) {
				if (e == null && objects.size() > 0) {
					// Find the driver
					driverActive = true;
					ParseQuery<ParseUser> userParseQuery = ParseUser.getQuery();
					userParseQuery.whereEqualTo("username", objects.get(0).getString("driverUsername"));
					userParseQuery.findInBackground(new FindCallback<ParseUser>() {
						@Override
						public void done(List<ParseUser> objects, ParseException e) {
							if (e == null && objects.size() > 0) {
								ParseGeoPoint driverLocation = objects.get(0).getParseGeoPoint("location");
								if (ContextCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
									Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
									if (lastKnownLocation != null) {
										ParseGeoPoint userLocation = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
										double distance = driverLocation.distanceInKilometersTo(userLocation);

										// Alert user that their driver has arrived
										if (distance < 0.1) {
											infoTextView.setText("Your driver is here!");
											ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
											query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
											query.findInBackground(new FindCallback<ParseObject>() {
												@Override
												public void done(List<ParseObject> objects, ParseException e) {
													if (e == null) {
														for (ParseObject object : objects) {
															object.deleteInBackground();
														}
													}
												}
											});
											handler.postDelayed(new Runnable() {
												@Override
												public void run() {
													infoTextView.setText("");
													requestUberButton.setVisibility(View.VISIBLE);
													requestUberButton.setText("Request \u040F\u0432\u0454\u0433");
													requestActive = false;
													driverActive = false;
												}
											}, 5000);
										}
										else {
											// Update map with new driver and rider locations
											Double distanceRounded = (double)Math.round((distance * 10) / 10);
											infoTextView.setText("Your driver is " + distanceRounded + " kilometers away!");

											LatLng driverLocationLatLng = new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude());
											LatLng requestLocationLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

											ArrayList<Marker> markers = new ArrayList<>();

											mMap.clear();
											markers.add(mMap.addMarker(new MarkerOptions().position(driverLocationLatLng).title("Driver Location")));
											markers.add(mMap.addMarker(new MarkerOptions().position(requestLocationLatLng).title("Your Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))));

											LatLngBounds.Builder builder = new LatLngBounds.Builder();
											for (Marker marker : markers) {
												builder.include(marker.getPosition());
											}
											LatLngBounds bounds = builder.build();
											int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,64, getApplicationContext().getResources().getDisplayMetrics());
											CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
											mMap.animateCamera(cameraUpdate);
											requestUberButton.setVisibility(View.INVISIBLE);
											handler.postDelayed(new Runnable() {
												@Override
												public void run() {
													checkForUpdates();
												}
											}, 3000);
										}

									}
								}
							}
						}
					});
				}
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						checkForUpdates();
					}
				}, 3000);
			}
		});
	}

	public void requestUber(View view) {
		if (requestActive) {
			// Cancel any active requests and delete them from Parse
			ParseQuery<ParseObject> query = new ParseQuery<>("Request");
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
						handler.postDelayed(new Runnable() {
							@Override
							public void run() {
								checkForUpdates();
							}
						}, 3000);
					}
				}
			});
		}
		else {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				// Create a new request
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
								checkForUpdates();
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

	public void logout(View view) {
		// Logout from Parse and pop to root
		ParseUser.logOut();
		Intent intent = new Intent(getApplicationContext(), MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
}
