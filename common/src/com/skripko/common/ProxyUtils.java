package com.skripko.common;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Screenshots;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.Proxy.ProxyType;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.closeWebDriver;
import static com.skripko.common.ExcelIO.readList;
import static com.skripko.common.ExcelIO.writeList;
import static com.skripko.common.SelenideUtils.BrowserType.CHROME;
import static com.skripko.common.SelenideUtils.debug;


public class ProxyUtils {

	public static void main(String[] args) throws Exception {
		SelenideUtils.configureBrowser(CHROME);
		List<String> proxyListRows = getProxyInfoList();

		proxyListRows.stream().parallel().forEach(proxyInfo -> {
			ExecutorService service = Executors.newSingleThreadExecutor();
			try {
				final Future<Boolean> oneTaskResult = service.submit(() -> { //transaction
					applyProxy(proxyInfo);
					debug(proxyInfo);
					open("http://2ip.ru");
					ElementsCollection proxyDesc = $("#content table").waitUntil(exist, 30 * 1000).$$("tr");
					if (proxyDesc.isEmpty()) {
						Screenshots.takeScreenShot(new Date().toString());
					}
					debug(proxyDesc.filter(matchText("Имя вашего компьютера:")).get(0).getText());
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

	public static List<String> getProxyInfoList() {
		String cacheFileName = "proxyListCache.txt";
//		if (fromCache.length > 0 && fromCache[0] && new File(cacheFileName).exists()) {
		if ((System.currentTimeMillis() - new File(cacheFileName).lastModified()) / (1000 * 60 * 60) > 0) {
			List<String> cachedProxies = readList(cacheFileName);
			if (cachedProxies != null) {
				return cachedProxies;
			}
		}

		open("http://proxylist.hidemyass.com/");
		debug("Hidemyass has been opened");
		ElementsCollection legends = $$("#proxy-search-form legend");

		ElementsCollection anonymityCheckboxes = legends.filter(hasText("anonymity")).get(0).parent().$$("div.row");
		anonymityCheckboxes.stream().filter(el -> el.has(text("none"))).map(el -> el.$("label")).forEach(SelenideElement::click);
		ElementsCollection speedCheckboxes = legends.filter(hasText("speed")).get(0).parent().$$("div.row");
		speedCheckboxes.stream().filter(el -> el.has(or("speed", text("slow"), text("medium")))).map(el -> el.$("label")).forEach(SelenideElement::click);
		ElementsCollection contimeCheckboxes = legends.filter(hasText("connection time")).get(0).parent().$$("div.row");
		contimeCheckboxes.stream().filter(el -> el.has(text("slow"))).map(el -> el.$("label")).forEach(SelenideElement::click);
		screenshot("filters.png");

		$("select[name=pp]").selectOption("100 per page");
		$("#proxy-list-upd-btn").click();
		debug("Hidemyass update button has been clicked");

		String tableSelector = "#listable tbody tr";
		SelenideUtils.strictWait();
		SelenideElement lastRowOfProxyList = $(tableSelector + ":nth-of-type(100)").should(exist).$("td");
		String lastProxyDuration = lastRowOfProxyList.getText();
		List<String> proxyListRows = $$(tableSelector).stream()//.parallel().unordered()
				.map(row -> row.$$("td").get(1).getText()
						+ ':' + row.$$("td").get(2).getText()).collect(Collectors.toList());

		debug("Collected proxies count: " + proxyListRows.size());
		debug("Last proxy duration: " + lastProxyDuration);
		closeWebDriver();
		writeList(cacheFileName, proxyListRows);

		return proxyListRows;
	}

	private static void applyProxy(String proxyInfo) {
		closeWebDriver();
		Proxy proxy = new Proxy().setHttpProxy(proxyInfo).setFtpProxy(proxyInfo).setSslProxy(proxyInfo);
		proxy.setProxyType(ProxyType.MANUAL);
		if (WebDriverRunner.isPhantomjs()) {
			SelenideUtils.customConfigurePhantom(proxy);
		}
		WebDriverRunner.setProxy(proxy);
	}
}