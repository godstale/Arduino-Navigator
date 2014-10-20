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

package com.hardcopy.arduinonavi.service;

import java.util.Timer;
import java.util.TimerTask;

import com.hardcopy.arduinonavi.bluetooth.*;
import com.hardcopy.arduinonavi.contents.NavigationInfo;
import com.hardcopy.arduinonavi.controller.GMapControl;
import com.hardcopy.arduinonavi.utils.AppSettings;
import com.hardcopy.arduinonavi.utils.Constants;
import com.hardcopy.arduinonavi.utils.Logs;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;


public class ArduinoNavigatorService extends Service {
	private static final String TAG = "LLService";
	
	// Context, System
	private Context mContext = null;
	private static Handler mActivityHandler = null;
	private ServiceHandler mServiceHandler = new ServiceHandler();
	private final IBinder mBinder = new ServiceBinder();
	
	// Bluetooth
	private BluetoothAdapter mBluetoothAdapter = null;		// local Bluetooth adapter managed by Android Framework
	private BluetoothManager mBtManager = null;
	private ConnectionInfo mConnectionInfo = null;		// Remembers connection info when BT connection is made 
	
	private TransactionBuilder mTransactionBuilder = null;
	private TransactionReceiver mTransactionReceiver = null;
	
	// Auto-refresh timer
	private Timer mRefreshTimer = null;
	
	// Navigation control
	private LocationManager mLocationManager;
	private LocationListenerImpl mLocationListener;
	private boolean mLocationServiceEnabled;
	private NavigationInfo mNaviInfo;
	
	private Location mCurrentLocation;
	private Location mDestinationLocation;
	
    
	
	/*****************************************************
	 *	Overrided methods
	 ******************************************************/
	@Override
	public void onCreate() {
		Logs.d(TAG, "# Service - onCreate() starts here");
		
		mContext = getApplicationContext();
		initialize();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Logs.d(TAG, "# Service - onStartCommand() starts here");
		
		// If service returns START_STICKY, android restarts service automatically after forced close.
		// At this time, onStartCommand() method in service must handle null intent.
		return Service.START_STICKY;
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig){
		// This prevents reload after configuration changes
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public IBinder onBind(Intent intent) {
		Logs.d(TAG, "# Service - onBind()");
		return mBinder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Logs.d(TAG, "# Service - onUnbind()");
		return true;
	}
	
	@Override
	public void onDestroy() {
		Logs.d(TAG, "# Service - onDestroy()");
		finalizeService();
	}
	
	@Override
	public void onLowMemory (){
		Logs.d(TAG, "# Service - onLowMemory()");
		// onDestroy is not always called when applications are finished by Android system.
		finalizeService();
	}

	
	/*****************************************************
	 *	Private methods
	 ******************************************************/
	private void initialize() {
		Logs.d(TAG, "# Service : initialize ---");
		
		AppSettings.initializeAppSettings(mContext);
		startServiceMonitoring();
		
		// Get connection info instance
		mConnectionInfo = ConnectionInfo.getInstance(mContext);
		
		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			return;
		}
		
		if (!mBluetoothAdapter.isEnabled()) {
			// BT is not on, need to turn on manually.
			// Activity will do this.
		} else {
			if(mBtManager == null) {
				setupBT();
			}
		}
		
		mNaviInfo = NavigationInfo.getInstance(mContext);
		initLocationManager();
		
		// Send navigation info to remote device periodically
		mRefreshTimer = new Timer();
		mRefreshTimer.schedule(new RefreshTimerTask(), 5*1000, 3*1000);
	}
	
	private void initLocationManager() {
		mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
		mLocationListener = new LocationListenerImpl();
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
	}
	
	private boolean isLocationServiceEnabled() {
		return mLocationServiceEnabled;
	}
	
	private void resumeUpdates() {
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
		mLocationServiceEnabled = true;
	}
	
