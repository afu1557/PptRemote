package edu.mit.afu.qrcodescanner;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import java.lang.*;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class ServerConnect extends AppCompatActivity implements MqttCallback {
    public String TAG;
    public String topic;
    public String dst;

    String clientId = MqttClient.generateClientId();
    MqttAndroidClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_connect);

        //get topic/dst from Main Activity
        Intent intent = getIntent();
        topic = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        //index of "ind="
        int indIndex = topic.toLowerCase().indexOf("ind=");
        //actual id index
        int idIndex = Integer.parseInt(topic.substring(indIndex+4));
        topic = topic.substring(0, indIndex);
        Log.i(TAG, topic);

        dst = topic.substring(0,idIndex)+"_"+topic.substring(idIndex,indIndex);
        Log.i(TAG, dst);
        topic = "athena/" + topic.substring(0,idIndex) + "/" + topic.substring(idIndex,indIndex);

        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://con.ap.baidu.com:443",
                clientId);
        client.setCallback(this);
        connect();
    }

    public void connect(){
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d(TAG, "onSuccess");
                    subscribe();
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d(TAG, "onFailure");
                    setContentView(R.layout.activity_warning);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void playPPT(View view){
        Log.i(TAG, "starting ppt");
        sendMessage("start");
        setContentView(R.layout.activity_ppt_controls);
    }

    public void next(View view){
        sendMessage("next");
    }

    public void previous(View view){
        sendMessage("prev");
    }

    public void stop(View view){sendMessage("stop");}

    public void sendMessage(String msg) {
        JSONObject m = new JSONObject();
        byte[] encodedMsg;
        try {
            m.put("msg", msg);
            m.put("dst", dst);
            m.put("src", clientId);
            Log.i(TAG, m.toString());
            encodedMsg = m.toString().getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedMsg);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void messageArrived(String topic, MqttMessage message) throws JSONException {
        String m = message.toString();
        Log.i(TAG, "received:");
        Log.i(TAG, m);
        JSONObject mssg = new JSONObject(m);
        String src = mssg.getString("src");
        Log.i(TAG, src);
        Log.i(TAG, dst);
        String dst2 = mssg.getString("dst");
        String msg = mssg.getString("msg");
        String state = mssg.getString("state");
        Boolean error = mssg.getBoolean("error");

        if (src.equals(dst)){
            Log.i(TAG, "from mac:");
            Log.i(TAG, msg);
            Log.i(TAG, state);
            if("state".equals(msg)){
                if("0".equals(state)){
                      connect();
                    }else if ("1".equals(state)) {
                    Log.i(TAG, "playing ppt!");
                    setContentView(R.layout.activity_ppt_controls);
                } else if ("3".equals(state) || "2".equals(state)) {
                        setContentView(R.layout.activity_ppt_controls);
                }
                }else if ("error".equals(msg)) {
                    setContentView(R.layout.activity_warning);
                } else if ("bye".equals(msg)) {
                        setContentView(R.layout.activity_warning);
                } else if ("quit".equals(msg)) {
                            setContentView(R.layout.activity_warning);
                } else if ("lost".equals(msg)) {
                            connect();
                } else if ("hello".equals(msg)) {
                            //sendMessage("hello");
                }
        }
    }

    public void deliveryComplete(IMqttDeliveryToken token) {}

    public void connectionLost(Throwable cause) {}

    public void subscribe(){
        int qos = 2;
        try {
            IMqttToken subToken = client.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // switch to Play Ppt display
                    setContentView(R.layout.activity_play_ppt);
                    Log.d(TAG, "subscribeSuccess");
                    sendMessage("hello");
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Log.d(TAG, "subscribeFailure");
                    // switch to Warning display
                    setContentView(R.layout.activity_warning);
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnect(View view){
        try {
            sendMessage("bye");
            IMqttToken disconToken = client.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // we are now successfully disconnected
                    Log.d(TAG, "disconnectSuccess");
                    // switch back to Main Activity
                    Intent intent = new Intent(ServerConnect.this, MainActivity.class);
                    startActivity(intent);

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {

                    Log.d(TAG, "disconnectFailure");
                    Intent intent = new Intent(ServerConnect.this, MainActivity.class);
                    startActivity(intent);
                    // something went wrong, but probably we are disconnected anyway
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}