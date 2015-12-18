/*
* Copyright (C) 2015 Symbol Technologies LLC
* All rights reserved.
*/
package com.symbol.pd40fwupdatesample;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Scroller;
import android.widget.Toast;
/**
 * This class creates the profile XML and sends the request to
 * Paymanet Manager Service to query the version or to update
 * the firmware of payment device based on the user selection. 
 * The response of Payment Manager Service will be recived through
 * the registered Broadcast reciever, parsed and status will be displayed
 * on activity. 
 */
public class ProfileHandler extends Activity  {

	static String TAG = ProfileHandler.class.getSimpleName();
	
	Button okButton;
	Button cancelButton;
	Button fileBrowseButton;
	EditText deviceAddressEditTxt;
	EditText binLocationEdtTxt;
	CheckBox upgradeCheckBox;
	CheckBox queryVersionCheckBox;
	CheckBox queryBatteryLevelCheckBox;
	
	Toast toast;

	ProgressDialog mProgressDialog;
	
	//Payment Manager Service Component information to start the service 
	String PAYMENT_SERVICE_PACKAG_NAME = "com.symbol.paymentmgr";
	String PAYMENT_SERVICE_CLASS_NAME = "com.symbol.paymentmgr.PaymentMgrService";
	String PAYMENT_SERVICE_PROFILE_EX_DATA_NAME = "com.symbol.paymentmgr.PROFILE_XML";

	String PAYMENT_SERVICE_RESULT_INTENT_NAME = "com.symbol.paymentmgr.RESULT";
	String PAYMENT_SERVICE_RESULT_EX_DATA_NAME = "com.symbol.paymentmgr.RESPONSE";

	private static final String INTENT_PAYMNT_SERVICE_RESULT = "com.symbol.paymentmgr.RESULT";

	private static final int REQUEST_PAIRED_DEVICE = 2;
	Button getPairedDevicesButton;
	BluetoothAdapter bluetoothAdapter;
	ArrayAdapter<String> listdapter = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(getResources().getString(R.string.app_title));
		
		if (MainActivity.requestType.contains("Query")) {
			setContentView(R.layout.createprofile_query);
			queryVersionCheckBox = (CheckBox) findViewById(R.id.checkBox1);
			queryBatteryLevelCheckBox = (CheckBox) findViewById(R.id.checkBox2);
			getPairedDevicesButton = (Button) findViewById(R.id.listpaireddevices);
		} else {
			setContentView(R.layout.createprofile);
			binLocationEdtTxt = (EditText) findViewById(R.id.editText1);
			binLocationEdtTxt.setScroller(new Scroller(getApplicationContext()));
			binLocationEdtTxt.setMaxLines(2);
			binLocationEdtTxt.setVerticalScrollBarEnabled(true);

			upgradeCheckBox = (CheckBox) findViewById(R.id.checkBox1);
			upgradeCheckBox.setChecked(true);
			fileBrowseButton = (Button) findViewById(R.id.browse);
			getPairedDevicesButton = (Button) findViewById(R.id.pairedDevice);			
			addBrowseBtnListner();
		}
		
		addListPairedDevicesBtnListner();

		deviceAddressEditTxt = (EditText) findViewById(R.id.editText2);

		deviceAddressEditTxt.setScroller(new Scroller(getApplicationContext()));
		deviceAddressEditTxt.setMaxLines(2);
		deviceAddressEditTxt.setVerticalScrollBarEnabled(true);
		
		okButton = (Button) findViewById(R.id.ok);
		cancelButton = (Button) findViewById(R.id.cancel);

		addCancelButtonListener();
		addOkButtonListener();
		
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		checkBlueToothState();

		mProgressDialog = new ProgressDialog(this);

		mProgressDialog.setCancelable(false);
		mProgressDialog.setCanceledOnTouchOutside(false);

