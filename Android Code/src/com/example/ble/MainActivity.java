package com.example.ble;

import android.R.bool;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Element.DataType;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

public class MainActivity extends Activity implements BluetoothAdapter.LeScanCallback {
	private static final String TAG = "BluetoothGattActivity";
	private static final String DEVICE_NAME = "YWMSy_1";
	
    public static final UUID YWMSY_SERVICE =     UUID.fromString("F9266FD7-EF07-45D6-8EB6-BD74F13620F9");
    public static final UUID YWMSy_Rx_CHAR =     UUID.fromString("4585C102-7784-40B4-88E1-3CB5C4FD37A3");
    public static final UUID YWMSy_Temp    =     UUID.fromString("4585C102-7784-40B4-88E1-3CB5C4FD37A4");
    public static final UUID YWMSy_Prox    =     UUID.fromString("4585C102-7784-40B4-88E1-3CB5C4FD37A5");
    public static final UUID YWMSy_Batt    =     UUID.fromString("4585C102-7784-40B4-88E1-3CB5C4FD37A6");
    public static final UUID YWMSy_Tx_CHAR =     UUID.fromString("E788D73B-E793-4D9E-A608-2F2BAFC59A00");
    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private BluetoothAdapter mBluetoothAdapter;
    private SparseArray<BluetoothDevice> mDevices;
    private BluetoothGatt mConnectedGatt;
    public TextView mRx, mTx, labelTx, labelRx, Temp, Batt, Prox;
    private ProgressDialog mProgress;
    Button OnButton;
    Button OffButton;
    Button AlarmOnButton;
    Button AlarmOffButton;
    Button Sound_Detect;
    boolean TurnOnButtonPressed = false;
    boolean OneTimeOn = true;

    
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);
        setProgressBarIndeterminate(true);
        
        mRx = (TextView) findViewById(R.id.RxTerminal);
        mTx = (TextView) findViewById(R.id.TxTerminal);
        Temp = (TextView) findViewById(R.id.Temp);
        Prox = (TextView) findViewById(R.id.Prox);
        Batt = (TextView) findViewById(R.id.Batt);
        labelTx = (TextView) findViewById(R.id.textView1);
        labelRx = (TextView) findViewById(R.id.textView2);
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
        mDevices = new SparseArray<BluetoothDevice>();
        
        mProgress = new ProgressDialog(this);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(false);
        OnButton = (Button)findViewById(R.id.TurnON);
        OffButton = (Button)findViewById(R.id.TurnOFF);
        AlarmOnButton = (Button)findViewById(R.id.ActivateAlarm);
        AlarmOffButton = (Button)findViewById(R.id.DeactivateAlarm);
        Sound_Detect = (Button)findViewById(R.id.DetectSound);
        OffButton.setVisibility(View.GONE);
        AlarmOnButton.setVisibility(View.GONE);
        AlarmOffButton.setVisibility(View.GONE);
    }
    
    protected void onResume(){
    	super.onResume();
    	if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
    		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    		startActivity(enableBtIntent);
    		//finish();
    		return;
    	}
    	
    	if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
    		Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
    		finish();
    		return;
    	}
    	clearDisplayValues();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	mProgress.dismiss();
    	mHandler.removeCallbacks(mStopRunnable);
    	mHandler.removeCallbacks(mStartRunnable);
    	mBluetoothAdapter.stopLeScan(this);	
    }
    
    @Override
    protected void onStop(){
    	super.onStop();
    	if(mConnectedGatt != null){
    		mConnectedGatt.disconnect();
    		mConnectedGatt = null;
    		
    	}
    }

    
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        
        for(int i = 0; i < mDevices.size(); i++){
        	BluetoothDevice device = mDevices.valueAt(i);
        	menu.add(0, mDevices.keyAt(i), 0, device.getName());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                mDevices.clear();
                startScan();
                return true;
            default:
                //Obtain the discovered device to connect with
                BluetoothDevice device = mDevices.get(item.getItemId());
//                Log.i(TAG, "Connecting to "+device.getName());
                /*
                 * Make a connection with the device using the special LE-specific
                 * connectGatt() method, passing in a callback for GATT events
                 */
                mConnectedGatt = device.connectGatt(this, false, mGattCallback);
                //Display progress UI
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Connecting to "+device.getName()+"..."));
                
                // my first button this button send data to YWMSy based on witch button i press
                // this button only works with 
                OnButton.setOnClickListener(new View.OnClickListener() {		
                	public void onClick(View v) {
                		TurnOnButtonPressed = true;
                		TurnOn();
                		
            		}
                	});
                
                OffButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View arg0) {
                		TurnOnButtonPressed = true;
                		TurnOff();
					}
				});
                AlarmOnButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
                		TurnOnButtonPressed = true;
                		AlarmOn();

						
					}
				});
                AlarmOffButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
                		TurnOnButtonPressed = true;
                		AlarmOff();

					}
				});
                Sound_Detect.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Detect_Sound();
						
					}
				});
                
                return super.onOptionsItemSelected(item);
        }
 
    }
    
    public void Detect_Sound()
    {
    	BluetoothGattCharacteristic characteristic = null;
		characteristic = mConnectedGatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Tx_CHAR);
        characteristic.setValue(new byte[] {0x68});//char g
        mConnectedGatt.writeCharacteristic(characteristic);
        OnButton.setVisibility(View.GONE);
        OffButton.setVisibility(View.VISIBLE);
        AlarmOnButton.setVisibility(View.GONE);
        AlarmOffButton.setVisibility(View.VISIBLE);
        labelTx.setVisibility(View.VISIBLE);
        labelRx.setVisibility(View.VISIBLE);
        mTx.setVisibility(View.VISIBLE);
        mRx.setVisibility(View.VISIBLE);
    }
    
    public void ProximityOn()
    {
    	BluetoothGattCharacteristic characteristic = null;
		characteristic = mConnectedGatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Tx_CHAR);
        characteristic.setValue(new byte[] {0x67});//char g
        mConnectedGatt.writeCharacteristic(characteristic);
        OnButton.setVisibility(View.GONE);
        OffButton.setVisibility(View.VISIBLE);
        AlarmOnButton.setVisibility(View.GONE);
        AlarmOffButton.setVisibility(View.VISIBLE);
        labelTx.setVisibility(View.VISIBLE);
        labelRx.setVisibility(View.VISIBLE);
        mTx.setVisibility(View.VISIBLE);
        mRx.setVisibility(View.VISIBLE);
    }
    
    public void TurnOn()
    {
		BluetoothGattCharacteristic characteristic = null;
		characteristic = mConnectedGatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Tx_CHAR);
        characteristic.setValue(new byte[] {0x61});//char a to turn on 
        mConnectedGatt.writeCharacteristic(characteristic);
        OnButton.setVisibility(View.GONE);
        OffButton.setVisibility(View.VISIBLE);
        AlarmOnButton.setVisibility(View.VISIBLE);
        labelTx.setVisibility(View.VISIBLE);
        labelRx.setVisibility(View.VISIBLE);
        mTx.setVisibility(View.VISIBLE);
        mRx.setVisibility(View.VISIBLE);
    }
    
    public void TurnOff(){
		BluetoothGattCharacteristic characteristic = null;
		characteristic = mConnectedGatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Tx_CHAR);
        characteristic.setValue(new byte[] {0x62});// char b for turning off 
        mConnectedGatt.writeCharacteristic(characteristic);
        OnButton.setVisibility(View.VISIBLE);
        OffButton.setVisibility(View.GONE);
        AlarmOnButton.setVisibility(View.GONE);
        AlarmOffButton.setVisibility(View.GONE);
        labelTx.setVisibility(View.VISIBLE);
        labelRx.setVisibility(View.VISIBLE);
        mTx.setVisibility(View.VISIBLE);
        mRx.setVisibility(View.VISIBLE);
    }
    
    public void AlarmOn()
    {
		BluetoothGattCharacteristic characteristic = null;
		characteristic = mConnectedGatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Tx_CHAR);
        characteristic.setValue(new byte[] {0x63});//char c for turning on alarm
        mConnectedGatt.writeCharacteristic(characteristic);
        OnButton.setVisibility(View.GONE);
        OffButton.setVisibility(View.VISIBLE);
        AlarmOnButton.setVisibility(View.GONE);
        AlarmOffButton.setVisibility(View.VISIBLE);
        labelTx.setVisibility(View.VISIBLE);
        labelRx.setVisibility(View.VISIBLE);
        mTx.setVisibility(View.VISIBLE);
        mRx.setVisibility(View.VISIBLE);
    }
    
    public void AlarmOff(){
		BluetoothGattCharacteristic characteristic = null;
		characteristic = mConnectedGatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Tx_CHAR);
        characteristic.setValue(new byte[] {0x64});//char d for disable alarm
        mConnectedGatt.writeCharacteristic(characteristic);
        OnButton.setVisibility(View.GONE);
        OffButton.setVisibility(View.VISIBLE);
        AlarmOnButton.setVisibility(View.VISIBLE);
        AlarmOffButton.setVisibility(View.GONE);
        labelTx.setVisibility(View.VISIBLE);
        labelRx.setVisibility(View.VISIBLE);
        mTx.setVisibility(View.VISIBLE);
        mRx.setVisibility(View.VISIBLE);
    }


    private void clearDisplayValues() {
		mTx.setText("--");
		mRx.setText("--");
		Temp.setText("Temperature: --");
		Prox.setText("Proximity: --");
		Batt.setText("Battery: --");
	}
    
    private Runnable mStartRunnable = new Runnable(){
    	@Override
    	public void run(){
    		startScan();
    	}
    };
    
    private Runnable mStopRunnable = new Runnable(){
    	@Override
    	public void run(){
    		stopScan();
    	}
    };

    private void startScan() {
        mBluetoothAdapter.startLeScan(this);
        setProgressBarIndeterminateVisibility(true);
        mHandler.postDelayed(mStopRunnable, 2500);
    }

    private void stopScan() {
        mBluetoothAdapter.stopLeScan(this);
        setProgressBarIndeterminateVisibility(false);
    }
    
	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
	        /* We are looking for SensorTag devices only, so validate the name
	           that each device reports before adding it to our collection*/
	        if (DEVICE_NAME.equals(device.getName())) {
	            mDevices.put(device.hashCode(), device);
	            //Update the overflow menu
	            invalidateOptionsMenu();
	        }

	}
	
	public BluetoothGattCallback mGattCallback = new BluetoothGattCallback(){

        /* State Machine Tracking */
        private int mState = 0;

        private void reset() { mState = 0; }

        private void advance() { mState++; }
        
        /*Send an enable command to each sensor by writing a configuration
           characteristic.  This is specific to the SensorTag to keep power
           low by disabling sensors you aren't using.*/  
        private void enableNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic = null;
            
            switch (mState) {
                case 0:
                    characteristic = gatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Tx_CHAR);
                    characteristic.setValue(new byte[] {0x7B});
                    gatt.writeCharacteristic(characteristic);
                    break;
                case 1:
                    characteristic = gatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Tx_CHAR);
                    characteristic.setValue(new byte[] {0x7B});
                    gatt.writeCharacteristic(characteristic);
                    break;
                case 2:
                    characteristic = gatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Tx_CHAR);
                    characteristic.setValue(new byte[] {0x7B});
                    gatt.writeCharacteristic(characteristic);
                    break;
                case 3:
                    characteristic = gatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Tx_CHAR);
                    characteristic.setValue(new byte[] {0x7B});
                    gatt.writeCharacteristic(characteristic);
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    return;
            }
            
            gatt.writeCharacteristic(characteristic);
        }


        private void readNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic = null;

                    characteristic = gatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Rx_CHAR);
                    gatt.readCharacteristic(characteristic);
                    characteristic = gatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Temp);
                    gatt.readCharacteristic(characteristic);
                    characteristic = gatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Prox);
                    gatt.readCharacteristic(characteristic);
                    characteristic = gatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Batt);
                    gatt.readCharacteristic(characteristic);
        }

        /*Enable notification of changes on the data characteristic for each sensor
          by writing the ENABLE_NOTIFICATION_VALUE flag to that characteristic's
          configuration descriptor.*/
        private void setNotifyNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic = null;
            switch (mState) {
                case 0:
                	Log.i(TAG, "Set notify on Temp Value");
                	characteristic = gatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Temp);
                	//Enable local notifications
                	gatt.setCharacteristicNotification(characteristic, true);
                	break;
                case 1:
                    Log.d(TAG, "Set notify on Rx Value");  
                    characteristic = gatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Rx_CHAR);
                    //Enable local notifications
                    gatt.setCharacteristicNotification(characteristic, true);
                    break;
                case 2:
                	Log.i(TAG, "Set notify on Proximity Value");
                	characteristic = gatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Prox);
                	//Enable local notifications
                	gatt.setCharacteristicNotification(characteristic, true);
                	break;
                case 3:
                	Log.i(TAG, "Set notify on Battery Value");
                	characteristic = gatt.getService(YWMSY_SERVICE).getCharacteristic(YWMSy_Batt);
                	//Enable local notifications
                	gatt.setCharacteristicNotification(characteristic, true);
                	break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    return;
            }

            //Enabled remote notifications
            BluetoothGattDescriptor desc = characteristic.getDescriptor(CONFIG_DESCRIPTOR);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
        }
        
        
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                /*Once successfully connected, we must next discover all the services on the
                  device before we can read and write their characteristics.*/
                gatt.discoverServices();
