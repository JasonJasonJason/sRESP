package com.NewApp;

import android.app.Activity;
import android.widget.Spinner;
import android.media.SoundPool;
import android.media.AudioManager;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

//import com.NewApp.android.R;


import android.R.*;
import android.app.Activity;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.SeekBar.OnSeekBarChangeListener;
import zephyr.android.BioHarnessBT.*;

public class MainActivity extends Activity {
    /** Called when the activity is first created. */
	BluetoothAdapter adapter = null;
	BTClient _bt;
	ZephyrProtocol _protocol;
	NewConnectedListener _NConnListener;
	private final int HEART_RATE = 0x100;
	private final int RESPIRATION_RATE = 0x101;
	private final int SKIN_TEMPERATURE = 0x102;
	private final int POSTURE = 0x103;
	private final int PEAK_ACCLERATION = 0x104;
	SoundPool soundPool;
	boolean[] songsPlaying;
	int[] streamIds;
	int[] soundIds;
	
	public Spinner spinnerTrack;
	
	//@Override
    //public void onCreate1(Bundle savedInstanceState) {
      //  super.onCreate1(savedInstanceState);
        //Log.d("Application", "onCreate");
        //setContentView(R.layout.main_menu);
        
       // spinnerTrack = (Spinner)findViewById(R.id.spinner1);
	//}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Application", "onCreate");   
        
        setContentView(R.layout.main);
        spinnerTrack = (Spinner)findViewById(R.id.spinner1);
        
        //Sending a message to android that we are going to initiate a pairing request
        IntentFilter filter = new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST");
        //Registering a new BTBroadcast receiver from the Main Activity context with pairing request event
       this.getApplicationContext().registerReceiver(new BTBroadcastReceiver(), filter);
        // Registering the BTBondReceiver in the application that the status of the receiver has changed to Paired
        IntentFilter filter2 = new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED");
       this.getApplicationContext().registerReceiver(new BTBondReceiver(), filter2);
        
      //Obtaining the handle to act on the CONNECT button
        TextView tv = (TextView) findViewById(R.id.labelStatusMsg);
		String ErrorText  = "Failed to connect!";
		 tv.setText(ErrorText);

        Button btnConnect = (Button) findViewById(R.id.ButtonConnect);
        if (btnConnect != null)
        {
        	btnConnect.setOnClickListener(new OnClickListener() {
        		public void onClick(View v) {
        			String BhMacID = "00:07:80:9D:8A:E8";
        			//String BhMacID = "00:07:80:88:F6:BF";
        			adapter = BluetoothAdapter.getDefaultAdapter();
        			
        			Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        			
        			if (pairedDevices.size() > 0) 
        			{
                        for (BluetoothDevice device : pairedDevices) 
                        {
	                        	if (device.getName().startsWith("BH")) 
	                        	{
	                        		BluetoothDevice btDevice = device;
	                        		BhMacID = btDevice.getAddress();
	                                break;
	                        	}
                        }                        
        			}
        			
        			//BhMacID = btDevice.getAddress();
        			BluetoothDevice Device = adapter.getRemoteDevice(BhMacID);
        			String DeviceName = Device.getName();
        			_bt = new BTClient(adapter, BhMacID);
        			_NConnListener = new NewConnectedListener(Newhandler,Newhandler);
        			_bt.addConnectedEventListener(_NConnListener);
        			
        			TextView tv1 = (EditText)findViewById(R.id.labelRespRate);
        			 tv1.setText("0.0");
        			 
        			if(_bt.IsConnected())
        			{
        				_bt.start();
        				TextView tv = (TextView) findViewById(R.id.labelStatusMsg);
        				String ErrorText  = "Connected to BioHarness "+DeviceName;
						 tv.setText(ErrorText);
        			}
        			else
        			{
        				TextView tv = (TextView) findViewById(R.id.labelStatusMsg);
        				String ErrorText  = "Unable to Connect !";
						 tv.setText(ErrorText);
        			}
        		}
        	});
        }
        //Obtaining the handle to act on the DISCONNECT button
        Button btnDisconnect = (Button) findViewById(R.id.ButtonDisconnect);
        if (btnDisconnect != null)
        {
        	btnDisconnect.setOnClickListener(new OnClickListener() {
				@Override
				//Functionality to act if the button DISCONNECT is touched
				public void onClick(View v) {
					// TODO Auto-generated method stub
					//Reset the global variables
					TextView tv = (TextView) findViewById(R.id.labelStatusMsg);
    				String ErrorText  = "Disconnected from BioHarness!";
					 tv.setText(ErrorText);

					//This disconnects listener from acting on received messages	
					_bt.removeConnectedEventListener(_NConnListener);
					//Close the communication with the device & throw an exception if failure
					_bt.Close();
				}
        	});
        }
        
