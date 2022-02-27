package com.dji.ux.sample.mqtt;

import android.content.Context;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class SmartMqtt {
    //客户端
    private MqttAndroidClient client;
    //连接选项
    private MqttConnectOptions mqttConnectOptions;

    private String clienID;

    private Context context;//上下文

    private static SmartMqtt instance;//入口操作管理

    private static boolean isInit = false;

    public static boolean isDebug = true;

    private IMqttCallBack iMqttCallBack;

    public static int ACTION_CONNECT = 301;
    public static int ACTION_SUBSCRIBE = 401;
    public static int ACTION_PUBLISH = 501;

    private IMqttActionListener mSendCallBack;
    private IMqttActionListener mConCallBack;

    private MqttCallback mCallBack;


    /**
     * 单例方式
     *
     * @return SmartMqtt
     */
    public static SmartMqtt getInstance() {
        if (instance == null) {
            synchronized (SmartMqtt.class) {
                if (instance == null) {
                    instance = new SmartMqtt();
                }
            }
        }
        return instance;
    }

    private SmartMqtt() {

    }

    public void init(Context context) {
        this.context = context;
        //配置连接信息
        mqttConnectOptions = new MqttConnectOptions();
        //是否清除缓存
        mqttConnectOptions.setCleanSession(false);
        //是否重连
        mqttConnectOptions.setAutomaticReconnect(true);
        //设置心跳,30s
        mqttConnectOptions.setKeepAliveInterval(30);
        //登陆的名字
        mqttConnectOptions.setUserName("root".trim());
        //登陆的密码
        mqttConnectOptions.setPassword("jzfw2021mqtt".trim().toCharArray());

        //超时时间
        mqttConnectOptions.setConnectionTimeout(100);
        mCallBack = new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                //连接丢失异常
                if (iMqttCallBack != null) {
                    iMqttCallBack.connectionLost(cause);
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                //收到服务器的信息
                if (iMqttCallBack != null) {
                    iMqttCallBack.messageArrived(topic, message);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                if (iMqttCallBack != null) {
                    iMqttCallBack.deliveryComplete(token);
                }

            }
        };
        //accessKey
        mSendCallBack = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                if (iMqttCallBack != null) {
                    iMqttCallBack.onActionSuccess(ACTION_PUBLISH, asyncActionToken);
                }

            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                if (iMqttCallBack != null) {
                    iMqttCallBack.onActionFailure(ACTION_PUBLISH, asyncActionToken, exception);
                }
            }
        };
        mConCallBack = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                if (iMqttCallBack != null) {
                    iMqttCallBack.onActionSuccess(ACTION_CONNECT, asyncActionToken);
                }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                if (iMqttCallBack != null) {
                    iMqttCallBack.onActionFailure(ACTION_CONNECT, asyncActionToken, exception);
                }

            }
        };

        isInit = true;

    }

    public void setIMqttCallBack(IMqttCallBack callBack) {
        this.iMqttCallBack = callBack;
    }

    public void removeIMqttCallBack() {
        this.iMqttCallBack = null;
    }

    public void connect(String serverURI, String clientId) {
        this.clienID = clientId;
        //第一个参数上下文，第二个 服务器地址， 第三个 客户端ID，如果存在此ID连接了服务器。那么连接失败！
        if (client == null) {
            client = new MqttAndroidClient(context, serverURI, clientId);
            client.setCallback(mCallBack);
        }
        //开始连接服务器
        try {

            client.connect(mqttConnectOptions, null, mConCallBack);

        } catch (MqttException e) {
            if (iMqttCallBack != null) {
                iMqttCallBack.onActionFailure(ACTION_CONNECT, e);
            }
            e.printStackTrace();
            //连接失败
        }
    }
    public void connect(String serverURI, String clientId,String user,String password) {
        this.clienID = clientId;
        //第一个参数上下文，第二个 服务器地址， 第三个 客户端ID，如果存在此ID连接了服务器。那么连接失败！
        //登陆的名字
        mqttConnectOptions.setUserName(user.trim());
        //登陆的密码
        mqttConnectOptions.setPassword(password.trim().toCharArray());
        if (client == null) {
            client = new MqttAndroidClient(context, serverURI, clientId);
            client.setCallback(mCallBack);
        }
        //开始连接服务器
        try {

            client.connect(mqttConnectOptions, null, mConCallBack);

        } catch (MqttException e) {
            if (iMqttCallBack != null) {
                iMqttCallBack.onActionFailure(ACTION_CONNECT, e);
            }
            e.printStackTrace();
            //连接失败
        }
    }

    public void sendData(String data, String topic) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(data.getBytes());
        if (mSendCallBack == null) {
            mSendCallBack = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (iMqttCallBack != null) {
                        iMqttCallBack.onActionSuccess(ACTION_PUBLISH, asyncActionToken);
                    }

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if (iMqttCallBack != null) {
                        iMqttCallBack.onActionFailure(ACTION_PUBLISH, asyncActionToken, exception);
                    }
                }
            };
        }

        try {
            client.publish(topic, mqttMessage, null, mSendCallBack);
        } catch (Exception e) {
            if (iMqttCallBack != null) {
                iMqttCallBack.onActionFailure(ACTION_PUBLISH, e);
            }
            e.printStackTrace();
        }
    }

    public void subscribe(String topic, int qos) {
        if (client == null || !client.isConnected()) {
            return;
        }
        try {
            client.unsubscribe(topic);

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            client.subscribe(topic, qos, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (iMqttCallBack != null) {
                        iMqttCallBack.onActionSuccess(ACTION_SUBSCRIBE, asyncActionToken);
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if (iMqttCallBack != null) {
                        iMqttCallBack.onActionFailure(ACTION_SUBSCRIBE, asyncActionToken, exception);
                    }
                }
            });

        } catch (MqttException e) {
            if (iMqttCallBack != null) {
                iMqttCallBack.onActionFailure(ACTION_SUBSCRIBE, e);
            }
            e.printStackTrace();
        }
    }
    public void subscribe(String deviceId) {
        if (client == null || !client.isConnected()) {
            return;
        }
        String topic = "user/" + deviceId;
        try {
            client.unsubscribe(topic);

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            client.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (iMqttCallBack != null) {
                        iMqttCallBack.onActionSuccess(ACTION_SUBSCRIBE, asyncActionToken);
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if (iMqttCallBack != null) {
                        iMqttCallBack.onActionFailure(ACTION_SUBSCRIBE, asyncActionToken, exception);
                    }
                }
            });

        } catch (MqttException e) {
            if (iMqttCallBack != null) {
                iMqttCallBack.onActionFailure(ACTION_SUBSCRIBE, e);
            }
            e.printStackTrace();
        }
    }
    public boolean isConnned() {
        return client != null && client.isConnected();
    }

    public MqttAndroidClient getClient() {
        return client;
    }

    public void release() throws MqttException {
        isInit = false;
        if (client !=null && client.isConnected()) {
            client.disconnect();
        }
        client = null;
        iMqttCallBack = null;
    }

}