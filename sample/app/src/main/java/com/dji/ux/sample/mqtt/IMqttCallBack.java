package com.dji.ux.sample.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface IMqttCallBack {
    void onActionSuccess(int action, IMqttToken asyncActionToken);

    void onActionFailure(int action, IMqttToken asyncActionToken, Throwable exception);

    void onActionFailure(int action, Exception e);
    //连接中断
    void connectionLost(Throwable cause);
    //服务器发来的消息，需要订阅
    void messageArrived(String topic, MqttMessage message);
    //发布消息送达
    void deliveryComplete(IMqttDeliveryToken token);
}