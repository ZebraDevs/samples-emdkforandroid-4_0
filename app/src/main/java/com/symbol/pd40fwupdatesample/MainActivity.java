/*
* Copyright (C) 2015 Symbol Technologies LLC
* All rights reserved.
*/
package com.symbol.pd40fwupdatesample;

import android.app.Activity;



import android.content.Intent;
import android.os.Bundle;

import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends Activity {

	Button applyBtn;
	Button runBtn;
	String xmlString;
	static String requestType;

	private final static int POFILE_HNDLE_ACTIVITY_REF_ID = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setTitle(getResources().getString(R.string.app_title));
		runBtn = (Button) findViewById(R.id.runBtn);

		addRunBtnListener();

	}

	private void addRunBtnListener() {

		runBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				requestType = ((Spinner) findViewById(R.id.type_spinner)).getSelectedItem().toString();
				Intent intent = new Intent(getApplicationContext(),
						ProfileHandler.class);
				startActivityForResult(intent, POFILE_HNDLE_ACTIVITY_REF_ID);

			}
		});

	}

	
	@Override
	protected void onActivityResult(int activityRefID, int statusCode, Intent data) {

		if (activityRefID == POFILE_HNDLE_ACTIVITY_REF_ID) {
			if (statusCode == RESULT_OK) {
				String result = data.getStringExtra("RESULT");
				((TextView) findViewById(R.id.textView1)).setText(
						 result);
			} else if (statusCode == RESULT_CANCELED && data != null) {
				String result = data.getStringExtra("RESULT");
				((TextView) findViewById(R.id.textView1)).setText(
						 result);
			} else if (statusCode == RESULT_CANCELED) {

				((TextView) findViewById(R.id.textView1)).setText(
						 "Operation is canceled by the user.");
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
