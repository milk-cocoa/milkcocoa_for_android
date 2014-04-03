package com.mlkcca.milkchat;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.net.ssl.SSLContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.mlkcca.android.client.milkcocoa.Callback;
import com.mlkcca.android.client.milkcocoa.DataStore;
import com.mlkcca.android.client.milkcocoa.MilkCocoa;
import com.mlkcca.android.client.milkcocoa.Query;

public class MainActivity extends Activity {

	private EditText editText;
	private ArrayAdapter<String> adapter;
	private SocketIO socket;
    private Handler handler = new Handler();
	private MilkCocoa milkcocoa;
	private DataStore messagesDataStore;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		// ListViewの設定
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		ListView listView = (ListView)findViewById(R.id.listView1);
		listView.setAdapter(adapter);

		editText = (EditText)findViewById(R.id.editText1);

		try {
			connect();
		} catch(Exception e) {
			e.printStackTrace();
		}
    }

    private void connect() {
        this.milkcocoa = new MilkCocoa();
		this.messagesDataStore = MainActivity.this.milkcocoa.dataStore("message");
    	try {
			//milkcocoa.init("http://10.0.2.2:3000", "mlkdev-000h", new Callback() {
			milkcocoa.init("https://mlkcca.com", "chatapp", new Callback() {

				@Override
				public void callback(Object err) {
					Query query;
					try {
						query = MainActivity.this.messagesDataStore.query(new JSONObject());
						query.limit(25);
						query.desort("time");
						query.done(new Callback() {

							@Override
							public void callback(Object arg0) {
								final JSONArray messages = (JSONArray)arg0;
								
								new Thread(new Runnable() {
									public void run() {
									handler.post(new Runnable() {
										public void run() {
												for(int i=0;i < messages.length();i++) {
													try {
														adapter.insert(messages.getJSONObject(i).getString("content"), i);
													} catch (JSONException e) {
														// TODO Auto-generated catch block
														e.printStackTrace();
													}
												}
											}
										});
									}
								}).start();
							}});
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					try {
						MainActivity.this.messagesDataStore.on("push", new Callback() {

							@Override
							public void callback(Object arg0) {
								final JSONObject pushed = (JSONObject)arg0;
								new Thread(new Runnable() {
									public void run() {
									handler.post(new Runnable() {
										public void run() {
												String content = "";
												try {
													content = pushed.getJSONObject("value").getString("content");
												} catch (JSONException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												}
												adapter.insert(content, 0);
											}
										});
									}
								}).start();
							}
							
						});
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			});
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	private IOCallback iocallback = new IOCallback() {

		@Override
		public void onConnect() {
		    System.out.println("onConnect");
		}

		@Override
		public void onDisconnect() {
		    System.out.println("onDisconnect");
		}

		@Override
		public void onMessage(JSONObject json, IOAcknowledge ack) {
			System.out.println("onMessage");
		}

		@Override
		public void onMessage(String data, IOAcknowledge ack) {
		    System.out.println("onMessage");
		}

		@Override
		public void on(String event, IOAcknowledge ack, Object... args) {
			final JSONObject message = (JSONObject)args[0];

			new Thread(new Runnable() {
				public void run() {
				handler.post(new Runnable() {
					public void run() {
						try {
							if(message.getString("message") != null) {
								// メッセージが空でなければ追加
								adapter.insert(message.getString("message"), 0);
							}

							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					});
				}
			}).start();
		}

		@Override
		public void onError(SocketIOException socketIOException) {
		    System.out.println("onError");
		    socketIOException.printStackTrace();
		}
    };

    public void sendEvent(View view){
		if (editText.getText().toString().length() == 0) {
		    return;
		}
		
		JSONObject params = new JSONObject();
		try {
			params.put("content", editText.getText().toString());
			Date date = new Date();
			params.put("date", date.toString());
			params.put("time", date.getTime());
			this.messagesDataStore.push(params);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
    	editText.setText("");
    }
}