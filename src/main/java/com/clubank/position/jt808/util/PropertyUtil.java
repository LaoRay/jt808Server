package com.clubank.position.jt808.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertyUtil {

	private static Properties props;

	static {
		String fileName = "config.properties";
		props = new Properties();
		try {
			props.load(
					new InputStreamReader(PropertyUtil.class.getClassLoader().getResourceAsStream(fileName), "UTF-8"));
		} catch (IOException e) {
			log.error("配置文件读取异常", e);
		}
	}

	public static String getProperty(String key) {
		String value = props.getProperty(key.trim());
		if (StringUtils.isBlank(value)) {
			return null;
		}
		return value.trim();
	}

	public static String getProperty(String key, String defaultValue) {
		String value = props.getProperty(key.trim());
		if (StringUtils.isBlank(value)) {
			value = defaultValue;
		}
		return value.trim();
	}
}
