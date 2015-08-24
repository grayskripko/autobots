package com.skripko.common;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Screenshots;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.Proxy.ProxyType;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
		TRUE
	}

	public static void main(String[] args) throws Exception {
		SelenideUtils.configureBrowser(CHROME);
		List<String> proxyListRows = getProxyInfoList();

		proxyListRows.stream().parallel().forEach(proxyInfo -> {
			ExecutorService service = Executors.newSingleThreadExecutor();
			try {
				final Future<Boolean> oneTaskResult = service.submit(() -> { //transaction
					applyProxy(proxyInfo);
					print(proxyInfo);
					open("http://2ip.ru");
					ElementsCollection proxyDesc = $("#content table").waitUntil(exist, 30 * 1000).$$("tr");
					if (proxyDesc.isEmpty()) {
						Screenshots.takeScreenShot(new Date().toString());
					}
					print(proxyDesc.filter(matchText("Имя вашего компьютера:")).get(0).getText());
					return true;
				});
				oneTaskResult.get(60, TimeUnit.SECONDS);
			} catch (Throwable th) {
				th.getMessage();
				closeWebDriver();
			} finally {
				service.shutdown();
			}
		});
	}

	public static List<String> getProxyInfoList(Option... option) {
		String cacheFileName = "proxyListCache.txt";
//		if (fromCache.length > 0 && fromCache[0] && new File(cacheFileName).exists()) {
		if (new File(cacheFileName).exists()
				&& (System.currentTimeMillis() - new File(cacheFileName).lastModified()) / (1000 * 60 * 60) < 1
				&& option.length == 0) {
			List<String> cachedProxies = readListFromTxt(cacheFileName);
			if (cachedProxies != null) {
				return cachedProxies;
			}
		}

		open("http://proxylist.hidemyass.com/");
		print("Hidemyass has been opened");
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
		print("Hidemyass update button has been clicked");

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

		return proxyListRows;
	}

	public static List<String> getFastestProxies(List<String> proxyInfos, int... wishedProxiesCount) {
		long fakeDurationForBadProxy = 99999;
		int maxProxyListCheckedLength = 30;

		List<String> proxyInfosCutted = proxyInfos.size() > maxProxyListCheckedLength ?
				proxyInfos.subList(0, maxProxyListCheckedLength) : proxyInfos;
		Map<String, Long> proxyTime = proxyInfosCutted.parallelStream().collect(Collectors.toMap(
				proxyInfo -> proxyInfo, proxyInfo -> {
					long start = System.currentTimeMillis();
					boolean isIpInvisible = isProxyWorks();
					return isIpInvisible ? System.currentTimeMillis() - start : fakeDurationForBadProxy;
				}));
		Map<String, Integer> proxyTimeSorted = AlgoUtils.sortByValue(proxyTime);
		List<String> resultSliced = new ArrayList<>(proxyTimeSorted.keySet());

		int defaultResultLimit = 5;
		return wishedProxiesCount.length == 0 ?
				resultSliced.subList(0, defaultResultLimit) : resultSliced.subList(0, wishedProxiesCount[0]);
	}

	public static void applyProxy(String proxyInfo) {
		closeWebDriver();
		print("Previous browser has been closed");
		Proxy proxy = new Proxy().setHttpProxy(proxyInfo).setFtpProxy(proxyInfo).setSslProxy(proxyInfo);
		proxy.setProxyType(ProxyType.MANUAL);
		if (WebDriverRunner.isPhantomjs()) {
			SelenideUtils.customConfigurePhantom(proxy);
		}
		WebDriverRunner.setProxy(proxy);
		print("Proxy applied: " + proxyInfo);
	}

	public static String getCurrentIp() {
		open("http://checkip.amazonaws.com/");
		String currentIp = $("pre").getText();
		realIp = realIp == null ? currentIp : realIp;
		return currentIp;
	}

	public static boolean isProxyWorks() {
		final List<Boolean> result = new LinkedList<>();
		Thread threadNewTab = new Thread(() -> {
			String ipGivenByFastestAmazon = getCurrentIp();
			boolean amazonOpinion = realIp != null && realIp.equals(ipGivenByFastestAmazon);
			open("http://whatismyipaddress.com/proxy-check");
			boolean whatismyipadressOpinion = realIp.equals($("#section_left_3rd > table tr:nth-child(1) > td:nth-child(2)").getText());
			if (amazonOpinion == whatismyipadressOpinion) {
				result.add(amazonOpinion);
			} else {
				print(String.format("Check ip opinions collision. RealIp: %s, amazonOpinion: %s, whatismyipadressOpinion: %s",
						realIp, amazonOpinion, whatismyipadressOpinion));
				result.add(false);
			}
		});

		threadNewTab.start();
		try {
			threadNewTab.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return result.get(0);
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