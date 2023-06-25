package paho.mqtt.java.example;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Response;
import paho.mqtt.java.example.adapter.HistoryAdapter;
import paho.mqtt.java.example.config.Config;
import paho.mqtt.java.example.model.TokenModel;
import paho.mqtt.java.example.util.ConnectionOptionWrapper;
import paho.mqtt.java.example.util.HttpUtils;

public class PahoExampleActivity extends Activity {

    private static final String TAG = "PahoExampleActivity";
    private Button btn_Sub;
    private Button btn_Pub;
    private EditText et_Topic;
    private EditText et_content;

    private HistoryAdapter mAdapter;
    MqttAndroidClient mqttAndroidClient;

    final String serverUri = "tcp://mqtt-cn-zpr39b17203.mqtt.aliyuncs.com:1883";
    private String clientId = "1"; // 客户端id为唯一标识符
    private String subscriptionTopic = "gankao-mdm-topic"; // 订阅话题
    private String publishTopic = "gankao-mdm-topic"; // 发布话题
    private String publishMessage = "来自 Message From Android"; // 发布内容
    private int i = 1;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                Toast.makeText(PahoExampleActivity.this, (String) msg.obj,
                        Toast.LENGTH_SHORT).show();

                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Notification notification = new Notification(R.drawable.icon, "Mqtt即时推送", System.currentTimeMillis());
                notification.contentView = new RemoteViews("com.hxht.testmqttclient", R.layout.activity_notification);
                notification.contentView.setTextViewText(R.id.tv_desc, (String) msg.obj);
                notification.defaults = Notification.DEFAULT_SOUND;
                notification.flags = Notification.FLAG_AUTO_CANCEL;
                manager.notify(i++, notification);

            } else if (msg.what == 2) {
                Log.e("TAG", "连接成功");
                Toast.makeText(PahoExampleActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
//                try {
//                    mqttAndroidClient.subscribe(subscriptionTopic, 1);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            } else if (msg.what == 3) {
                Toast.makeText(PahoExampleActivity.this, "连接失败，系统正在重连", Toast.LENGTH_SHORT).show();
                System.out.println("连接失败，系统正在重连");
            } else if (msg.what == 4) {
                String token = (String) msg.obj;
                toSubscribe(token);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_scrolling);
        setContentView(R.layout.activity_main2);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        btn_Sub = (Button) findViewById(R.id.btn_Sub);
        btn_Pub = (Button) findViewById(R.id.btn_Pub);
        et_Topic = (EditText) findViewById(R.id.et_Topic);
        et_content = (EditText) findViewById(R.id.et_content);

        btn_Sub.setOnClickListener(v -> subscribeToTopic());

        btn_Pub.setOnClickListener(v -> {
            //publishMessage();
            //publishP2PMessage();
            sendP2PMsg();
        });

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                publishMessage();
//            }
//        });


        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.history_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new HistoryAdapter(new ArrayList<>());
        mRecyclerView.setAdapter(mAdapter);

        getToken();
    }

    private void toSubscribe(String token) {
        Log.d(TAG, "toSubscribe: " + token);
        clientId = Config.GROUP_ID + "@@@" + clientId + System.currentTimeMillis();

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    addToHistory("Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    //subscribeToTopic();
                } else {
                    addToHistory("Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                addToHistory("The Connection was lost." + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                addToHistory("Incoming message: " + new String(message.getPayload()));
                Message msg = new Message();
                msg.what = 1;
                msg.obj = subscriptionTopic + ":" + message.toString();
                handler.sendMessage(msg);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        // Token方式
        mqttConnectOptions.setUserName("Token|" + Config.ACCESS_KEY + "|" + Config.INSTANCE_ID);
        mqttConnectOptions.setPassword(("RW|" + token).toCharArray());
        mqttConnectOptions.setConnectionTimeout(10);
        mqttConnectOptions.setKeepAliveInterval(20);


        try {
            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    //subscribeToTopic(); // 订阅话题
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Failed to connect to: " + serverUri);
                }
            });


        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 添加到历史
     *
     * @param mainText
     */
    private void addToHistory(String mainText) {
        System.out.println("LOG: " + mainText);
        mAdapter.add(mainText);
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    /**
     * 订阅话题
     */
    public void subscribeToTopic() {

        // 获取要订阅的主题
        subscriptionTopic = et_Topic.getText().toString().trim();
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    addToHistory("Subscribed!");
                    Toast.makeText(PahoExampleActivity.this, "订阅成功", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Failed to subscribe");
                }
            });

            // THIS DOES NOT WORK!
            mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
//                    System.out.println("Message: " + topic + " : " + new String(message.getPayload()));
//                    Toast.makeText(PahoExampleActivity.this, "Message:" + topic + "," + new String(message.getPayload()), Toast.LENGTH_SHORT).show();

                    Message msg = new Message();
                    msg.what = 1;
                    msg.obj = subscriptionTopic + ":" + message.toString();
                    handler.sendMessage(msg);
                }
            });

        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    /**
     * 发布消息
     */
    public void publishMessage() {
        try {
            MqttMessage message = new MqttMessage();
//            message.setPayload(publishMessage.getBytes());
            message.setPayload(et_content.toString().getBytes());
            mqttAndroidClient.publish(publishTopic, message);
            addToHistory("Message Published");
            if (!mqttAndroidClient.isConnected()) {
                addToHistory(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void getToken() {
        String url = "http://118.31.73.63:5369/mqtt/applyToken";
        executorService.submit(() -> {
            try {
                Response response = HttpUtils.get(url);
                String json = response.body().string();
                Log.d(TAG, "getToken: " + json);

                TokenModel tokenModel = new Gson().fromJson(json, TokenModel.class);
                if (tokenModel.getCode() == 20000) {
                    String token = tokenModel.getData().getToken();
                    Log.d(TAG, "token: " + token);
                    Message msg = new Message();
                    msg.what = 4;
                    msg.obj = token;
                    handler.sendMessage(msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "getToken: " + e.getMessage());
            }
        });
    }

    private void publishP2PMessage() {
        /**
         * MQ4IOT 实例 ID，购买后控制台获取
         */
        String instanceId = Config.INSTANCE_ID;
        /**
         * 接入点地址，购买 MQ4IOT 实例，且配置完成后即可获取，接入点地址必须填写分配的域名，不得使用 IP 地址直接连接，否则可能会导致客户端异常。
         */
        String endPoint = Config.END_POINT;
        /**
         * 账号 accesskey，从账号系统控制台获取
         * 阿里云账号AccessKey拥有所有API的访问权限，建议您使用RAM用户进行API访问或日常运维。
         * 强烈建议不要把AccessKey ID和AccessKey Secret保存到工程代码里，否则可能导致AccessKey泄露，威胁您账号下所有资源的安全。
         * 本示例以把AccessKey ID和AccessKey Secret保存在环境变量为例说明。运行本代码示例之前，请先配置环境变量MQTT_AK_ENV和MQTT_SK_ENV
         * 例如：export MQTT_AK_ENV=<access_key_id>
         *      export MQTT_SK_ENV=<access_key_secret>
         * 需要将<access_key_id>替换为已准备好的AccessKey ID，<access_key_secret>替换为AccessKey Secret。
         */
        String accessKey = Config.ACCESS_KEY;
        /**
         * 账号 secretKey，从账号系统控制台获取，仅在Signature鉴权模式下需要设置
         */
        String secretKey = Config.SECRET_KEY;
        /**
         * MQ4IOT clientId，由业务系统分配，需要保证每个 tcp 连接都不一样，保证全局唯一，如果不同的客户端对象（tcp 连接）使用了相同的 clientId 会导致连接异常断开。
         * clientId 由两部分组成，格式为 GroupID@@@DeviceId，其中 groupId 在 MQ4IOT 控制台申请，DeviceId 由业务方自己设置，clientId 总长度不得超过64个字符。
         */
        String clientId = Config.GROUP_ID + "@@@" + "Global_Consumer";
        /**
         * MQ4IOT 消息的一级 topic，需要在控制台申请才能使用。
         * 如果使用了没有申请或者没有被授权的 topic 会导致鉴权失败，服务端会断开客户端连接。
         */
        /**
         * QoS参数代表传输质量，可选0，1，2，根据实际需求合理设置，具体参考 https://help.aliyun.com/document_detail/42420.html?spm=a2c4g.11186623.6.544.1ea529cfAO5zV3
         */
        final int qosLevel = 0;
        try {
            ConnectionOptionWrapper connectionOptionWrapper = new ConnectionOptionWrapper(instanceId, accessKey, secretKey, clientId);
            final MemoryPersistence memoryPersistence = new MemoryPersistence();
            /**
             * 客户端使用的协议和端口必须匹配，具体参考文档 https://help.aliyun.com/document_detail/44866.html?spm=a2c4g.11186623.6.552.25302386RcuYFB
             * 如果是 SSL 加密则设置ssl://endpoint:8883
             */
            final MqttClient mqttClient = new MqttClient("tcp://" + endPoint + ":1883", clientId, memoryPersistence);
            /**
             * 客户端设置好发送超时时间，防止无限阻塞
             */
            mqttClient.setTimeToWait(5000);
            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    // 客户端连接成功后就需要尽快订阅需要的 topic
                    Log.d(TAG, "connectComplete: connect success");
                }

                @Override
                public void connectionLost(Throwable throwable) {
                    throwable.printStackTrace();
                }

                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                    /**
                     * 消费消息的回调接口，需要确保该接口不抛异常，该接口运行返回即代表消息消费成功。
                     * 消费消息需要保证在规定时间内完成，如果消费耗时超过服务端约定的超时时间，对于可靠传输的模式，服务端可能会重试推送，业务需要做好幂等去重处理。超时时间约定参考限制
                     * https://help.aliyun.com/document_detail/63620.html?spm=a2c4g.11186623.6.546.229f1f6ago55Fj
                     */
                    Log.d(TAG, "receive msg from topic " + s + " , body is " + new String(mqttMessage.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    Log.d(TAG, "send msg succeed topic is : " + iMqttDeliveryToken.getTopics()[0]);
                }
            });

            mqttClient.connect(connectionOptionWrapper.getMqttConnectOptions());


        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendP2PMsg() {
        try {
            /**
             * MQ4IoT支持点对点消息，即如果发送方明确知道该消息只需要给特定的一个设备接收，且知道对端的 clientId，则可以直接发送点对点消息。
             * 点对点消息不需要经过订阅关系匹配，可以简化订阅方的逻辑。点对点消息的 topic 格式规范是  {{parentTopic}}/p2p/{{targetClientId}}
             */
            final int qosLevel = 0;
            String clientId = Config.GROUP_ID + "@@@" + "Global_Consumer";
            final String p2pSendTopic = publishTopic + "/p2p/" + clientId;
            MqttMessage message = new MqttMessage("Hello！From Android p2p MQTT message.".getBytes());
            message.setQos(qosLevel);
            mqttAndroidClient.publish(p2pSendTopic, message);
            addToHistory("Message Published");
            if (!mqttAndroidClient.isConnected()) {
                addToHistory(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            e.printStackTrace();
            Log.e(TAG, "Error Publishing: " + e.getMessage());
        }
    }

}
