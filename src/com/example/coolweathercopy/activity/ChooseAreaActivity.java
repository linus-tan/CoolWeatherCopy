package com.example.coolweathercopy.activity;

import java.util.ArrayList;
import java.util.List;

import com.example.coolweathercopy.R;
import com.example.coolweathercopy.db.CoolWeatherDB;
import com.example.coolweathercopy.model.City;
import com.example.coolweathercopy.model.County;
import com.example.coolweathercopy.model.Province;
import com.example.coolweathercopy.util.HttpCallbackListener;
import com.example.coolweathercopy.util.HttpUtil;
import com.example.coolweathercopy.util.Utility;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAreaActivity extends Activity {

	// 省（初始）级别
	public static final int LEVEL_PROVINCE = 0;
	// 市级别
	public static final int LEVEL_CITY = 1;
	// 县级别
	public static final int LEVEL_COUNTY = 2;

	private ProgressDialog mProgressDialog;
	private TextView mtTextView;
	private ListView mListView;
	private ArrayAdapter<String> adapter;
	private List<String> dataList = new ArrayList<String>();
	private CoolWeatherDB coolWeatherDB;

	/**
	 * 省列表
	 */
	private List<Province> provinceList;

	/**
	 * 市列表
	 */
	private List<City> cityList;

	/**
	 * 县列表
	 */
	private List<County> countyList;

	/**
	 * 选中的省份
	 */
	private Province selectedProvince;

	/**
	 * 选中的城市
	 */
	private City selectedCity;

	/**
	 * 当前选中的级别
	 */
	private int currentLevel;

	/**
	 * 是否从WeatherActivity跳转过来
	 */
	private boolean isFromWeatherActivity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 有米广告初始化，以后补充

		// 是否从 天气展示页 调转过来，以后补充

		// 是否以选择了城市，直接跳转到 天气展示页面
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (prefs.getBoolean("city_selected", false) && !isFromWeatherActivity) {
			Intent intent = new Intent(this, WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_choose_area);

		// 初始化数据库
		coolWeatherDB = CoolWeatherDB.getInstance(this);

		mListView = (ListView) findViewById(R.id.list_view);
		mtTextView = (TextView) findViewById(R.id.title_text);

		// 初始化listView
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, dataList);
		mListView.setAdapter(adapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				// 判断当前选择层级，决定单击的操作
				if (currentLevel == LEVEL_PROVINCE) {
					// 得到当前选择的省份
					selectedProvince = provinceList.get(position);
					// 查询城市
					queryCities();
				} else if (currentLevel == LEVEL_CITY) {
					// 得到当前选择的城市
					selectedCity = cityList.get(position);
					// 查询县
					queryCounties();
				} else if (currentLevel == LEVEL_COUNTY) {
					String countyCode = countyList.get(position)
							.getCountyCode();
					// 天气展示页面还没写，回头过来改
					Intent intent = new Intent(ChooseAreaActivity.this,
							WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
				}
			}

		});

		// 拉取省列表
		queryProvinces();

	}

	/**
	 * 查询省（优先数据库，没有再去服务器）
	 */
	protected void queryProvinces() {
		provinceList = coolWeatherDB.loadProvinces();
		if (provinceList.size() > 0) {
			dataList.clear();
			for (Province province : provinceList) {
				dataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			// 将列表设置到起始位置
			mListView.setSelection(0);
			mtTextView.setText("中国");

			// 当前选择层级为 省级
			currentLevel = LEVEL_PROVINCE;
		} else {
			queryFromService(null, "province");
		}
	}

	/**
	 * 查询城市
	 */
	protected void queryCities() {
		// TODO Auto-generated method stub
		cityList = coolWeatherDB.loadCities(selectedProvince.get_id());
		if (cityList.size() > 0) {
			dataList.clear();
			for (City city : cityList) {
				dataList.add(city.getCityName());
			}
			// 通知数据更改
			adapter.notifyDataSetChanged();
			mListView.setSelection(0);
			mtTextView.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;

		} else {
			queryFromService(selectedProvince.getProvinceName(), "city");
		}
	}

	/**
	 * 查询县
	 */
	protected void queryCounties() {
		// TODO Auto-generated method stub
		countyList = coolWeatherDB.loadCounty(selectedCity.get_id());
		if (countyList.size() > 0) {
			dataList.clear();
			for (County county : countyList) {
				dataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			mListView.setSelection(0);
			mtTextView.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTY;
		} else {
			queryFromService(selectedCity.getCityCode(), "county");
		}

	}

	/**
	 * 向服务器发送请求，拉取省市县信息
	 * 
	 * @param code
	 * @param type
	 */
	private void queryFromService(final String code, final String type) {
		// TODO Auto-generated method stub
		String address;

		// 地址格式类似，通过是否包含市县代码区分
		if (TextUtils.isEmpty(code)) {
			// 省
			address = "http://www.weather.com.cn/data/list3/city.xml";
		} else {
			// 市、县
			address = "http://www.weather.com.cn/data/list3/city" + code
					+ ".xml";
		}

		// 显示加载对话框
		showProgressDialog();

		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {

			@Override
			public void onFinish(String response) {
				// TODO Auto-generated method stub

				// 解析返回的数据 并存储到数据库中
				boolean result = false;
				if ("province".equals(code)) {
					result = Utility.handleProvincesResponse(coolWeatherDB,
							response);
				} else if ("city".equals(code)) {
					result = Utility.handleCitiesResponse(coolWeatherDB,
							response, selectedProvince.get_id());
				} else if ("county".equals(code)) {
					result = Utility.handleCountiesResponse(coolWeatherDB,
							response, selectedCity.get_id());
				}

				// 解析成功，重新到数据库拉数据展示
				if (result) {

					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							// 关闭加载框
							closeProgressDialog();
							if ("province".equals(type)) {
								queryProvinces();
							} else if ("city".equals(type)) {
								queryCities();
							} else if ("county".equals(type)) {
								queryCounties();
							}
						}

					});

				}

			}

			// 此方法还是在子线程中的
			@Override
			public void onError(Exception e) {
				// TODO Auto-generated method stub

				// 回到主线程处理UI
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						// 关闭加载框
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "网络访问遇到错误",
								Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}

	/**
	 * 显示进度对话框
	 */
	private void showProgressDialog() {
		if (mProgressDialog == null) {
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setMessage("正在加载。。。");
			// 对话框外触摸不退出
			mProgressDialog.setCanceledOnTouchOutside(false);
		}
		mProgressDialog.show();
	}

	/**
	 * 关闭进度对话框
	 */
	private void closeProgressDialog() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}
	}

	/**
	 * 捕获back按键，返回市，省，退出
	 */
	@Override
	public void onBackPressed() {
		if (currentLevel == LEVEL_COUNTY) {
			queryCities();
		} else if (currentLevel == LEVEL_CITY) {
			queryProvinces();
		} else if (currentLevel == LEVEL_PROVINCE) {

			// 如果从 天气展示列表跳转过来，就回到展示页面
			if (isFromWeatherActivity) {
				Intent intent = new Intent(this, WeatherActivity.class);
				startActivity(intent);
				finish();
			}

			finish();
		}
	}
}
