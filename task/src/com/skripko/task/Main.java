package com.skripko.task;

import static com.codeborne.selenide.Selenide.open;
import static com.skripko.common.ProxyUtils.getFastProxies;
import static com.skripko.common.ProxyUtils.getProxyInfoList;
import static com.skripko.common.SelenideUtils.BrowserType.CHROME;
import static com.skripko.common.SelenideUtils.configureBrowser;
import static com.skripko.common.SelenideUtils.print;

//todo make module for google image module with high resolution
//todo make Object-Modules: Paginator(Type.Scroll), List, Item, ScreenshotForSpeed ... Or maybe it is more appropriate to use functional approach
//todo smart email scraper. Google -> right filter first 3 results -> Find about, contacts, persons links and buttons


public class Main {
	public static final boolean isBrowserConfigured = configureBrowser(CHROME);

	public static void main(String[] args) throws Exception {
		print(getFastProxies(getProxyInfoList(), 4));
		open("https://nces.ed.gov/surveys/pss/privateschoolsearch/");
		Thread.sleep(30000);
//		PrivateSchool.process();
	}

}