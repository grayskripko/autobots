package com.skripko.common;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Condition.and;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.*;
import static com.skripko.common.ExcelIO.Mode.WRITE;
import static com.skripko.common.SelenideUtils.BrowserType.CHROME;
import static com.skripko.common.SelenideUtils.*;

//todo check first 30 proxies by 3 threads, compare with original time, and sort them before write. Use whatismyip service for checking visibiltity of real ip.
//todo intellegent email scraper. Find about, contacts, persons links and buttons.
//todo choose second speed proxy for checking bun settings

public class Main {
	public static final String START_URL = "https://nces.ed.gov/surveys/pss/privateschoolsearch/";

	public static void main(String[] args) throws Exception {
		configureBrowser(CHROME);
//		List<String> proxyListRows = getProxyInfoList(ForceUpdate.TRUE);
//		applyProxy(proxyListRows.get(0));
//		debug("isProxyWorks(): " + isProxyWorks$Refresh());
		open(START_URL);

		SelenideElement selectEl = $("font > select");
		List<String> statesOption = new ArrayList<>(Arrays.asList(selectEl.$$("option").getTexts()));
		statesOption.remove(0);

		List<String> choosedStates = statesOption.stream().filter(
				state -> state.contains("Missi") || state.contains("Colu")).collect(Collectors.toList());

		class School {
			String state;
			String schoolName;
			String grade;
			String affiliation;

			School(String state) {
				this.state = state;
			}
		}
		List<School> schools = new LinkedList<>();

		for (String choosedState : choosedStates) {
			selectEl.selectOption(choosedState);
			openNewTabAndMap($("input[type=\"submit\"]"), uselessIn -> { //for every choosed state
				while (true) {
					SelenideElement rawTableEl = $$("div.sfsContent > table").filter(and(null, text("School Name"), text("Phone"))).get(0);
					ElementsCollection searchResultTitles = rawTableEl.$$("td[bgcolor='#EDFFE8'] > table > tbody > tr");
					schools.addAll(searchResultTitles.stream().map(schoolListRow -> {
						SelenideElement schoolNameEl = schoolListRow.$("a");
						School bean = new School(choosedState);
						bean.schoolName = schoolNameEl.getText();
						bean.grade = schoolListRow.$("tr table tr > td:last-child").getText();
						bean.affiliation = openNewTabAndMap(schoolNameEl, useless2In -> {
							String affiliationInner = $("div.sfsContent > table > tbody > tr:nth-child(10) > td > table > tbody > tr > td:nth-child(3) > table > tbody > tr:nth-child(12) > td:nth-child(2)").getText();
							return affiliationInner.isEmpty() ? "N" : affiliationInner;
						}).get(0);
						humanWait(1200);
						return bean;
					}).collect(Collectors.toList()));

					SelenideElement element = $(By.xpath("//a[@class='ignoredclass1' and contains(., 'Next >>')]"));
					if (!element.exists()) {
						break;
					}
					new ExcelIO("School.xlsx", WRITE, true).writeList(schools);
					element.click();
				}
				return null;
			});
		}

		ExcelIO.writeListToTxt("School.txt", schools);
		new ExcelIO("School.xlsx", WRITE, true).writeList(schools);
		debug("<<");
	}
}