		//Register for Broadcast receiver to receive response from Payment Manager Service.
		IntentFilter filter = new IntentFilter();
		filter.addAction(INTENT_PAYMNT_SERVICE_RESULT);
		getApplicationContext().registerReceiver(broadcastReceiver, filter);
	}

	

	private void addListPairedDevicesBtnListner() {
		getPairedDevicesButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				getPairedDeviceList();
			}
		});
	}

	private void checkBlueToothState() {
		if (bluetoothAdapter != null) {
			
			if (bluetoothAdapter.isEnabled()) {
				getPairedDevicesButton.setEnabled(true);
			} else {
				Toast.makeText(getApplicationContext(),
						"Bluetooth is not Enabled", Toast.LENGTH_SHORT).show();
			}			
		}else {
			Toast.makeText(getApplicationContext(), "Bluetooth is not supported",
					Toast.LENGTH_SHORT).show();
		} 
	}

	protected void getPairedDeviceList() {

		Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

		List<String> pairedPmtDevices1 = new ArrayList<String>();

		for (BluetoothDevice device : pairedDevices) {

			if(device.getName().startsWith("MPOS-")) {
				pairedPmtDevices1.add(device.getName());					
			}				
		}
		if(!pairedPmtDevices1.isEmpty()){
			
			final String[] pairedPmtDevices = new String[pairedPmtDevices1.size()];
			
			int index = 0;
			for(String deviceAddress: pairedPmtDevices1) {
				pairedPmtDevices[index++] = deviceAddress;
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Paired Devices");
			
			ListView pairedDeviceList = new ListView(this);
			listdapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1, android.R.id.text1,
					pairedPmtDevices);
			pairedDeviceList.setAdapter(listdapter);

			builder.setItems(pairedPmtDevices,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							if (deviceAddressEditTxt.getText().toString().length() ==0) {
								deviceAddressEditTxt.append(pairedPmtDevices[which]);
							} else {
								deviceAddressEditTxt.append("|");
								deviceAddressEditTxt.append(pairedPmtDevices[which]);
								deviceAddressEditTxt.setScroller(new Scroller(getApplicationContext()));
								deviceAddressEditTxt.setMaxLines(3);
								deviceAddressEditTxt.setVerticalScrollBarEnabled(true);
							}
						}
					});

			final Dialog dialog = builder.create();
			dialog.show();
		}else {		
			Toast.makeText(getApplicationContext(), "Payment devices are not paired.",
				Toast.LENGTH_SHORT).show();
		}
	}

	private void addBrowseBtnListner() {

		fileBrowseButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent intent = new Intent(getApplicationContext(),
						FilePicker.class);
				startActivityForResult(intent, 1);

			}

		});

	}

	@Override
	protected void onActivityResult(int activityRefID, int status, Intent data) {

		if (status == RESULT_OK) {

			switch (activityRefID) {

			case 1:

				if (data.hasExtra(FilePicker.EXTRA_FILE_PATH)) {

					File selectedFile = new File(
							data.getStringExtra(FilePicker.EXTRA_FILE_PATH));

					binLocationEdtTxt.setScroller(new Scroller(getApplicationContext()));
					binLocationEdtTxt.setMaxLines(2);
					binLocationEdtTxt.setVerticalScrollBarEnabled(true);

					binLocationEdtTxt.getText().clear(); 
				
					binLocationEdtTxt.setFocusable(true);
					binLocationEdtTxt.setText(selectedFile.getPath());
					
					binLocationEdtTxt.setSelection(1);
				}
				
				break;
			case 2:
				checkBlueToothState();
				if (activityRefID == REQUEST_PAIRED_DEVICE) {
					
					break;
				}
			}
		}
	}
	


	private void addCancelButtonListener() {
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent returnIntent = new Intent();
				returnIntent.putExtra("RESULT", "Operation was canceled by the user.");
				ProfileHandler.this.setResult(RESULT_CANCELED, returnIntent);
				finish();

			}
		});

	}

	String profileXmlString = "";
	private void addOkButtonListener() {
	
		okButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (toast != null)
					toast.cancel();

				okButton.setEnabled(false);
				cancelButton.setEnabled(false);

				String errorMessage = "";
				
				//Create the Profile XML as per the Payment Manager Specification
				String deviceAddress = ((EditText) findViewById(R.id.editText2)).getText().toString();
				
				profileXmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ "<wap-provisioningdoc>"
						+ "<characteristic type=\"PaymentMgr\" version =\"0.1\">"
						+ "<parm name=\"DeviceAddress\" value=\"" + deviceAddress + "\"/>" ;
				
				if (MainActivity.requestType.contains("Query")) {
					
					//Create version query request XML
					if(!queryBatteryLevelCheckBox.isChecked() && !queryVersionCheckBox.isChecked()){
						errorMessage = "Please provide the input for query";
					} else {
						
						//Create PD40 battery Level query request
						if (queryBatteryLevelCheckBox.isChecked()) {
							profileXmlString += "<parm-query name=\"BatteryLevel\"/>";
						} 
						
						//Create PD40 FW version query request
						if (queryVersionCheckBox.isChecked()) {
							profileXmlString += "<parm-query name=\"Version\"/>" ;
						} 
					}					
				
				} else {
					
					//Create firmware update request XML					
					String filePath = ((EditText) findViewById(R.id.editText1)).getText().toString();
					
					if(filePath.length() != 0) {
						
						//Set download type as FIRMWARE
						profileXmlString += "<parm name=\"DownloadType\" value=\""	+ "FIRMWARE" + "\"/>";
						
						//Set the download file path
						profileXmlString += "<parm name=\"DownloadFile\" value=\""	+ filePath + "\"/>";
			
						//Set the IsUpgradeOnly 
						if (upgradeCheckBox.isChecked()) {
							profileXmlString += "<parm name=\"IsUpgradeOnly\" value=\""	+ "true" + "\"/>";
						} else {
							profileXmlString += "<parm name=\"IsUpgradeOnly\" value=\""	+ "false" + "\"/>";
						}
						
					} else {
						errorMessage = "Please provide the value for update file path field";
					}
				}
				
				profileXmlString += "</characteristic>" + "</wap-provisioningdoc>";
	
				if(errorMessage.length() == 0) {
					
					mProgressDialog.show();

					new Thread(new Runnable() {

						@Override
						public void run() {
							Intent intent = new Intent();
							intent.setComponent(new ComponentName(
									PAYMENT_SERVICE_PACKAG_NAME,
									PAYMENT_SERVICE_CLASS_NAME));
							intent.putExtra(
									PAYMENT_SERVICE_PROFILE_EX_DATA_NAME,
									profileXmlString);

							if (startService(intent) == null) {
								Intent returnIntent = new Intent();
								returnIntent.putExtra("RESULT", "Payment Manager Service not installed, Please try after installing...");
								ProfileHandler.this.setResult(RESULT_CANCELED,
										returnIntent);
								finish();
							}
						}
					}).start();
				} else {
					showToast(errorMessage);
				}
			}
		});

	}

	private void showToast(String msg) {
		toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
		toast.show();
		okButton.setEnabled(true);
		cancelButton.setEnabled(true);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (toast != null)
			toast.cancel();
		
		if(mProgressDialog.isShowing() || mProgressDialog != null)
			mProgressDialog.dismiss();

	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			
			String statusString = "Error";
			if (intent!=null && intent.getAction().equals(INTENT_PAYMNT_SERVICE_RESULT)) {
				
				statusString = intent.getStringExtra("com.symbol.paymentmgr.RESPONSE");
				Log.d(TAG, "Payment Manager Service Response String" + statusString);
				
				if (statusString.startsWith("<?xml version")) {
					statusString = validateResponse(statusString);
				} 
			}

			mProgressDialog.dismiss();
			okButton.setEnabled(true);
			cancelButton.setEnabled(true);
			Intent returnIntent = new Intent();
			returnIntent.putExtra("RESULT", statusString);
			ProfileHandler.this.setResult(RESULT_OK, returnIntent);
			finish();

		}
	};

	// Method to parse the XML response using XML Pull Parser
	public String validateResponse(String responseXmlString) {
		
		XmlPullParser myParser = Xml.newPullParser();
		try {
			myParser.setInput(new StringReader(responseXmlString));
		} catch (XmlPullParserException e1) {
						e1.printStackTrace();
			return "" + e1.getMessage();
		}
		
		String fieldValues = "";

		// Provides the error type for characteristic-error
		 String errorType = "";

		// Provides the parm name for parm-error
		 String parmName = "";

		// Provides error description
		 String errorDescription = "";

		// Provides error string with type/name + description
		 String errorString = "";

		
		int event;
		try {
			// Retrieve error details if parm-error in the response XML
			event = myParser.getEventType();
			while (event != XmlPullParser.END_DOCUMENT) {
				String name = myParser.getName();
				switch (event) {
				case XmlPullParser.START_TAG:

					if (name.equals("parm-error")) {
						parmName = myParser.getAttributeValue(null, "name");
						errorDescription = myParser.getAttributeValue(null,
								"desc");
						errorString = errorDescription;
					} else if (name.equals("parm")) {

						if (myParser.getAttributeValue(null, "name")
								.equalsIgnoreCase("DeviceAddress")) {
							fieldValues = "\nPD40 Address : "
									+ splitPD40Address(myParser
											.getAttributeValue(null, "value"))
									+ "\n";

						}

						if (myParser.getAttributeValue(null, "name")
								.equalsIgnoreCase("BatteryLevel")) {
							fieldValues += "\nBatteryLevel : "
									+ myParser.getAttributeValue(null, "value")
									+ "\n";
						}

						if (myParser.getAttributeValue(null, "name")
								.equalsIgnoreCase("Version")) {
							fieldValues += "\nVersion : "
									+ myParser.getAttributeValue(null, "value")
											 + "\n";
						}

					}

					break;
				case XmlPullParser.END_TAG:

					break;
				}
				event = myParser.next();
			}
			// Retrieve error details if characteristic-error in the response XML
			myParser.setInput(new StringReader(responseXmlString));
			if (TextUtils.isEmpty(parmName)
					&& TextUtils.isEmpty(errorDescription)) {
			
				event = XmlPullParser.START_DOCUMENT;
				while (event != XmlPullParser.END_DOCUMENT) {
					String name = myParser.getName();
					switch (event) {
					case XmlPullParser.START_TAG:

						if (name.equals("characteristic-error")) {
							errorType = myParser
									.getAttributeValue(null, "type");
							errorDescription = myParser.getAttributeValue(null,
									"desc");
							errorString = errorDescription;
						}

						break;
					case XmlPullParser.END_TAG:

						break;
					}
					event = myParser.next();
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String statusString = "";
		if (TextUtils.isEmpty(parmName)
				&& TextUtils.isEmpty(errorType)
				&& TextUtils.isEmpty(errorDescription)
				&& TextUtils.isEmpty(errorString)) {

			statusString = " Success.\n" + fieldValues;
		} else {
			Log.i("fieldValues", " " + fieldValues
					+ "   errorString" + errorString);

			if (errorString.contains("SUCCESS"))

				statusString = " Failed.\n\n" + errorString
						+ "\n\n" + fieldValues;
			else

				statusString = " Failed.\n\n" + errorString;
		}
		
		return statusString;
	}

	private String splitPD40Address(String fieldValues) {
		String[] address = fieldValues.split("\\|");

		String values = new String();
		for (int i = 0; i < address.length; i++) {
			if (!(i == address.length - 1))
				values += address[i] + "|" + "\n";
			else
				values += address[i];
		}

		return values;

	}

	
	

}
