package com.skripko.common;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import com.google.gson.JsonObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.logging.Level;

import static org.openqa.selenium.remote.CapabilityType.*;

public class SelenideUtils {
	public static final long TIMEOUT = 10000;
	public static final int AJAX_WAIT = 3;
	public static final String DRIVER_PATH = System.getProperty("user.dir") + "\\common\\src\\resources\\";
	public static long delayBetweenClicks = 1000;
	public enum BrowserType {
		CHROME, FIREFOX, PHANTOMJS, HTMLUNIT
	}

	public static void configureBrowser(BrowserType browserType) {
		Configuration.holdBrowserOpen = false;
		Configuration.startMaximized = false;
		Configuration.holdBrowserOpen = false;
		Configuration.screenshots = false;
		Configuration.timeout = TIMEOUT;

		switch (browserType) {
			case CHROME:
				System.setProperty("webdriver.chrome.driver", DRIVER_PATH + "chromedriver.exe");
				Configuration.browser = "chrome"; //System.setProperty("selenide.browser", "chrome"); //-Dbrowser=chrome
				configureChrome();
				break;
			case FIREFOX:
				FirefoxProfile firefoxProfile = new FirefoxProfile();
				firefoxProfile.setPreference("permissions.default.stylesheet", 2);
				firefoxProfile.setPreference("permissions.default.image", 2);
				firefoxProfile.setPreference("javascript.enabled", false);
				firefoxProfile.setPreference("dom.ipc.plugins.enabled.libflashplayer.so", "false");
				WebDriverRunner.setWebDriver(new FirefoxDriver(firefoxProfile));
				break;
			case PHANTOMJS:
				System.setProperty("phantomjs.binary.path", DRIVER_PATH + "phantomjs.exe");
				System.setProperty("browser", "phantomjs"); //-Dbrowser=chrome
				configurePhantom();
				break;
			case HTMLUNIT:
		}
	}

	private static void configureChrome() {
//		Map<String, Object> contentSettings = new HashMap<>();
//		contentSettings.put("images", 2);
//		Map<String, Object> preferences = new HashMap<>();
//		preferences.put("profile.default_content_settings", contentSettings);

		ChromeOptions options = new ChromeOptions();
		options.addArguments("test-type");
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("profile.default_content_settings", 2);
		options.setExperimentalOption("prefs", jsonObject);

		DesiredCapabilities caps = DesiredCapabilities.chrome();
//		caps.setCapability("chrome.prefs", preferences);
		caps.setCapability(ChromeOptions.CAPABILITY, options);

		WebDriverRunner.setWebDriver(new ChromeDriver(caps));
	}

	public static void configurePhantom(Proxy... proxies) {
		System.setProperty("phantomjs.binary.path", DRIVER_PATH + "phantomjs.exe");
		System.setProperty("browser", "phantomjs"); //-Dbrowser=chrome

		LoggingPreferences logs = new LoggingPreferences();
		logs.enable(LogType.DRIVER, Level.WARNING);
		logs.enable(LogType.BROWSER, Level.WARNING);
		logs.enable(LogType.CLIENT, Level.WARNING);
		logs.enable(LogType.PERFORMANCE, Level.WARNING);
		logs.enable(LogType.SERVER, Level.WARNING);

		DesiredCapabilities dcaps = DesiredCapabilities.phantomjs();
		dcaps.setCapability(CapabilityType.LOGGING_PREFS, logs);
		dcaps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[]{
				"--webdriver-loglevel=ERROR", "--web-security=false", "--ignore-ssl-errors=true", "--load-images=false"}); //NONE
		dcaps.setJavascriptEnabled(true);
		dcaps.setCapability(TAKES_SCREENSHOT, false);
		dcaps.setCapability(ACCEPT_SSL_CERTS, true);
		dcaps.setCapability(SUPPORTS_ALERTS, true);
		if (proxies != null && proxies.length == 1) {
			dcaps.setCapability("proxy", proxies[0]);
		}
		WebDriverRunner.setWebDriver(new PhantomJSDriver(dcaps));
	}

	protected void waitForPageLoad(int timeout) {
		Wait<WebDriver> wait = new WebDriverWait(WebDriverRunner.getWebDriver(), timeout);
		wait.until(driver -> {
			String state = String.valueOf(((JavascriptExecutor) driver).executeScript("return document.readyState"));
			System.out.printf("current window state=%s\n", state);
			return "complete".equals(state);
		});
	}

	public static void humanWait() {
		humanWait(delayBetweenClicks);
	}

	public static void humanWait(long timeoutMillis) {
		int range = 1000;
		long duration = Math.max(0, timeoutMillis - range >> 1) + (long) (Math.random() * range);
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void strictWait() {
		long duration = 2000;
		strictWait(duration);
	}

	public static void strictWait(long duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/*
		WebDriverRunner.getWebDriver();
        $(By.name("email")).sendKeys("johny");
        $(By.name("email")).shouldHave(Condition.text("Selenide.org"));
        $$("#ires li.g").shouldHave(size(10));
        $("#ires").find(By.linkText("selenide.org")).shouldBe(visible);
        $("#username").shouldHave(cssClass("green-text"));
        setValue(By.name("user.name"), "johny");
        selectRadio("user.gender", "male");
        selectOption(By.name("user.preferredLayout"), "plain");
        selectOptionByText(By.name("user.securityQuestion"), "What is my first car?");
        followLink(By.id("submit"));
        takeScreenShot("complex-form.png");
        waitFor("#username");
        waitUntil(By.id("username"), hasText("Hello, Johny!"));
        waitUntil("#username", hasText("Hello, Johny!"));
        waitUntil("#username", hasAttribute("name", "user.name"));
        waitUntil("#username", hasClass("green-button"));
        waitUntil("#username", hasValue("Carlson"));
        waitUntil("#username", appears);
        waitUntil("#username", disappears);
        $("#customerContainer").shouldNot(exist);
        $("TEXTAREA").shouldHave(value("John"));
        $(byText("Customer profile"));
        $("#customerContainer").should(matchText("profile"));
        $("li", 5); //element by index
        selectRadio(By.name("sex"), "woman");
        refresh();         url();         title();         source();
        $("#username").waitUntil(hasText("Hello, Johny!"), 8000);
        WebDriverRunner.setDriver(new org.openqa.selenium.phantomjs.PhantomJSDriver(capabilities));
        ((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView();" ,webElement);
        ((JavascriptExecutor)driver).executeScript("window.scrollBy(" + x + "," + y + ");");
        String code = "window.scroll(" + (webElement.getLocation().x + x) + "," + (webElement.getLocation().y + y) + ");";
		((JavascriptExecutor)driver).executeScript(code, webElement, x, y);
		((JavascriptExecutor) WebDriverRunner.getWebDriver()).executeScript("return document.readyState;");
		getWebDriver().getWindowHandles()
		Selenide.switchToWindow("Test::alerts");
		switchToWindow(0);
		new Actions(WebDriver)
			.KeyDown(Keys.Control)
			.KeyDown(Keys.Shift)
			.Click(tab)
			.KeyUp(Keys.Shift)
			.KeyUp(Keys.Control)
			.Perform();
		new Actions(WebDriver)
			.SendKeys(Keys.Control + "w")
			.Perform();
     */
}