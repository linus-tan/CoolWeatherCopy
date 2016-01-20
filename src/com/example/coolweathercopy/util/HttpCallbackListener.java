package com.example.coolweathercopy.util;

public interface HttpCallbackListener {

	void onFinish(String response);
	
	void onError(Exception e);
}
