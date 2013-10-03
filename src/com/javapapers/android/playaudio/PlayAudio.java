package com.javapapers.android.playaudio;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.media.SoundPool;
import android.media.AudioManager;


public class PlayAudio extends Service{
	
	private static final String LOGCAT = null;
	int sound1, sound2;
	SoundPool soundPool;
	
	

	public void onCreate(){
		
	    super.onCreate();
	    Log.d(LOGCAT, "Service Started!");
	    
	    soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
	    sound1 = soundPool.load(getBaseContext(), R.raw.snd_stage_clear, 1);
	    sound2 = soundPool.load(getBaseContext(), R.raw.snd_game_over, 1);
	}
	

	public int onStartCommand(Intent intent, int flags, int startId){

		soundPool.play(sound1, 1, 1, 1, 0, 1);
		soundPool.play(sound2, 1, 1, 1, 0, 1);    
	    return 1;
	}

	
	public void onStop(){
		
	}
	
	
	public void onPause(){
	}
	
	
	public void onDestroy(){
		soundPool.release();
	}

	
	@Override
	public IBinder onBind(Intent objIndent) {
	    return null;
	}
}
