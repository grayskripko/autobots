package com.skripko.common;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Screenshots;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.skripko.common.object.Transaction;
import com.sun.istack.internal.Nullable;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.Proxy.ProxyType;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.closeWebDriver;
import static com.skripko.common.ExcelIO.readListFromTxt;
import static com.skripko.common.ExcelIO.writeListToTxt;
import static com.skripko.common.SelenideUtils.BrowserType.CHROME;
import static com.skripko.common.SelenideUtils.print;

public class ProxyUtils {
	public static String realIp;
	public enum Option {
		FORCE_UPDATE
	}

	public static void main(String[] args) throws Exception {
		SelenideUtils.configureBrowser(CHROME);
		List<String> proxyListRows = getProxyInfoList();

		proxyListRows.stream().parallel().forEach(proxyInfo -> new Transaction(60).executeWithTimeLimit(() -> {
			applyProxy$ClosePrevBrowser(proxyInfo);
			print(proxyInfo);
			open("http://2ip.ru");
			ElementsCollection proxyDesc = $("#content table").waitUntil(exist, 30 * 1000).$$("tr");
			if (proxyDesc.isEmpty()) {
				Screenshots.takeScreenShot(new Date().toString());
			}
			print(proxyDesc.filter(matchText("Имя вашего компьютера:")).get(0).getText());
			return true;
		}));
	}

	public static List<String> getProxyInfoList(Option... option) {
		print(">> getProxyInfoList");
		String cacheFileName = "proxyListCache.txt";
		int timeForUpdate = 20;
//		if (fromCache.length > 0 && fromCache[0] && new File(cacheFileName).exists()) {
		if (new File(cacheFileName).exists()
				&& (System.currentTimeMillis() - new File(cacheFileName).lastModified()) / (1000 * 60) < timeForUpdate
				&& option.length == 0) {
			List<String> cachedProxies = readListFromTxt(cacheFileName);
			if (cachedProxies != null) {
				return cachedProxies;
			}
		}

		open("http://proxylist.hidemyass.com/");
		ElementsCollection legends = $$("#proxy-search-form legend");

		ElementsCollection anonymityCheckboxes = legends.filter(hasText("anonymity")).get(0).parent().$$("div.row");
		anonymityCheckboxes.stream().filter(el -> el.has(or("anon", text("none"), text("low")))).map(el -> el.$("label")).forEach(SelenideElement::click);
		ElementsCollection speedCheckboxes = legends.filter(hasText("speed")).get(0).parent().$$("div.row");
		speedCheckboxes.stream().filter(el -> el.has(or("speed", text("slow"), text("medium")))).map(el -> el.$("label")).forEach(SelenideElement::click);
		ElementsCollection contimeCheckboxes = legends.filter(hasText("connection time")).get(0).parent().$$("div.row");
		contimeCheckboxes.stream().filter(el -> el.has(text("slow"))).map(el -> el.$("label")).forEach(SelenideElement::click);
		screenshot("filters.png");

		$("select[name=pp]").selectOption("100 per page");
		$("#proxy-list-upd-btn").click();

		String tableSelector = "#listable tbody tr";
		SelenideUtils.strictWait();
		SelenideElement lastRowOfProxyList = $(tableSelector + ":nth-of-type(100)").should(exist).$("td");
		String lastProxyDuration = lastRowOfProxyList.getText();
		List<String> proxyListRows = $$(tableSelector).stream()//.parallel().unordered()
				.map(row -> row.$$("td").get(1).getText()
						+ ':' + row.$$("td").get(2).getText()).collect(Collectors.toList());

		print("Collected proxies count: " + proxyListRows.size());
		print("Last proxy duration: " + lastProxyDuration);
		closeWebDriver();
		writeListToTxt(cacheFileName, proxyListRows);
		print("<< getProxyInfoList");
		return proxyListRows;
	}

