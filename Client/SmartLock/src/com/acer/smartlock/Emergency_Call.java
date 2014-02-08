package com.acer.smartlock;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

import android.content.Intent;
import android.view.View;
import android.widget.Button;

public class Emergency_Call extends Activity {
	
	Button emergency_call_btn, cancel_btn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.emergency_call);
		
		emergency_call_btn = (Button) findViewById(R.id.emergency_call);
		cancel_btn = (Button) findViewById(R.id.cancel);
		
		emergency_call_btn.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent dial = new Intent();
				dial.setAction("android.intent.action.CALL");
				dial.setData(Uri.parse("tel:0975077995"));
				startActivity(dial);
			}
		});
		
		cancel_btn.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent();
				intent.setClass(Emergency_Call.this, Entrance_Log.class);
				startActivity(intent);
				Emergency_Call.this.finish();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.emergency__call, menu);
		return true;
	}

}