//Toast.makeText(MainActivity.this, "I am in on Service Discover", Toast.LENGTH_LONG).show();
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Discovering Services..."));
            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                //If at any point we disconnect, send a message to clear the weather values out of the UI
                mHandler.sendEmptyMessage(MSG_CLEAR);
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                 // If there is a failure at any stage, simply disconnect
                gatt.disconnect();
            }
        }
        
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Enabling Sensors..."));
    
            /* With services discovered, we are going to reset our state machine and start
               working through the sensors we need to enable */
            reset();
            enableNextSensor(gatt);

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//        	Log.i(TAG, "Start of onCharacteristicRead");
            //For each read, pass the data up to the UI thread to update the display
            if (YWMSy_Rx_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_Rx, characteristic));
            }
            if (YWMSy_Temp.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_Temp, characteristic));
            }
            if(YWMSy_Prox.equals(characteristic.getUuid())){
            	mHandler.sendMessage(Message.obtain(null, MSG_Prox,characteristic));
            	
            }
            if(YWMSy_Batt.equals(characteristic.getUuid())){
            	mHandler.sendMessage(Message.obtain(null, MSG_Batt,characteristic));
            }
           // if (YWMSy_Tx_CHAR.equals(characteristic.getUuid())) {
           // mHandler.sendMessage(Message.obtain(null, MSG_BUTTON, characteristic));
           // }
            //After reading the initial value, next we enable notifications both for temp and read Rx
            setNotifyNextSensor(gatt);
            setNotifyNextSensor(gatt);
            setNotifyNextSensor(gatt);
            setNotifyNextSensor(gatt);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //After writing the enable flag, next we read the initial value
            readNextSensor(gatt);
            if(TurnOnButtonPressed == true){
                mHandler.sendMessage(Message.obtain(null, MSG_BUTTON, characteristic));
                TurnOnButtonPressed = false;
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            /* After notifications are enabled, all updates from the device on characteristic
             * value changes will be posted here.  Similar to read, we hand these up to the
             * UI thread to update the display.*/
            if (YWMSy_Rx_CHAR.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_Rx, characteristic));
            }
            if (YWMSy_Temp.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_Temp, characteristic));
            }
            if (YWMSy_Prox.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_Prox, characteristic));
            }
            if (YWMSy_Batt.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_Batt, characteristic));
            }
            
            //if(TurnOnButtonPressed == true){
            //    mHandler.sendMessage(Message.obtain(null, MSG_BUTTON, characteristic));
            //    TurnOnButtonPressed = false;
           // }

        }
        
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //Once notifications are enabled, we move to the next sensor and start over with enable
            advance();
            enableNextSensor(gatt);
        }
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        }

        private String connectionState(int status) {
            switch (status) {
                case BluetoothProfile.STATE_CONNECTED:
                    return "Connected";
                case BluetoothProfile.STATE_DISCONNECTED:
                    return "Disconnected";
                case BluetoothProfile.STATE_CONNECTING:
                    return "Connecting";
                case BluetoothProfile.STATE_DISCONNECTING:
                    return "Disconnecting";
                default:
                    return String.valueOf(status);
            }
        }
    };


    
    
   // We have a Handler to process event results on the main thread
    private static final int MSG_Rx = 103;
    private static final int MSG_Temp = 102;
    private static final int MSG_Prox = 101;
    private static final int MSG_Batt = 100;
    private static final int MSG_PROGRESS = 201;
    private static final int MSG_DISMISS = 202;
    private static final int MSG_CLEAR = 301;
    private static final int MSG_BUTTON = 302;
    
    private Handler mHandler = new Handler() {
        
    	@Override
        public void handleMessage(Message msg) {
            BluetoothGattCharacteristic characteristic = null;

            switch (msg.what) {
                case MSG_Rx:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining Rx value");
                        return;
                    }
                    updateRxValue(characteristic);
                    break;
                case MSG_Temp:
                	characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining Temp value");
                        return;
                    }
                    updateTempValue(characteristic);
                    break;
                case MSG_Prox:
                	characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining Proximity value");
                        return;
                    }
                    updateProxValue(characteristic);
                    break;
                case MSG_Batt:
                	characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining Battery value");
                        return;
                    }
                    updateBattValue(characteristic);
                    break;
                case MSG_PROGRESS:
                    mProgress.setMessage((String) msg.obj);
                    if (!mProgress.isShowing()) {
                        mProgress.show();
                    }
                    break;
                case MSG_DISMISS:
                    mProgress.hide();
                    break;
                case MSG_CLEAR:
                    clearDisplayValues();
                    break;
                case MSG_BUTTON:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining Tx value");
                        return;
                    }
                	updateTxValue(characteristic);
                	break;
                	
            }
        }

        
    };


    /* Methods to extract sensor data and update the UI */
    private void updateRxValue(BluetoothGattCharacteristic characteristic) {
    	String data_2 = null;
        data_2 = characteristic.getStringValue(0);
        if(data_2.contains("f")){
        	//Toast.makeText(MainActivity.this, "it contains f", Toast.LENGTH_LONG).show();
        	//mRx.setVisibility(View.VISIBLE);
        	mRx.setText(" "+data_2);
        }
        try{
        	int numb = (int)(Integer.valueOf(data_2.toString()));
        	mRx.setText(" "+data_2);
        }catch(Exception e)
        {
        	//Toast.makeText(MainActivity.this, "Error Rx Value", Toast.LENGTH_LONG).show();
        }
 
        	
    }
    //display the temp value
    private void updateTempValue(BluetoothGattCharacteristic characteristic) {
    	//Toast.makeText(MainActivity.this, "In Temp", Toast.LENGTH_LONG).show();	
       	String data_2 = null;
        data_2 = characteristic.getStringValue(0);
        float tempertureC = (float)(Float.valueOf(data_2.toString()));
        short tempertureF = (short) (((tempertureC * 9.0 )/ 5.0) + 32.0);
        //Toast.makeText(MainActivity.this, String.valueOf(tempertureF), Toast.LENGTH_LONG).show();
        Temp.setText("Temperature:"+data_2.toString()+"C"+"   "+String.valueOf(tempertureF)+"F");

    }
    
	private void updateTxValue(BluetoothGattCharacteristic characteristic) {
		// TODO Auto-generated method stub
    	String data_2 = null;
        data_2 = characteristic.getStringValue(0);
        mTx.setText(" "+data_2);
	}
	
	private void updateProxValue(BluetoothGattCharacteristic characteristic){
		//Toast.makeText(MainActivity.this, "Iam in Proximity value", Toast.LENGTH_LONG).show();
		char holder = '0';
		char holder_1 = '2';
		String data_2 = characteristic.getStringValue(0);
		Prox.setText("Proximity: " + String.valueOf(data_2));
		if(data_2.charAt(0) == holder && OneTimeOn)
		{
			OneTimeOn = false;
			ProximityOn();
		}
		if(data_2.charAt(0) == holder_1)
		{
			OneTimeOn = true;
			//AlarmOff();
			//TurnOff();
		}
	}
	
	private void updateBattValue(BluetoothGattCharacteristic characteristic){
		//Toast.makeText(MainActivity.this, "Iam in Batt value", Toast.LENGTH_LONG).show();
		String BattValue = characteristic.getStringValue(0);
		char var_1 = BattValue.charAt(0);
		int var_2 = (int) var_1;
		Batt.setText("Battery: " + var_2);

		
	}
}
