/*
 * Copyright (C) 2014 Bluetooth Connection Template
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hardcopy.arduinonavi.utils;

import com.hardcopy.arduinonavi.controller.GMapControl;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettings {

	// Constants
	public static final int SETTINGS_BACKGROUND_SERVICE = 1;
	public static final int SETTINGS_UNIT_TYPE = 2;
	public static final int SETTINGS_NAVI_MODE = 3;
	public static final int SETTINGS_SET_DEST_LATITUDE = 4;
	public static final int SETTINGS_SET_DEST_LONGITUDE = 5;
	
	
	private static boolean mIsInitialized = false;
	private static Context mContext;
	
	// Setting values
	private static boolean mUseBackgroundService;
	private static int mUnitType;
	private static int mNaviMode;
	private static double mDestinationLatitude;
	private static double mDestinationLongitude;
	
	
	public static void initializeAppSettings(Context c) {
		if(mIsInitialized)
			return;
		
		mContext = c;
		
		// Load setting values from preference
		mUseBackgroundService = loadBgService();
		mUnitType = loadUnitType();
		mNaviMode = loadNaviMode();
		loadDestinationLoc();
		
		mIsInitialized = true;
	} 
	
	// Remember setting value
	public static void setSettingsValue(int type, boolean boolValue, int intValue, float floatValue, String stringValue) {
		if(mContext == null)
			return;
		
		SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		switch(type) {
		case SETTINGS_BACKGROUND_SERVICE:
			editor.putBoolean(Constants.PREFERENCE_KEY_BG_SERVICE, boolValue);
			editor.commit();
			mUseBackgroundService = boolValue;
			break;
		case SETTINGS_UNIT_TYPE:
			editor.putInt(Constants.PREFERENCE_KEY_UNIT_TYPE, intValue);
			editor.commit();
			mUnitType = intValue;
			Logs.d("Set distance unit type = "+intValue);
			break;
		case SETTINGS_NAVI_MODE:
			editor.putInt(Constants.PREFERENCE_KEY_NAVI_MODE, intValue);
			editor.commit();
			mNaviMode = intValue;
			break;
			
		case SETTINGS_SET_DEST_LATITUDE:
			editor.putFloat(Constants.PREFERENCE_KEY_DEST_LATITUDE, floatValue);
			editor.commit();
			mDestinationLatitude = floatValue;
			break;
		case SETTINGS_SET_DEST_LONGITUDE:
			editor.putFloat(Constants.PREFERENCE_KEY_DEST_LONGITUDE, floatValue);
			editor.commit();
			mDestinationLongitude = floatValue;
			break;
			
		default:
			editor.commit();
			break;
		}
	}
	
	/**
	 * Load 'Run in background' setting value from preferences
	 * @return	boolean		is true
	 */
	public static boolean loadBgService() {
		SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
		boolean isTrue = prefs.getBoolean(Constants.PREFERENCE_KEY_BG_SERVICE, false);
		return isTrue;
	}
	
	/**
	 * Returns 'Run in background' setting value
	 * @return	boolean		is true
	 */
	public static boolean getBgService() {
		return mUseBackgroundService;
	}
	
	public static int loadUnitType() {
		SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
		int unitType = prefs.getInt(Constants.PREFERENCE_KEY_UNIT_TYPE, GMapControl.UNIT_TYPE_METERS);
		return unitType;
	}
	
	public static int getUnitType() {
		return mUnitType;
	}
	
	public static int loadNaviMode() {
		SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
		int naviMode = prefs.getInt(Constants.PREFERENCE_KEY_NAVI_MODE, GMapControl.NAVI_MODE_COMPASS);
		return naviMode;
	}
	
	public static int getNaviMode() {
		return mNaviMode;
	}

	
	public static double getDestinationLat() {
		return mDestinationLatitude;
	}
	
	public static double getDestinationLng() {
		return mDestinationLongitude;
	}
	
	public static void loadDestinationLoc() {
		SharedPreferences prefs = mContext.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
		mDestinationLatitude = prefs.getInt(Constants.PREFERENCE_KEY_DEST_LATITUDE, 0);
		mDestinationLongitude = prefs.getInt(Constants.PREFERENCE_KEY_DEST_LONGITUDE, 0);
	}
	
	
}
