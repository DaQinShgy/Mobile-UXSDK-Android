package com.dji.ux.sample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.dji.mapkit.core.maps.DJIMap;
import com.dji.ux.sample.mqtt.IMqttCallBack;
import com.dji.ux.sample.mqtt.SmartMqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import androidx.annotation.NonNull;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.util.CommonCallbacks;
import dji.keysdk.CameraKey;
import dji.keysdk.KeyManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.ux.panel.CameraSettingAdvancedPanel;
import dji.ux.panel.CameraSettingExposurePanel;
import dji.ux.utils.DJIProductUtil;
import dji.ux.widget.FPVOverlayWidget;
import dji.ux.widget.FPVWidget;
import dji.ux.widget.MapWidget;
import dji.ux.widget.ThermalPaletteWidget;
import dji.ux.widget.config.CameraConfigApertureWidget;
import dji.ux.widget.config.CameraConfigEVWidget;
import dji.ux.widget.config.CameraConfigISOAndEIWidget;
import dji.ux.widget.config.CameraConfigSSDWidget;
import dji.ux.widget.config.CameraConfigShutterWidget;
import dji.ux.widget.config.CameraConfigStorageWidget;
import dji.ux.widget.config.CameraConfigWBWidget;
import dji.ux.widget.controls.CameraControlsWidget;
import dji.ux.widget.controls.LensControlWidget;

import static com.dji.ux.sample.mqtt.SmartMqtt.ACTION_CONNECT;

/**
 * Activity that shows all the UI elements together
 */
public class CompleteWidgetActivity extends Activity {

    private MapWidget mapWidget;
    private ViewGroup parentView;
    private FPVWidget fpvWidget;
    private FPVWidget secondaryFPVWidget;
    private FPVOverlayWidget fpvOverlayWidget;
    private RelativeLayout primaryVideoView;
    private FrameLayout secondaryVideoView;
    private boolean isMapMini = true;

    private CameraSettingExposurePanel cameraSettingExposurePanel;
    private CameraSettingAdvancedPanel cameraSettingAdvancedPanel;
    private CameraConfigISOAndEIWidget cameraConfigISOAndEIWidget;
    private CameraConfigShutterWidget cameraConfigShutterWidget;
    private CameraConfigApertureWidget cameraConfigApertureWidget;
    private CameraConfigEVWidget cameraConfigEVWidget;
    private CameraConfigWBWidget cameraConfigWBWidget;
    private CameraConfigStorageWidget cameraConfigStorageWidget;
    private CameraConfigSSDWidget cameraConfigSSDWidget;
    private CameraControlsWidget controlsWidget;
    private LensControlWidget lensControlWidget;
    private ThermalPaletteWidget thermalPaletteWidget;


