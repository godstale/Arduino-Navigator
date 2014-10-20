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

package com.hardcopy.arduinonavi.bluetooth;

import java.util.Arrays;

import com.hardcopy.arduinonavi.controller.GMapControl;
import com.hardcopy.arduinonavi.utils.Constants;


import android.os.Handler;
import android.util.Log;

/**
 * If you want to send something to remote
 * add methods here.
 * 
 * begin() : Initialize parameters
 * setXxxx() : Add methods as you wish
 * settingFinished() : Every data is ready.
 * sendTransaction() : Send to remote
 * 
 */
public class TransactionBuilder {
	
	private static final String TAG = "TransactionBuilder";
	
	private BluetoothManager mBTManager = null;
	private Handler mHandler = null;
	
	public TransactionBuilder(BluetoothManager bm, Handler errorHandler) {
		mBTManager = bm;
		mHandler = errorHandler;
	}
	
	public Transaction makeTransaction() {
		return new Transaction();
	}
	
	public class Transaction {
		
		public static final int MAX_MESSAGE_LENGTH = 16;
		
		// Transaction instance status
		private static final int STATE_NONE = 0;		// Instance created
		private static final int STATE_BEGIN = 1;		// Initialize transaction
		private static final int STATE_SETTING_FINISHED = 2;	// End of setting parameters 
		private static final int STATE_TRANSFERED = 3;	// End of sending transaction data
		private static final int STATE_ERROR = -1;		// Error occurred
		
		public static final int MODE_COMPASS = GMapControl.NAVI_MODE_COMPASS;
		public static final int MODE_DIRECTION = GMapControl.NAVI_MODE_DIRECTION;
		
		public static final int UNIT_TYPE_METERS = GMapControl.UNIT_TYPE_METERS;
		public static final int UNIT_TYPE_KMETERS = GMapControl.UNIT_TYPE_KMETERS;
		public static final int UNIT_TYPE_FEETS = GMapControl.UNIT_TYPE_FEET;
		public static final int UNIT_TYPE_MILES = GMapControl.UNIT_TYPE_MILES;
		
		public static final int DIRECTION_LEFT = 1;
		public static final int DIRECTION_RIGHT = 2;
		public static final int DIRECTION_STRAIGHT = 3;
		public static final int DIRECTION_ARRIVING = 4;
		
		
		
		// Transaction parameters
		private int mState = STATE_NONE;
		private byte[] mBuffer = null;
		private String mMsg = null;			// Disabled: for future use
		
		
		
		
		/***************************************************************************
		 * Protocol specifications 
		 **************************************************************************/
		/**************************************************************************
		[Start bytes] : 2 bytes, Indicates start of protocol (0xfdfe)

		[body]
			[Navigation mode] 1 bit (0: compass mode, 1: direction mode)
			[Distance unit] : 2 bits (00: meters, 01:kilometers, 10: feets, 11: miles)
			[Direction] : 2 bits (00: left ,01: right , 10: straight ,11: arriving)
			[Distance] : 10 bits - Android converts into lower distance unit if distance is shorter than 1.
			[Angle] : 9 bits

		[End bytes] : 2 bytes, Indicates end of protocol (0xfefe)
		*************************************************************************/
		
		
		/**
		 * Make new transaction instance
		 */
		public void begin() {
			mState = STATE_BEGIN;
			mMsg = null;
			mBuffer = new byte[7];
			Arrays.fill(mBuffer, (byte)0x00);
			
			// set start bytes
			mBuffer[0] = (byte)0xfd;
			mBuffer[1] = (byte)0xfe;
			// set end bytes
			mBuffer[5] = (byte)0xfe;
			mBuffer[6] = (byte)0xfe;
		}
		
