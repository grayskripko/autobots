package com.skripko.common;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.codeborne.selenide.Selenide.*;
import static com.skripko.common.ProxyUtils.getProxyInfoList;
import static com.skripko.common.SelenideUtils.BrowserType.CHROME;
import static com.skripko.common.SelenideUtils.openNewTabAndMap;


public class Main {
	public static String START_URL = "https://nces.ed.gov/surveys/pss/privateschoolsearch/";
	private static long startTime = System.currentTimeMillis();

	public static void main(String[] args) throws Exception {
		SelenideUtils.configureBrowser(CHROME);
		List<String> proxyListRows = getProxyInfoList();
		open(START_URL);

		SelenideElement selectEl = $("font > select");
		List<String> list = new ArrayList<>(Arrays.asList(selectEl.$$("option").getTexts()));
		list.remove(0);
		selectEl.selectOption("Mississippi");
		$("input[type=\"submit\"]").click();

		ElementsCollection searchResultTitles = $$("div.sfsContent > table:nth-child(4) tr:nth-child(-2n+30)");
		searchResultTitles.stream().forEach(schoolListRow -> {
			SelenideElement schoolNameEl = schoolListRow.$("a");
			String schoolName = schoolNameEl.getText();
			String grade = schoolListRow.$("tr table tr > td:last-child").getText();

			String affiliation = openNewTabAndMap(schoolNameEl, in -> {
				String affiliationInner = $("body > div.hfsMain > div.hfsContent > div.sfsContent > table > tbody > tr:nth-child(10) > td > table > tbody > tr > td:nth-child(3) > table > tbody > tr:nth-child(12) > td:nth-child(2)").getText();
				return affiliationInner.isEmpty() ? "N" : affiliationInner;
			}).get(0);

		});

//		closeWebDriver();
	}
}