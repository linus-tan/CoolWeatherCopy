package com.example.coolweathercopy.util;

import java.net.ContentHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.R.string;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.example.coolweathercopy.db.CoolWeatherDB;
import com.example.coolweathercopy.model.City;
import com.example.coolweathercopy.model.County;
import com.example.coolweathercopy.model.Province;

public class Utility {

	// 省（初始）级别
	public static final int PROVINCE = 0;
	// 市级别
	public static final int CITY = 1;
	// 县级别
	public static final int COUNTY = 2;

	/**
	 * 解析省份响应
	 * 
	 * @param coolWeatherDB
	 * @param response
	 * @return
	 */
	public static boolean handleProvincesResponse(CoolWeatherDB coolWeatherDB,
			String response) {

		if (!TextUtils.isEmpty(response)) {
			// 解析
			return resolveResponse(coolWeatherDB, response, 0, PROVINCE);
		}
		return false;
	}

	public static boolean handleCitiesResponse(CoolWeatherDB coolWeatherDB,
			String response, int provinceId) {
		if (!TextUtils.isEmpty(response)) {
			// 解析
			return resolveResponse(coolWeatherDB, response, provinceId, CITY);
		}
		return false;
	}

	public static boolean handleCountiesResponse(CoolWeatherDB coolWeatherDB,
			String response, int cityId) {
		if (!TextUtils.isEmpty(response)) {
			// 解析
			return resolveResponse(coolWeatherDB, response, cityId, COUNTY);
		}
		return false;
	}

	/**
	 * 解析响应的字符串
	 * 
	 * @param coolWeatherDB
	 * @param response
	 * @param foreignKey
	 *            外键，省-没有外键， 市-省ID， 县-市ID
	 * @param resolveType
	 * @return
	 */
	private static boolean resolveResponse(CoolWeatherDB coolWeatherDB,
			String response, int foreignKey, int resolveType) {
		String[] all = response.split(",");
		if (all != null && all.length > 0) {
			for (String i : all) {
				String[] array = i.split("\\|");

				// 根据解析类型选择实体模型 并存入对应的表
				switch (resolveType) {
				case PROVINCE:
					Province province = new Province();
					province.setProvinceCode(array[0]);
					province.setProvinceName(array[1]);
					// 将解析出的数据存入数据库（注意这里的db帮助方法是一条一条存的）
					if (coolWeatherDB != null) {
						coolWeatherDB.saveProvince(province);
					}
					break;
				case CITY:
					City city = new City();
					city.setCityCode(array[0]);
					city.setCityName(array[1]);
					city.setProvinceId(foreignKey);
					if (coolWeatherDB != null) {
						coolWeatherDB.saveCity(city);
					}
					break;
				case COUNTY:
					County county = new County();
					county.setCountyCode(array[0]);
					county.setCountyName(array[1]);
					county.setCityId(foreignKey);
					if (coolWeatherDB != null) {
						coolWeatherDB.saveCounty(county);
					}
					break;
				default:
					break;
				}
			}
			return true;
		}

		return false;
	}

	/**
	 * 解析服务器返回的json数据，并将返回的数据存储到本地
	 * 
	 * @param context
	 * @param response
	 */
	public static void handleWeatherResponse(Context context, String response) {
		try {
			JSONObject jsonObject = new JSONObject(response);
			JSONObject weatherInfo = jsonObject.getJSONObject("weatherinfo");
			String cityName = weatherInfo.getString("city");
			String weatherCode = weatherInfo.getString("cityid");
			String temp1 = weatherInfo.getString("temp1");
			String temp2 = weatherInfo.getString("temp2");
			String weatherDesp = weatherInfo.getString("weather");
			String publishTime = weatherInfo.getString("ptime");

			saveWeatherInfo(context, cityName, weatherCode, temp1, temp2,
					weatherDesp, publishTime);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * 存储到SharePreferences
	 * 
	 * @param context
	 * @param cityName
	 * @param weatherCode
	 * @param temp1
	 * @param temp2
	 * @param weatherDesp
	 * @param publishTime
	 */
	public static void saveWeatherInfo(Context context, String cityName,
			String weatherCode, String temp1, String temp2, String weatherDesp,
			String publishTime) {

		// 对时间的格式化器
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.CHINA);

		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context).edit();

		editor.putBoolean("city_selected", true);
		editor.putString("city_name", cityName);
		editor.putString("weather_code", weatherCode);
		editor.putString("temp1", temp1);
		editor.putString("temp2", temp2);
		editor.putString("weather_desp", weatherDesp);
		editor.putString("publish_time", publishTime);
		editor.putString("current_date", sdf.format(new Date()));

		editor.commit();

	}
}
