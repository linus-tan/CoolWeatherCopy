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

	// ʡ����ʼ������
	public static final int LEVEL_PROVINCE = 0;
	// �м���
	public static final int LEVEL_CITY = 1;
	// �ؼ���
	public static final int LEVEL_COUNTY = 2;

	private ProgressDialog mProgressDialog;
	private TextView mtTextView;
	private ListView mListView;
	private ArrayAdapter<String> adapter;
	private List<String> dataList = new ArrayList<String>();
	private CoolWeatherDB coolWeatherDB;

	/**
	 * ʡ�б�
	 */
	private List<Province> provinceList;

	/**
	 * ���б�
	 */
	private List<City> cityList;

	/**
	 * ���б�
	 */
	private List<County> countyList;

	/**
	 * ѡ�е�ʡ��
	 */
	private Province selectedProvince;

	/**
	 * ѡ�еĳ���
	 */
	private City selectedCity;

	/**
	 * ��ǰѡ�еļ���
	 */
	private int currentLevel;

	/**
	 * �Ƿ��WeatherActivity��ת����
	 */
	private boolean isFromWeatherActivity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// ���׹���ʼ�����Ժ󲹳�

		// �Ƿ�� ����չʾҳ ��ת�������Ժ󲹳�

		// �Ƿ���ѡ���˳��У�ֱ����ת�� ����չʾҳ��
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

		// ��ʼ�����ݿ�
		coolWeatherDB = CoolWeatherDB.getInstance(this);

		mListView = (ListView) findViewById(R.id.list_view);
		mtTextView = (TextView) findViewById(R.id.title_text);

		// ��ʼ��listView
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, dataList);
		mListView.setAdapter(adapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				// �жϵ�ǰѡ��㼶�����������Ĳ���
				if (currentLevel == LEVEL_PROVINCE) {
					// �õ���ǰѡ���ʡ��
					selectedProvince = provinceList.get(position);
					// ��ѯ����
					queryCities();
				} else if (currentLevel == LEVEL_CITY) {
					// �õ���ǰѡ��ĳ���
					selectedCity = cityList.get(position);
					// ��ѯ��
					queryCounties();
				} else if (currentLevel == LEVEL_COUNTY) {
					String countyCode = countyList.get(position)
							.getCountyCode();
					// ����չʾҳ�滹ûд����ͷ������
					Intent intent = new Intent(ChooseAreaActivity.this,
							WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
				}
			}

		});

		// ��ȡʡ�б�
		queryProvinces();

	}

	/**
	 * ��ѯʡ���������ݿ⣬û����ȥ��������
	 */
	protected void queryProvinces() {
		provinceList = coolWeatherDB.loadProvinces();
		if (provinceList.size() > 0) {
			dataList.clear();
			for (Province province : provinceList) {
				dataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			// ���б����õ���ʼλ��
			mListView.setSelection(0);
			mtTextView.setText("�й�");

			// ��ǰѡ��㼶Ϊ ʡ��
			currentLevel = LEVEL_PROVINCE;
		} else {
			queryFromService(null, "province");
		}
	}

	/**
	 * ��ѯ����
	 */
	protected void queryCities() {
		// TODO Auto-generated method stub
		cityList = coolWeatherDB.loadCities(selectedProvince.get_id());
		if (cityList.size() > 0) {
			dataList.clear();
			for (City city : cityList) {
				dataList.add(city.getCityName());
			}
			// ֪ͨ���ݸ���
			adapter.notifyDataSetChanged();
			mListView.setSelection(0);
			mtTextView.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;

		} else {
			queryFromService(selectedProvince.getProvinceName(), "city");
		}
	}

	/**
	 * ��ѯ��
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
	 * �����������������ȡʡ������Ϣ
	 * 
	 * @param code
	 * @param type
	 */
	private void queryFromService(final String code, final String type) {
		// TODO Auto-generated method stub
		String address;

		// ��ַ��ʽ���ƣ�ͨ���Ƿ�������ش�������
		if (TextUtils.isEmpty(code)) {
			// ʡ
			address = "http://www.weather.com.cn/data/list3/city.xml";
		} else {
			// �С���
			address = "http://www.weather.com.cn/data/list3/city" + code
					+ ".xml";
		}

		// ��ʾ���ضԻ���
		showProgressDialog();

		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {

			@Override
			public void onFinish(String response) {
				// TODO Auto-generated method stub

				// �������ص����� ���洢�����ݿ���
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

				// �����ɹ������µ����ݿ�������չʾ
				if (result) {

					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							// �رռ��ؿ�
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

			// �˷������������߳��е�
			@Override
			public void onError(Exception e) {
				// TODO Auto-generated method stub

				// �ص����̴߳���UI
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						// �رռ��ؿ�
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "���������������",
								Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}

	/**
	 * ��ʾ���ȶԻ���
	 */
	private void showProgressDialog() {
		if (mProgressDialog == null) {
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setMessage("���ڼ��ء�����");
			// �Ի����ⴥ�����˳�
			mProgressDialog.setCanceledOnTouchOutside(false);
		}
		mProgressDialog.show();
	}

	/**
	 * �رս��ȶԻ���
	 */
	private void closeProgressDialog() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}
	}

	/**
	 * ����back�����������У�ʡ���˳�
	 */
	@Override
	public void onBackPressed() {
		if (currentLevel == LEVEL_COUNTY) {
			queryCities();
		} else if (currentLevel == LEVEL_CITY) {
			queryProvinces();
		} else if (currentLevel == LEVEL_PROVINCE) {

			// ����� ����չʾ�б���ת�������ͻص�չʾҳ��
			if (isFromWeatherActivity) {
				Intent intent = new Intent(this, WeatherActivity.class);
				startActivity(intent);
				finish();
			}

			finish();
		}
	}
}
