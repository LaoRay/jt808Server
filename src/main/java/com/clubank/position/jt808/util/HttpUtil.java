package com.clubank.position.jt808.util;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpUtil {

	public static String post(String url, String data) {
		try {
			HttpClient client = new HttpClient();
			PostMethod method = new PostMethod(url);

			client.getParams().setContentCharset("UTF-8");
			method.setRequestHeader("Content-Type", "application/json;charset=UTF-8");

			StringRequestEntity requestEntity = new StringRequestEntity(data, "Content-Type", "UTF-8");

			method.setRequestEntity(requestEntity);

			client.executeMethod(method);

			String result = method.getResponseBodyAsString();
			System.out.println(result);

			return result;
		} catch (Exception e) {
			log.error("request failed", e);
			e.printStackTrace();
		}
		return null;
	}

}
