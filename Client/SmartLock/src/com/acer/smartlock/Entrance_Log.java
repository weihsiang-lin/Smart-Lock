package com.acer.smartlock;

import static com.acer.smartlock.CommonUtilities.SERVER_URL;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Entrance_Log extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entrance_log);
		
		String entrance_log_Url = SERVER_URL + "/demo/logs/android";
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			new EntranceLogTask().execute(entrance_log_Url);
		} else {
			System.out.println("Smart Lock: No network connection available.");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.entrance__log, menu);
		return true;
	}

	@Override
	public void onBackPressed() {
		Intent intent = new Intent();
		intent.setClass(Entrance_Log.this, DemoActivity.class);
		startActivity(intent);
		Entrance_Log.this.finish();
	}
	
	private class EntranceLogTask extends AsyncTask<String, Void, List<Map<String, String>>> {
		@Override
		protected List<Map<String, String>> doInBackground(String...urls) {
			try {
				return get_entrance_log(urls[0]);
			} catch(IOException e) {
				return null;
			}
		}
		@Override
		protected void onPostExecute(List<Map<String, String>> result) {
			ListView lv = (ListView) findViewById(R.id.entrance_log);
			lv.setAdapter(new SimpleAdapter(Entrance_Log.this, result, android.R.layout.simple_expandable_list_item_2,
					new String[] {"Name", "Time"}, new int[] {android.R.id.text1, android.R.id.text2}));
		}
	}
	
	private List<Map<String, String>> get_entrance_log(String myurl) throws IOException {
		InputStream is = null;
		try {
			URL url = new URL(myurl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000 /*timeoutMillis*/);
			conn.setConnectTimeout(15000 /*timeoutMillis*/);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			// Starts the query
			conn.connect();
			is = conn.getInputStream();
			List<Map<String, String>> content = ParsingJSON(is);
			return content;
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}
	
	public List<Map<String, String>> ParsingJSON(InputStream stream) {
		String time = "";
		List<Map<String, String>> listItems = new ArrayList<Map<String, String>>();
		
		try{
			String line;
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
			while((line = reader.readLine()) != null) {
				JSONArray jsonArr = new JSONArray(line);
				for (int i = 0; i < jsonArr.length(); i++) {
					HashMap<String, String> items = new HashMap<String, String>();
					JSONObject jsonObj = (JSONObject) jsonArr.get(i);
					String fields = jsonObj.getString("fields");
					JSONObject innerObj = new JSONObject(fields);
					SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
					try {
						Date date = date_format.parse(innerObj.getString("time"));
						// Format transfer
						SimpleDateFormat str_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						time = str_format.format(date);
					} catch(Exception ex) {
						ex.printStackTrace();
					}
					items.put("ID", jsonObj.getString("pk"));
					items.put("Name", innerObj.getString("user").subSequence(2, innerObj.getString("user").length()-2).toString());
					items.put("Time", time);
					listItems.add(items);
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return listItems;
	}
}
