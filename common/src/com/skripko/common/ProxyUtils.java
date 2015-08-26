package com.skripko.common;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Screenshots;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.skripko.object.Transaction;
import com.sun.istack.internal.Nullable;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.Proxy.ProxyType;

import java.io.File;
import java.util.*;
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
		final int timeForUpdate = 20;
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

	public static List<String> getFastProxies(List<String> proxyInfos, int... wishedProxiesCount) {
		print(">> getFastProxies");
		final long fakeDurationForBadProxy = 99999;
		final long effectiveTimeoutForProxy = 20; //Clear ip speed: 1249, {14.152.49.193:8080=14853, 222.39.64.74:8118=15063, 117.136.234.4:80=15532, 117.136.234.2:80=15619, 117.136.234.8:80=16037, 117.136.234.1:80=16317, 14.152.49.194:8080=16860, 36.250.75.99:80=16907, 58.30.233.196:8080=16916, 113.207.56.77:80=17287, 112.93.114.49:8080=17695, 117.136.234.12:80=18360, 113.207.56.78:8088=18435, 222.39.87.21:8118=20215, 220.255.3.131:8080=20927, 117.136.234.18:80=25244, 122.143.19.147:8088=26094, 94.45.65.94:3128=26214, 112.93.114.49:8088=27618, 106.39.79.67:80=37552, 14.152.49.193:80=44280, 183.207.128.47:13101=45302, 120.198.237.5:80=47449, 36.250.75.98:80=48017, 183.250.91.33:8080=48111, 183.250.91.33:8088=53968, 94.225.49.208:80=99999, 113.215.0.130:80=99999, 62.176.13.22:8088=99999, 120.198.237.5:9000=99999}
		int resultLimit = wishedProxiesCount.length == 0 ? 5 : wishedProxiesCount[0];

		Map<String, Long> proxyTimeLimited = new LinkedHashMap<>();
		for (String proxyInfo : proxyInfos) {
			if (resultLimit < 1) {
				break;
			}
			Transaction tx = new Transaction(effectiveTimeoutForProxy, Transaction.Option.SOFT_ERROR);
			Long txExecutionResult = (Long) tx.executeWithTimeLimit(() -> {
				long start = System.currentTimeMillis();
				applyProxy$ClosePrevBrowser(proxyInfo);
				boolean isIpInvisible = isProxyWorks$Refresh();
				Long result = null; //merge this null with null of timeout exception
				if (isIpInvisible) {
					result = System.currentTimeMillis() - start;
				}
				return result;
			});
			if (txExecutionResult != null) {
				proxyTimeLimited.put(proxyInfo, txExecutionResult);
				print(String.format("Rest: %s, proxy: %s, speed: %s", --resultLimit, proxyInfo, txExecutionResult));
			}
		}

		Map<String, Integer> proxyTimeSorted = AlgoUtils.sortMapByValue(proxyTimeLimited);
		List<String> resultSliced = new ArrayList<>(proxyTimeSorted.keySet());
		print(proxyTimeSorted);

		applyProxy$ClosePrevBrowser(null);
		print("Clear ip speed: " + getConnectionSpeed());
		print("<< getFastProxies");
		return resultSliced;
	}

	/**
	 * It can be used for removing proxy
	 */
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

	public static long getConnectionSpeed(boolean... printAmazonIp) {
		long start = System.currentTimeMillis();
		String amazonIp = getCurrentIpAmazon();
		if (printAmazonIp.length != 0 && printAmazonIp[0]) {
			print("Current IP: " + amazonIp);
		}
		getCurrentIpWhatismy();
		return System.currentTimeMillis() - start;
	}

	public static boolean isProxyWorks$Refresh() {
		if (realIp == null) {
			throw new IllegalStateException();
		}

		try {
			String amazonIp = getCurrentIpAmazon();
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