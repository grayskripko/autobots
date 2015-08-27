package com.skripko.common;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.skripko.object.Transaction;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.switchTo;
import static com.codeborne.selenide.WebDriverRunner.closeWebDriver;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.skripko.common.ProxyUtils.getConnectionSpeed;
import static com.skripko.object.Transaction.Option.SOFT_ERROR;
import static org.openqa.selenium.remote.CapabilityType.*;

public class SelenideUtils {
	private static long startTime = System.currentTimeMillis();
	public static final long TIMEOUT = 10000;
	public static final int AJAX_WAIT = 3;
	public static final String DRIVER_PATH = System.getProperty("user.dir") + "\\common\\src\\resources\\";
	public static long delayBetweenClicks = 1000;

	public enum BrowserType {
		CHROME, FIREFOX, PHANTOMJS, HTMLUNIT
	}

	public enum BrowserCapability {
		holdBrowserOpen, startMaximized, screenshots
	}

	public static boolean configureBrowser(BrowserType browserType, BrowserCapability... capabilities) {
		Configuration.holdBrowserOpen = false;
		Configuration.startMaximized = false;
		Configuration.screenshots = false;
		if (capabilities != null) {
			Arrays.asList(capabilities).stream().forEach(cap -> {
				try {
					Configuration.class.getField(cap.toString()).setBoolean(null, true);
				} catch (IllegalAccessException | NoSuchFieldException e) {
					throw new RuntimeException(e);
				}
			});
		}
		Configuration.timeout = TIMEOUT;

		switch (browserType) {
			case CHROME:
				System.setProperty("webdriver.chrome.driver", DRIVER_PATH + "chromedriver.exe");
				Configuration.browser = "chrome"; //System.setProperty("selenide.browser", "chrome"); //-Dbrowser=chrome
//				customConfigureChrome();
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
				customConfigurePhantom();
				break;
			case HTMLUNIT:
		}

		Thread thread = new Thread(() -> {
			print("Current speed: " + getConnectionSpeed(true));
			closeWebDriver();
		});
		thread.start();
		/*try {
			thread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}*/

		return true;
	}

	private static void customConfigureChrome() {
		ChromeOptions options = new ChromeOptions();
		options.addArguments("test-type");
		options.addExtensions(new File("D:\\dev\\projects\\Java\\autobots\\common\\src\\resources\\Block-image_v1.1.crx"));

		DesiredCapabilities caps = DesiredCapabilities.chrome();
		caps.setCapability(ChromeOptions.CAPABILITY, options);
		WebDriverRunner.setWebDriver(new ChromeDriver(caps));
	}

