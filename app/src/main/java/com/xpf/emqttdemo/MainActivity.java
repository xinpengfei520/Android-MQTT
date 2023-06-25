package com.xpf.emqttdemo;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.xpf.emqttdemo.MqttConstants.host;
import static com.xpf.emqttdemo.MqttConstants.passWord;
import static com.xpf.emqttdemo.MqttConstants.userName;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private EditText etTopic;
    private Button btnSub;
    private EditText etContent;
    private Button btnPub;

    private int i = 1;
    private MqttClient client;
    private MqttConnectOptions options;
    private String mTopic = "World"; // 默认话题为"World"
    private ScheduledExecutorService scheduler;

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                Toast.makeText(MainActivity.this, (String) msg.obj,
                        Toast.LENGTH_SHORT).show();

                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Notification notification = new Notification(R.drawable.icon, "Mqtt即时推送", System.currentTimeMillis());
                notification.contentView = new RemoteViews("com.hxht.testmqttclient", R.layout.activity_notification);
                notification.contentView.setTextViewText(R.id.tv_desc, (String) msg.obj);
                notification.defaults = Notification.DEFAULT_SOUND;
                notification.flags = Notification.FLAG_AUTO_CANCEL;
                manager.notify(i++, notification);

            } else if (msg.what == 2) {
                Log.d(TAG, "连接成功");
                Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                try {
                    client.subscribe(mTopic, 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (msg.what == 3) {
                Toast.makeText(MainActivity.this, "连接失败，系统正在重连", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "连接失败，系统正在重连");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();
        init();
        startReconnect();
    }

    private void findViews() {
        etTopic = (EditText) findViewById(R.id.et_Topic);
        btnSub = (Button) findViewById(R.id.btn_Sub);
        etContent = (EditText) findViewById(R.id.et_content);
        btnPub = (Button) findViewById(R.id.btn_Pub);
        btnSub.setOnClickListener(this);
        btnPub.setOnClickListener(this);
    }

    private void init() {
        try {
            // host为主机名，test为clientid即连接MQTT的客户端ID，一般以客户端唯一标识符表示，MemoryPersistence设置clientid的保存形式，默认为以内存保存
            client = new MqttClient(host, "1", new MemoryPersistence());
            // MQTT的连接设置
            options = new MqttConnectOptions();
            // 设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            options.setCleanSession(true);
            // 设置连接的用户名
            options.setUserName(userName);
            // 设置连接的密码
            options.setPassword(passWord.toCharArray());
            // 设置超时时间,单位为秒
            options.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            options.setKeepAliveInterval(20);
            // 设置回调
            client.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable cause) {
                    // 连接丢失后，一般在这里面进行重连
                    Log.d(TAG, "connectionLost----------");
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    //publish后会执行到这里
                    Log.d(TAG, "deliveryComplete---------" + token.isComplete());
                }

                @Override
                public void messageArrived(String topicName, MqttMessage message)
                        throws Exception {
                    // subscribe后得到的消息会执行到这里面
                    Log.d(TAG, "messageArrived----------");
                    Message msg = new Message();
                    msg.what = 1;
                    msg.obj = topicName + ":" + message.toString();
                    handler.sendMessage(msg);
                    Log.e(TAG, "messageArrived" + message.toString());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startReconnect() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!client.isConnected()) {
                    connect();
                }
            }
        }, 0 * 1000, 10 * 1000, TimeUnit.MILLISECONDS);
    }

    private void connect() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    client.connect(options);
                    Message msg = new Message();
                    msg.what = 2;
                    handler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                    Message msg = new Message();
                    msg.what = 3;
                    handler.sendMessage(msg);
                }
            }
        }).start();
    }


    @Override
    public void onClick(View v) {
        if (v == btnSub) {
            subscribeToTopic();
        } else if (v == btnPub) {

        }
    }

    public void subscribeToTopic() {
//        try {
//            client.subscribe(mTopic, 0, null, new IMqttActionListener() {
//                @Override
//                public void onSuccess(IMqttToken asyncActionToken) {
//                    Toast.makeText(MainActivity.this, "订阅成功", Toast.LENGTH_SHORT).show();
//                }
//
//                @Override
//                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                    addToHistory("Failed to subscribe");
//                    Toast.makeText(MainActivity.this, "订阅失败", Toast.LENGTH_SHORT).show();
//                }
//            });
//
//            // THIS DOES NOT WORK!
//            client.subscribe(mTopic, 0, new IMqttMessageListener() {
//                @Override
//                public void messageArrived(String topic, MqttMessage message) throws Exception {
//                    // message Arrived!
//                    System.out.println("Message: " + topic + " : " + new String(message.getPayload()));
////                    Toast.makeText(PahoExampleActivity.this, "Message:" + topic + "," + new String(message.getPayload()), Toast.LENGTH_SHORT).show();
//                }
//            });

        try {
            client.subscribe(mTopic, 0, new IMqttMessageListener() {


                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        scheduler.shutdown();
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

}
