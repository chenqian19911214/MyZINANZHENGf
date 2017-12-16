package com.example.chenq.myzinanzheng;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;
import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.Trace;
import com.baidu.trace.model.OnTraceListener;
import com.baidu.trace.model.PushMessage;

import java.util.List;
import java.util.UUID;

/**
 * chenqian UUID
 */
public class MainActivity extends Activity implements SensorEventListener, View.OnClickListener {

    private SensorManager manager;
    private TextView mtextview, textViewjd, textViewhp, textxinsi, text_extedid2;
    private LinearLayout lineat;
    private int value = 0;
    private String JD = "";
    private Sensor sensor;
    private ImageView mimageView;
    // private AnimationSet mAnimationSet;
    private RelativeLayout relativeLayout;
    public LocationClient mLocationClient = null;
    private long serviceID = 150116;
    private String entityName;
    boolean isNeedObjectStorage = false;
    // 初始化轨迹服务
    private Trace mTrace;
    // 初始化轨迹服务客户端
    private LBSTraceClient mTraceClient;

    // 定位周期(单位:秒)
    private int gatherInterval = 5;
    // 打包回传周期(单位:秒)
    private int packInterval = 10;
   private int Surpport = 0;
    private StringBuffer sb1;
    private Handler mHander = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    int du = msg.arg1;
                    String jd = (String) msg.obj;
                    mtextview.setText(jd + du + "");
                    break;
                case 2:
                    String xian = msg.obj.toString();
                    if (textxinsi!=null) {
                        textxinsi.setText(xian);
                    }
                    break;
                default:
            }
        }
    };
    private TextView tv_message;

    /**
     * 获取到设备id
     */
    private String getUniquePsuedoID() {

        String m_szDevIDShort = "35" + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10) + (Build.CPU_ABI.length() % 10) + (Build.DEVICE.length() % 10) + (Build.MANUFACTURER.length() % 10) + (Build.MODEL.length() % 10) + (Build.PRODUCT.length() % 10);
        String serial = null;
        try {
            serial = android.os.Build.class.getField("SERIAL").get(null).toString();
            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        } catch (Exception e) {

            serial = "serial"; // some value
        }
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mtextview = findViewById(R.id.edittext);
        mimageView = findViewById(R.id.imageview);
        textViewjd = findViewById(R.id.edittextjd);
        relativeLayout = findViewById(R.id.chuangku);
        textViewhp = findViewById(R.id.edittexthp);
        textxinsi = findViewById(R.id.text_extedid);
        text_extedid2 = findViewById(R.id.text_extedid2);
        lineat = findViewById(R.id.chuangkus);
        entityName = getUniquePsuedoID();
        float width = relativeLayout.getX();
        relativeLayout.setY(width);
        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mimageView.setKeepScreenOn(true);//屏幕高亮
        sensor = manager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        if (sensor == null) {
            mtextview.setText("此手机没有方向传感器");
            mtextview.setTextSize(20);
        } else {

            //  sensor = manager.getDefaultSensor(sensor.TYPE);
            //调用方向传感器
            manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 123);
        }
        mLocationClient = new LocationClient(getApplicationContext());

        mTrace = new Trace(serviceID, entityName, isNeedObjectStorage);

        mTraceClient = new LBSTraceClient(getApplicationContext());
        // 设置定位和打包周期
        mTraceClient.setInterval(gatherInterval, packInterval);
        initLocation();
        //声明LocationClient类
        mLocationClient.registerLocationListener(mListener);
        //注册监听函数

        mTraceClient.setOnTraceListener(new MyOnTraceListener());
        // 开启服务
        mTraceClient.startTrace(mTrace, new MyOnTraceListener());
        // 开启采集
        mTraceClient.startGather(new MyOnTraceListener());
        mLocationClient.start();
        //      mAnimationSet = new AnimationSet(true);
        List<Sensor> list = manager.getSensorList(Sensor.TYPE_ALL);

        Log.i("chen", "硬件列表 ： " + list.size());
        for (int i = 0; i < list.size(); i++) {

            Log.i("chen", "硬件名字 ： " + list.get(i).getName());
            Log.i("chen", "硬件类型 ： " + list.get(i).getType());
        }


        relativeLayout.setOnClickListener(this);
        lineat.setOnClickListener(this);
        textxinsi.setMovementMethod(ScrollingMovementMethod.getInstance());

        if (!isOPenGPS(getApplication())) {
            showNormalDialog();
        }
    }

    private void showDialog(StringBuffer info){

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.show();

        Window window = alertDialog.getWindow();
        window.setContentView(R.layout.textlayout);
        float pwidth= getApplication().getResources().getDisplayMetrics().widthPixels;
        WindowManager.LayoutParams layoutParams =window.getAttributes();

        window.setAttributes(layoutParams);
        TextView tv_title =  window.findViewById(R.id.app_name_weiz);
        tv_title.setText("详细信息");
        tv_message = window.findViewById(R.id.textlingid);
        tv_message.setMovementMethod(ScrollingMovementMethod.getInstance());
        tv_message.setText(info);
    }
    /**
     * @setIcon 设置对话框图标
     * @setTitle 设置对话框标题
     * @setMessage 设置对话框消息提示
     * setXXX方法返回Dialog对象，因此可以链式设置属性
     */
    private void showNormalDialog() {

        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        normalDialog.setIcon(R.drawable.gpsicon);
        normalDialog.setTitle("提示");
        normalDialog.setMessage("你的GPS未打开，请打开GPS");
        normalDialog.setPositiveButton("打开",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openGPS(getApplicationContext());
                    }
                });
        normalDialog.setNegativeButton("取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "为了你的精确位置，请打开GPS", Toast.LENGTH_SHORT).show();
                    }
                });
        // 显示
        normalDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLocationClient = new LocationClient(getApplicationContext());
                initLocation();
                //声明LocationClient类
                mLocationClient.registerLocationListener(mListener);
                //注册监听函数

                mLocationClient.start();
            } else {
                Toast.makeText(this, "请打开定位权限", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initLocation() {

        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备

        option.setCoorType("bd09ll");
        //可选，默认gcj02，设置返回的定位结果坐标系

        int span = 1000;
        option.setScanSpan(span);
        //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的

        option.setIsNeedAddress(true);
        //可选，设置是否需要地址信息，默认不需要

        option.setOpenGps(true);
        //可选，默认false,设置是否使用gps

        option.setLocationNotify(true);
        //可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果

        option.setIsNeedLocationDescribe(true);
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”

        option.setIsNeedLocationPoiList(true);
        //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到

        option.setIgnoreKillProcess(false);
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死

        option.setEnableSimulateGps(false);
        //可选，默认false，设置是否需要过滤GPS仿真结果，默认需要

        // option.setWifiValidTime(5*60*1000);
        //可选，7.2版本新增能力，如果您设置了这个接口，首次启动定位时，会先判断当前WiFi是否超出有效期，超出有效期的话，会先重新扫描WiFi，然后再定位
        mLocationClient.setLocOption(option);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        manager.unregisterListener(this, sensor);
        mLocationClient.stop();
    }

    private float startangle = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {

        float animvaul = event.values[0];
        //获取具体的参数文字部分
        value = (int) event.values[0];

        if (value == 0) {
            JD = "北";
        } else if (0 < value && value < 90) {
            JD = "东北";
        } else if (value == 90) {
            JD = "东";
        } else if (90 < value && value < 180) {
            JD = "东南";
        } else if (value == 180) {
            JD = "南";
        } else if (180 < value && value < 270) {
            JD = "西南";
        } else if (value == 270) {
            JD = "西";
        } else if (270 < value && value < 360) {
            JD = "西北";
        }

        RotateAnimation rotateAnimation = new RotateAnimation(startangle, animvaul, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(3000);
        mimageView.startAnimation(rotateAnimation);
        Message message = new Message();
        message.what=1;
        message.obj = JD;
        message.arg1 = value;
        mHander.sendMessage(message);
        startangle = -animvaul;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
    private BDAbstractLocationListener mListener = new BDAbstractLocationListener() {

        @Override
        public void onReceiveLocation(BDLocation location) {

            SharedPreferences spf =getSharedPreferences("heightData",MODE_PRIVATE);
            SharedPreferences.Editor editor = spf.edit();
            if (null != location && location.getLocType() != BDLocation.TypeServerError) {
                StringBuffer sb = new StringBuffer(256);
                StringBuilder stringBuilder = new StringBuilder();
                if (location.hasAddr()) {

                    stringBuilder.append("经纬度:" + location.getLongitude() + " , " + location.getLatitude() + "  ");
                    textViewjd.setText(location.getAddrStr());
                }
                // *****如果有海拔高度*****
                if (location.hasAltitude()) {
                  double altiude=  location.getAltitude();
                    editor.putString("height",altiude+"");
                    editor.commit();
                }
              //速度
               float speed = location.getSpeed();

                stringBuilder.append("速度:" + speed + " km/h ");
                String height = spf.getString("height","0");
                stringBuilder.append("海拔:" + height + " m ");
                textViewhp.setText(stringBuilder.toString());
            }
            sb1 = new StringBuffer(256);
            //获取定位结果
            sb1.append("定位时间:" + location.getTime());//获取定位时间
            sb1.append("\n定位类型:" + location.getLocType());
            sb1.append("\n纬度信息:" + location.getLatitude());
            sb1.append("\n经度信息:" + location.getLongitude());
            sb1.append("\n定位精准度:" + location.getRadius());
            sb1.append("\n地址信息:" + location.getAddrStr());
            sb1.append("\n国家信息：" + location.getCountry());
            sb1.append("\n国家码：" + location.getCountryCode());
            sb1.append("\n城市信息：" + location.getCity());
            sb1.append("\n城市码：" + location.getCityCode());
            sb1.append("\n区县信息：" + location.getDistrict());
            sb1.append("\n街道信息：" + location.getStreet());
            sb1.append("\n街道码：" + location.getStreetNumber());
            sb1.append("\n当前位置描述信息：" + location.getLocationDescribe());
            List<Poi> poi = location.getPoiList();
            for (int i = 0; i < poi.size(); i++) {
                sb1.append("\n位置周边POI信息：" + poi.get(i).getName());
            }
            sb1.append("\n室内精准定位下，楼宇ID：" + location.getBuildingID());
            sb1.append("\n室内精准定位下，楼宇名称：" + location.getBuildingName());
            sb1.append("\n室内精准定位下，当前楼层信息：" + location.getFloor());

            Surpport = location.getIndoorLocationSurpport();
            sb1.append("\n是否支持室内定位：" + Surpport);

            if (location.getLocType() == BDLocation.TypeGpsLocation) {

                sb1.append("\nGPS定位结果_速度：" + location.getSpeed());
                sb1.append("\nGPS定位结果_卫星数:" + location.getSatelliteNumber());
                sb1.append("\nGPS定位结果_海拔高度:" + location.getAltitude());
                sb1.append("\nGPS定位结果_方向信息:" + location.getDirection());

            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {

                String operators = null;
                switch (location.getOperators()) {
                    case BDLocation.OPERATORS_TYPE_UNKONW: //未知
                        operators = "未知";
                        break;
                    case BDLocation.OPERATORS_TYPE_UNICOM: //中国联通
                        operators = "中国联通";
                        break;
                    case BDLocation.OPERATORS_TYPE_TELECOMU: //中国电信
                        operators = "中国电信";
                        break;
                    case BDLocation.OPERATORS_TYPE_MOBILE: //中国移动
                        operators = "中国移动";
                        break;
                    default:

                }
                sb1.append("\n网络定位_运营商信息:" + operators);
                sb1.append("\n网络定位_通wifi还是GPS得结果:" + location.getNetworkLocationType());

            } else if (location.getLocType() == BDLocation.TypeServerError) {

                sb1.append("\n定位失败");
                //当前网络定位失败
                //可将定位唯一ID、IMEI、定位失败时间反馈至loc-bugs@baidu.com

            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                sb1.append("\n当前网络不通");

            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {

                //当前缺少定位依据，可能是用户没有授权，建议弹出提示框让用户开启权限
                //可进一步参考onLocDiagnosticMessage中的错误返回码

            }
            Message message = new Message();
            message.what=2;
            message.obj = sb1;
            mHander.sendMessage(message);
        }

        /**
         * 回调定位诊断信息，开发者可以根据相关信息解决定位遇到的一些问题
         * 自动回调，相同的diagnosticType只会回调一次
         *
         * @param locType           当前定位类型
         * @param diagnosticType    诊断类型（1~9）
         * @param diagnosticMessage 具体的诊断信息释义
         */
        @Override
        public void onLocDiagnosticMessage(int locType, int diagnosticType, String diagnosticMessage) {
            super.onLocDiagnosticMessage(locType, diagnosticType, diagnosticMessage);

            StringBuffer sb1 = new StringBuffer(256);

            if (diagnosticType == LocationClient.LOC_DIAGNOSTIC_TYPE_BETTER_OPEN_GPS) {

                //建议打开GPS
                sb1.append("\n建议打开GPS");
            } else if (diagnosticType == LocationClient.LOC_DIAGNOSTIC_TYPE_BETTER_OPEN_WIFI) {

                sb1.append("\n建议打开wifi，不必连接，这样有助于提高网络定位精度！");
                //建议打开wifi，不必连接，这样有助于提高网络定位精度！

            } else if (diagnosticType == LocationClient.LOC_DIAGNOSTIC_TYPE_NEED_CHECK_LOC_PERMISSION) {

                //定位权限受限，建议提示用户授予APP定位权限！

                sb1.append("\n定位权限受限，建议提示用户授予APP定位权限！");
            } else if (diagnosticType == LocationClient.LOC_DIAGNOSTIC_TYPE_NEED_CHECK_NET) {

                //网络异常造成定位失败，建议用户确认网络状态是否异常！
                sb1.append("\n网络异常造成定位失败，建议用户确认网络状态是否异常！");
            } else if (diagnosticType == LocationClient.LOC_DIAGNOSTIC_TYPE_NEED_CLOSE_FLYMODE) {

                //手机飞行模式造成定位失败，建议用户关闭飞行模式后再重试定位！
                sb1.append("\n手机飞行模式造成定位失败，建议用户关闭飞行模式后再重试定位！");
            } else if (diagnosticType == LocationClient.LOC_DIAGNOSTIC_TYPE_NEED_INSERT_SIMCARD_OR_OPEN_WIFI) {

                //无法获取任何定位依据，建议用户打开wifi或者插入sim卡重试！
                sb1.append("\n无法获取任何定位依据，建议用户打开wifi或者插入sim卡重试！");
            } else if (diagnosticType == LocationClient.LOC_DIAGNOSTIC_TYPE_NEED_OPEN_PHONE_LOC_SWITCH) {

                //无法获取有效定位依据，建议用户打开手机设置里的定位开关后重试！
                sb1.append("\n无法获取有效定位依据，建议用户打开手机设置里的定位开关后重试！");
            } else if (diagnosticType == LocationClient.LOC_DIAGNOSTIC_TYPE_SERVER_FAIL) {
                sb1.append("\n百度定位服务端定位失败");
                //百度定位服务端定位失败
                //建议反馈location.getLocationID()和大体定位时间到loc-bugs@baidu.com

            } else if (diagnosticType == LocationClient.LOC_DIAGNOSTIC_TYPE_FAIL_UNKNOWN) {
                sb1.append("\n无法获取有效定位依据，但无法确定具体原因");
                //无法获取有效定位依据，但无法确定具体原因
                //建议检查是否有安全软件屏蔽相关定位权限
                //或调用LocationClient.restart()重新启动后重试！
            }

            text_extedid2.setText(sb1);
        }
    };

    /**
     * 判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
     *
     * @param context
     * @return true 表示开启
     */
    public static final boolean isOPenGPS(final Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps && network) {
            return true;
        }
        return false;
    }

    /**
     * 强制帮用户打开GPS
     *
     * @param context
     */
    public final void openGPS(Context context) {
        // 转到手机设置界面，用户设置GPS
        Intent intent = new Intent(
                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(intent, 0); // 设置完成后返回到原来的界面
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(requestCode+"",resultCode+"");
        if (requestCode==0){
           if (!isOPenGPS(getApplicationContext())){
               Toast.makeText(this, "请打开GPS", Toast.LENGTH_SHORT).show();
           }else {
               mLocationClient.restart();
           }
        }
    }

    /**
     * 监听事件
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.chuangku:
                if (Surpport != 0) {
                    mLocationClient.restart();
                    mLocationClient.startIndoorMode();
                } else {
                    mLocationClient.restart();

                }
                Toast.makeText(this, "重新定位成功", Toast.LENGTH_SHORT).show();

                break;
            case R.id.chuangkus:
                if (sb1!=null){
                    showDialog(sb1);
                }

                break;
            default:
        }
    }

    class MyOnTraceListener implements OnTraceListener {

        //绑定服务回调
        @Override
        public void onBindServiceCallback(int i, String s) {

            Toast.makeText(MainActivity.this,"绑定服务回调:"+s, Toast.LENGTH_SHORT).show();
        }

        // 开启服务回调
        @Override
        public void onStartTraceCallback(int i, String s) {
            Toast.makeText(MainActivity.this, "开启服务回调:"+s, Toast.LENGTH_SHORT).show();
        }

        // 停止服务回调
        @Override
        public void onStopTraceCallback(int i, String s) {
            Toast.makeText(MainActivity.this, "停止服务回调:"+s, Toast.LENGTH_SHORT).show();
        }

        // 开启采集回调
        @Override
        public void onStartGatherCallback(int i, String s) {
            Toast.makeText(MainActivity.this, "开启采集回调:"+s, Toast.LENGTH_SHORT).show();

        }

        // 停止采集回调
        @Override
        public void onStopGatherCallback(int i, String s) {
         Toast.makeText(MainActivity.this,"停止采集回调:"+s,Toast.LENGTH_LONG);
        }

        // 推送回调
        @Override
        public void onPushCallback(byte b, PushMessage pushMessage) {

            Toast.makeText(MainActivity.this, "推送回调:", Toast.LENGTH_SHORT).show();

        }
    }
}
