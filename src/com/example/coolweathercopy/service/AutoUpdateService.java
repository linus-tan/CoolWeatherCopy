package com.example.coolweathercopy.service;

import com.example.coolweathercopy.receiver.AutoUpdateReceiver;
import com.example.coolweathercopy.util.HttpCallbackListener;
import com.example.coolweathercopy.util.HttpUtil;
import com.example.coolweathercopy.util.Utility;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

//每隔一段时间运行一次的更新服务
public class AutoUpdateService extends Service {

	//服务绑定到activity，通过binder对象控制服务的行为
	@Override
	public IBinder onBind(Intent intent){
		return null;
	}
	
	//服务启动后，即运行的方法
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		
		//在新线程中 执行 更新的任务
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
		
				//这里执行更新方法
				updateWeather();
			}
		}).start();
		
		
		//启动一个闹钟服务，每隔一段时间发送一个广播，该广播再次唤醒该服务
		AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
		//8小时的毫秒数
		int eightHour = 8*60*60*1000;
		//开机逝去的时间 + 8小时
		long triggerAtTime = SystemClock.elapsedRealtime() + eightHour;
		
		//以下操作是在 指定时间 发送一个广播
		Intent i = new Intent(this,AutoUpdateReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
		manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	/**
	 * 更新天气信息
	 */
	private void updateWeather(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String weatherCode = prefs.getString("weather_code", "");
		
		if(!TextUtils.isEmpty(weatherCode)){
			String address = "http://www.weather.com.cn/date/cityinfo/" +weatherCode+".html";
			HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
				
				@Override
				public void onFinish(String response) {
					// TODO Auto-generated method stub
					Log.d("TAG", response);
					//存储到SharePreferences就可以了
					Utility.handleWeatherResponse(AutoUpdateService.this, response);
				}
				
				@Override
				public void onError(Exception e) {
					// TODO Auto-generated method stub
					e.printStackTrace();
				}
			});
		}
		
	}
	
	
}
