/*
* Copyright (C) 2014-2015 Symbol Technologies LLC
* All rights reserved.
*/
package com.symbol.profiledatacapturesample1;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileConfig;
import com.symbol.emdk.ProfileManager;
import com.symbol.emdk.ProfileConfig.ENABLED_STATE;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

public class MainActivity extends Activity implements EMDKListener {

	//Assign the profile name used in EMDKConfig.xml
	private String profileName = "DataCaptureProfile-1";
	
	//Declare a variable to store ProfileManager object 
	private ProfileManager profileManager = null;
	
	//Declare a variable to store EMDKManager object 
	private EMDKManager emdkManager = null;
	
	//Declare a variable to store ProfileConfig object
	ProfileConfig profileConfigObj = null;
	
	 TextView statusTextView = null;	
	 CheckBox checkBoxCode128 = null;
	 CheckBox checkBoxCode39 = null;
	 CheckBox checkBoxEAN8 = null;
	 CheckBox checkBoxEAN13 = null;
	 CheckBox checkBoxUPCA = null;
	 CheckBox checkBoxUPCE0 = null;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		statusTextView = (TextView) findViewById(R.id.textViewStatus);	
		checkBoxCode128 = (CheckBox) findViewById(R.id.checkBoxCode128);
		checkBoxCode39 = (CheckBox) findViewById(R.id.checkBoxCode39);
		checkBoxEAN8 = (CheckBox) findViewById(R.id.checkBoxEAN8);
		checkBoxEAN13 = (CheckBox) findViewById(R.id.checkBoxEAN13);
		checkBoxUPCA = (CheckBox) findViewById(R.id.checkBoxUPCE);
		checkBoxUPCE0 = (CheckBox) findViewById(R.id.checkBoxUPCE0);		
		
		//Set listener to the button
		addSetButtonListener();
	    
		//The EMDKManager object will be created and returned in the callback.
		EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);

		//Check the return status of processProfile
		if(results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
			//EMDKManager object creation success
		}else {
			//EMDKManager object creation failed
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
	
		super.onDestroy();
		
		//Clean up the objects created by EMDK manager
		if (profileManager != null)
			profileManager = null;
		
		if (emdkManager != null) {
			emdkManager.release();
			emdkManager = null;
		}
	}
	
	@Override
	public void onClosed() {
		
		//This callback will be issued when the EMDK closes unexpectedly.
		if (emdkManager != null) {
			emdkManager.release();
			emdkManager = null;	
		}
		
		statusTextView.setText("Status: " + "EMDK closed unexpectedly! Please close and restart the application.");	
	}
	
	@Override
	public void onOpened(EMDKManager emdkManager) {
		
		//This callback will be issued when the EMDK is ready to use.
		statusTextView.setText("EMDK open success.");
		
		this.emdkManager = emdkManager;
		
	    //Get the ProfileManager object to process the profiles
        profileManager = (ProfileManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.PROFILE);
		
		String[] modifyData = new String[1];
		
		new ProcessProfileInitTask().execute(modifyData[0]);	
	} 
	
	
	private void addSetButtonListener()
	{
		Button buttonSet = (Button)findViewById(R.id.buttonSet);
		buttonSet.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				//Call modifyProfile_ProfileConfig() to modify existing profile using ProfileConfig class
				modifyProfile_ProfileConfig();
				
				//Call modifyProfile_XMLString() to modify existing profile using XML String. Also make sure to uncomment 
				//the implementation of the modifyProfile_XMLString() function and the ProcessProfileSetXMLTask class in the bottom.
				//modifyProfile_XMLString();
			}	
		});
	}
	
	private void modifyProfile_ProfileConfig()
	{
		statusTextView.setText("");

		//Create the ProfileConfig object
		profileConfigObj = new ProfileConfig();
	
		new ProcessProfileGetConfigTask().execute("");
	}
	
	//Uncomment the following function implementation if you are calling modifyProfile_XMLString() 
	//from the OnClickListener