	private void stopLocationUpdates() {
		mLocationManager.removeUpdates(mLocationListener);
		mLocationServiceEnabled = false;
	}
	
	
	/**
	 * Send message to device.
	 * @param message		message to send
	 */
	private void sendMessageToDevice(int mode, int unitType, int distance, int angle) {
		if(distance < 0 || angle > 360 || angle < 0)
			return;
		
		TransactionBuilder.Transaction transaction = mTransactionBuilder.makeTransaction();
		transaction.begin();
		transaction.setMessage(mode, unitType, distance, angle);
		transaction.settingFinished();
		transaction.sendTransaction();
	}
	
	
	/*****************************************************
	 *	Public methods
	 ******************************************************/
	public void finalizeService() {
		Logs.d(TAG, "# Service : finalize ---");
		
		// Stop the bluetooth session
		mBluetoothAdapter = null;
		if (mBtManager != null)
			mBtManager.stop();
		mBtManager = null;
		
		// Stop the timer
		if(mRefreshTimer != null) {
			mRefreshTimer.cancel();
			mRefreshTimer = null;
		}
		stopLocationUpdates();
	}
	
	/**
	 * Setting up bluetooth connection
	 * @param h
	 */
	public void setupService(Handler h) {
		mActivityHandler = h;
		
		// Double check BT manager instance
		if(mBtManager == null)
			setupBT();
		
		// Initialize transaction builder & receiver
		if(mTransactionBuilder == null)
			mTransactionBuilder = new TransactionBuilder(mBtManager, mActivityHandler);
		if(mTransactionReceiver == null)
			mTransactionReceiver = new TransactionReceiver(mActivityHandler);
		
		// If ConnectionInfo holds previous connection info,
		// try to connect using it.
		if(mConnectionInfo.getDeviceAddress() != null && mConnectionInfo.getDeviceName() != null) {
			connectDevice(mConnectionInfo.getDeviceAddress());
		} 
		// or wait in listening mode
		else {
			if (mBtManager.getState() == BluetoothManager.STATE_NONE) {
				// Start the bluetooth service
				mBtManager.start();
			}
		}
	}
	
    /**
     * Setup and initialize BT manager
     */
	public void setupBT() {
        Logs.d(TAG, "Service - setupBT()");

        // Initialize the BluetoothManager to perform bluetooth connections
        if(mBtManager == null)
        	mBtManager = new BluetoothManager(this, mServiceHandler);
    }
	
    /**
     * Check bluetooth is enabled or not.
     */
	public boolean isBluetoothEnabled() {
		if(mBluetoothAdapter==null) {
			Log.e(TAG, "# Service - cannot find bluetooth adapter. Restart app.");
			return false;
		}
		return mBluetoothAdapter.isEnabled();
	}
	
	/**
	 * Get scan mode
	 */
	public int getBluetoothScanMode() {
		int scanMode = -1;
		if(mBluetoothAdapter != null)
			scanMode = mBluetoothAdapter.getScanMode();
		
		return scanMode;
	}

    /**
     * Initiate a connection to a remote device.
     * @param address  Device's MAC address to connect
     */
	public void connectDevice(String address) {
		Logs.d(TAG, "Service - connect to " + address);
		
		// Get the BluetoothDevice object
		if(mBluetoothAdapter != null) {
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
			
			if(device != null && mBtManager != null) {
				mBtManager.connect(device);
			}
		}
	}
	
    /**
     * Connect to a remote device.
     * @param device  The BluetoothDevice to connect
     */
	public void connectDevice(BluetoothDevice device) {
		if(device != null && mBtManager != null) {
			mBtManager.connect(device);
		}
	}

	/**
	 * Get connected device name
	 */
	public String getDeviceName() {
		return mConnectionInfo.getDeviceName();
	}

	/**
	 * Send message to remote device using Bluetooth
	 */
	public void sendMessageToRemote() {
		int mode = AppSettings.getNaviMode();
		int unitType = AppSettings.getUnitType();
		int distance = (int)mNaviInfo.getDistance();
		int angle = (int)mNaviInfo.getAngle();
		
		if(distance < 0 || angle < 0 || angle > 360)
			return;
		
		if(unitType == GMapControl.UNIT_TYPE_METERS) {
			if(distance > 1000) {
				distance = distance / 1000;
				unitType = GMapControl.UNIT_TYPE_KMETERS;
			}
		} else if(unitType == GMapControl.UNIT_TYPE_FEET) {
			Logs.d("Distance in meter = "+distance);
			distance *= 3.2808;
			Logs.d("Distance in feet = "+distance);
			if(distance > 5280) {		// convert in miles
				distance = distance / 5280;		// I assumed that 1 Mile = 5280 Feet.
				unitType = GMapControl.UNIT_TYPE_MILES;
			} else {		// convert in feets
				// Distance in feets could flow over 10 bit max value. So we divide this with 5.
				// Receiver should multiply distance value if it's arriving in 'feet' type
				distance /= 5; 
			}
		}
		
		sendMessageToDevice(mode, unitType, distance, angle);
	}
	
