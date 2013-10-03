package com.javapapers.android.playaudio;

import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.media.SoundPool.OnLoadCompleteListener;

public class MainActivity extends Activity {

	SoundPool soundPool;
	boolean[] songsPlaying;
	int[] streamIds;
	int[] soundIds;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    
    private void init(){
    	
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
}
