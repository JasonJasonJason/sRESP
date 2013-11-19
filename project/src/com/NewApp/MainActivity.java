package com.NewApp;


import android.app.Activity;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.ViewGroup;
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


public class MainActivity extends Activity{
    /** Called when the activity is first created. */
	BluetoothAdapter adapter = null;
	BTClient _bt; 
	ZephyrProtocol _protocol;
	NewConnectedListener _NConnListener;
	private final int RESPIRATION_RATE = 0x101;
	DistortionType distortionType = DistortionType.WhiteNoise;
	MediaPlayer[] mediaPlayers;
    boolean[] currentlyPlaying;
	MediaPlayer noise;
	BioHarnessController bhController;
	boolean onMainScreen = false;

    ListView musiclist;
    Cursor musiccursor;
    int music_column_index;
    int count;
    MediaPlayer mMediaPlayer;
    private int currentSongNumber;

    private boolean goToGraphFlag;
    private MediaObserver mediaObserver;


    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        Log.d("Application", "onCreate");

        //Sending a message to android that we are going to initiate a pairing request
        IntentFilter filter = new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST");
        //Registering a new BTBroadcast receiver from the Main Activity context with pairing request event
        this.getApplicationContext().registerReceiver(new BTBroadcastReceiver(), filter);
        //Registering the BTBondReceiver in the application that the status of the receiver has changed to Paired
        IntentFilter filter2 = new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED");
        this.getApplicationContext().registerReceiver(new BTBondReceiver(), filter2);
        
        goToConnectScreen();

