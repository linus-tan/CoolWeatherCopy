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

//ÿ��һ��ʱ������һ�εĸ��·���
public class AutoUpdateService extends Service {

	//����󶨵�activity��ͨ��binder������Ʒ������Ϊ
	@Override
	public IBinder onBind(Intent intent){
		return null;
	}
	
	//���������󣬼����еķ���
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		
		//�����߳��� ִ�� ���µ�����
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
		
				//����ִ�и��·���
				updateWeather();
			}
		}).start();
		
		
		//����һ�����ӷ���ÿ��һ��ʱ�䷢��һ���㲥���ù㲥�ٴλ��Ѹ÷���
		AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
		//8Сʱ�ĺ�����
		int eightHour = 8*60*60*1000;
		//������ȥ��ʱ�� + 8Сʱ
		long triggerAtTime = SystemClock.elapsedRealtime() + eightHour;
		
		//���²������� ָ��ʱ�� ����һ���㲥
		Intent i = new Intent(this,AutoUpdateReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
		manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	/**
	 * ����������Ϣ
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
					//�洢��SharePreferences�Ϳ�����
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