		/**
		 * Set string to send
		 */
		public void setMessage(int mode, int unitType, int distance, int angle) {
			if(mode == MODE_COMPASS) {
				//mBuffer[2] |= (byte)0x00;
			} else if(mode == MODE_DIRECTION) {
				mBuffer[2] |= (byte)0x80;
			} else {
				return;
			}
			
			if(distance < 0) return;
			if(unitType == UNIT_TYPE_METERS) {
				//mBuffer[2] |= (byte)0x00;
			} else if(unitType == UNIT_TYPE_KMETERS) {
				mBuffer[2] |= (byte)0x20;
			} else if(unitType == UNIT_TYPE_FEETS) {
				mBuffer[2] |= (byte)0x40;
			} else if(unitType == UNIT_TYPE_MILES) {
				mBuffer[2] |= (byte)0x60;
			} else {
				return;
			}
			
			if(angle < 0 || angle > 360) return;
			if(angle > 45 && angle <= 135) {		// Go right
				mBuffer[2] |= (byte)0x08;
			} else if(angle > 135 && angle <= 225) {	// Go Back
				mBuffer[2] |= (byte)0x18;
			} else if(angle > 225 && angle <= 315) {	// Go left
				//mBuffer[2] |= (byte)0x00;
			} else {
				mBuffer[2] |= (byte)0x10;		// Go forward
			}
			
			mBuffer[2] |= (distance & 0x03ff) >> 7;		// set first 3 bit
			mBuffer[3] |= (distance & 0x7f) << 1;		// set following 7 bit
			
			mBuffer[3] |= (angle & 0x01ff) >> 8;		// set first 1 bit
			mBuffer[4] |= (angle & 0xff);		// set following 8 bit
		}
		
		/**
		 * Ready to send data to remote
		 */
		public void settingFinished() {
			mState = STATE_SETTING_FINISHED;
		}
		
		/**
		 * Send packet to remote
		 * @return	boolean		is succeeded
		 */
		public boolean sendTransaction() {
			if(mBuffer == null || mBuffer.length < 1) {
				Log.e(TAG, "##### Ooooooops!! No sending buffer!! Check command!!");
				return false;
			}
			
			// For debug. Comment out below lines if you want to see the packets
			if(mBuffer.length > 0) {
				StringBuilder sb = new StringBuilder();
				sb.append("Message : \n");
				
				for(int i=0; i<mBuffer.length; i++) {
					//sb.append(String.format("%02X, ", mBuffer[i]));
					for(int j=0; j<8; j++) {
						sb.append( ((mBuffer[i] >> (7-j)) & 0x01) == 0x00 ? 0 : 1 );
					}
					sb.append("\n");
				}
				
				Log.d(TAG, " ");
				Log.d(TAG, sb.toString());
				
				// Check validation of outgoing packet
				int navMode = (mBuffer[2] & 0x80)>>7;
				Log.d(TAG, "Mode : " + navMode);
				String navUnit = "none";
				switch(mBuffer[2] & 0x60){
				case 0x00 :
					navUnit = "meter";
					break;
				case 0x20 :
					navUnit = "kilo meter";
					break;
				case 0x40 :
					navUnit = "feet";
					break;
				case 0x60 :
					navUnit = "mile";
					break;
				}
				Log.d(TAG, "Mode : " + navUnit);
				
				String navDir = "none";
				switch(mBuffer[2] & 0x18){
				case 0x08 :
					navDir = "right";
					break;
				case 0x18 :
					navDir = "back";
					break;
				case 0x00 :
					navDir = "left";
					break;
				case 0x10 :
					navDir = "forward";
					break;
				}
				Log.d(TAG, "Direction : " + navDir);
				
				int navDist = 0;
				navDist+= (mBuffer[2] & 0x07) << 7;
				navDist+= (mBuffer[3] & 0xfe) >> 1;
				if(navUnit.contains("feet"))
					navDist *= 5;
				Log.d(TAG, "Distance : " + navDist);
				
				int navAng = 0;
				navAng+= (mBuffer[3] & 0x01) << 8;
				navAng+= (mBuffer[4] & 0xff);
				Log.d(TAG, "Angle : " + navAng);
				
			}
			
			if(mState == STATE_SETTING_FINISHED) {
				if(mBTManager != null) {
					// Check that we're actually connected before trying anything
					if (mBTManager.getState() == BluetoothManager.STATE_CONNECTED) {
						// Check that there's actually something to send
						if (mBuffer.length > 0) {
							// Get the message bytes and tell the BluetoothManager to write
							mBTManager.write(mBuffer);
							
							mState = STATE_TRANSFERED;
							return true;
						}
						mState = STATE_ERROR;
					}
					// Report result
					mHandler.obtainMessage(Constants.MESSAGE_CMD_ERROR_NOT_CONNECTED).sendToTarget();
				}
			}
			return false;
		}
		
		/**
		 * Get buffers to send to remote
		 */
		public byte[] getPacket() {
			if(mState == STATE_SETTING_FINISHED) {
				return mBuffer;
			}
			return null;
		}
		
	}	// End of class Transaction

}