        //Obtaining the handle to act on the CONNECT button
        TextView tv = (TextView) findViewById(R.id.labelStatusMsg);
        String ErrorText  = "Ready to connect";
        tv.setText(ErrorText);
	}
    
    @Override
    public void onPause(){ 
        super.onPause();

        if(goToGraphFlag)
        {
            goToGraphFlag = false;
            return;
        }

        Log.d("Application", "onPause called");

        if(distortionType == DistortionType.Layering)
            layeringPause(false);
        else if (distortionType == DistortionType.WhiteNoise)
            noisePause();
    }
    
    @Override
    public void onResume(){
        super.onResume();

        if(!onMainScreen)
            return;
        Log.d("Application", "onResume called");

        if(distortionType == DistortionType.Layering)
            layeringResume();
        else if (distortionType == DistortionType.WhiteNoise)
            noiseResume();
    }

    @Override
    public void onBackPressed() {

        if(onMainScreen)
            goToConnectScreen();
        else{
            super.onBackPressed();
        }
    }

    private void goToConnectScreen(){

        if(onMainScreen && distortionType == DistortionType.WhiteNoise)
            noisePause();
        if(onMainScreen && distortionType == DistortionType.Layering)
            layeringPause(true);

        onMainScreen = false;
        setContentView(R.layout.bh_connection);

        //create connect button and connect
        final Button btnConnect = (Button) findViewById(R.id.LayeringConnect);
        if (btnConnect != null)
        {
            btnConnect.setOnClickListener(new buttonListener(DistortionType.Layering, btnConnect));
        }
        final Button btnNoise = (Button) findViewById(R.id.NoiseConnect);
        if (btnNoise != null)
        {
            btnNoise.setOnClickListener(new buttonListener(DistortionType.WhiteNoise, btnNoise));
        }
    }


    class buttonListener implements OnClickListener {

        public DistortionType newDistortionType;
        public Button btn;

        public buttonListener(DistortionType constructDistortion, Button btnConnect) {
            newDistortionType = constructDistortion;
            btn = btnConnect;
        }

        public void onClick(View v)
        {
            btn.setPressed(true);
            TextView tv = (TextView) findViewById(R.id.labelStatusMsg);
            tv.setText("Connecting...");

            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                initMainScreen(newDistortionType);
            }
            else{
                initBioHarnessConnection(newDistortionType);
            }
        }
    }


    private void initMainScreen(DistortionType newDistortionType){

        distortionType = newDistortionType;

        if(distortionType == DistortionType.WhiteNoise)
            initNoiseMainScreen();
        if(distortionType == DistortionType.Layering)
            initLayeringMainScreen();
    }

    private void initNoiseMainScreen()
    {
        setContentView(R.layout.main_menu_layering);

        EditText textBox = (EditText)findViewById(R.id.labelRespRate);
        textBox.setGravity(Gravity.CENTER);
        mMediaPlayer = new MediaPlayer();
        initMediaController();
        init_phone_music_grid();

        Thread t = new Thread() {
            public void run() {
                noiseInit();

                initManualInputBox();

                //Initializing BioHarnessController
                bhController = new BioHarnessController();

                onMainScreen = true;
            }
        };
        t.start();
    }

    private void initLayeringMainScreen()
    {
        setContentView(R.layout.main_menu_noise);

        EditText textBox = (EditText)findViewById(R.id.labelRespRate);
        textBox.setGravity(Gravity.CENTER);

        findViewById(R.id.progressBar).setVisibility(View.GONE);
        findViewById(R.id.buttonPrevious).setVisibility(View.GONE);
        findViewById(R.id.buttonNext).setVisibility(View.GONE);

        Thread t = new Thread() {
            public void run() {

                layeringInit();

                initManualInputBox();


                ((Button) findViewById(R.id.buttonPlayPause)).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Button playPauseButton = (Button) findViewById(R.id.buttonPlayPause);

                        boolean anyPlaying = false;
                        for(int i=0; i<currentlyPlaying.length; i++)
                            if(currentlyPlaying[i])
                                anyPlaying = true;

                        if(anyPlaying)
                        {
                            playPauseButton.setText("Play");
                            layeringPause(true);
                        }
                        else
                        {
                            playPauseButton.setText("Pause");
                            layeringResume();
                        }


                    }
                });

                //Initializing BioHarnessController
                bhController = new BioHarnessController();

                onMainScreen = true;
            }
        };
        t.start();
    }



    private void init_phone_music_grid() {
        System.gc();
        String[] proj = { MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST};
        musiccursor = managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                proj, null, null, null);
        count = musiccursor.getCount();
        musiclist = (ListView) findViewById(R.id.PhoneMusicList);
        //ArrayAdapter musicAdapter = new ArrayAdapter(getApplicationContext(), R.layout.line_item);
        //musiclist.setAdapter(musicAdapter);
        musiclist.setAdapter(new MusicAdapter(getApplicationContext()));
        musiclist.setOnItemClickListener(musicgridlistener);
        mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Log.d("Playing song...", "Playing song again");
                playSong(currentSongNumber+1);
            }
        });
    }

    private ListView.OnItemClickListener musicgridlistener = new ListView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
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

            LinearLayout layoutShell = (LinearLayout)getLayoutInflater().inflate(R.layout.line_item, null);
            TextView titleTextView = (TextView)layoutShell.findViewById(R.id.SongTitle);
            TextView artistTextView = (TextView)layoutShell.findViewById(R.id.Artist);

            if (convertView == null) {
                musiccursor.moveToPosition(position);
                String titleString = " " + musiccursor.getString(musiccursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String artistString = "    " + musiccursor.getString(musiccursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                titleTextView.setText(titleString);
                artistTextView.setText(artistString);
            } else {
                return (LinearLayout) convertView;
            }
            return layoutShell;
        }
    }

    private void playSong(int songNumber)
    {
        System.gc();
        currentSongNumber = songNumber;
        if(currentSongNumber >= count)
            currentSongNumber = 0;
        if(currentSongNumber < 0)
            currentSongNumber = count-1;

        music_column_index = musiccursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        musiccursor.moveToPosition(currentSongNumber);
        String filename = musiccursor.getString(music_column_index);

        try {
            if(mMediaPlayer.isPlaying())
                mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(filename);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            noise.start();
        } catch (Exception e) {
        }
    }

    private void initMediaController(){

        final Button playPauseButton = (Button)findViewById(R.id.buttonPlayPause);
        Button previousButton = (Button)findViewById(R.id.buttonPrevious);
        Button nextButton = (Button)findViewById(R.id.buttonNext);
        SeekBar progressBar = (SeekBar) findViewById(R.id.progressBar);

        playPauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(distortionType == DistortionType.WhiteNoise)
                {
                    if(noise.isPlaying())
                    {
                        noisePause();
                        playPauseButton.setText("Play");
                    }
                    else
                    {
                        noiseResume();
                        playPauseButton.setText("Pause");
                    }
                }
            }
        });
        nextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                playSong(currentSongNumber+1);
            }
        });
        previousButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                playSong(currentSongNumber-1);
            }
        });

        mediaObserver = new MediaObserver(progressBar);
        new Thread(mediaObserver).start();
    }

    private class MediaObserver implements Runnable {
        private boolean stop = false;
        SeekBar progressBar;

        public MediaObserver(SeekBar newProgressBar) {
            progressBar = newProgressBar;
        }

        public void stop() {
            stop = true;
        }

        @Override
        public void run() {

            while (!stop) {
                if(progressBar != null && mMediaPlayer != null && mMediaPlayer.isPlaying()){
                    float progress = (float)mMediaPlayer.getCurrentPosition() / (float)mMediaPlayer.getDuration() * 100;
                    progressBar.setProgress((int)progress);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e("Error", "Error in progress bar updating");
                }
            }
        }
    }


    private void initBioHarnessConnection(DistortionType newDistortionType){

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
        BluetoothDevice Device = adapter.getRemoteDevice(BhMacID);
        String DeviceName = Device.getName();
        _bt = new BTClient(adapter, BhMacID);
        _NConnListener = new NewConnectedListener(Newhandler,Newhandler);
        _bt.addConnectedEventListener(_NConnListener);

        //TextView tv1 = (EditText)findViewById(R.id.labelRespRate);
        //tv1.setText("0.0");

        if(_bt.IsConnected())
        {
            Log.d("Bluetooth", "Blootooth is connected. Starting blootooth.");
            _bt.start();

            TextView tv = (TextView) findViewById(R.id.labelStatusMsg);
            tv.setText("Connected to BioHarness "+DeviceName);
            initMainScreen(newDistortionType);
        }
        else
        {
            Log.d("Bluetooth", "Could not connect!");
            TextView tv = (TextView) findViewById(R.id.labelStatusMsg);
            tv.setText("Unable to connect!");
        }
    }
    
    
    private void layeringPause(boolean immediate){
    		
        for(int i=0; i<mediaPlayers.length; i++)
        {
            if(currentlyPlaying[i])
                stopChannel(i, immediate);
        }

        if(mediaObserver != null)
            mediaObserver.stop();
    }
    
    private void noisePause(){
    		
        noise.pause();
        mMediaPlayer.pause();
    }
    
    private void layeringResume(){

        adjustAudio();
        mediaObserver = new MediaObserver((SeekBar)findViewById(R.id.progressBar));
        new Thread(mediaObserver).start();
    }

    private void noiseResume(){
        adjustAudio();
        noise.start();
        mMediaPlayer.start();
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
        goToGraphFlag = true;
        Intent intent = new Intent(this, GraphActivity.class);
        startActivity(intent);
    }
    
    
    private void layeringInit(){

		mediaPlayers = new MediaPlayer[6];
		mediaPlayers[0] = initMediaPlayer(R.raw.track1);
		mediaPlayers[1] = initMediaPlayer(R.raw.track2);
		mediaPlayers[2] = initMediaPlayer(R.raw.track3);
		mediaPlayers[3] = initMediaPlayer(R.raw.track4);
		mediaPlayers[4] = initMediaPlayer(R.raw.track5);
		mediaPlayers[5] = initMediaPlayer(R.raw.track6);

        currentlyPlaying = new boolean[mediaPlayers.length];
        for(int i=0; i<currentlyPlaying.length; i++)
            currentlyPlaying[i] = false;

        for(int i=0; i<mediaPlayers.length; i++)
        {
            mediaPlayers[i].seekTo(0);
        }
        startSongs();

		//playChannels( channelArrayForRelaxLevel(6) );
    }
    
    private void noiseInit(){
        noise = initMediaPlayer(R.raw.white_noise);
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
    
    private void startChannel(int channel, boolean immediate)
    {
        Log.d("Audio", "Starting audio channel #" + channel);
        fadeChannel(1, mediaPlayers[channel], immediate);
        currentlyPlaying[channel] = true;
    }
    
    
    private void stopChannel(int channel, boolean immediate)
    {
        Log.d("Audio", "Stopping audio channel #" + channel);
        fadeChannel(0, mediaPlayers[channel], immediate);
        currentlyPlaying[channel] = false;
    }
    
    
    private void playChannels(int[] channels){
    	
        for(int channel=0; channel<mediaPlayers.length; channel++)
        {
            if(arrayContains(channels, channel))
            {
                if(!currentlyPlaying[channel])
                    startChannel(channel, false);
            }
            else
            {
                if(currentlyPlaying[channel])
                    stopChannel(channel, false);
                currentlyPlaying[channel] = false;
            }
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
        Log.d("Audio", "Adjusting audio");

        int respirationRate = (int) Math.floor(bhController.getRespirationRate());
        if(distortionType == DistortionType.Layering)
            adjustLayeringAudio(respirationRate);
        else if (distortionType == DistortionType.WhiteNoise)
            adjustNoiseAudio(respirationRate);
    }
    
    private void adjustLayeringAudio(int respirationRate){

        Log.d("Audio", "Adjusting layered aduio");
        switch(respirationRate)
		{
			case 0:
			case 1:	playChannels( channelArrayForRelaxLevel(0) ); break;
			case 2:	playChannels( channelArrayForRelaxLevel(1) ); break;
			case 3:	playChannels( channelArrayForRelaxLevel(3) ); break; 
			case 4:	playChannels( channelArrayForRelaxLevel(5) ); break;
			case 5:
			case 6:
			case 7: playChannels( channelArrayForRelaxLevel(6) ); break;
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
            case 0: return new int[] {0};
            case 1: return new int[] {1};
            case 2: return new int[] {2};
            case 3: return new int[] {3};
            case 4: return new int[] {4};
            case 5: return new int[] {5};
            case 6: return new int[] {5};
        }
        return new int[] {};
    }
    
    private void adjustNoiseAudio(int respirationRate){

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

    public void fadeChannel(float volume, MediaPlayer mp, boolean immediate){

        if(immediate){
            mp.setVolume(volume, volume);
        }
        else{
            FadeOutMusic fadeOut = (FadeOutMusic)new FadeOutMusic(volume, mp).execute( );
        }
    }

    //
    //  background processing ... fade out music stream
    //
    public class FadeOutMusic extends AsyncTask<String,Integer,String> {

        private float targetVolume;
        private MediaPlayer mediaPlayer;

        public FadeOutMusic(float v, MediaPlayer mp) {
            targetVolume = v;
            mediaPlayer = mp;
        }

        @Override
        protected String doInBackground(String... args) {

            float currentVolume = targetVolume == 1 ? 0 : 1;
            float stepAmount = targetVolume == 1 ? 0.02f : -0.02f;

            for(int i=0; i<50; i++){

                currentVolume += stepAmount;

                if(mediaPlayer != null){
                    mediaPlayer.setVolume(currentVolume, currentVolume);
                }
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return "dummy";
        }

        @Override
        protected void onPostExecute( String dummy ) {
            if(mediaPlayer != null){
                mediaPlayer.setVolume(targetVolume, targetVolume);
            }
        }

        @Override
        public void onCancelled() {
            if(mediaPlayer != null){
                mediaPlayer.setVolume(targetVolume, targetVolume);
            }
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


