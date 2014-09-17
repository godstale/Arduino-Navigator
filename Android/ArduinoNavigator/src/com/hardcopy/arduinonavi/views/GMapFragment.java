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

package com.hardcopy.arduinonavi.views;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.hardcopy.arduinonavi.controller.GMapControl;

import android.content.Context;
import android.os.Handler;


/**
 * 
 * WARNING: Do not use this
 * For future use
 *
 */
public class GMapFragment extends SupportMapFragment {

	private Context mContext = null;
	private IFragmentListener mFragmentListener = null;
	private Handler mActivityHandler = null;
	
	// Google map
	private GMapControl mMapControl;
	private UiSettings mUiSettings;
	
	private double mCurrentLatitude = 0.;
	private double mCurrentLongitude = 0.;
	
	// Layout
	//----- Main Layout
	public static int mScreenW = 0;
	public static int mScreenH = 0;
	
	
	
	
	public GMapFragment(Context c, IFragmentListener l, Handler h) {
		mContext = c;
		mFragmentListener = l;
		mActivityHandler = h;
	}
	
	
	
	/*****************************************************
	*		Override methods
	******************************************************/
	
//	@Override
//	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//		View rootView = inflater.inflate(R.layout.fragment_main_dummy, container, false);
//		
//		initialize();
//		
//		return rootView;
//	}
	
	/*
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mMapControl.stopUpdates();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mMapControl.resumeUpdates();
	}
	*/
	
	
	/*****************************************************
	*		Private methods
	******************************************************/
//	private void initialize() {
//		Display display = getWindowManager().getDefaultDisplay();
//		mScreenW = display.getWidth();
//		mScreenH = display.getHeight();
//		
//		GoogleMap gmap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_map)).getMap();
//		mMapControl = new GMapControl(mContext, null, gmap, mScreenW, mScreenH);
//	}
	
	
	
	/*****************************************************
	*		Public methods
	******************************************************/
	
	
	
	
}
