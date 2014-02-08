/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.acer.smartlock;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main UI for the demo app.
 */
public class DemoActivity extends Activity {

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    
    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_MILLI_SECONDS = 2000;
    private static final Random random = new Random();
    private static final String SERVER_URL = "http://61.219.119.246:12980/gcm/v1/device/";

    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    String SENDER_ID = "724597471916";

    /**
     * Tag used on log messages.
     */
    static final String TAG = "GCM Demo";

    Button user_management_btn, entrance_log_btn; 
    TextView mDisplay;
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    Context context;

    String regid, IMEI;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        mDisplay = (TextView) findViewById(R.id.welcome);
        entrance_log_btn = (Button) findViewById(R.id.entrance_log);
        user_management_btn = (Button) findViewById(R.id.user_management);
        
        IMEI = getIMEI();

        context = getApplicationContext();

        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);

            if (regid.isEmpty()) {
                registerInBackground();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
        
        user_management_btn.setOnClickListener(new Button.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		System.out.println("Smart Lock: User Management Button");
        		Intent intent = new Intent();
        		intent.setClass(DemoActivity.this, User_Management.class);
        		startActivity(intent);
        		DemoActivity.this.finish();
        	}
        });
        
        entrance_log_btn.setOnClickListener(new Button.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		System.out.println("Smart Lock: Entrance Log Button");
        		Intent intent = new Intent();
        		intent.setClass(DemoActivity.this, Entrance_Log.class);
        		startActivity(intent);
        		DemoActivity.this.finish();
        	}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check device for Play Services APK.
        checkPlayServices();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend(regid);

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                mDisplay.append(msg + "\n");
            }
        }.execute(null, null, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(DemoActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }
    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend(String regId) {
      // Your implementation here.
    	System.out.println("SmartLock: Start to send register ID to server.");
    	String serverUrl = SERVER_URL + "register/";
    	Map<String, String> params = new TreeMap<String, String>();
    	params.put("dev_id", IMEI);
    	params.put("reg_id", regId);
    	long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
    	for(int i = 1; i <= MAX_ATTEMPTS; i++) {
    		System.out.println("SmartLock: Attempt #" + i + " to register");
    		try {
    			post(serverUrl, params);
    			return;
    		}catch(IOException e) {
    			System.out.println("SmartLock: Failed to register on attempt #" + i + ":" + e);
    			if(i == MAX_ATTEMPTS) {
    				break;
    			}
    			try {
    				System.out.println("SmartLock: Sleeping for " + backoff + " ms before retry");
    				Thread.sleep(backoff);
    			}catch(InterruptedException ex) {
    				System.out.println("SmartLock: Thread interrupted: abort remaining retries!");
    				Thread.currentThread().interrupt();
    				return;
    			}
    			// increase backoff exponentially
    			backoff *= 2;
    		}
    	}
    }
    
    /**
     * Issue a POST request to the server.
     *
     * @param endpoint POST address.
     * @param params request parameters.
     *
     * @throws IOException propagated from POST.
     */
    private static void post(String endpoint, Map<String, String> params) 
    		throws IOException {
    	URL url;
    	try {
    		url = new URL(endpoint);
    	} catch(MalformedURLException e) {
    		throw new IllegalArgumentException("invalid url: " + endpoint);
    	}
    	// constructs the POST body using the parameters
    	String construct_json = "{";
    	Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
    	while(iterator.hasNext()) {
    		Entry<String, String> param = iterator.next();
    		construct_json = construct_json + '"' + param.getKey() + '"' + ':' + '"'
    				+ param.getValue() + '"' + ',';
    	}
    	String body = construct_json.substring(0, construct_json.length()-1);
    	body = body + "}";
    	System.out.println("SmartLock: Posting " + body + " to " + url);
    	byte[] bytes = body.getBytes();
    	HttpURLConnection conn = null;
    	try {
    		conn = (HttpURLConnection) url.openConnection();
    		conn.setDoOutput(true);
    		conn.setUseCaches(false);
    		conn.setFixedLengthStreamingMode(bytes.length);
    		conn.setRequestMethod("POST");
    		conn.setRequestProperty("Content-Type", "application/json");
    		// post the request
    		OutputStream out = conn.getOutputStream();
    		out.write(bytes);
    		out.close();
    		// handle the response
    		int status = conn.getResponseCode();
    		if(status != 200) {
    			throw new IOException("Post failed with error code " + status);
    		}
    	}finally {
    		if(conn != null) {
    			conn.disconnect();
    		}
    	}
    }
    
    private String getIMEI() {
    	String tmp_imei;
    	TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    	System.out.println("Smart Lock: " + telephonyManager.getDeviceId());
    	tmp_imei = telephonyManager.getDeviceId();
    	return tmp_imei;
    }
}