	public static void customConfigurePhantom(Proxy... proxies) {
		System.setProperty("phantomjs.binary.path", DRIVER_PATH + "phantomjs.exe");
		System.setProperty("browser", "phantomjs"); //-Dbrowser=chrome

		LoggingPreferences logs = new LoggingPreferences();
		logs.enable(LogType.DRIVER, Level.WARNING);
		logs.enable(LogType.BROWSER, Level.WARNING);
		logs.enable(LogType.CLIENT, Level.WARNING);
		logs.enable(LogType.PERFORMANCE, Level.WARNING);
		logs.enable(LogType.SERVER, Level.WARNING);

		DesiredCapabilities dcaps = DesiredCapabilities.phantomjs();
		dcaps.setCapability(LOGGING_PREFS, logs);
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

	/**
	 * more flexible with function vararg, don't touch without monitoring all dependencies
	 */
	public static List<String> openNewTabAndMap(SelenideElement mainEl, Supplier<String>... suppliers) {
		if (suppliers == null) {
			throw new IllegalArgumentException();
		}
		String mainTabHandle = getWebDriver().getWindowHandle();
		int openTabsCount = getWebDriver().getWindowHandles().size();

		new Actions(getWebDriver()).keyDown(Keys.CONTROL).click(mainEl).keyUp(Keys.CONTROL).perform();
		WebDriver secondDriver;
		try {
			secondDriver = switchTo().window(openTabsCount);
		} catch (IndexOutOfBoundsException ignored) {
			humanWait(4000);
			try {
				secondDriver = switchTo().window(openTabsCount);
			} catch (IndexOutOfBoundsException ignoredTwo) {
				print("!!! Can not found second tab");
				return null;
			}
		}
		List<String> result = Arrays.asList(suppliers).stream().map(Supplier::get).collect(Collectors.toList());
//		$("#content").screenshot();
		secondDriver.close();
		switchTo().window(mainTabHandle);
		return result;
	}

	//todo change for work
	public static List<String> txOpenNewTabAndMap(SelenideElement mainEl, Supplier<String>... suppliers) {
		Thread mainThread = Thread.currentThread();

		List<String> txResult = (List<String>) new Transaction(20000, SOFT_ERROR)
				.executeWithTimeLimit(() -> openNewTabAndMap(mainEl, suppliers)
		);
		return txResult;
	}

	protected void waitForPageLoad(int timeout) {
		Wait<WebDriver> wait = new WebDriverWait(getWebDriver(), timeout);
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

	public static void print(Object mesObj) {
		System.out.println(String.format("[%ssec] %s", (System.currentTimeMillis() - startTime) / 1000, String.valueOf(mesObj)));
	}

	public static void print(Object mesObj, Object... values) {
		String mes = String.format(String.valueOf(mesObj), values);
		System.out.println(String.format("[%ssec] %s", (System.currentTimeMillis() - startTime) / 1000, mes));
	}

	public static Object retryUntilAttached(Supplier<Object> callable) { //todo add attempts count and null return
		while (true) {
			try {
				return callable.get();
			} catch (StaleElementReferenceException e) {
				print("Trying once again");
			}
		}
	}

	public static boolean saveImage(String imageUrl, String imageName) { //"http://www.yahoo.com/image_to_read.jpg"
		BufferedImage image = null;
		try {
			URL url = new URL(imageUrl);
			image = ImageIO.read(url);
			String extension = AlgoUtils.getFirstRegexMatch(imageUrl, "(?<=\\.)\\w+\\s*$");
			return ImageIO.write(image, extension, new File(ExcelIO.DEFAULT_FILE_LOCATION + imageName));
		} catch (IOException e) {
			print("Can not save image: %s", imageUrl);
			return false;
		}
	}
	/*
		setValue(By.name("user.name"), "johny");
        selectRadio("user.gender", "male");
        selectOption(By.name("user.preferredLayout"), "plain");
        selectOptionByText(By.name("user.securityQuestion"), "What is my first car?");
        followLink(By.id("submit"));
        takeScreenShot("complex-form.png");
        waitFor("#username");
        waitUntil(By.id("username"), hasText("Hello, Johny!"));
        waitUntil("#username", hasAttribute("name", "user.name"));
        waitUntil("#username", disappears);
        $("#customerContainer").shouldNot(exist);
        $("TEXTAREA").shouldHave(value("John"));
        $(byText("Customer profile"));
        $("#customerContainer").should(matchText("profile"));
        selectRadio(By.name("sex"), "woman");
        refresh();         url();         title();         source();
        $("#username").waitUntil(hasText("Hello, Johny!"), 8000);
        WebDriverRunner.setDriver(new org.openqa.selenium.phantomjs.PhantomJSDriver(capabilities));
        ((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView();" ,webElement);
        ((JavascriptExecutor)driver).executeScript("window.scrollBy(" + x + "," + y + ");");
        String code = "window.scroll(" + (webElement.getLocation().x + x) + "," + (webElement.getLocation().y + y) + ");";
		((JavascriptExecutor)driver).executeScript(code, webElement, x, y);
		((JavascriptExecutor) WebDriverRunner.getWebDriver()).executeScript("return document.readyState;");
		Selenide.switchToWindow("Test::alerts");
     */
}