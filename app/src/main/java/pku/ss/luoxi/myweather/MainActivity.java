package pku.ss.luoxi.myweather;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;
import pku.ss.luoxi.app.MyApplication;
import pku.ss.luoxi.app.MyFragment;
import pku.ss.luoxi.app.MyService;
import pku.ss.luoxi.bean.FutureWeather;
import pku.ss.luoxi.bean.TodayWeather;
import pku.ss.luoxi.util.MyLocationListener;
import pku.ss.luoxi.util.NetUtil;
import pku.ss.luoxi.util.ParseXML;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.BDNotifyListener;//假如用到位置提醒功能，需要import该类
import com.baidu.location.Poi;
import com.umeng.analytics.MobclickAgent;

/**
 * Created by admin on 2016/9/20.
 */
public class MainActivity extends FragmentActivity implements View.OnClickListener{
    public LocationClient mLocationClient = null;
    public BDLocationListener myListener = new MyLocationListener();

    private static final int UPDATE_TODAY_WEATHER = 1;
    private static final int UPDATE_FUTURE_WEATHER = 2;
    private ImageView mUpdateBtn,mLocationBtn,mShareBtn;
    private ImageView mCitySelect;
    private ProgressBar mTitleUpdateProgress;
    private TextView cityTv,timeTv,humidityTv,weekTv,pmDataTv,pmQualityTv,temperatureTv,climateTv,windTv,city_name_Tv,temp_now_Tv,textView;
    private ImageView weatherImg,pmImg;
    public static final String ACTION_SERVICE_UPDATE = "action.serviceUpdate";
    private UpdateBroadcastReceiver broadcastReceiver;

    private SharedPreferences sharedPreferences;
    private NewFragmentPageAdapter nfpAdapter;
    private ViewPager vp;
    private List<Fragment> fragments;

    private String shareText = "";
    private ImageView[] dots;
    private int[] ids = {R.id.iv4,R.id.iv5};

