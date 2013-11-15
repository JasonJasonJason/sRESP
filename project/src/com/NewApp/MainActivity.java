package com.NewApp;


import android.app.Activity;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.text.Editable;
import android.text.TextWatcher;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import zephyr.android.BioHarnessBT.*;


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
	static int channelsReady = 0;
	BioHarnessController bhController;
	boolean started = false;



    ListView musiclist;
    Cursor musiccursor;
    int music_column_index;
    int count;
    MediaPlayer mMediaPlayer;
    private int currentSongNumber;


    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        Log.d("Application", "onCreate");   
        
        setContentView(R.layout.bh_connection);
        
        //create connect button and connect
        final Button btnConnect = (Button) findViewById(R.id.ButtonConnect);
        if (btnConnect != null)
        {
        		btnConnect.setOnClickListener(new OnClickListener() 
        		{
        			public void onClick(View v)
        			{
        				btnConnect.setPressed(true);
        				TextView tv = (TextView) findViewById(R.id.labelStatusMsg);
    					tv.setText("Connecting...");

                        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                            initMainScreen();
                        }
                        else{
                            initBioHarnessConnection();
                        }
        			}
        		});
        	}
	        //Sending a message to android that we are going to initiate a pairing request
	        IntentFilter filter = new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST");
	        //Registering a new BTBroadcast receiver from the Main Activity context with pairing request event
	        this.getApplicationContext().registerReceiver(new BTBroadcastReceiver(), filter);
	        //Registering the BTBondReceiver in the application that the status of the receiver has changed to Paired
	        IntentFilter filter2 = new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED");
	        this.getApplicationContext().registerReceiver(new BTBondReceiver(), filter2);
	        
	       	//Obtaining the handle to act on the CONNECT button
	        TextView tv = (TextView) findViewById(R.id.labelStatusMsg);
			String ErrorText  = "Ready to connect";
			tv.setText(ErrorText);
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
    
    @Override
    public void onResume(){
    		super.onResume();
    		
    		if(!started)
	    		return;
    		Log.d("Application", "onResume called");
    		
    		if(distortionType == DistortionType.Layering)
    			layeringResume();
    		else if (distortionType == DistortionType.WhiteNoise)
    			noiseResume();
    }


    private void initMainScreen(){

        setContentView(R.layout.main_menu);
        
        EditText textBox = (EditText)findViewById(R.id.labelRespRate);
        textBox.setGravity(Gravity.CENTER);
        init_phone_music_grid();
        
        Thread t = new Thread() {
            public void run() {
                //spinnerTrack = (Spinner)findViewById(R.id.spinner1);
                layeringInit();
                noiseInit();

                initManualInputBox();

                //Initializing BioHarnessController
                bhController = new BioHarnessController();

                started = true;


            }
        };
        t.start();

    }



    private void init_phone_music_grid() {
        System.gc();
        String[] proj = { MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE };
        musiccursor = managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                proj, null, null, null);
        count = musiccursor.getCount();
        musiclist = (ListView) findViewById(R.id.PhoneMusicList);
        musiclist.setAdapter(new MusicAdapter(getApplicationContext()));

        musiclist.setOnItemClickListener(musicgridlistener);
        mMediaPlayer = new MediaPlayer();
    }

    private AdapterView.OnItemClickListener musicgridlistener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position,
                                long id) {
            playSong(position);
        }
    };

    public class MusicAdapter extends BaseAdapter {
        private Context mContext;

        public MusicAdapter(Context c) {
            mContext = c;
        }

        public int getCount() {
            return count;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            System.gc();
            TextView tv = new TextView(mContext.getApplicationContext());
            String id = null;
            if (convertView == null) {
                music_column_index = musiccursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                musiccursor.moveToPosition(position);
                id = musiccursor.getString(music_column_index);
                music_column_index = musiccursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
                musiccursor.moveToPosition(position);
                id += " Size(KB):" + musiccursor.getString(music_column_index);
                tv.setText(id);
            } else
                tv = (TextView) convertView;
            return tv;
        }
    }

    private void playSong(int songNumber)
    {
        System.gc();
        music_column_index = musiccursor
                .getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        musiccursor.moveToPosition(songNumber);
        currentSongNumber = songNumber;
        String filename = musiccursor.getString(music_column_index);

        try {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.reset();
            }
            mMediaPlayer.setDataSource(filename);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    playSong(currentSongNumber+1);
                }
            });
        } catch (Exception e) {
        }
    }




    private void initBioHarnessConnection(){

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
            tv.setText("Connected to BioHarness "+DeviceName);
            initMainScreen();
        }
        else
        {
            TextView tv = (TextView) findViewById(R.id.labelStatusMsg);
            tv.setText("Unable to connect!");
        }
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
    
    private void layeringResume(){
    		adjustAudio();
    }
    
    private void noiseResume(){
    		adjustAudio();
    }
    
    
    public void playpause(View view)
    {
    		ToggleButton btn = (ToggleButton)findViewById(R.id.toggleButton1);
    		if(btn.isChecked())
    		{
    			if(distortionType == DistortionType.Layering)
    				layeringPause();
    			else
    				noisePause();
    		}
    		else
    		{
    			adjustAudio();
    		}
    }
    
    
    private void initManualInputBox()
    {
    	EditText textMessage = (EditText)findViewById(R.id.labelRespRate);
        textMessage.addTextChangedListener(new TextWatcher(){
        		@Override
            public void afterTextChanged(Editable s) {
        			try{
        				Log.d("Respiration Rate", "Respiration Rate: " + s.toString());
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
    
    
    private void layeringInit(){
	
		mediaPlayers = new MediaPlayer[12];
		mediaPlayers[0] = initMediaPlayer(R.raw.kick_1);
		mediaPlayers[1] = initMediaPlayer(R.raw.snare_2);
		mediaPlayers[2] = initMediaPlayer(R.raw.overheads_3);
		mediaPlayers[3] = initMediaPlayer(R.raw.tom_4);
		mediaPlayers[4] = initMediaPlayer(R.raw.tom_5);
		mediaPlayers[5] = initMediaPlayer(R.raw.bass_6);
		mediaPlayers[6] = initMediaPlayer(R.raw.cello_11);
		mediaPlayers[7] = initMediaPlayer(R.raw.cello_12);
		mediaPlayers[8] = initMediaPlayer(R.raw.mandolin_9);
		mediaPlayers[9] = initMediaPlayer(R.raw.mandolin_10);
		mediaPlayers[10] = initMediaPlayer(R.raw.guitar_7);
		mediaPlayers[11] = initMediaPlayer(R.raw.guitar_8);
		
		replaySongs(); 
		
		mediaPlayers[0].setOnCompletionListener(new OnCompletionListener(){
			@Override
			public void onCompletion(MediaPlayer arg0) { 
				replaySongs();
			}
		});
		
		playChannels( channelArrayForRelaxLevel(4) );
    }
    
    private void noiseInit(){
    		noiseSound = initMediaPlayer(R.raw.all);
    		noise = initMediaPlayer(R.raw.white_noise);
    		
    		noiseSound.setLooping(true);
    		noise.setLooping(true);
    }
    
    private void startSongs()
    {
    		for(int i=0; i<mediaPlayers.length; i++)
		{
    			mediaPlayers[i].start();
		}
    }
    
    private void replaySongs()
    {
    		for(int i=0; i<mediaPlayers.length; i++)
		{
    			mediaPlayers[i].pause();
		}
		
    		for(int i=0; i<mediaPlayers.length; i++)
    		{
    			mediaPlayers[i].seekTo(0);
    		}
    		startSongs();
    		
	    	Log.d("Audio", "Repeating layered audio");
    }
    
    private MediaPlayer initMediaPlayer(int songId)
    {
    		MediaPlayer mediaPlayer = MediaPlayer.create(getBaseContext(), songId);

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
			case 1:	playChannels( channelArrayForRelaxLevel(0) ); break;
			case 2:	playChannels( channelArrayForRelaxLevel(1) ); break;
			case 3:	playChannels( channelArrayForRelaxLevel(3) ); break; 
			case 4:	playChannels( channelArrayForRelaxLevel(5) ); break;
			case 5:
			case 6:
			case 7: 	playChannels( channelArrayForRelaxLevel(6) ); break;
			case 8:	playChannels( channelArrayForRelaxLevel(5) ); break;
			case 9:	
			case 10:playChannels( channelArrayForRelaxLevel(4) ); break;
			case 11:playChannels( channelArrayForRelaxLevel(3) ); break;
			case 12:playChannels( channelArrayForRelaxLevel(2) ); break;
			case 13:
			case 14:playChannels( channelArrayForRelaxLevel(1) ); break;
			case 15:
			case 16:playChannels( channelArrayForRelaxLevel(0) ); break;
			default:playChannels( channelArrayForRelaxLevel(0) ); break;
		}
    }
    
    private int[] channelArrayForRelaxLevel(int level)
    {
    		switch(level)
    		{
    			case 0: return new int[] {0,1};
    			case 1: return new int[] {0,1,2,3};
    			case 2: return new int[] {0,1,2,3,4,5};
    			case 3: return new int[] {0,1,2,3,4,5,6};
    			case 4: return new int[] {0,1,2,3,4,5,6,7};
    			case 5: return new int[] {0,1,2,3,4,5,6,7,8,9};
    			case 6: return new int[] {0,1,2,3,4,5,6,7,8,9,10,11};
    		}
    		return new int[] {};
    }
    
    private void adjustNoiseAudio(int respirationRate){
    	
    		noiseSound.setVolume(1, 1);
    		switch(respirationRate)
		{
			case 0:
			case 1: setVolume(0.16f); break;
			case 2:	setVolume(0.12f); break;
			case 3: setVolume(0.08f); break;
			case 4: setVolume(0.04f); break;
			case 5:
			case 6:
			case 7: setVolume(0f); break;
			case 8: setVolume(0.02f); break;
			case 9: setVolume(0.04f); break;
			case 10: setVolume(0.06f); break;
			case 11: setVolume(0.08f); break;
			case 12: setVolume(0.11f); break;
			case 13: setVolume(0.14f); break;
			case 14: setVolume(0.17f);break;
            case 15: setVolume(0.20f);break;
			default: setVolume(0.23f); break;
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


