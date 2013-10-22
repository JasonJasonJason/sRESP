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
import android.text.Editable;
import android.text.TextWatcher;
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
<<<<<<< HEAD
	
	public Spinner spinnerTrack;
=======
	BioHarnessController bhController;
	 
>>>>>>> d6073ca74d2b409aa20dc97ac957216492b599d8
	
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
        
        //Initializing BioHarnessController
        bhController = new BioHarnessController();
        
        EditText textMessage = (EditText)findViewById(R.id.editText1);
        textMessage.addTextChangedListener(new TextWatcher(){
        		@Override
            public void afterTextChanged(Editable s) {
        			try{
        				handleNewRespirationRate(Float.parseFloat(s.toString()));
        			}
        			catch(Exception e)
        			{
        				Log.e("EditText", "Unparseable float: " + s.toString());
        			}
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        }); 
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
    	
    	soundIds[0] = soundPool.load(getBaseContext(), R.raw.bass, 1);
	    soundIds[1] = soundPool.load(getBaseContext(), R.raw.drumz, 1);
	    soundIds[2] = soundPool.load(getBaseContext(), R.raw.synth, 1);
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
    
    
    private void startChannel(int channel)
    {
		streamIds[channel] = soundPool.play(soundIds[channel], 1, 1, 1, 0, 1);
    }
    
    
    private void stopChannel(int channel)
    {
    		soundPool.stop(streamIds[channel]);
    }
    
    
    private void playChannels(int[] channels){
    	
    		for(int channel=0; channel<streamIds.length; channel++)
    		{
    			stopChannel(channel);
    		}
    	
    		for(int channel : channels)
    		{
    			startChannel(channel);
    		}
    		
    }
    
    
    private void adjustAudio()
    {
    		int respirationRate = (int) Math.floor(bhController.getRespirationRate());
    		switch(respirationRate)
    		{
    			case 0:
    			case 1:
    			case 2:	
    				playChannels( new int[] {0} );
    				break;
    			case 3:
    			case 4:
    				playChannels( new int[] {0,2});
    				break;
    			case 5:
    			case 6:
    			case 7:
    				playChannels( new int[] {0,1,2});
    				break;
    			case 8:
    			case 9:
    			case 10:
    			case 11:
    				playChannels( new int[] {0,2});
    				break;
    			case 12:
    			case 13:
    			case 14:
    			case 15:
    			default:
    				playChannels( new int[] {0});
    				break;
    		}
    }
    
    
    private void handleNewRespirationRate(float respirationRate)
    {
    		float previousRespirationRate = bhController.getRespirationRate();
    		bhController.setRespirationRate(respirationRate);
    		
    		if(Math.floor(previousRespirationRate) != Math.floor(respirationRate))
    		{
    			adjustAudio();
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
	    		try{
	    			float respirationRate = Float.parseFloat(RespirationRatetext);
	    			handleNewRespirationRate(respirationRate);
    			}
			catch(Exception e)
			{
				Log.e("BioHarness Handler", "Unparseable float: " + RespirationRatetext);
			}
    			tv = (EditText)findViewById(R.id.labelRespRate);
    			if (tv != null)tv.setText(RespirationRatetext);    		
    			break;    		
    		}
    	}

    };
    
}


