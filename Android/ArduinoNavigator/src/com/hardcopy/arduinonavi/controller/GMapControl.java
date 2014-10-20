package com.hardcopy.arduinonavi.controller;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
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
import com.google.android.gms.maps.model.PolylineOptions;
import com.hardcopy.arduinonavi.R;
import com.hardcopy.arduinonavi.contents.NavigationInfo;
import com.hardcopy.arduinonavi.utils.AppSettings;
import com.hardcopy.arduinonavi.views.IFragmentListener;


public class GMapControl {

	public static final String tag = "GMapControl";

	private static final float INITIAL_ZOOM_LEVEL = 14f;
	
	public static final int NAVI_MODE_COMPASS = 1;
	public static final int NAVI_MODE_DIRECTION = 2;
	
	public static final int UNIT_TYPE_METERS = 1;
	public static final int UNIT_TYPE_KMETERS = 2;
	public static final int UNIT_TYPE_FEET = 3;
	public static final int UNIT_TYPE_MILES = 4;
	
	
	// Context, System
	private Context mContext;
	private Handler mHandler;
	private IFragmentListener mFragmentListener = null;

	// Google map
	private GoogleMap mGoogleMap;
	private UiSettings mUiSettings;
	
	private Location mCurrentLocation;
	private Location mDestinationLocation;
	
	private float mCurrentZoom = 9f;
	
	// Layout
	//----- Main Layout
	public static int mScreenW = 0;
	public static int mScreenH = 0;
	
	// Global
	private boolean mIsInitialized = false;
	private int mNaviMode = NAVI_MODE_COMPASS;
	private int mUnitType = UNIT_TYPE_METERS;
	private NavigationInfo mNaviInfo = null;
	
	
	/*****************************************************
	*		Initialization methods
	******************************************************/
	
	public GMapControl(Context c, Handler h, IFragmentListener l, GoogleMap gmap, int screenSizeX, int screenSizeY) {
		mContext = c;
		mHandler = h;
		mFragmentListener = l;
		mGoogleMap = gmap;
		mScreenW = screenSizeX;
		mScreenH = screenSizeY;
		mNaviInfo = NavigationInfo.getInstance(mContext);
		
		initialize();
	}

	
	private void initialize() {
		initMap();
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
				if(mDestinationLocation == null || mCurrentLocation == null)
					return;
				
				mDestinationLocation.setLatitude(latLng.latitude);
				mDestinationLocation.setLongitude(latLng.longitude);
				mNaviInfo.setDestLocation(latLng);

				mGoogleMap.clear();
				
				drawDestination(latLng, true);
				
				mUnitType = AppSettings.getUnitType();
				StringBuilder sb = new StringBuilder();
				sb.append(mNaviMode==NAVI_MODE_COMPASS?"compass":"direction");
				sb.append(",");
				sb.append(mUnitType==UNIT_TYPE_METERS?"meters":"feet");
				if(mCurrentLocation != null) {
					mFragmentListener.OnFragmentCallback(IFragmentListener.CALLBACK_MAP_UPDATE_NAVI_INFO, 
							(int)mNaviInfo.getDistance(), (int)mNaviInfo.getAngle(),
							String.format("%.3f", mDestinationLocation.getLatitude()), 
							String.format("%.3f", mDestinationLocation.getLongitude()), 
							sb.toString()); 
				}
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

				mGoogleMap.clear();
				
				if(mDestinationLocation == null) {
					mDestinationLocation = new Location(loc);	// Destination is not selected yet. Initialize destination
					mNaviInfo.initDestLocation(mDestinationLocation);
					// If there is pre-used destination, show it on map 
					if(mNaviInfo.getDestLatitude() != 0 && mNaviInfo.getDestLongitude() != 0) {
						drawDestination(new LatLng(mNaviInfo.getDestLatitude(), mNaviInfo.getDestLongitude()), false);
					}
				} else {
					mNaviInfo.setCurLocation(loc);
				}
				
				if(!mIsInitialized) {
					zoomLevel = INITIAL_ZOOM_LEVEL;
				} else {
					zoomLevel = 0;
				}
				

				addMarkerOnMap(loc.getLatitude(), loc.getLongitude(), 
						"I'm here", 		// title
						loc.getLatitude() + ", " + loc.getLongitude(), 			// snippet
						false,				// draggable
						MARKER_DEFAULT, 	// marker
						zoomLevel, 			// 0 means keep current zoom level
						!mIsInitialized);				// move camera only when it's first time
				
				if(mNaviInfo.getDestLatitude() == 0 && mNaviInfo.getDestLongitude() == 0) {
					// Destination is not set. Do nothing
				} else {
					drawDestination(new LatLng(mNaviInfo.getDestLatitude(), mNaviInfo.getDestLongitude()), false);
				}
				
				mIsInitialized = true;
				
				mUnitType = AppSettings.getUnitType();
				StringBuilder sb = new StringBuilder();
				sb.append(mNaviMode==NAVI_MODE_COMPASS?"compass":"direction");
				sb.append(",");
				sb.append(mUnitType==UNIT_TYPE_METERS?"meters":"feet");
				mFragmentListener.OnFragmentCallback(IFragmentListener.CALLBACK_MAP_UPDATE_NAVI_INFO,
						(int)mNaviInfo.getDistance(), (int)mNaviInfo.getAngle(),
						String.format("%.3f", mDestinationLocation.getLatitude()), 
						String.format("%.3f", mDestinationLocation.getLongitude()), 
						sb.toString());
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
	
	private void drawDestination(LatLng latLng, boolean doCameraWork) {
		if(mCurrentLocation != null) {
			mGoogleMap.addPolyline(new PolylineOptions()
					.add(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), latLng)
					.width(5)
					.color(Color.RED));
		}
		
		addMarkerOnMap(latLng.latitude, latLng.longitude, 
				mContext.getResources().getString(R.string.title_destination), 
				String.format("%.3f", latLng.latitude) + ", " + String.format("%.3f", latLng.latitude), 
				true, MARKER_RED, mCurrentZoom, doCameraWork);
	}
	
	
	
	/*****************************************************
	*		Public methods
	******************************************************/
	
	public void setNaviMode(int nm) {
		mNaviMode = nm;
	}
	
	public void setUnitType(int ut) {
		mUnitType = AppSettings.getUnitType();
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
	
	
	public static final float MARKER_DEFAULT = BitmapDescriptorFactory.HUE_AZURE;	// Blue marker
	public static final float MARKER_RED = BitmapDescriptorFactory.HUE_RED;
	public static final float MARKER_GREEN = BitmapDescriptorFactory.HUE_GREEN;
	
	public void addMarkerOnMap(double lat1, double long1, String title, String snippet, boolean draggable, float markerType, float zoomFactor, boolean doCameraWork) {
		float markerStyle = MARKER_DEFAULT;
		if(markerType < 0 || markerType > 360) {
			// use default marker
		} else {
			markerStyle = markerType;
		}
		
		LatLng ll = new LatLng(lat1, long1);		// Lat: -90 ~ 90, Lng: -180 ~ 180
		mGoogleMap.addMarker(new MarkerOptions()
								.title(title)
								.snippet(snippet)
								.position(ll)
								.draggable(draggable)
								.icon( BitmapDescriptorFactory.defaultMarker(markerStyle) )
							);

		if(zoomFactor < 2.0f || zoomFactor > 21.0f || !doCameraWork) {
			// No camera animation
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
	
	
}