    private String cityName;
    //更新后的cityCode
    private String newCityCode = "101010100";
    private Handler mHandler = new Handler(){
        public void handleMessage(android.os.Message msg){
            switch (msg.what){
                case UPDATE_TODAY_WEATHER:
                    updateTodayWeather((TodayWeather) msg.obj);
                    break;
                case UPDATE_FUTURE_WEATHER:
                    //initAfterView((List<FutureWeather>) msg.obj);
                    updateFutureWeather((List<FutureWeather>) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_info);

        //集成调试
        MobclickAgent.setDebugMode( true );
        //分享
        ShareSDK.initSDK(this);
        mShareBtn = (ImageView) findViewById(R.id.title_share);
        mShareBtn.setOnClickListener(this);

        mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类
        mLocationClient.registerLocationListener( myListener );    //注册监听函数
        initLocation();


        //sharedPreferences判断加初始化
        initSharedPreference();

        mTitleUpdateProgress = (ProgressBar) findViewById(R.id.title_update_progress);
        mUpdateBtn = (ImageView) findViewById(R.id.title_update_btn);
        mUpdateBtn.setOnClickListener(this);

        mLocationBtn = (ImageView) findViewById(R.id.title_location);
        mLocationBtn.setOnClickListener(this);

        if(NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
            Log.d("myWeather","网络OK");
            Toast.makeText(MainActivity.this,"网络OK！",Toast.LENGTH_LONG).show();
        }else {
            Log.d("myWeather","网络挂了");
            Toast.makeText(MainActivity.this,"网络挂了！",Toast.LENGTH_LONG).show();
        }

        mCitySelect = (ImageView) findViewById(R.id.title_city_manager);
        mCitySelect.setOnClickListener(this);
        initView();
        initAfterView(null);
        //注册广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SERVICE_UPDATE);
        broadcastReceiver = new UpdateBroadcastReceiver();
        registerReceiver(broadcastReceiver, filter);

        // 启动服务
        startService(new Intent(this,MyService.class));
    }

    //sharedPreferences判断加初始化
    private void initSharedPreference(){
        //记住上次退出应用之前选择的城市
        sharedPreferences = getSharedPreferences("config",MODE_PRIVATE);
        String cityCode = sharedPreferences.getString("main_city_code","");
        Log.d("test1",cityCode);
        if(!cityCode.isEmpty()){
            newCityCode = cityCode;
        }
    }
    //定义广播接收器
    private class UpdateBroadcastReceiver extends BroadcastReceiver {
         @Override
        public void onReceive(Context context, Intent intent) {
             Log.d("MyApp:","定时更新");
            queryWeatherCode(newCityCode);
             //textView.setText(String.valueOf(intent.getExtras().getInt("count")));
        }

    }
    //停止服务
    @Override
    protected void onDestroy() {
        stopService(new Intent(MainActivity.this, MyService.class));//停止更新时间服务
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }
    //初始化页面控件
    void initView(){
        city_name_Tv = (TextView) findViewById(R.id.title_city_name);
        cityTv = (TextView) findViewById(R.id.city);
        timeTv = (TextView) findViewById(R.id.time);
        humidityTv = (TextView) findViewById(R.id.humidity);
        weekTv = (TextView) findViewById(R.id.week_today);
        pmDataTv = (TextView) findViewById(R.id.pm_data);
        pmQualityTv = (TextView) findViewById(R.id.pm2_5_quality);
        pmImg = (ImageView) findViewById(R.id.pm2_5_img);
        temperatureTv = (TextView) findViewById(R.id.temperature);
        climateTv = (TextView) findViewById(R.id.climate);
        windTv = (TextView) findViewById(R.id.wind);
        weatherImg = (ImageView) findViewById(R.id.weather_img);
        temp_now_Tv = (TextView) findViewById(R.id.temperature_now);

        cityTv.setText("N/A");
        timeTv.setText("N/A");
        humidityTv.setText("N/A");
        weekTv.setText("N/A");
        pmDataTv.setText("N/A");
        pmQualityTv.setText("N/A");
        temperatureTv.setText("N/A");
        climateTv.setText("N/A");
        windTv.setText("N/A");
        city_name_Tv.setText("N/A");
        temp_now_Tv.setText("N/A");
        //textView = (TextView) findViewById(R.id.textView);
    }

    void initAfterView(List<FutureWeather> list){
        dots = new ImageView[2];
        for(int i=0;i<2;i++){
            dots[i] = (ImageView) findViewById(ids[i]);
        }
        fragments=new ArrayList<Fragment>();
        List<FutureWeather> listOne = new ArrayList<FutureWeather>();
        List<FutureWeather> listTwo = new ArrayList<FutureWeather>();
        if(list != null){
            for(int i=0;i<list.size();i++){
                if(i<3){
                    listOne.add(list.get(i));
                }else{
                    listTwo.add(list.get(i));
                }
            }
        }

        Log.d("Test",listOne.toString());
        Log.d("Test",listTwo.toString());
        vp = (ViewPager) findViewById(R.id.main_viewpager);
        for (int i=0;i<2;i++){
            Bundle bundle=new Bundle();
            if(i==0){
                bundle.putSerializable("list", (Serializable) listOne);
            }else{
                bundle.putSerializable("list", (Serializable) listTwo);
            }
            MyFragment myFragment=new MyFragment();
            myFragment.setArguments(bundle);
            fragments.add(myFragment);
        }
        nfpAdapter = new NewFragmentPageAdapter(getSupportFragmentManager(),fragments);
        vp.setAdapter(nfpAdapter);

        vp.setOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageSelected(int index)//设置标题的颜色以及下划线的移动效果
            {
                for(int i=0;i<ids.length;i++){
                    if(i==index){
                        dots[i].setImageResource(R.drawable.page_indicator_focused_red);
                    }else {
                        dots[i].setImageResource(R.drawable.page_indicator_unfocused_red);
                    }
                }
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2)
            {
            }

            @Override
            public void onPageScrollStateChanged(int index)
            {
            }
        });  ;
    }

