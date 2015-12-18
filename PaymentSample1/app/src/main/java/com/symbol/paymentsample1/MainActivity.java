/*
* Copyright (C) 2015 Symbol Technologies LLC
* All rights reserved.
*/
package com.symbol.paymentsample1;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.payment.CardData;
import com.symbol.emdk.payment.InterfaceConfig;
import com.symbol.emdk.payment.MenuData;
import com.symbol.emdk.payment.PaymentConfig;
import com.symbol.emdk.payment.PaymentData;
import com.symbol.emdk.payment.PaymentDevice;
import com.symbol.emdk.payment.PaymentException;
import com.symbol.emdk.payment.PaymentManager;
import com.symbol.emdk.payment.PaymentDevice.DataListener;
import com.symbol.emdk.payment.PaymentDevice.ReadMode;
import com.symbol.emdk.payment.PaymentManager.DeviceIdentifier;
import com.symbol.emdk.payment.PaymentResults;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements EMDKListener, DataListener{

	private EMDKManager emdkManager = null;	
	private PaymentManager paymentManager = null;	
	private PaymentDevice paymentDevice = null;
	
	private TextView statusTextView = null;
	private Button enableButton = null;
	private Button disableButton = null;
	private Button transactionButton = null;
	
	DataListener dataCallbackObj = this; 
	
	private double billAmount = 1.0; 
	
	private String maskedPAN = "";
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		statusTextView = (TextView) findViewById(R.id.textViewStatusData);
		
		// The EMDKManager object creation and object will be returned in the callback.
		EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);

		// Check the return status of getEMDKManager ()
		if (results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
			statusTextView.setText("Please wait, initialization in progress...");
		} else {
			statusTextView.setText("Initialization failed!");
		}
		
		registerForButtonEvents ();
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

		if (emdkManager != null) {
			// Clean up the objects created by EMDK manager
			emdkManager.release();
			emdkManager=null;
		}
	}
	
	StringBuilder responseString = new StringBuilder();
	
	boolean isDebit = false;
	
	@Override
	public void onData(PaymentData paymentData) {

		final PaymentData data = paymentData;
		runOnUiThread(new Runnable() {

			@Override
			public void run() {	
				
				switch (data.getDataType()) {

				case PROMPTMESSAGE:
					responseString.append("\n");
										
					if(data.getResult() == PaymentResults.OK_KEY_PRESSED) {
						
						responseString.append("\nUser pressed ENTER key");
						
						responseString.append("\n");
						
						PaymentResults result = paymentDevice.promptMenu("Select payment method", "", "CASH    ", "CREDIT    ", "DEBIT    ", "", 10000);
						if(result == PaymentResults.SUCCESS) {
							responseString.append("\nWaiting for payment method..");
							break;
						} else {
							responseString.append("\nSelection is incorrect or error occurred!!: " + result.getDescription());								
						}
					} else {
						responseString.append("\nUser cancelled or error occurred. " + data.getResult().getDescription());
					}
					transactionCancelled();
					break;
					
				case PROMPTMENU:
					responseString.append("\n");
					if(data.getResult() == PaymentResults.SUCCESS) {
						
						String choice = ((MenuData)data).getChoiceString();
						
						responseString.append("\nUser selected payment method is " + choice );
						
						responseString.append("\n");
						
						isDebit = false;
						if(choice.equals("CREDIT")) {

							PaymentResults result = paymentDevice.readCardData(billAmount, 0, ReadMode.ALL, 10000);
							if(result == PaymentResults.SUCCESS) {
								responseString.append("\nWaiting for card info...");
								break;
							}else {
								responseString.append("\nCard info status: " + result.getDescription());
							}
						} else if(choice.equals("DEBIT")) {
							isDebit = true;	
							PaymentResults result = paymentDevice.readCardData(billAmount, 0, ReadMode.ALL, 10000);
							
							if(result == PaymentResults.SUCCESS) {
								responseString.append("\nWaiting for card info...");
								break;
							}else {
								responseString.append("\nCard info status: " + result.getDescription());
							}						
						} 
					} else {
						responseString.append("\nSelection is incorrect or error occurred!!:" + data.getResult().getDescription());
						transactionCancelled();
						break;
					}
					transactionComplete();
					break;

				case READCARDDATA:
					responseString.append("\n");
					maskedPAN = "";
					if(data.getResult() == PaymentResults.SUCCESS) {
						CardData cardData = (CardData) data;
						maskedPAN = cardData.getMaskedPAN();
						
						responseString.append("\nAccount Number="	+ cardData.getMaskedPAN()
										+ "\nExpiry Date=" + cardData.getExpiryDate()
										+ "\nCardHolderName=" + cardData.getCardHolderName());

						/*
						responseString.append("\n\nTrack Data"
								+ "\nTrack1Data=" + cardData.getTrack1Data()
								+ "\nTrack2Data=" + cardData.getTrack2Data()
								+ "\nTrack3Data=" + cardData.getTrack3Data());
						
						responseString.append("\n\nEMV Tag Data");
						for(TagData tagData : cardData.getTagData()) {	
							responseString.append("\nTLV String=" + tagData.getTlvString());
						}	
						*/					
						responseString.append("\n");
						
						if(isDebit) {
							PaymentResults result = paymentDevice.promptPin(maskedPAN, 2, 4, true, 10000);
							maskedPAN = "";
							if(result == PaymentResults.SUCCESS) {
								responseString.append("\nWaiting for Pin...");
								break;
							}else {
								responseString.append("\nPin request Status:" + result.getDescription());
								transactionCancelled();
								break;
							}
						} 
						
					} else {
						responseString.append("\nCard info request Status: " + data.getResult().getDescription());
						transactionCancelled();
						break;
					}
					transactionComplete();
					break;	
					
				case PROMPTPIN: 
					responseString.append("\n");
					if(data.getResult() == PaymentResults.SUCCESS) {
						//PinData pin = (PinData)data;
						responseString.append("\nReceived the PIN");
						transactionComplete();
					} else {
						responseString.append("\nPin request status:" + data.getResult().getDescription());
						transactionCancelled();
					}
					break;
					
				case ENABLE:
					responseString.append("\n");
					if(data.getResult() == PaymentResults.SUCCESS || data.getResult() == PaymentResults.ALREADY_ENABLED) {
						transactionButton.setEnabled(true);
						enableButton.setEnabled(false);
						disableButton.setEnabled(true);
						transactionButton.setEnabled(true);						
					}
					responseString.append("\nConnection to payment device status: " + data.getResult().getDescription());
					
					if(data.getResult() == PaymentResults.SUCCESS) {
						try {							
							PaymentConfig config = paymentDevice.getConfig();
							config.sleepModeTimeout = 45 * 1000 ; //45 seconds 
							paymentDevice.setConfig(config);
							
						} catch (PaymentException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							responseString.append(e.getResult().getDescription());
						}
					}										
					break;
					
				default:
					responseString.append("\n");
					responseString.append(data.getResult().getDescription());
					
					if(data.getResult() == PaymentResults.DISABLED ){
						
						try {
							paymentDevice.removeDataListener(dataCallbackObj);
						} catch (PaymentException e) {
														
							responseString.append("\nDisable Status:" + e.getResult().getDescription());
						}
						paymentDevice = null;
						transactionButton.setEnabled(false);
						enableButton.setEnabled(true);
						disableButton.setEnabled(false);
					}
					break;
				}
				statusTextView.setText(responseString.toString());
			}
		});				
	}

	@Override
	public void onClosed() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {				
				statusTextView.setText("Error!! Restart the application!!");
			}
		});		
	}

	@Override
	public void onOpened(EMDKManager emdkManager) {
		this.emdkManager = emdkManager;	

		runOnUiThread(new Runnable() {
			@Override
			public void run() {				
				statusTextView.setText("Application Initialized.");
			}
		});	
	}
	
	void transactionCancelled() {
		
		responseString.append("\n\n********Transaction Cancelled********\n");
		transactionButton.setEnabled(true);			
		statusTextView.setText(responseString);
	}

	void transactionComplete() {
		
		responseString.append("\n\n********Transaction Completed********\n");
		transactionButton.setEnabled(true);			
		statusTextView.setText(responseString);
	}
	
	private void registerForButtonEvents() {		
		
		addEnableButtonEvents();
		addDisableButtonEvents();
		addTransactionButtonEvents();		
	}
	
	private void addEnableButtonEvents() {
		enableButton = (Button) findViewById(R.id.buttonEnable);
		enableButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				responseString.setLength(0);

				try {

					if (paymentDevice == null) {
						paymentManager = (PaymentManager) emdkManager.getInstance(FEATURE_TYPE.PAYMENT);
						paymentDevice = paymentManager.getDevice(DeviceIdentifier.DEFAULT);
						
						
						paymentDevice.addDataListener(dataCallbackObj);
					}

					if (paymentDevice != null) {
						paymentDevice.enable();
						
						
						responseString.append("\n Connecting to payment device...");
						transactionButton.setEnabled(false);

					} else {
						responseString.append("\nError occurred.");
					}
				} catch (PaymentException ex) {
					responseString.append(ex.getResult().getDescription());
				} catch (Exception e) {
					responseString.append(e.getMessage());
				}
				statusTextView.setText(responseString.toString());
			}
		});
	}
	
	private void addDisableButtonEvents() {
		disableButton = (Button) findViewById(R.id.buttonDisable);
		disableButton.setEnabled(false);
		disableButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				try {						
						if(paymentDevice != null ) {
							
							paymentDevice.removeDataListener(dataCallbackObj);
							paymentDevice.disable();								
																
							responseString.append("\nPayment device is disconnected.");
						} 
						
						paymentDevice = null;
						transactionButton.setEnabled(false);
						enableButton.setEnabled(true);
						disableButton.setEnabled(false);
						
					} catch(PaymentException ex) {
						if(ex.getResult() == PaymentResults.ALREADY_CLOSED)
						{
							paymentDevice = null;
							transactionButton.setEnabled(false);
							enableButton.setEnabled(true);
							disableButton.setEnabled(false);
						}
						
						responseString.append("\nDisable Status:" + ex.getResult().getDescription());
					}
					catch (Exception e) {
						String status = "\nDisable Status:Error";
						if(e != null) {
							status= "\nDisable Status:"+ e.getMessage();
						}
						responseString.append(status);
					}							
				   statusTextView.setText("\nDisable Status:"+ responseString.toString());
				}
		});	
	}
	
	private void addTransactionButtonEvents() {
		transactionButton = (Button) findViewById(R.id.buttonTransaction);	
		transactionButton.setEnabled(false);
		
		transactionButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				responseString.setLength(0);
				
				if(paymentDevice != null ) {
					
					PaymentResults results = paymentDevice.enableKeypad();
					 if(results == PaymentResults.SUCCESS ) {
						String messageLine1 = "Total:  $1.00";
						String messageLine2 = "";
						String messageLine3 = "";
						String messageLine4 = "ENTER or CANCEL";
						boolean getUserConfirmation = true;
						results = paymentDevice.promptMessage(messageLine1, messageLine2, messageLine3, messageLine4, getUserConfirmation, 10000);
						
						 if(results == PaymentResults.SUCCESS ) {
							responseString.append("\n Message prompted on device:");
							responseString.append("\n" + messageLine1 + " " + messageLine2); 
							responseString.append("\n" + messageLine3 + " " + messageLine4); 
							transactionButton.setEnabled(false);
						 } else {
							 responseString.append("\n PromptMessage Error:" + results.getDescription());
						 }
						
					 } else {						 
						 responseString.append( "\n EnableKeypad Error:" + results.getDescription());
					 }
				} 					
				statusTextView.setText(responseString.toString());
			}
		});
	}
}