        //Initializing Sounds
        initSounds();
        
        //Initializing SeekBars
        initSeekBars();
    }
    
    @Override
    public void onPause(){
    		super.onPause();
    		Log.d("Application", "onPause");
    		for(int i=0; i<soundIds.length; i++){
    			songsPlaying[i] = false;
    			soundPool.stop(streamIds[i]);
    		}
    }
    
    public void goToGraph(View view)
    {
    		Intent intent = new Intent(this, GraphActivity.class);
        startActivity(intent);
    }
    
    private void initSeekBars(){
    	
    		SeekBar[] seekBars = new SeekBar[3];
    		seekBars[0] = ((SeekBar) findViewById(R.id.seekBar1));
    		seekBars[1] = ((SeekBar) findViewById(R.id.seekBar2));
    		seekBars[2] = ((SeekBar) findViewById(R.id.seekBar3));
    		
    		initSeekBar(seekBars[0], 0);
    		initSeekBar(seekBars[1], 1);
    		initSeekBar(seekBars[2], 2);
    }
    
    private void initSeekBar(SeekBar seekBar, final int index)
    {
    			seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar arg0, int seekPercentage, boolean arg2) {
            		float volume = (float)seekPercentage/100;
            		soundPool.setVolume(soundIds[index], volume, volume);
            }
			@Override public void onStartTrackingTouch(SeekBar seekBar) {	}
			@Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    private void initSounds(){
    	
    	songsPlaying = new boolean[3];
    	soundIds = new int[3];
    	streamIds = new int[3];
    	
    	for(int i=0; i<songsPlaying.length; i++)
    	{
    		songsPlaying[i] = false;
    	}
    	
    	soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
    	
    	soundIds[0] = soundPool.load(getBaseContext(), R.raw.song1, 1);
	    soundIds[1] = soundPool.load(getBaseContext(), R.raw.song2, 1);
	    soundIds[2] = soundPool.load(getBaseContext(), R.raw.song3, 1);
	    soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
	        public void onLoadComplete(SoundPool soundPool, int sampleId,int status) {
	        	onSoundsLoadComplete(sampleId);
	        }
	    });
	    	    
    }
    
    private void onSoundsLoadComplete(int sampleId){
    	//soundPool.play(sampleId, 1, 1, 1, 0, 1);
    }
    
    public void toggle1(View view) {
    	toggle(0);
    }
    
    public void toggle2(View view) {
    	toggle(1);
    }
    
    public void toggle3(View view) {
    	toggle(2);
    }
    
    private void toggle(int songNumber){
	    	
	    	if(!songsPlaying[songNumber])
	    	{
	    		streamIds[songNumber] = soundPool.play(soundIds[songNumber], 1, 1, 1, 0, 1);
	    		songsPlaying[songNumber] = true;
	    	}
	    	else {
	    		soundPool.stop(streamIds[songNumber]);
	    		songsPlaying[songNumber] = false;    
	    	}
    }
    
    
    private class BTBondReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle b = intent.getExtras();
			BluetoothDevice device = adapter.getRemoteDevice(b.get("android.bluetooth.device.extra.DEVICE").toString());
			Log.d("Bond state", "BOND_STATED = " + device.getBondState());
		}
    }
    private class BTBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("BTIntent", intent.getAction());
			Bundle b = intent.getExtras();
			Log.d("BTIntent", b.get("android.bluetooth.device.extra.DEVICE").toString());
			Log.d("BTIntent", b.get("android.bluetooth.device.extra.PAIRING_VARIANT").toString());
			try {
				BluetoothDevice device = adapter.getRemoteDevice(b.get("android.bluetooth.device.extra.DEVICE").toString());
				Method m = BluetoothDevice.class.getMethod("convertPinToBytes", new Class[] {String.class} );
				byte[] pin = (byte[])m.invoke(device, "1234");
				m = device.getClass().getMethod("setPin", new Class [] {pin.getClass()});
				Object result = m.invoke(device, pin);
				Log.d("BTTest", result.toString());
			} catch (SecurityException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (NoSuchMethodException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }
    

    final  Handler Newhandler = new Handler(){
    	public void handleMessage(Message msg)
    	{
    		TextView tv;
    		switch (msg.what)
    		{
    		case RESPIRATION_RATE:
    			String RespirationRatetext = msg.getData().getString("RespirationRate");
    			tv = (EditText)findViewById(R.id.labelRespRate);
    			if (tv != null)tv.setText(RespirationRatetext);    		
    			break;    		
    		}
    	}

    };
    
}


