/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.example.android.BT_LIFA;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BluetoothLIFA extends Activity {
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    static final int PIN_AMOUNT = 6;//Must match layout
    private CheckBox[] mPinsCheckBox = new CheckBox[PIN_AMOUNT];
    private EditText mFrequencyEditText, mSamplesEditText;
    private Button mContinuousStartButton,mContinuousStopButton,mFiniteStartButton;
    private TextView mLastReadingTextView;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothLIFAService mChatService = null;
    
    
    //The active pins
    ActivePins mActivePins =new ActivePins();
    
    //LIFA specific values
    static final int COMMAND_LENGTH = 15;
    
    //Commands
    static final byte CONTINUOS_AQUISTION_MODE_ON = 0x36;
    static final byte FINITE_AQUISTION_MODE_ON = 0x35;
    static final byte CONTINUOS_AQUISTION_MODE_OFF = 0x2b;
    
    //Progream Status
    private byte mStatus = CONTINUOS_AQUISTION_MODE_OFF;
    	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        //Add by me
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        // Set up the window layout
        setContentView(R.layout.main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothLIFAService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the Check boxes
        for(int i=0; i<PIN_AMOUNT; i++) {
        	   
        	    String chkBoxID = "checkBoxPin" + i ;
        	    int resID = getResources().getIdentifier(chkBoxID, "id", getPackageName());
        	    mPinsCheckBox[i] = ((CheckBox) findViewById(resID));
        	    
        	   
        	}

        // Initialize the text inputs
        mFrequencyEditText = (EditText) findViewById(R.id.editTextFreq);
        mSamplesEditText = (EditText) findViewById(R.id.editTextSamples);
        
        // Initialize the text output
        mLastReadingTextView=(TextView) findViewById(R.id.textViewLastRead);

        // Initialize the buttons with a listener that for click events
        
        mContinuousStartButton = (Button) findViewById(R.id.buttonConStart);
        mContinuousStartButton.setEnabled(true);
        mContinuousStartButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                
            	Toast.makeText(BluetoothLIFA.this, "Continuous Start clicked.", Toast.LENGTH_SHORT).show(); 
            	//Disable the start buttons and enable the stop button
            	mContinuousStartButton.setEnabled(false);
            	mFiniteStartButton.setEnabled(false);
            	mContinuousStopButton.setEnabled(true);
            	byte[] command = generateCommand(CONTINUOS_AQUISTION_MODE_ON);
            	mStatus=CONTINUOS_AQUISTION_MODE_ON;
            	sendCommand(command);
            	// TODO Add the part that reads the values
            }

			
            
        });
        
        mContinuousStopButton = (Button) findViewById(R.id.buttonConStop);
        	mContinuousStopButton.setEnabled(false);
        mContinuousStopButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	//Disable the stop button and enable the start buttons
            	mContinuousStartButton.setEnabled(true);
            	mFiniteStartButton.setEnabled(true);
            	mContinuousStopButton.setEnabled(false);
            	Toast.makeText(BluetoothLIFA.this, "Continuous Stop clicked.", Toast.LENGTH_SHORT).show();
            	byte[] command = generateCommand(CONTINUOS_AQUISTION_MODE_OFF);
            	mStatus=CONTINUOS_AQUISTION_MODE_OFF;
            	sendCommand(command);
            	// TODO Stop the part that reads the command
            }
        });
        mFiniteStartButton = (Button) findViewById(R.id.buttonFinStart);
        mFiniteStartButton.setEnabled(true);
        mFiniteStartButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {                
            	Toast.makeText(BluetoothLIFA.this, "Finite Start clicked.", Toast.LENGTH_SHORT).show();
            	//Disable all buttons
            	//TODO make sure they are enabled once the running is done
            	mContinuousStartButton.setEnabled(false);
            	mFiniteStartButton.setEnabled(false);
            	mContinuousStopButton.setEnabled(false);
            	byte[] command = generateCommand(FINITE_AQUISTION_MODE_ON);
            	mStatus=FINITE_AQUISTION_MODE_ON;
            	sendCommand(command);
            	// TODO Add the part that reads the values
            }
        });
        		
        
        /*mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });*/

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothLIFAService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }
    
    private byte[] generateCommand(byte commandType) {
		byte[] command =new byte[COMMAND_LENGTH];//Initalized to zero by default
    	command[0]=(byte) 0xff;//The verification byte
    	command[1]=commandType;//Command Type
    	
    	if(commandType==CONTINUOS_AQUISTION_MODE_ON || commandType==FINITE_AQUISTION_MODE_ON){
    		//Get the active pins from the check box
        	
        	mActivePins.getPinData();
        	command[2] = (byte) mActivePins.onPinAmount;
        	command[7] = mActivePins.pinBits;
        	//Get the frequency
        	int frequency=0;
        	try {
        		frequency = Integer.parseInt(mFrequencyEditText.getText().toString());
        		frequency=(int) (1.024*frequency);//To account for arduino clock issues
        		Toast.makeText(BluetoothLIFA.this, String.valueOf(frequency) , Toast.LENGTH_LONG).show(); 
        	} catch(NumberFormatException nfe) {//Input should be a number, this is a just an extra measure
        		Log.e(TAG, "Frequency was not a number");
        	} 
        	if (frequency>1000){
        		Toast.makeText(BluetoothLIFA.this, "Ardunio sampling at over 1KHz may not work", Toast.LENGTH_LONG).show(); 
        	}
        	
        	command[3] = (byte) (frequency & 0xff);//Least significant byte
        	command[4] = (byte) ((frequency>>8) & 0xff);//Most significant byte
    		
    	}
    	
    	if(commandType==FINITE_AQUISTION_MODE_ON){
    		int samples=0;
        	try {
        		samples = Integer.parseInt(mSamplesEditText.getText().toString());
        	} catch(NumberFormatException nfe) {//Input should be a number, this is a just an extra measure
        		Log.e(TAG, "Samples was not a number");
        	} 
        	
        	
        	command[5] = (byte) (samples & 0xff);//Least significant byte
        	command[6] = (byte) ((samples>>8) & 0xff);//Most significant byte
    	}
    	//Create checksum
    	int checksum = 0;//Since unsigned char is not a value in java
    	for (int i=0; i<(COMMAND_LENGTH-1); i++)
    	  {
    	    checksum += command[i]; 
    	  }
    	command[14]=(byte) checksum;
		return command;
	}
    public class ActivePins {
    	int onPinAmount;
        byte pinBits;
    	public ActivePins() {
	        onPinAmount=0;
	        pinBits=0;
    	}
        
        public void getPinData() {   
        	onPinAmount=0;
        	pinBits=0;
        	for(int i=0; i<PIN_AMOUNT; i++) {//If more than 8 pins are used (say arduino mega) this needs to be modified
        		if (mPinsCheckBox[i].isChecked()) {
        			pinBits=(byte) (pinBits | (0x80 >>> i));//Unsigned bit shift to the bins location
        			onPinAmount=onPinAmount+1;
                }
        	    
        	    
         	   
        	}
    		
        }
        
    }

    

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothLIFAService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mFrequencyEditText.setText(mOutStringBuffer);
        }
    }
    
    /**
     * Sends a command.
     * @param message  A byte array that is the command to send.
     */
    private void sendCommand(byte[] command) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothLIFAService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        
            mChatService.write(command);

            // Reset out string buffer to zero
            mOutStringBuffer.setLength(0);
            
        
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };

    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }
    

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothLIFAService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                    //mConversationArrayAdapter.clear();
                    //TODO delete the conversation adapter
                    break;
                case BluetoothLIFAService.STATE_CONNECTING:
                    setStatus(R.string.title_connecting);
                    break;
                case BluetoothLIFAService.STATE_LISTEN:
                case BluetoothLIFAService.STATE_NONE:
                    setStatus(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
            	//TODO read the messages
            	int pinAmount = mActivePins.onPinAmount;
                byte[] readBuf = (byte[]) msg.obj;
                switch(mStatus){
                case FINITE_AQUISTION_MODE_ON:
                	int samples=0;
                	try {
                		samples = Integer.parseInt(mSamplesEditText.getText().toString());
                	} catch(NumberFormatException nfe) {//Input should be a number, this is a just an extra measure
                		Log.e(TAG, "Samples was not a number");
                	}
                	
                	
                }
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
        case R.id.secure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        /*case R.id.insecure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            return true;*/
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }

}