	/**
	 * Start service monitoring. Service monitoring prevents
	 * unintended close of service.
	 */
	public void startServiceMonitoring() {
		if(AppSettings.getBgService()) {
			ServiceMonitoring.startMonitoring(mContext);
		} else {
			ServiceMonitoring.stopMonitoring(mContext);
		}
	}
	
	
	
	/*****************************************************
	 *	Handler, Listener, Timer, Sub classes
	 ******************************************************/
	public class ServiceBinder extends Binder {
		public ArduinoNavigatorService getService() {
			return ArduinoNavigatorService.this;
		}
	}
	
    /**
     * Receives messages from bluetooth manager
     */
	class ServiceHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg) {
			
			switch(msg.what) {
			// Bluetooth state changed
			case BluetoothManager.MESSAGE_STATE_CHANGE:
				// Bluetooth state Changed
				Logs.d(TAG, "Service - MESSAGE_STATE_CHANGE: " + msg.arg1);
				
				switch (msg.arg1) {
				case BluetoothManager.STATE_NONE:
					mActivityHandler.obtainMessage(Constants.MESSAGE_BT_STATE_INITIALIZED).sendToTarget();
					if(mRefreshTimer != null) {
						mRefreshTimer.cancel();
						mRefreshTimer = null;
					}
					break;
					
				case BluetoothManager.STATE_LISTEN:
					mActivityHandler.obtainMessage(Constants.MESSAGE_BT_STATE_LISTENING).sendToTarget();
					break;
					
				case BluetoothManager.STATE_CONNECTING:
					mActivityHandler.obtainMessage(Constants.MESSAGE_BT_STATE_CONNECTING).sendToTarget();
					break;
					
				case BluetoothManager.STATE_CONNECTED:
					mActivityHandler.obtainMessage(Constants.MESSAGE_BT_STATE_CONNECTED).sendToTarget();
					break;
				}
				break;

			// If you want to send data to remote
			case BluetoothManager.MESSAGE_WRITE:
				Logs.d(TAG, "Service - MESSAGE_WRITE: ");
				break;

			// Received packets from remote
			case BluetoothManager.MESSAGE_READ:
				Logs.d(TAG, "Service - MESSAGE_READ: ");
				
				byte[] readBuf = (byte[]) msg.obj;
				int readCount = msg.arg1;
				// construct commands from valid bytes in the buffer
				if(mTransactionReceiver != null) {
					// TODO: Do something with incoming data					
					//mTransactionReceiver.setByteArray(readBuf, readCount);
					//Object obj = mTransactionReceiver.getObject();
				}
				break;
				
			case BluetoothManager.MESSAGE_DEVICE_NAME:
				Logs.d(TAG, "Service - MESSAGE_DEVICE_NAME: ");
				
				// save connected device's name and notify using toast
				String deviceAddress = msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_ADDRESS);
				String deviceName = msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_NAME);
				
				if(deviceName != null && deviceAddress != null) {
					// Remember device's address and name
					mConnectionInfo.setDeviceAddress(deviceAddress);
					mConnectionInfo.setDeviceName(deviceName);
					
					Toast.makeText(getApplicationContext(), 
							"Connected to " + deviceName, Toast.LENGTH_SHORT).show();
				}
				break;
				
			case BluetoothManager.MESSAGE_TOAST:
				Logs.d(TAG, "Service - MESSAGE_TOAST: ");
				
				Toast.makeText(getApplicationContext(), 
						msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_TOAST), 
						Toast.LENGTH_SHORT).show();
				break;
				
			}	// End of switch(msg.what)
			
			super.handleMessage(msg);
		}
	}	// End of class MainHandler
	
	
	
	public class LocationListenerImpl implements LocationListener {
		@Override
		public void onLocationChanged(Location location) {
			mCurrentLocation = location;
			if(mNaviInfo != null) {
				mNaviInfo.setCurLocation(location);
				if(mDestinationLocation == null) {
					mNaviInfo.initDestLocation(location);
					mDestinationLocation = location;
				}
			}
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
	
	
    /**
     * Auto-refresh Timer
     */
	private class RefreshTimerTask extends TimerTask {
		public RefreshTimerTask() {}
		
		public void run() {
			mServiceHandler.post(new Runnable() {
				public void run() {
					sendMessageToRemote();
				}
			});
		}
	}
	
	
	
}
