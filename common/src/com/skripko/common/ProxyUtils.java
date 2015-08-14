package com.skripko.common;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.Proxy.ProxyType;

import java.util.List;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.closeWebDriver;
import static com.skripko.common.SelenideUtils.BrowserType.CHROME;

//todo adjusting disable images, css rendering and javascript
//todo method alternative for adjusting timeout like transaction style

public class ProxyUtils {
	private static long startTime = System.currentTimeMillis();

	public static void main(String[] args) throws Exception {
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
		List<String> proxyListRows = $$(tableSelector).stream()
				.map(row -> row.$$("td").get(1).getText()
						+ ':' + row.$$("td").get(2).getText()).collect(Collectors.toList());
		debug("Collected proxies count: " + proxyListRows.size());
		debug("Last proxy duration: " + lastProxyDuration);

		proxyListRows.stream().forEach(proxyInfo -> {
			applyProxy(proxyInfo);
			try {
				open("http://2ip.ru");
			} catch (Throwable e) {
				e.printStackTrace();
				System.exit(1);
			}

			debug($$("#content table tr").size());
			debug($("#content table").getText());
			debug($$("#content table tr").filter(matchText("Откуда вы:")).get(0).getText());
			SelenideUtils.humanWait(5000);
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
		} else {
			WebDriverRunner.setProxy(proxy);
		}
		WebDriverRunner.setProxy(proxy);
	}
}