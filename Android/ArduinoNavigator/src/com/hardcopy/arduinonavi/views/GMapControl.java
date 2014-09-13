package com.hardcopy.arduinonavi.views;

import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class GMapControl {

	public static final String tag = "GMapControl";

	private static final float INITIAL_ZOOM_LEVEL = 14f;
	
	// Context, System
	private Context mContext;
	private Handler mHandler;
	private LocationManager mLocationManager;
	private LocationListenerImpl mLocationListener;

	// Google map
	private GoogleMap mGoogleMap;
	private UiSettings mUiSettings;
	
	private boolean mLocationServiceEnabled = false;
	private Location mCurrentLocation;
	
	private float mCurrentZoom = 9f;
	
	// Layout
	//----- Main Layout
	public static int mScreenW = 0;
	public static int mScreenH = 0;
	
	// Global
	private boolean mIsInitialized = false;
	
	
	/*****************************************************
	*		Initialization methods
	******************************************************/
	
	public GMapControl(Context c, Handler h, GoogleMap gmap, int screenSizeX, int screenSizeY) {
		mContext = c;
		mHandler = h;
		mGoogleMap = gmap;
		mScreenW = screenSizeX;
		mScreenH = screenSizeY;
		
		initialize();
	}

	
	private void initialize() {
		initLocationManager();
		initMap();
	}
	
	private void initLocationManager() {
		mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
		mLocationListener = new LocationListenerImpl();
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
		mLocationServiceEnabled = true;
	}
	
	private void initMap() {
		if(mGoogleMap != null) {
			mUiSettings = mGoogleMap.getUiSettings();
			
			setMapFeatures();
			
			setOnMapClickListener();
			setOnCameraChangeListener();
			setOnMyLocationChangedListener();
		}
	}
	
	private void setMapFeatures() {
		mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);	// MAP_TYPE_HYBRID, MAP_TYPE_NORMAL, MAP_TYPE_SATELLITE, MAP_TYPE_TERRAIN
		mGoogleMap.setMyLocationEnabled(true);
		
		mUiSettings.setCompassEnabled(true);
		mUiSettings.setZoomControlsEnabled(true);
		mUiSettings.setMyLocationButtonEnabled(true);
	}
	
	
	
	
	/*****************************************************
	*		Private methods
	******************************************************/
	
	private void setOnMapClickListener() {
		mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
			@Override
			public void onMapClick(LatLng latLng) {
				MarkerOptions markerOptions = new MarkerOptions();
				//markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.map_icon01));
				markerOptions.title("Destination");
				markerOptions.position(latLng);
				
				mGoogleMap.clear();
				mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
				mGoogleMap.addMarker(markerOptions);

			}
		});
		mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
	}
	
	private void setOnMarkerClickListener() {
		mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
			@Override
			public boolean onMarkerClick(Marker marker) {
				marker.getPosition();
				marker.showInfoWindow();

				marker.setTitle("Longitude=" + marker.getPosition().longitude);
				marker.setSnippet("Latitude=" + marker.getPosition().latitude);
				
				return true;
			}
		});
	}
	
	private void setOnMyLocationChangedListener() {
		mGoogleMap.setOnMyLocationChangeListener(new OnMyLocationChangeListener() {
			@Override
			public void onMyLocationChange(Location loc) {
				float zoomLevel = mCurrentZoom; 
				mCurrentLocation = loc;
				
				if(!mIsInitialized) {
					zoomLevel = INITIAL_ZOOM_LEVEL;
					mIsInitialized = true;
				}
				
				addMarkerOnMap(loc.getLatitude(), loc.getLongitude(), 
						"I'm here", 	// title
						loc.getLatitude() + ", " + loc.getLongitude(), 			// snippet
						false,			// draggable
						zoomLevel);	// 0 means keep current zoom level
			}
		});
	}
	
	private void setOnCameraChangeListener() {
		mGoogleMap.setOnCameraChangeListener(new OnCameraChangeListener() {
			@Override
			public void onCameraChange(CameraPosition pos) {
				mCurrentZoom = pos.zoom;
			}
		});
	}
	
	private double calcDistance(double lat1, double lon1, double lat2, double lon2) {
		double EARTH_R, Rad, radLat1, radLat2, radDist; 
		double distance, ret;

		EARTH_R = 6371000.0;
		Rad = Math.PI/180;
		radLat1 = Rad * lat1;
		radLat2 = Rad * lat2;
		radDist = Rad * (lon1 - lon2);

		distance = Math.sin(radLat1) * Math.sin(radLat2);
		distance = distance + Math.cos(radLat1) * Math.cos(radLat2) * Math.cos(radDist);
		ret = EARTH_R * Math.acos(distance);

		return  Math.round(Math.round(ret) / 1000);
	}
	
	private float calcAngle(double lat1, double lon1, double lat2, double lon2) {
		float angle = 0;
		
		
		
		return angle;
	}
	
	
	
	/*****************************************************
	*		Public methods
	******************************************************/
	
	public void stopUpdates() {
		mLocationManager.removeUpdates(mLocationListener);
		mLocationServiceEnabled = false;
	}
	
	public void resumeUpdates() {
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
		mLocationServiceEnabled = true;
	}
	
	public boolean isLocationServiceEnabled() {
		return mLocationServiceEnabled;
	}
	
	public LatLng convertPointToLatLng(Point point) {
		if(point.x < 0 || point.x > mScreenW) return null;
		if(point.y < 0 || point.y > mScreenH) return null;
		
		return mGoogleMap.getProjection().fromScreenLocation(point);
	}
	
	public void setMarkerOnPoint(int x, int y, String title, String snippet, boolean draggable) {
		if(x < 0 || x > mScreenW) return;
		if(y < 0 || y > mScreenH) return;
		
		Point point = new Point(x, y);
		LatLng loc = mGoogleMap.getProjection().fromScreenLocation(point);
		Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(loc)
				.title(title).snippet(snippet).draggable(draggable));
		// marker.getPosition();
		marker.showInfoWindow();
	}
	
	
	
	public void addMarkerOnMap(double lat1, double long1, String title, String snippet, boolean draggable, float zoomFactor) {
		LatLng ll = new LatLng(lat1, long1);		// Lat: -90 ~ 90, Lng: -180 ~ 180
		mGoogleMap.addMarker(new MarkerOptions()
								.title(title)
								.snippet(snippet)
								.position(ll)
								.draggable(draggable)
								.icon( BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE) )
							);

		if(zoomFactor < 2.0f || zoomFactor > 21.0f) {
			mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(ll));
		} else {
			mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, zoomFactor));		// zoom out ~ zoom in : 2.0f ~ 21.0f
		}
	}
	
	public void animateMap(LatLng ll){
		//mGoogleMap.animateCamera(CameraUpdateFactory.zoomIn());
		//mGoogleMap.animateCamera(CameraUpdateFactory.zoomOut());
		//mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(float));	// set the zoom level To the given value
		//mGoogleMap.animateCamera(CameraUpdateFactory.zoomBy(float));	// increases (or decreases, if the value is negative) the zoom level by the given value
		//mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(37, 123)));
		//mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()));
		mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 12.0f));
	}
	
	public void moveMap(LatLng ll){
		mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(ll));
	}
	
	public void setZoomLevel(float zoomLevel) {
		if(mGoogleMap.getMaxZoomLevel() > zoomLevel)		// Check zoom level boundary
			zoomLevel = mGoogleMap.getMaxZoomLevel();
		if(mGoogleMap.getMinZoomLevel() < zoomLevel)
			zoomLevel = mGoogleMap.getMinZoomLevel();

		mGoogleMap.animateCamera( CameraUpdateFactory.zoomTo( zoomLevel ) );	// From 2.0 to 21.0 (max zoom - min zoom)
	}
	
	
	
	/*****************************************************
	*		Sub classes
	******************************************************/
	
	/**
	 * For future use. 
	 */
	public class LocationListenerImpl implements LocationListener {
		@Override
		public void onLocationChanged(Location location) {
//			mCurrentLatitude = location.getLatitude();
//			mCurrentLongitude = location.getLongitude();
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
		}
	}
	
	
	
}
