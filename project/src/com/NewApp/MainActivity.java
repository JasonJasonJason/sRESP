package com.NewApp;

import android.app.Activity;
import android.widget.Spinner;
import android.media.SoundPool;
import android.media.AudioManager;
import android.media.SoundPool.OnLoadCompleteListener;
import android.media.MediaPlayer;
import android.os.Bundle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
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


enum DistortionType {
	WhiteNoise,
	Layering
}


public class MainActivity extends Activity {
    /** Called when the activity is first created. */
	BluetoothAdapter adapter = null;
	BTClient _bt;
	ZephyrProtocol _protocol;
	NewConnectedListener _NConnListener;
	private final int RESPIRATION_RATE = 0x101;
	DistortionType distortionType = DistortionType.Layering;
	MediaPlayer[] mediaPlayers;
	MediaPlayer noiseSound;
	MediaPlayer noise;
	public Spinner spinnerTrack;

	BioHarnessController bhController;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Application", "onCreate");   
        
        setContentView(R.layout.main_menu);
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
				public void onClick(View v) { 
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
        layeringInit();
        noiseInit();
        
        //Initializing SeekBars
        initSeekBars();
        
        //Initializing BioHarnessController
        bhController = new BioHarnessController();
        
        initManualInputBox(); 
    }
    
    @Override
    public void onPause(){
    		super.onPause();
    		Log.d("Application", "onPause called");
    		
    		if(distortionType == DistortionType.Layering)
    			layeringPause();
    		else if (distortionType == DistortionType.WhiteNoise)
    			noisePause();
    }
    
    public void onRadioButtonClicked(View view) {
	    // Is the button now checked?
	    boolean checked = ((RadioButton) view).isChecked();
	    
	    // Check which radio button was clicked
	    switch(view.getId()) {
	    		case R.id.radio_layering:
	            if (checked)
	            {
	            		distortionType = DistortionType.Layering;
	            		noisePause();
	            		adjustAudio();
	            }
	            break;
	        case R.id.radio_noise:
	            if (checked)
	            {
	            		distortionType = DistortionType.WhiteNoise;
	            		layeringPause();
	            		adjustAudio();
	            }
	            break;
	    }
	}
    
    
    private void layeringPause(){
    		
    		for(int i=0; i<mediaPlayers.length; i++)
    		{
    			stopChannel(i);
    		}
    }
    
    private void noisePause(){
    		
    		noise.setVolume(0, 0);
    		noiseSound.setVolume(0, 0);
    }
    
    
    private void initManualInputBox()
    {
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
    
    
    public void goToGraph(View view)
    {
    	Intent intent = new Intent(this, GraphActivity.class);
        startActivity(intent);
    }
    
    private void initSeekBars(){
    	
    		SeekBar[] seekBars = new SeekBar[1];
    		seekBars[0] = ((SeekBar) findViewById(R.id.seekBar1));
    		
    		initSeekBar(seekBars[0], 0);
    }
    
    private void initSeekBar(SeekBar seekBar, final int index)
    {
    			seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar arg0, int seekPercentage, boolean arg2) {
            		//float volume = (float)seekPercentage/100;
            		//soundPool.setVolume(soundIds[index], volume, volume);
            }
			@Override public void onStartTrackingTouch(SeekBar seekBar) {	}
			@Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    private void layeringInit(){
	
		mediaPlayers = new MediaPlayer[4];
		mediaPlayers[0] = initMediaPlayer(R.raw.bass);
		mediaPlayers[1] = initMediaPlayer(R.raw.drums);
		mediaPlayers[2] = initMediaPlayer(R.raw.drums_layer2);
		mediaPlayers[3] = initMediaPlayer(R.raw.plucks);
		
		playChannels(new int[] {0,1,2});
    }
    
    private void noiseInit(){
    		noiseSound = initMediaPlayer(R.raw.noise_track);
    		noise = initMediaPlayer(R.raw.white_noise);
    }
    
    private MediaPlayer initMediaPlayer(int songId)
    {
    		MediaPlayer mediaPlayer = MediaPlayer.create(getBaseContext(), songId);
		mediaPlayer.setLooping(true);
		mediaPlayer.setVolume(0,0);
		mediaPlayer.start();
		
		return mediaPlayer;
    }
    
    private void startChannel(int channel)
    {
    		Log.d("Audio", "Starting audio channel #" + channel);
    		mediaPlayers[channel].setVolume(1,1);
    }
    
    
    private void stopChannel(int channel)
    {
    		Log.d("Audio", "Stopping audio channel #" + channel);
    		mediaPlayers[channel].setVolume(0,0);
    }
    
    
    private void playChannels(int[] channels){
    	
    		for(int channel=0; channel<mediaPlayers.length; channel++)
    		{
    			if(arrayContains(channels, channel))
    				startChannel(channel);
    			else
    				stopChannel(channel);
    		}
    }
    
    private boolean arrayContains(int[] arr, int item)
    {
    		for(int i=0; i<arr.length; i++)
    			if(arr[i] == item)
    				return true;
    		return false;
    }
    
    private void setVolume(float d)
    {
    		noise.setVolume(d, d);
    }
    
    
    private void adjustAudio()
    {
    		int respirationRate = (int) Math.floor(bhController.getRespirationRate());
    		if(distortionType == DistortionType.Layering)
    			adjustLayeringAudio(respirationRate);
    		else if (distortionType == DistortionType.WhiteNoise)
    			adjustNoiseAudio(respirationRate);
    }
    
    private void adjustLayeringAudio(int respirationRate){
    	
    		switch(respirationRate)
		{
			case 0:
			case 1:	playChannels( new int[] {0} ); break;
			case 2:
			case 3: playChannels( new int[] {0,1}); break;
			case 4:  playChannels( new int[] {0,1,2}); break;
			case 5:
			case 6:
			case 7: playChannels( new int[] {0,1,2,3}); break;
			case 8:
			case 9: playChannels( new int[] {0,1,2}); break;
			case 10:
			case 11: playChannels( new int[] {0,2}); break;
			case 12:
			case 13:
			case 14:
			case 15:
			default:playChannels( new int[] {0}); break;
		}
    }
    
    private void adjustNoiseAudio(int respirationRate){
    	
    		noiseSound.setVolume(1, 1);
    		switch(respirationRate)
		{
			case 0:
			case 1: setVolume(0.22f); break;
			case 2:	setVolume(0.16f); break;
			case 3: setVolume(0.1f); break;
			case 4: setVolume(0.04f); break;
			case 5:
			case 6:
			case 7: setVolume(0f); break;
			case 8: setVolume(0.04f); break;
			case 9: setVolume(0.08f); break;
			case 10: setVolume(0.12f); break;
			case 11: setVolume(0.16f); break;
			case 12: setVolume(0.2f); break;
			case 13: setVolume(0.24f); break;
			case 14: setVolume(0.28f);break;
			case 15:
			default: setVolume(0.3f); break;
		}
    }
    
    
    private void handleNewRespirationRate(float respirationRate)
    {
    		Log.d("Respiration Rate", "Respiration Rate: " + respirationRate);
        ((sRESPApplication) getApplication()).setRespirationRate(respirationRate);
	
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
	    			if(RespirationRatetext.length() > 0)
	    			{
		    			float respirationRate = Float.parseFloat(RespirationRatetext);
		    			handleNewRespirationRate(respirationRate);
	    			}
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


