package com.hardcopy.arduinonavi.contents;

import java.util.ArrayList;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.hardcopy.arduinonavi.controller.GMapControl;
import com.hardcopy.arduinonavi.utils.AppSettings;


public class NavigationInfo {

	private Context mContext;
	
	private Location mCurLocation;
	private Location mDestLocation;
	private double mCurLatitude = 0;
	private double mCurLongitude = 0;
	private double mDestLatitude = 0;
	private double mDestLongitude = 0;
	
	private float mDistance = 0;
	private float mAngle = 0;
	
	private int mUnitType = GMapControl.UNIT_TYPE_METERS;
	private int mNaviMode = GMapControl.NAVI_MODE_COMPASS;
	
	
	private static NavigationInfo mNaviInfo = null;		// Singleton pattern
	
	
	
	private NavigationInfo(Context c) {
		mContext = c;
		AppSettings.initializeAppSettings(mContext);
		initialize();
	}
	
	
	// Singleton pattern
	public static synchronized NavigationInfo getInstance(Context c) {
		if(mNaviInfo == null) {
			mNaviInfo = new NavigationInfo(c);
		}
		
		return mNaviInfo;
	}
	

	
	/*****************************************************
	 *	Private methods
	 ******************************************************/
	private void initialize() {
		mCurLatitude = 0;
		mCurLongitude = 0;
		mDestLatitude = AppSettings.getDestinationLat();
		mDestLongitude = AppSettings.getDestinationLng();
		mUnitType = AppSettings.getUnitType();
		mNaviMode = AppSettings.getNaviMode();
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
		// TODO: 
		return angle;
	}
	
	
	
	/*****************************************************
	 *	Public methods
	 ******************************************************/
	public synchronized void setCurLocation(Location loc) {
		if(loc == null)
			return;
		
		if(mCurLocation == null) {
			mCurLocation = new Location(loc);
		} else {
			mCurLocation.setLatitude(loc.getLatitude());
			mCurLocation.setLongitude(loc.getLongitude());
		}
		
		mCurLatitude = loc.getLatitude();
		mCurLongitude = loc.getLongitude();
	}
	
	public synchronized void setCurLocation(LatLng latlng) {
		if(mCurLocation == null)
			return;
		
		mCurLocation.setLatitude(latlng.latitude);
		mCurLocation.setLongitude(latlng.longitude);
		mCurLatitude = latlng.latitude;
		mCurLongitude = latlng.longitude;
	}
	
	public synchronized void setCurLocation(double latitude, double longitude) {
		if(mCurLocation == null)
			return;
		
		mCurLocation.setLatitude(latitude);
		mCurLocation.setLongitude(longitude);
		mCurLatitude = latitude;
		mCurLongitude = longitude;
	}
	
	public synchronized double getCurLatitude() {
		return mCurLatitude;
	}
	
	public synchronized double getCurLongitude() {
		return mCurLongitude;
	}
	
	
	
	public synchronized void initDestLocation(Location loc) {
		if(loc == null)
			return;
		if(mDestLocation == null) {
			mDestLocation = new Location(loc);		// This is dummy location instance.
		}
		// At this time Location instance is dummy instance. 
		// So we must update location information with destination data loaded from preference.
		mDestLocation.setLatitude(mDestLatitude);
		mDestLocation.setLongitude(mDestLongitude);
	}
	
	public synchronized void setDestLocation(Location loc) {
		if(loc != null)
			mDestLocation = new Location(loc);		// This is dummy location instance.
		mDestLatitude = loc.getLatitude();
		mDestLongitude = loc.getLongitude();
	}
	
	public synchronized void setDestLocation(LatLng latlng) {
		mDestLocation.setLatitude(latlng.latitude);
		mDestLocation.setLongitude(latlng.longitude);
		mDestLatitude = latlng.latitude;
		mDestLongitude = latlng.longitude;
	}
	
	public synchronized void setDestLocation(double latitude, double longitude) {
		mDestLocation.setLatitude(latitude);
		mDestLocation.setLongitude(longitude);
		mDestLatitude = latitude;
		mDestLongitude = longitude;
	}
	
	public synchronized double getDestLatitude() {
		return mDestLocation.getLatitude();
	}
	
	public synchronized double getDestLongitude() {
		return mDestLocation.getLongitude();
	}
	
	
	
	public synchronized float getDistance() {
		if( mCurLocation == null || mDestLocation == null
				|| (mCurLatitude == 0 && mCurLongitude == 0)
				|| (mDestLatitude == 0 && mDestLongitude == 0) ) {
			return -1;
		} else {
			mDistance = mCurLocation.distanceTo(mDestLocation);
		}

		return mDistance;
	} 
	
	public synchronized float getAngle() {
		if( mCurLocation == null || mDestLocation == null
				|| (mCurLatitude == 0 && mCurLongitude == 0)
				|| (mDestLatitude == 0 && mDestLongitude == 0) ) {
			return -1;
		} else {
			mAngle = mCurLocation.bearingTo(mDestLocation);		// Angles: -180 ~ 180
		}

		if(mAngle < 0) {
			mAngle += 360;	// convert to 0~360 range
		}
		
		return mAngle;
	}
	
	public int getNaviMode() {
		mNaviMode = AppSettings.getNaviMode();
		return mNaviMode;
	}
	
	public int getUnitType() {
		mUnitType = AppSettings.getUnitType();
		return mUnitType;
	}
	
	
	
	
}
