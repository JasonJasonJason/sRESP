package com.javapapers.android.playaudio;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.content.Intent;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void playAudio(View view) {
	    Intent objIntent = new Intent(this, PlayAudio.class);
	    startService(objIntent);
    }
    
    public void stopAudio(View view) {
    	Intent objIntent = new Intent(this, PlayAudio.class);
	    stopService(objIntent);    
    }
}