    private int height;
    private int width;
    private int margin;
    private int deviceWidth;
    private int deviceHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_widgets);

        height = DensityUtil.dip2px(this, 100);
        width = DensityUtil.dip2px(this, 150);
        margin = DensityUtil.dip2px(this, 12);

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        final Display display = windowManager.getDefaultDisplay();
        Point outPoint = new Point();
        display.getRealSize(outPoint);
        deviceHeight = outPoint.y;
        deviceWidth = outPoint.x;

        mapWidget = (MapWidget) findViewById(R.id.map_widget);
        mapWidget.initAMap(map -> map.setOnMapClickListener((DJIMap.OnMapClickListener) latLng -> onViewClick(mapWidget)));
        mapWidget.onCreate(savedInstanceState);

        initCameraView();
        parentView = (ViewGroup) findViewById(R.id.root_view);

        fpvWidget = findViewById(R.id.fpv_widget);
        fpvWidget.setOnClickListener(view -> onViewClick(fpvWidget));
        fpvOverlayWidget = findViewById(R.id.fpv_overlay_widget);
        primaryVideoView = findViewById(R.id.fpv_container);
        secondaryVideoView = findViewById(R.id.secondary_video_view);
        secondaryFPVWidget = findViewById(R.id.secondary_fpv_widget);
        secondaryFPVWidget.setOnClickListener(view -> swapVideoSource());

        fpvWidget.setCameraIndexListener((cameraIndex, lensIndex) -> cameraWidgetKeyIndexUpdated(fpvWidget.getCameraKeyIndex(), fpvWidget.getLensKeyIndex()));
        updateSecondaryVideoVisibility();

        initSerialNumber();

        SmartMqtt.getInstance().init(this);
        SmartMqtt.getInstance().connect("tcp://mqtt.zjlin123.com:1883", "dji" + UUID.randomUUID().toString());
        SmartMqtt.getInstance().setIMqttCallBack(new IMqttCallBack() {
            @Override
            public void onActionSuccess(int action, IMqttToken asyncActionToken) {
                if (action == ACTION_CONNECT) {
                    initTimer();
                }
            }

            @Override
            public void onActionFailure(int action, IMqttToken asyncActionToken, Throwable exception) {

            }

            @Override
            public void onActionFailure(int action, Exception e) {

            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        String rtmpUrl = getIntent().getStringExtra(MainActivity.LAST_USED_RTMP);
        startLiveShow(rtmpUrl);
    }

    private Timer timer;

    private void initTimer() {
        if (timer != null) {
            return;
        }
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("dataVersion", 1);
                    jsonObject.put("dataCmds", 1);
                    jsonObject.put("uavSn", "0ASUG1B00400HN");
                    JSONObject jsonObjectInner = new JSONObject();
                    jsonObjectInner.put("timestamp", System.currentTimeMillis());
                    jsonObjectInner.put("droneLongitude", mAircraftLng);//无人机经度
                    jsonObjectInner.put("droneLatitude", mAircraftLat);//无人机纬度
                    jsonObjectInner.put("altitude", mAircraftAltitude);//无人机高度
                    jsonObjectInner.put("speed", mSpeed);//无人机飞行速度：单位：米/秒）
                    jsonObjectInner.put("electricQuantity", "");//无人机剩余电量，取值范围（0-100）
                    jsonObjectInner.put("orientation", "");//无人机机头朝向
                    jsonObjectInner.put("seqNum", "");//卫星信号强度
                    jsonObjectInner.put("receivedSeqNum", "");//接收卫星数量
                    jsonObjectInner.put("homeLongitude", mAircraftHomeLng);//无人机返航点经度
                    jsonObjectInner.put("homeLatitude", mAircraftHomeLat);//无人机返航点纬度
                    jsonObjectInner.put("appGPSLongitude", "");//飞手位置经度
                    jsonObjectInner.put("appGPSLatitude", "");//飞手位置纬度
                    jsonObjectInner.put("status", "");//无人机当前状态
                    jsonObject.put("data", jsonObjectInner);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                SmartMqtt.getInstance().sendData(jsonObject.toString(), "uav.dji.status." + serialNumber);
                Log.e("tag", "==========");
            }
        };
        timer.schedule(task, 0, 100);
    }

    private FlightController flightController;
    private String serialNumber = "";
    private double mAircraftLat = 0.0;
    private double mAircraftLng = 0.0;
    private float mAircraftAltitude = 0.0f;
    private float mSpeed = 0.0f;
    private double mAircraftHomeLat = 0.0;
    private double mAircraftHomeLng = 0.0;

    private void initSerialNumber() {
        Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
        if (null != aircraft && null != aircraft.getFlightController()) {
            flightController = aircraft.getFlightController();
            flightController.getSerialNumber(new CommonCallbacks.CompletionCallbackWith<String>() {
                @Override
                public void onSuccess(String s) {
                    serialNumber = s;
                }

                @Override
                public void onFailure(DJIError djiError) {
                    Toast.makeText(CompleteWidgetActivity.this, "getSerialNumber failed: " + djiError.getDescription(), Toast.LENGTH_SHORT).show();
                }
            });
            flightController.setStateCallback(flightControllerState -> {
                mAircraftLat = flightControllerState.getAircraftLocation().getLatitude();
                mAircraftLng = flightControllerState.getAircraftLocation().getLongitude();
                mAircraftAltitude = flightControllerState.getAircraftLocation().getAltitude();
                mAircraftHomeLat = flightControllerState.getHomeLocation().getLatitude();
                mAircraftHomeLng = flightControllerState.getHomeLocation().getLongitude();
            });
            flightController.getCinematicYawSpeed(new CommonCallbacks.CompletionCallbackWith<Float>() {
                @Override
                public void onSuccess(Float aFloat) {
                    mSpeed = aFloat;
                }

                @Override
                public void onFailure(DJIError djiError) {

                }

            });
        }
    }

    private void startLiveShow(String liveShowUrl) {
        if (DJISDKManager.getInstance().getLiveStreamManager().isStreaming()) {
            Toast.makeText(this, "already started!", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread() {
            @Override
            public void run() {
                DJISDKManager.getInstance().getLiveStreamManager().setLiveUrl(liveShowUrl);
                int result = DJISDKManager.getInstance().getLiveStreamManager().startStream();
                DJISDKManager.getInstance().getLiveStreamManager().setStartTime();

                Log.e("live", "startLive:" + result +
                        "\n isVideoStreamSpeedConfigurable:" + DJISDKManager.getInstance().getLiveStreamManager().isVideoStreamSpeedConfigurable() +
                        "\n isLiveAudioEnabled:" + DJISDKManager.getInstance().getLiveStreamManager().isLiveAudioEnabled());
            }
        }.start();
    }

    private void initCameraView() {
        cameraSettingExposurePanel = findViewById(R.id.camera_setting_exposure_panel);
        cameraSettingAdvancedPanel = findViewById(R.id.camera_setting_advanced_panel);
        cameraConfigISOAndEIWidget = findViewById(R.id.camera_config_iso_and_ei_widget);
        cameraConfigShutterWidget = findViewById(R.id.camera_config_shutter_widget);
        cameraConfigApertureWidget = findViewById(R.id.camera_config_aperture_widget);
        cameraConfigEVWidget = findViewById(R.id.camera_config_ev_widget);
        cameraConfigWBWidget = findViewById(R.id.camera_config_wb_widget);
        cameraConfigStorageWidget = findViewById(R.id.camera_config_storage_widget);
        cameraConfigSSDWidget = findViewById(R.id.camera_config_ssd_widget);
        lensControlWidget = findViewById(R.id.camera_lens_control);
        controlsWidget = findViewById(R.id.CameraCapturePanel);
        thermalPaletteWidget = findViewById(R.id.thermal_pallette_widget);
    }

    private void onViewClick(View view) {
        if (view == fpvWidget && !isMapMini) {
            resizeFPVWidget(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0);
            reorderCameraCapturePanel();
            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, deviceWidth, deviceHeight, width, height, margin);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = true;
        } else if (view == mapWidget && isMapMini) {
            hidePanels();
            resizeFPVWidget(width, height, margin, 12);
            reorderCameraCapturePanel();
            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, width, height, deviceWidth, deviceHeight, 0);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = false;
        }
    }

    private void resizeFPVWidget(int width, int height, int margin, int fpvInsertPosition) {
        RelativeLayout.LayoutParams fpvParams = (RelativeLayout.LayoutParams) primaryVideoView.getLayoutParams();
        fpvParams.height = height;
        fpvParams.width = width;
        fpvParams.rightMargin = margin;
        fpvParams.bottomMargin = margin;
        if (isMapMini) {
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        } else {
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        }
        primaryVideoView.setLayoutParams(fpvParams);

        parentView.removeView(primaryVideoView);
        parentView.addView(primaryVideoView, fpvInsertPosition);
    }

    private void reorderCameraCapturePanel() {
        View cameraCapturePanel = findViewById(R.id.CameraCapturePanel);
        parentView.removeView(cameraCapturePanel);
        parentView.addView(cameraCapturePanel, isMapMini ? 9 : 13);
    }

    private void swapVideoSource() {
        if (secondaryFPVWidget.getVideoSource() == FPVWidget.VideoSource.SECONDARY) {
            fpvWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
            secondaryFPVWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
        } else {
            fpvWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
            secondaryFPVWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
        }
    }

    private void cameraWidgetKeyIndexUpdated(int keyIndex, int subKeyIndex) {
        controlsWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraSettingExposurePanel.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraSettingAdvancedPanel.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraConfigISOAndEIWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraConfigShutterWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraConfigApertureWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraConfigEVWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraConfigWBWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraConfigStorageWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        cameraConfigSSDWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        controlsWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        lensControlWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
        thermalPaletteWidget.updateKeyOnIndex(keyIndex, subKeyIndex);

        fpvOverlayWidget.updateKeyOnIndex(keyIndex, subKeyIndex);
    }

    private void updateSecondaryVideoVisibility() {
        if (secondaryFPVWidget.getVideoSource() == null || !DJIProductUtil.isSupportMultiCamera()) {
            secondaryVideoView.setVisibility(View.GONE);
        } else {
            secondaryVideoView.setVisibility(View.VISIBLE);
        }
    }

    private void hidePanels() {
        //These panels appear based on keys from the drone itself.
        if (KeyManager.getInstance() != null) {
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.HISTOGRAM_ENABLED, fpvWidget.getCameraKeyIndex()), false, null);
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.COLOR_WAVEFORM_ENABLED, fpvWidget.getCameraKeyIndex()), false, null);
        }

        //These panels have buttons that toggle them, so call the methods to make sure the button state is correct.
        controlsWidget.setAdvancedPanelVisibility(false);
        controlsWidget.setExposurePanelVisibility(false);

        //These panels don't have a button state, so we can just hide them.
        findViewById(R.id.pre_flight_check_list).setVisibility(View.GONE);
        findViewById(R.id.rtk_panel).setVisibility(View.GONE);
        //findViewById(R.id.simulator_panel).setVisibility(View.GONE);
        findViewById(R.id.spotlight_panel).setVisibility(View.GONE);
        findViewById(R.id.speaker_panel).setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Hide both the navigation bar and the status bar.
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        mapWidget.onResume();
    }

    @Override
    protected void onPause() {
        mapWidget.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapWidget.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapWidget.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapWidget.onLowMemory();
    }

    private class ResizeAnimation extends Animation {

        private View mView;
        private int mToHeight;
        private int mFromHeight;

        private int mToWidth;
        private int mFromWidth;
        private int mMargin;

        private ResizeAnimation(View v, int fromWidth, int fromHeight, int toWidth, int toHeight, int margin) {
            mToHeight = toHeight;
            mToWidth = toWidth;
            mFromHeight = fromHeight;
            mFromWidth = fromWidth;
            mView = v;
            mMargin = margin;
            setDuration(300);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height = (mToHeight - mFromHeight) * interpolatedTime + mFromHeight;
            float width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth;
            RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) mView.getLayoutParams();
            p.height = (int) height;
            p.width = (int) width;
            p.rightMargin = mMargin;
            p.bottomMargin = mMargin;
            mView.requestLayout();
        }
    }
}