    void updateFutureWeather(List<FutureWeather> list){
        // 可以删除这段代码看看，数据源更新而viewpager不更新的情况
        // 在数据源更新前增加的代码，将上一次数据源的fragment对象从FragmentManager中删除
        if (vp.getAdapter() != null) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            List<Fragment> fragments = fm.getFragments();
            if(fragments != null && fragments.size() >0){
                for (int i = 0; i < fragments.size(); i++) {
                    ft.remove(fragments.get(i));
                }
            }
            ft.commit();
        }
        // End
        List<FutureWeather> listOne = new ArrayList<FutureWeather>();
        List<FutureWeather> listTwo = new ArrayList<FutureWeather>();
        for(int i=0;i<list.size();i++){
            if(i<3){
                listOne.add(list.get(i));
            }else{
                listTwo.add(list.get(i));
            }
        }

        fragments.clear();
        for (int i=0;i<2;i++){
            Bundle bundle=new Bundle();
            if(i==0){
                bundle.putSerializable("list", (Serializable) listOne);
            }else{
                bundle.putSerializable("list", (Serializable) listTwo);
            }
            MyFragment myFragment=new MyFragment();
            myFragment.setArguments(bundle);
            fragments.add(myFragment);
        }
        // 重写adapter的notifyDataChanged方法
        nfpAdapter.notifyDataSetChanged();
    }
    //更新图片
    void updateTodayImg(TodayWeather todayWeather){
        //对pm进行判断
        if(todayWeather.getPm25()!=null){
            int pm25 = Integer.parseInt(todayWeather.getPm25());
            if(pm25>=0&&pm25<=50){
                pmImg.setImageResource(R.drawable.biz_plugin_weather_0_50);
            }else if(pm25<=100){
                pmImg.setImageResource(R.drawable.biz_plugin_weather_51_100);
            }else if(pm25<=150){
                pmImg.setImageResource(R.drawable.biz_plugin_weather_101_150);
            }else if(pm25<=200){
                pmImg.setImageResource(R.drawable.biz_plugin_weather_151_200);
            }else if(pm25<=300){
                pmImg.setImageResource(R.drawable.biz_plugin_weather_201_300);
            }else {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_greater_300);
            }
        }

        String weather = todayWeather.getType();

        switch (weather){
            case "暴雪":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_baoxue);
                break;
            case "暴雨":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_baoyu);
                break;
            case "大暴雨":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_dabaoyu);
                break;
            case "大雪":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_daxue);
                break;
            case "大雨":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_dayu);
                break;
            case "多云":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_duoyun);
                break;
            case "雷阵雨":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_leizhenyu);
                break;
            case "晴":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_qing);
                break;
            case "沙尘暴":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_shachenbao);
                break;
            case "特大暴雨":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_tedabaoyu);
                break;
            case "雾":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_wu);
                break;
            case "小雪":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_xiaoxue);
                break;
            case "小雨":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_xiaoyu);
                break;
            case "阴":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_yin);
                break;
            case "雨夹雪":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhenxue);
                break;
            case "阵雪":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_baoxue);
                break;
            case "阵雨":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhenyu);
                break;
            case "中雪":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhongxue);
                break;
            case "中雨":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhongyu);
                break;
            case "晴转霾":
                weatherImg.setImageResource(R.drawable.biz_plugin_weather_qing);
                break;
        }
    }

    //更新其他信息
    void updateTodayWeather(TodayWeather todayWeather){
        city_name_Tv.setText(todayWeather.getCity()+"天气");
        cityTv.setText(todayWeather.getCity());
        timeTv.setText(todayWeather.getUpdatetime()+"发布");
        humidityTv.setText("湿度："+todayWeather.getShidu());
        if(todayWeather.getPm25()==null){
            pmDataTv.setText("N/A");
            Log.d("pm","success");
        }else{
            pmDataTv.setText(todayWeather.getPm25());
        }
        if(todayWeather.getQuality()==null){
            pmQualityTv.setText("N/A");
        }else{
            pmQualityTv.setText(todayWeather.getQuality());
        }

        weekTv.setText(todayWeather.getDate());
        temperatureTv.setText(todayWeather.getHigh()+"~"+todayWeather.getLow());
        climateTv.setText(todayWeather.getType());
        windTv.setText("风力:"+todayWeather.getFengli());
        temp_now_Tv.setText(todayWeather.getWendu()+"℃");
        updateTodayImg(todayWeather);
        //更新完成显示
        mUpdateBtn.setVisibility(View.VISIBLE);
        mTitleUpdateProgress.setVisibility(View.INVISIBLE);

        shareText = "当前"+todayWeather.getCity()+"气温"+todayWeather.getWendu()+"℃"+"，最高气温"+todayWeather.getHigh()+"，最低气温"+todayWeather.getLow()+"，风力:"+todayWeather.getFengli();
        Toast.makeText(MainActivity.this,"更新成功",Toast.LENGTH_SHORT).show();
    }
    //根据cityCode，获取城市天气信息
    private  void queryWeatherCode(String cityCode){
        final String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey="+cityCode;
        final String futureAddress = "http://api.k780.com:88/?app=weather.future&weaid="+cityCode+"&appkey=10003&sign=b59bc3ef6191eb9f747dd4e83c99f2a4&format=xml"+cityCode;
        Log.d("myWeather",address);
        new Thread(new Runnable() {
            @Override
            public void run() {
                TodayWeather todayWeather = null;
                ParseXML parseXML = new ParseXML();
                try {
                    String responseStr = parseXML.readData(address);
                    String responseFutureStr = parseXML.readData(futureAddress);
                    List<FutureWeather> list = new ArrayList<FutureWeather>();
                    //获取并解析今日天气以及未来天气
                    list = parseXML.parseFutureJSON(responseFutureStr);
                    if(list != null){
                        Message msg = new Message();
                        msg.what = UPDATE_FUTURE_WEATHER;
                        msg.obj = list;
                        mHandler.sendMessage(msg);
                    }
                    todayWeather = parseXML.parseTodayXML(responseStr);
                    if(todayWeather != null){
                        Log.d("myWeather",todayWeather.toString());

                        Message msg = new Message();
                        msg.what = UPDATE_TODAY_WEATHER;
                        msg.obj = todayWeather;
                        mHandler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public class MyLocationListener implements BDLocationListener{
        @Override
        public void onReceiveLocation(BDLocation location) {
            //Receive Location
            StringBuffer sb = new StringBuffer(256);
            sb.append("time : ");
            sb.append(location.getTime());
            sb.append("\nerror code : ");
            sb.append(location.getLocType());
            sb.append("\nlatitude : ");
            sb.append(location.getLatitude());
            sb.append("\nlontitude : ");
            sb.append(location.getLongitude());
            sb.append("\nradius : ");
            sb.append(location.getRadius());
            if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
                sb.append("\nspeed : ");
                sb.append(location.getSpeed());// 单位：公里每小时
                sb.append("\nsatellite : ");
                sb.append(location.getSatelliteNumber());
                sb.append("\nheight : ");
                sb.append(location.getAltitude());// 单位：米
                sb.append("\ndirection : ");
                sb.append(location.getDirection());// 单位度
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                sb.append("\ndescribe : ");
                sb.append("gps定位成功");

            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                //运营商信息
                sb.append("\noperationers : ");
                sb.append(location.getOperators());
                sb.append("\ndescribe : ");
                sb.append("网络定位成功");
            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
                sb.append("\ndescribe : ");
                sb.append("离线定位成功，离线定位结果也是有效的");
            } else if (location.getLocType() == BDLocation.TypeServerError) {
                sb.append("\ndescribe : ");
                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                sb.append("\ndescribe : ");
                sb.append("网络不同导致定位失败，请检查网络是否通畅");
            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                sb.append("\ndescribe : ");
                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
            }
            sb.append("\nlocationdescribe : ");
            sb.append(location.getLocationDescribe());// 位置语义化信息
            List<Poi> list = location.getPoiList();// POI数据
            if (list != null) {
                sb.append("\npoilist size = : ");
                sb.append(list.size());
                for (Poi p : list) {
                    sb.append("\npoi= : ");
                    sb.append(p.getId() + " " + p.getName() + " " + p.getRank());
                }
            }
            cityName = location.getCity();
            String code = MyApplication.getInstance().getCityCodeByName(cityName);
            Toast.makeText(MainActivity.this,"当前地点："+cityName,Toast.LENGTH_LONG).show();
            queryWeatherCode(code);
            mLocationClient.stop();
            //Log.i("BaiduLocationApiDem", sb.toString());
        }
    }
    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy
        );//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        int span = 1000;
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        mLocationClient.setLocOption(option);
    }
    //点击事件
    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.title_city_manager) {
            //统计点击获取城市列表次数
            MobclickAgent.onEvent(getApplicationContext(),"list");
            Intent i = new Intent(this,SelectCity.class);
            i.putExtra("cityCode",newCityCode);
            //startActivity(i);
            startActivityForResult(i,1);
        }
        if(v.getId() == R.id.title_update_btn){
            //统计点击刷新按钮次数
            MobclickAgent.onEvent(getApplicationContext(),"update");
            //将刷新按钮设为不可见;
            mTitleUpdateProgress.setVisibility(View.VISIBLE);
            mUpdateBtn.setVisibility(View.INVISIBLE);
            sharedPreferences = getSharedPreferences("config",MODE_PRIVATE);
            String cityCode = sharedPreferences.getString("main_city_code",newCityCode);
            Log.d("myWeather1",cityCode);

            if(NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
                Log.d("myWeather","网络OK");
                queryWeatherCode(cityCode);
            }else {
                Log.d("myWeather","网络挂了");
                Toast.makeText(MainActivity.this,"网络挂了！",Toast.LENGTH_LONG).show();
            }
        }
        if(v.getId() == R.id.title_location) {
            //统计点击定位按钮次数
            MobclickAgent.onEvent(getApplicationContext(),"location");
            mLocationClient.start();
        }
        if(v.getId() == R.id.title_share) {
            //统计点击定位按钮次数
            MobclickAgent.onEvent(getApplicationContext(),"share");
            showShare();
        }
    }
    //接收返回的数据
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1 && resultCode == RESULT_OK) {
            newCityCode=data.getStringExtra("cityCode");

            sharedPreferences = getSharedPreferences("config",MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("main_city_code",newCityCode);
            editor.commit();

            Log.d("myWeather","选择的城市代码为"+newCityCode);

            if(NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
                Log.d("myWeather","网络OK");
                queryWeatherCode(newCityCode);
            }else {
                Log.d("myWeather","网络挂了");
                Toast.makeText(MainActivity.this,"网络挂了！",Toast.LENGTH_LONG).show();
            }
        }
    }

    //收集数据
    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    private void showShare() {
        OnekeyShare oks = new OnekeyShare();
        //关闭sso授权
        oks.disableSSOWhenAuthorize();
        // title标题，印象笔记、邮箱、信息、微信、人人网、QQ和QQ空间使用
        oks.setTitle("锦鲤天气");
        // titleUrl是标题的网络链接，仅在Linked-in,QQ和QQ空间使用
        oks.setTitleUrl("http://pku.xixi.kkxixi.com");
        // text是分享文本，所有平台都需要这个字段
        if(shareText.isEmpty()){
            oks.setText("我在锦鲤天气");
        }else{
            oks.setText(shareText);
        }

        //分享网络图片，新浪微博分享网络图片需要通过审核后申请高级写入接口，否则请注释掉测试新浪微博
        //oks.setImageUrl("http://f1.sharesdk.cn/imgs/2014/02/26/owWpLZo_638x960.jpg");
        // imagePath是图片的本地路径，Linked-In以外的平台都支持此参数
        //oks.setImagePath("/sdcard/test.jpg");//确保SDcard下面存在此张图片
        // url仅在微信（包括好友和朋友圈）中使用
        oks.setUrl("http://pku.xixi.kkxixi.com");
        // comment是我对这条分享的评论，仅在人人网和QQ空间使用
        oks.setComment("我在锦鲤天气");
        // site是分享此内容的网站名称，仅在QQ空间使用
        oks.setSite("锦鲤天气");
        // siteUrl是分享此内容的网站地址，仅在QQ空间使用
        oks.setSiteUrl("http://pku.xixi.kkxixi.com");

// 启动分享GUI
        oks.show(this);
    }
}
