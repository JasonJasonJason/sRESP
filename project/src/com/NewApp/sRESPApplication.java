package com.NewApp;

import android.app.Application;

public class sRESPApplication extends Application{
	
	float currentRespirationRate = 20.0f;
	
	public void setRespirationRate(float newRespirationRate)
	{
		currentRespirationRate = newRespirationRate;
	}
	
	public float getRespirationRate()
	{
		return currentRespirationRate;
	}

}