package com.skripko.common;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Screenshots;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.Proxy.ProxyType;

import java.util.ArrayList;
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
import static com.skripko.common.SelenideUtils.BrowserType.CHROME;


public class ProxyUtils {
	private static long startTime = System.currentTimeMillis();

	public static void main(String[] args) throws Exception {
		int size = (int) 1e7;
		List<Double> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			list.add(Math.random() * size);
		}
//		list = list.stream().parallel().unordered().map(i -> Math.pow(i, 10)).collect(Collectors.toList());
//		debug(" ");
//		if(true)return;

		SelenideUtils.configureBrowser(CHROME);
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

		proxyListRows.stream().parallel().forEach(proxyInfo -> {
			ExecutorService service = Executors.newSingleThreadExecutor();
			try {
				final Future<Boolean> oneTaskResult = service.submit(() -> {
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

	private static void debug(Object str) {
		System.out.println(String.format("[%ssec] %s", ((System.currentTimeMillis() - startTime) / 1000), String.valueOf(str)));
	}

	private static void applyProxy(String proxyInfo) {
		closeWebDriver();
		Proxy proxy = new Proxy().setHttpProxy(proxyInfo).setFtpProxy(proxyInfo).setSslProxy(proxyInfo);
		proxy.setProxyType(ProxyType.MANUAL);
		if (WebDriverRunner.isPhantomjs()) {
			SelenideUtils.configurePhantom(proxy);
		}
		WebDriverRunner.setProxy(proxy);
	}
}