	public static List<String> getFastestProxies(List<String> proxyInfos, int... wishedProxiesCount) {
		print(">> getFastestProxies");
		final long fakeDurationForBadProxy = 99999;
		final int maxProxyListCheckedLength = 30;
		final long timeout = 60;

		List<String> proxyInfosCutted = proxyInfos.size() > maxProxyListCheckedLength ?
				proxyInfos.subList(0, maxProxyListCheckedLength) : proxyInfos;
		Map<String, Long> proxyTime = proxyInfosCutted.stream().collect(Collectors.toMap(
				proxyInfo -> proxyInfo, proxyInfo -> {
					Transaction tx = new Transaction(timeout, Transaction.Option.SOFT_ERROR);
					return (Long) tx.executeWithTimeLimit(() -> {
						long start = System.currentTimeMillis();
						applyProxy$ClosePrevBrowser(proxyInfo);
						boolean isIpInvisible = isProxyWorks$Refresh();
						return isIpInvisible ? System.currentTimeMillis() - start : fakeDurationForBadProxy;
					});
				}));

		applyProxy$ClosePrevBrowser(null);
		print("cleared ip. IsProxyWorks$Refresh(): " + isProxyWorks$Refresh());

		Map<String, Integer> proxyTimeSorted = AlgoUtils.sortMapByValue(proxyTime);
		print(proxyTimeSorted); //todo remove
		List<String> resultSliced = new ArrayList<>(proxyTimeSorted.keySet());

		int resultLimit = 5;
		print("<< getFastestProxies");
		return wishedProxiesCount.length == 0 ?
				resultSliced.subList(0, resultLimit) : resultSliced.subList(0, wishedProxiesCount[0]);
	}

	/**
	 * It can be used for removing proxy
	 * */
	public static void applyProxy$ClosePrevBrowser(@Nullable String proxyInfo) {
		closeWebDriver();
		Proxy proxy = null;

		if (proxyInfo != null) {
			proxy = new Proxy().setHttpProxy(proxyInfo).setFtpProxy(proxyInfo).setSslProxy(proxyInfo);
			proxy.setProxyType(ProxyType.MANUAL);
		}
		if (WebDriverRunner.isPhantomjs()) {
			SelenideUtils.customConfigurePhantom(proxy);
		}
		WebDriverRunner.setProxy(proxy);
		print("Proxy applied: " + proxyInfo);
	}

	public static String getCurrentIpAmazon() { //first time it is called from configureBrowser()
		open("http://checkip.amazonaws.com/");
		String currentIp = $("pre").getText();
		realIp = realIp == null ? currentIp : realIp;
		return currentIp;
	}

	public static String getCurrentIpWhatismy() {
		open("http://whatismyipaddress.com/proxy-check");
		String currentIp = $("#section_left_3rd > table tr:nth-child(1) > td:nth-child(2)").getText();
		realIp = realIp == null ? currentIp : realIp;
		return currentIp;
	}

	public static long getConnectionSpeed() {
		long start = System.currentTimeMillis();
		getCurrentIpAmazon();
		getCurrentIpWhatismy();
		return System.currentTimeMillis() - start;
	}

	public static boolean isProxyWorks$Refresh() {
		if (realIp == null) {
			throw new IllegalStateException();
		}

		try {
			String amazonIp =  getCurrentIpAmazon();
			boolean amazonOpinion = !realIp.equals(amazonIp);
			String whatismyIp = getCurrentIpWhatismy();
			boolean whatismyOpinion = !realIp.equals(whatismyIp);

			if (amazonOpinion != whatismyOpinion) {
				print(String.format("Check ip opinions collision. AmazonOpinion: %s, whatismyOpinion: %s", amazonOpinion, whatismyOpinion));
				return false;
			}
			return amazonOpinion;
		} catch (Throwable e) { // it seems it is good standard for exception catching and error message
			throw new RuntimeException(Thread.currentThread() + " >> " + e);
		}
	}

	public static String getCurrentIpFrom2ipRu() {
		open("http://2ip.ru/privacy/");
		String messageAboutIp = $("#content > p").getText();
		Matcher matcher = Pattern.compile("([\\d\\.]+)").matcher(messageAboutIp);
		if (!matcher.find()) {
			return null;
		}
		String currentIp = matcher.group(0);
		realIp = realIp == null ? currentIp : realIp;
		return currentIp;
	}
}