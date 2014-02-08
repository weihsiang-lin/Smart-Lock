package acer.com.socketclient;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import java.io.BufferedInputStream;
//import java.io.BufferedReader;
import java.io.BufferedWriter;
//import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketClient extends Activity {
	
	private Socket socket;
	private static final int SERVER_PORT = 8888;
	private static final String SERVER_IP = "10.36.63.138";
	
	TextView tv;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.socketclient);
		
		tv = (TextView) findViewById(R.id.textView1);
		
		new Thread(new SocketClientThread()).start();
	}
	
	Handler SocketHandler = new Handler() {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			String message = (String) msg.obj;
			tv.setText(message);
		}
	};
	
	class SocketClientThread implements Runnable {
		@Override
		public void run() {
			try {
				InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
				socket = new Socket(serverAddr, SERVER_PORT);
				String cmd = "curl http://61.219.119.242/demo/entrance/098765432123456/";
				PrintWriter out = new PrintWriter( new BufferedWriter( new OutputStreamWriter(socket.getOutputStream())), true);
				out.println(cmd);
				BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
				byte[] buffer = new byte[4096];
				int length = in.read(buffer);
				String data = new String(buffer, 0, length);
				System.out.println("Smart: " + data);
				Message msg = Message.obtain();
				msg.obj = data;
				msg.setTarget(SocketHandler);
				msg.sendToTarget();
			}catch(UnknownHostException ex) {
				ex.printStackTrace();
			}catch(IOException ex) {
				ex.printStackTrace();
			}
			
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.socket_client, menu);
		return true;
	}

}
