package com.sebastianlundquist.uber;

import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

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
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class DriverLocationActivity extends FragmentActivity implements OnMapReadyCallback {

	Intent intent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_driver_location);
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);
		intent = getIntent();
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		GoogleMap mMap = googleMap;

		// Show driver's and rider's location on the map
		LatLng driverLocation = new LatLng(intent.getDoubleExtra("driverLatitude", 0), intent.getDoubleExtra("driverLongitude", 0));
		LatLng requestLocation = new LatLng(intent.getDoubleExtra("requestLatitude", 0), intent.getDoubleExtra("requestLongitude", 0));
		ArrayList<Marker> markers = new ArrayList<>();
		markers.add(mMap.addMarker(new MarkerOptions().position(driverLocation).title("Your Location")));
		markers.add(mMap.addMarker(new MarkerOptions().position(requestLocation).title("Request Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))));
		LatLngBounds.Builder builder = new LatLngBounds.Builder();
		for (Marker marker : markers) {
			builder.include(marker.getPosition());
		}
		LatLngBounds bounds = builder.build();
		int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,64, getApplicationContext().getResources().getDisplayMetrics());
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
		mMap.animateCamera(cameraUpdate);
	}

	public void acceptRequest(View view) {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
		query.whereEqualTo("username", intent.getStringExtra("username"));
		query.findInBackground(new FindCallback<ParseObject>() {
			@Override
			public void done(List<ParseObject> objects, ParseException e) {
				if (e == null) {
					if (objects.size() > 0) {
						for (ParseObject object : objects) {
							// Notify server that request has been accepted
							object.put("driverUsername", ParseUser.getCurrentUser().getUsername());
							object.saveInBackground(new SaveCallback() {
								@Override
								public void done(ParseException e) {
									if (e == null) {
										// Launch google maps activity with directions from driver location to rider location
										Intent directionsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?saddr=" + intent.getDoubleExtra("driverLatitude", 0) + "," + intent.getDoubleExtra("driverLongitude", 0) + "&daddr=" + intent.getDoubleExtra("requestLatitude", 0) + "," + intent.getDoubleExtra("requestLongitude", 0)));
										startActivity(directionsIntent);
									}
								}
							});
						}
					}
				}
			}
		});
	}
}
