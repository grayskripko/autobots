package com.skripko.common;

public class StringUtils {

	public static String getSiteName(String url) {
		if (url == null || url.isEmpty() || !url.startsWith("http")) {
			throw new IllegalArgumentException("url must not be empty and starts with [http]");
		}
		String beforeDomen = url.replaceAll("(\\.ru.*|\\.com.*|\\.org.*|\\.net.*)", "");
		String[] arr = beforeDomen.split("(/|\\\\|\\.)");
		return arr[arr.length - 1];
	}
}