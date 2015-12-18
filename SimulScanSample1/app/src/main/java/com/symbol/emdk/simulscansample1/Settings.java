/*
* Copyright (C) 2015 Symbol Technologies LLC
* All rights reserved.
*/
package com.symbol.emdk.simulscansample1;

import java.io.File;
import java.util.ArrayList;

public class Settings {
	int selectedFileIndex = 0;
	ArrayList<File> fileList;
	public Boolean enableAutoCapture;
	public Boolean enableDebugMode;
	public Boolean enableFeedbackAudio;
	public Boolean enableHaptic;
	public Boolean enableLED;
	public Boolean enableResultConfirmation;
	public int identificationTimeout;
	public int processingTimeout;
}
