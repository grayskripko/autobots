package com.skripko.common;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlgoUtils {

	public static String getSiteName(String url) {
		if (url == null || url.isEmpty() || !url.startsWith("http")) {
			throw new IllegalArgumentException("url must not be empty and starts with [http]");
		}
		String beforeDomen = url.replaceAll("(\\.ru.*|\\.com.*|\\.org.*|\\.net.*)", "");
		String[] arr = beforeDomen.split("(/|\\\\|\\.)");
		return arr[arr.length - 1];
	}

	public enum Option {
		DESC
	}
	public static Map sortMapByValue(Map unsortMap, Option... option) {
		List list = new LinkedList(unsortMap.entrySet());
		if (option.length != 0 && option[0] == Option.DESC) {
			Collections.sort(list, (o1, o2) -> ((Comparable) ((Entry) o2).getValue())
					.compareTo(((Entry) o1).getValue()));
		} else {
			Collections.sort(list, (o1, o2) -> ((Comparable) ((Entry) o1).getValue())
					.compareTo(((Entry) o2).getValue()));
		}

		Map sortedMap = new LinkedHashMap();
		for (Object aList : list) {
			Entry entry = (Entry) aList;
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	public static String getFirstRegexMatch(String text, String regex) {
		Matcher matcher = Pattern.compile(regex).matcher(text);
		if (!matcher.find()) {
			return null;
		}
		return matcher.group(0);
	}
}