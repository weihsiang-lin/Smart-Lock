package com.acer.smartlock;

import static com.acer.smartlock.CommonUtilities.SERVER_URL;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class User_Management extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.user_management);
		
		String user_mgmt_Url = SERVER_URL + "/demo/users/android";
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			new UserMgmtTask().execute(user_mgmt_Url);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.user__management, menu);
		return true;
	}

	@Override
	public void onBackPressed() {
		Intent intent = new Intent();
		intent.setClass(User_Management.this, DemoActivity.class);
		startActivity(intent);
		User_Management.this.finish();
	}
	
	private class UserMgmtTask extends AsyncTask<String, Void, List<Map<String, String>>> {
		@Override
		protected List<Map<String, String>> doInBackground(String...urls) {
			try {
				return get_user_list(urls[0]);
			} catch (IOException e) {
				return null;
			}
		}
		@Override
		protected void onPostExecute(List<Map<String, String>> result) {
			ListView lv = (ListView) findViewById(R.id.user_management);
			lv.setAdapter(new SimpleAdapter(User_Management.this, result, R.layout.user_list,
					new String[] {"UUID", "Name", "IMEI", "Authority"}, 
					new int[] {R.id.UUID, R.id.Name, R.id.IMEI, R.id.Authority}));
			lv.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					final TextView uuid = (TextView) view.findViewById(R.id.UUID);
					System.out.println("Smart Lock: UUID = " + uuid.getText().toString());
					final TextView authority = (TextView) view.findViewById(R.id.Authority);
					System.out.println("Smart Lock: Authority = " + authority.getText().toString());
					AlertDialog.Builder actions = new AlertDialog.Builder(User_Management.this);
					actions.setTitle("Actions");
					actions.setMessage("What do you want to do?");
					actions.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							String user_delete_Url = SERVER_URL + "/demo/users/android/delete/" + uuid.getText().toString() + "/";
							new UserDeleteTask().execute(user_delete_Url);
						}
					});
					if (authority.getText().toString() == "No") {
						actions.setNeutralButton("Enable", new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								String user_enable_Url = SERVER_URL + "/demo/users/android/enable/" + uuid.getText().toString() + "/";
								new UserEnableTask().execute(user_enable_Url);
							}
						});	
					} else {
						actions.setNeutralButton("Disable", new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								String user_disable_Url = SERVER_URL + "/demo/users/android/disable/" + uuid.getText().toString() + "/";
								new UserDisableTask().execute(user_disable_Url);
							}
						});
					}
					actions.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							Toast.makeText(User_Management.this, "Cancel", Toast.LENGTH_LONG).show();
						}
					});
					actions.show();
				}
			});
		}
	}
	
	private List<Map<String, String>> get_user_list(String myurl) throws IOException {
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
		List<Map<String, String>> listItems = new ArrayList<Map<String, String>>();
		try {
			String line;
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
			while((line = reader.readLine()) != null) {
				JSONArray jsonArr = new JSONArray(line);
				for (int i = 0; i < jsonArr.length(); i++) {
					HashMap<String, String> items = new HashMap<String, String>();
					JSONObject jsonObj = (JSONObject) jsonArr.get(i);
					String fields = jsonObj.getString("fields");
					JSONObject innerObj = new JSONObject(fields);
					items.put("UUID", jsonObj.getString("pk"));
					items.put("Name", innerObj.getString("name"));
					items.put("IMEI", innerObj.getString("imei"));
					if (innerObj.getInt("auth") == 1) {
						items.put("Authority", "Yes");
					} else {
						items.put("Authority", "No");
					}
					listItems.add(items);
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return listItems;
	}
	
	private class UserDeleteTask extends AsyncTask<String, Void, Void> {
		
		@Override
		protected Void doInBackground(String...urls) {
			URL u_deleteUrl = null;
			HttpURLConnection httpCon = null;
			try {
				u_deleteUrl = new URL(urls[0]);
				httpCon = (HttpURLConnection) u_deleteUrl.openConnection();
				httpCon.setReadTimeout(10000 /*timeoutMillis*/);
				httpCon.setConnectTimeout(15000 /*timeoutMillis*/);
				httpCon.setRequestMethod("GET");
				httpCon.setDoInput(true);
				httpCon.connect();
				System.out.println("Smart Lock: Response code = " + httpCon.getResponseCode());
				return null;
			} catch (MalformedURLException e) {
				System.out.println("Smart Lock: " + e.getMessage());
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				System.out.println("Smart Lock: " + e.getMessage());
				e.printStackTrace();
				return null;
			} catch (Exception e) {
				System.out.println("Smart Lock: " + e.getMessage());
				e.printStackTrace();
				return null;
			} finally {
				if (httpCon != null) {
					httpCon.disconnect();
				}
			}
		}
		
		@Override
		protected void onPostExecute(Void v) {
			Intent intent = new Intent();
			intent.setClass(User_Management.this, User_Management.class);
			startActivity(intent);
			User_Management.this.finish();
		}
	}
	
	private class UserEnableTask extends AsyncTask<String, Void, Void> {
		
		@Override
		protected Void doInBackground(String...urls) {
			URL u_enableUrl = null;
			HttpURLConnection httpCon = null;
			try {
				u_enableUrl = new URL(urls[0]);
				httpCon = (HttpURLConnection) u_enableUrl.openConnection();
				httpCon.setReadTimeout(10000 /*timeoutMillis*/);
				httpCon.setConnectTimeout(15000 /*timeoutMillis*/);
				httpCon.setRequestMethod("GET");
				httpCon.setDoInput(true);
				httpCon.connect();
				System.out.println("Smart Lock: Response code = " + httpCon.getResponseCode());
				return null;
			} catch (MalformedURLException e) {
				System.out.println("Smart Lock: " + e.getMessage());
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				System.out.println("Smart Lock: " + e.getMessage());
				e.printStackTrace();
				return null;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			} finally {
				if (httpCon != null) {
					httpCon.disconnect();
				}
			}
		}
		
		@Override
		protected void onPostExecute(Void v) {
			Intent intent = new Intent();
			intent.setClass(User_Management.this, User_Management.class);
			startActivity(intent);
			User_Management.this.finish();
		}
	}
	
	private class UserDisableTask extends AsyncTask<String, Void, Void> {
		
		@Override
		protected Void doInBackground(String...urls) {
			URL u_disableUrl = null;
			HttpURLConnection httpCon = null;
			try {
				u_disableUrl = new URL(urls[0]);
				System.out.println("Smart Lock: " + urls[0]);
				httpCon = (HttpURLConnection) u_disableUrl.openConnection();
				httpCon.setReadTimeout(10000 /*timeoutMillis*/);
				httpCon.setConnectTimeout(15000 /*timeoutMillis*/);
				httpCon.setRequestMethod("GET");
				httpCon.setDoInput(true);
				httpCon.connect();
				System.out.println("Smart Lock: Response code = " + httpCon.getResponseCode());
				return null;
			} catch (Exception e) {
				System.out.println("Smart Lock: " + e.getMessage());
				e.printStackTrace();
				return null;
			} finally {
				if (httpCon != null) {
					httpCon.disconnect();
				}
			}
		}
		
		@Override
		protected void onPostExecute(Void v) {
			Intent intent = new Intent();
			intent.setClass(User_Management.this, User_Management.class);
			startActivity(intent);
			User_Management.this.finish();
		}
	}
}
