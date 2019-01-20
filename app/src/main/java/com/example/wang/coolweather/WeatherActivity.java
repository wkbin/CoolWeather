package com.example.wang.coolweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.wang.coolweather.gson.HuangLi;
import com.example.wang.coolweather.gson.Weather;
import com.example.wang.coolweather.gson.Yesterday;
import com.example.wang.coolweather.service.AutoUpdateService;
import com.example.wang.coolweather.util.HttpUtil;
import com.example.wang.coolweather.util.Utility;
import com.google.gson.Gson;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    // 黄历key
    public final String KEY = "821f2544cdb8336afb8404aaa8986ad2";
    public DrawerLayout drawerLayout;
    public SwipeRefreshLayout swipeRefresh;
    private String mWeatherId;
    private String mDate;

    private Button navButton;

    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView shiduText;
    private TextView pm25Text;
    private TextView yinliText;
    private TextView wuxingText;
    private TextView chongshaText;
    private TextView baijiText;
    private TextView jishenText;
    private TextView xiongshenText;
    private TextView yiText;
    private TextView jiText;

    private ImageView bingPicImg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int flagTranslucentStatus = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            int flagTranslucentNavigation = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                WindowManager.LayoutParams attributes = window.getAttributes();
                attributes.flags |= flagTranslucentNavigation;
                window.setAttributes(attributes);
                getWindow().setStatusBarColor(Color.TRANSPARENT);
            } else {
                Window window = getWindow();
                WindowManager.LayoutParams attributes = window.getAttributes();
                attributes.flags |= flagTranslucentStatus | flagTranslucentNavigation;
                window.setAttributes(attributes);
            }
        }

        setContentView(R.layout.activity_weather);
        // 初始化各控件
        swipeRefresh = findViewById(R.id.swip_refresh);
        drawerLayout = findViewById(R.id.drawer_layout);
        navButton = findViewById(R.id.nav_button);
        bingPicImg = findViewById(R.id.bing_pic_img);
        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        shiduText = findViewById(R.id.shidu_text);
        pm25Text = findViewById(R.id.pm25_txt);
        yinliText = findViewById(R.id.yinli_text);
        wuxingText = findViewById(R.id.wuxing_text);
        chongshaText = findViewById(R.id.chongsha_text);
        baijiText = findViewById(R.id.baiji_text);
        jishenText = findViewById(R.id.jishen_text);
        xiongshenText = findViewById(R.id.xiongshen_text);
        yiText = findViewById(R.id.yi_text);
        jiText = findViewById(R.id.ji_text);

        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        swipeRefresh.setColorSchemeColors(R.color.colorPrimary);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);
        String huangLiString = prefs.getString("huangLi",null);
        String bingPic = prefs.getString("bing_pic",null);
        if (bingPic != null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else{
            loadBingPic();
        }
        if (weatherString != null){
            Log.d("WeatherActivity","有天气缓存");
            // 有缓存时直接解析天气数据
            Weather weather = new Gson().fromJson(weatherString,Weather.class);
            showWeatherInfo(weather);
        }else {
            // 无缓存时去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
            Log.d("WeatherActivity","weatherId = "+mWeatherId);
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }
        if (huangLiString != null){
            Log.d("WeatherActivity","有黄历缓存");
            //有缓存时解析黄历数据
            HuangLi huangLi = new Gson().fromJson(huangLiString,HuangLi.class);
            showHuangLiInfo(huangLi);
        }else{
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            mDate = sdf.format(new Date());
            // 无缓存时去服务器查询黄历数据
            requestHuangLi(mDate);
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
                requestHuangLi(mDate);
            }
        });
    }

    /**
     * 根据天气id请求城市天气信息
     */
    public void requestWeather(final String weatherId){
        String weatherUrl = "http://t.weather.sojson.com/api/weather/city/"+weatherId;
        Log.d("WeatherActivity","url = "+weatherUrl);
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = new Gson().fromJson(responseText,Weather.class);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "200".equals(weather.status)){
                            Toast.makeText(WeatherActivity.this,weather.message,Toast.LENGTH_SHORT).show();
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        }else{
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }
    /**
     * 处理并展示Weather实体类中的数据
     */
    private void showWeatherInfo(Weather weather){
        if (weather != null && "200".equals(weather.status)){
            Intent intent = new Intent(this,AutoUpdateService.class);
            startService(intent);
        }else{
            Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
            return;
        }
        String cityName = weather.cityInfo.city;
        String updateTime = weather.cityInfo.updateTime;
        String shidu = weather.data.shidu;
        String pm25 = weather.data.pm25;
        String min = weather.data.forecast.get(0).low.substring(2);
        String max = weather.data.forecast.get(0).high.substring(2);
        String weatherInfo = weather.data.forecast.get(0).type;
        min = min.substring(0,min.length()-3);
        max = max.substring(0,max.length()-3);
        String degree = min+"/"+max+"℃";
        shiduText.setText(shidu);
        pm25Text.setText(pm25.substring(0,pm25.length()-2));
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        weatherLayout.setVisibility(View.VISIBLE);
        forecastLayout.removeAllViews();

        for (Yesterday yesterday:weather.data.forecast){
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);
            dateText.setText(yesterday.ymd);
            infoText.setText(yesterday.type);
            String mMin = yesterday.low.substring(2);
            String mMax = yesterday.high.substring(2);
            mMin = mMin.substring(0,mMin.length()-3);
            mMax = mMax.substring(0,mMax.length()-3);
            maxText.setText(mMax);
            minText.setText(mMin);
            forecastLayout.addView(view);
        }
        Yesterday yesterday = weather.data.yesterday;
        View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
        TextView dateText = view.findViewById(R.id.date_text);
        TextView infoText = view.findViewById(R.id.info_text);
        TextView maxText = view.findViewById(R.id.max_text);
        TextView minText = view.findViewById(R.id.min_text);
        dateText.setText(yesterday.ymd);
        infoText.setText(yesterday.type);
        String mMin = yesterday.low.substring(2);
        String mMax = yesterday.high.substring(2);
        mMin = mMin.substring(0,mMin.length()-3);
        mMax = mMax.substring(0,mMax.length()-3);
        maxText.setText(mMax);
        minText.setText(mMin);
        forecastLayout.addView(view,0);

    }
    /**
     * 根据日期请求黄历
     */
    public void requestHuangLi(final String mDate){
        String huangLiUrl = "http://v.juhe.cn/laohuangli/d?date="+mDate+"&key="+KEY;
        Log.d("WeatherActivity","hangLiUrl = "+huangLiUrl);
        HttpUtil.sendOkHttpRequest(huangLiUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取黄历信息失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final HuangLi huangLi = new Gson().fromJson(responseText,HuangLi.class);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (huangLi.reason.equals("successed")){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("huangLi",responseText);
                            editor.apply();
                            showHuangLiInfo(huangLi);
                        }else{
                            Toast.makeText(WeatherActivity.this,"获取黄历信息失败",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
    private void showHuangLiInfo(HuangLi mHuangLi){
        String yinli = "阴历："+mHuangLi.result.yinli;
        String wuxing = "五行："+mHuangLi.result.wuxing;
        String chongsha = "冲煞："+mHuangLi.result.chongsha;
        String baiji = "彭祖百忌："+mHuangLi.result.baiji;
        String jishen = "吉神宜趋："+mHuangLi.result.jishen;
        String xiongshen = "凶神宜忌："+mHuangLi.result.xiongshen;
        String yi = "宜："+mHuangLi.result.yi;
        String ji = "忌："+mHuangLi.result.ji;

        yinliText.setText(yinli);
        wuxingText.setText(wuxing);
        chongshaText.setText(chongsha);
        baijiText.setText(baiji);
        jishenText.setText(jishen);
        xiongshenText.setText(xiongshen);
        yiText.setText(yi);
        jiText.setText(ji);

    }
    /**
     * 加载必应每日一图
     */
    private void loadBingPic(){
        final String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }
}
