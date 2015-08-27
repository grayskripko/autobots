package com.skripko.object.dao;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import com.skripko.common.AlgoUtils;
import com.skripko.object.FieldSelector;

import java.util.*;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.$;
import static com.skripko.common.SelenideUtils.print;

public class Item extends StorageObject {
	private static String itemCssPath;
	private static Deque<FieldSelector> contentNameCssPath; //labels inside

	private Item() {
	}

	public static Item collect(String itemUrlId) {
		if (itemCssPath == null || contentNameCssPath == null) {
			throw new IllegalStateException();
		}

		List<String> notFoundEls = new ArrayList<>();
		Item item = new Item();
		if (check404()) {
			print("404 - %s", itemUrlId);
			for (FieldSelector fieldSelector : contentNameCssPath) {
				item.pairs.add(new Pair(fieldSelector.getName(), null));
			}
			item.pairs.get(0).changeValue("404: " + itemUrlId);
			return item;
		}
		SelenideElement root = $(itemCssPath);
		for (FieldSelector fieldSelector : contentNameCssPath) {
			String val = null;
			try { //many items will be NA and throw ElementNotFound
				if (fieldSelector.isNeedFastProcess()
						&& !root.getText().toLowerCase().contains(fieldSelector.getName().toLowerCase())) {
					throw new ElementNotFound("fast ElementNotFound", null, null);
				}
				SelenideElement el = root.$(fieldSelector.getCssPath());
				if (fieldSelector.hasOption()) {
					switch (fieldSelector.getOption()) {
						case GET_HREF:
							val = el.getAttribute("href");
							break;
					}
				} else {
					val = el.getText().trim();
					if (fieldSelector.hasRegex()) {
						val = AlgoUtils.getFirstRegexMatch(val, fieldSelector.getRegex());
					}
				}
			} catch (ElementNotFound e) {
				notFoundEls.add(fieldSelector.getName()); //todo what if val exists but is shifted?
			}
			item.pairs.add(new Pair(fieldSelector.getName(), val));
		}

		if (!notFoundEls.isEmpty()) {
			print("Missed: %s, id: %s", notFoundEls.stream().collect(Collectors.joining(", ")), itemUrlId);
		}
		return item;
	}

	private static boolean check404() {
		SelenideElement title = $("h1.content-detail-title");
		if (title.exists()) {
			return title.getText().contains("Errore 404");
		}
		return false;
	}

	public static void build(String itemCssPathArg, FieldSelector... contentNameCssPathArg) {
		if (contentNameCssPathArg.length == 0) {
			throw new IllegalArgumentException();
		}

		itemCssPath = itemCssPathArg;
		contentNameCssPath = new ArrayDeque<>();
		Collections.addAll(contentNameCssPath, contentNameCssPathArg);
	}

	public static List<String> getLabels() {
		if (contentNameCssPath == null) {
			throw new IllegalStateException();
		}
		List<String> result = new ArrayList<>();
		contentNameCssPath.stream().forEachOrdered(selector -> result.add(selector.getName()));
		return result;
	}
}