/*	private void modifyProfile_XMLString()
	{
		statusTextView.setText("");
		
		//Prepare XML to modify the existing profile
		String[] modifyData = new String[1];
		modifyData[0]=
				"<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
				"<characteristic type=\"Profile\">" +
			        "<characteristic type=\"Barcode\" version=\"0.1\">" +
			            "<characteristic type=\"Decoders\">" +
			                "<parm name=\"decoder_code128\" value=\"false\"/>" +
			                "<parm name=\"decoder_code39\" value=\"true\"/>" +
			                "<parm name=\"decoder_ean8\" value=\"false\"/>" +
			                "<parm name=\"decoder_ean13\" value=\"true\"/>" +
			                "<parm name=\"decoder_upca\" value=\"false\"/>" +
			                "<parm name=\"decoder_upce0\" value=\"true\"/>" +
			            "</characteristic>" +
			        "</characteristic>"+						
				"</characteristic>";
	
		new ProcessProfileSetXMLTask().execute(modifyData[0]);
	}*/
	
	private class ProcessProfileInitTask extends AsyncTask<String, Void, EMDKResults> {
		
		@Override
		protected EMDKResults doInBackground(String... params) {	

			//Call processPrfoile with profile name and SET flag to create the profile. The params can be null.
			EMDKResults results = profileManager.processProfile(profileName, ProfileManager.PROFILE_FLAG.SET, params);

			return results;
		}

		@Override
		protected void onPostExecute(EMDKResults results) {
		
			super.onPostExecute(results);
			
			String resultString;
			
			//Check the return status of processProfile
			if(results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
				
				resultString = "Profile initialization success.";
				
			}else {
				
				resultString = "Profile initialization failed.";
			}
			
			statusTextView.setText(resultString);
		}
	}
	
	private class ProcessProfileGetConfigTask extends AsyncTask<String, Void, EMDKResults> {
	
		@Override
		protected EMDKResults doInBackground(String... params) {
			
			//Get the ProfileConfig from the profile XML
			EMDKResults results = profileManager.processProfile(profileName, ProfileManager.PROFILE_FLAG.GET, profileConfigObj);
			
			return results;
		}
		
		@Override
		protected void onPostExecute(EMDKResults results) {
		
			super.onPostExecute(results);

			//Check the return status of processProfile
			if(results.statusCode == EMDKResults.STATUS_CODE.FAILURE) {
			
				statusTextView.setText("Failed to get Profile.");
			}
			else {
				
				//Set the code128
				if (checkBoxCode128.isChecked() ) {
					
					profileConfigObj.dataCapture.barcode.decoders.code128 = ENABLED_STATE.TRUE;
					
				} else {
					
					profileConfigObj.dataCapture.barcode.decoders.code128 = ENABLED_STATE.FALSE;
				}
					
				//set code39
				if (checkBoxCode39.isChecked() ) {
					
					profileConfigObj.dataCapture.barcode.decoders.code39 = ENABLED_STATE.TRUE;
					
				} else {
					
					profileConfigObj.dataCapture.barcode.decoders.code39 = ENABLED_STATE.FALSE;
				}

				//set EAN8
				if (checkBoxEAN8.isChecked() ) {
					
					profileConfigObj.dataCapture.barcode.decoders.ean8 = ENABLED_STATE.TRUE;
					
				} else {
					
					profileConfigObj.dataCapture.barcode.decoders.ean8 = ENABLED_STATE.FALSE;
				}
				
				//set ENA13
				if (checkBoxEAN13.isChecked() ) {
					
					profileConfigObj.dataCapture.barcode.decoders.ean13 = ENABLED_STATE.TRUE;
					
				} else {
					
					profileConfigObj.dataCapture.barcode.decoders.ean13 = ENABLED_STATE.FALSE;
				}
				
				//set upca
				if (checkBoxUPCA.isChecked() ) {
					
					profileConfigObj.dataCapture.barcode.decoders.upca = ENABLED_STATE.TRUE;
					
				} else {
					
					profileConfigObj.dataCapture.barcode.decoders.upca = ENABLED_STATE.FALSE;
				}
				
				//set upce0
				if (checkBoxUPCE0.isChecked() ) {
					
					profileConfigObj.dataCapture.barcode.decoders.upce0 = ENABLED_STATE.TRUE;
					
				} else {
					
					profileConfigObj.dataCapture.barcode.decoders.upce0 = ENABLED_STATE.FALSE;
				}	
				
				new ProcessProfileSetConfigTask().execute("");
			}
		}
	}
	
	
	private class ProcessProfileSetConfigTask extends AsyncTask<String, Void, EMDKResults> {
		
		@Override
		protected EMDKResults doInBackground(String... params) {
			
			//Call processPrfoile with profile name, SET flag and config data to update the profile
			EMDKResults results = profileManager.processProfile(profileName, ProfileManager.PROFILE_FLAG.SET, profileConfigObj);
			
			return results;
		}

		@Override
		protected void onPostExecute(EMDKResults results) {
		
			super.onPostExecute(results);
		
			String resultString;
			
			//Check the return status of processProfile
			if(results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
				
				resultString = "Profile update success.";
				
			}else {
				
				resultString = "Profile update failed.";
			}
			
			statusTextView.setText(resultString);
		}
	}
	
	//Uncomment the following class if you are calling modifyProfile_XMLString() 
	//from the OnClickListener
/*	private class ProcessProfileSetXMLTask extends AsyncTask<String, Void, EMDKResults> {
		
		@Override
		protected EMDKResults doInBackground(String... params) {
			
			//Call processPrfoile with profile name, SET flag and config data to update the profile
			EMDKResults results = profileManager.processProfile(profileName, ProfileManager.PROFILE_FLAG.SET, params);
			
			return results;
		}

		@Override
		protected void onPostExecute(EMDKResults results) {
		
			super.onPostExecute(results);
		
			String resultString;
			
			//Check the return status of processProfile
			if(results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
				
				resultString = "Profile update success.";
				
			}else {
				
				resultString = "Profile update failed.";
			}
			
			statusTextView.setText(resultString);
		}
	}*/
}

