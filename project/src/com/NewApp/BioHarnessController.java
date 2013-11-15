package com.NewApp;


enum DistortionType {
    WhiteNoise,
    Layering
}

public class BioHarnessController {
	
	//instance variables
	float RespirationRate = 0;
	
	//Constructors
	BioHarnessController(){ }
	
	
	public void setRespirationRate(float newRespirationRate){
		
		RespirationRate = newRespirationRate;
	}
	
	
	public float getRespirationRate(){
		return RespirationRate;
	}
	
	
}
