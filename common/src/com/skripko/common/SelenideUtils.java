package com.skripko.common;

import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

public class SelenideUtils {
	public static final long TIMEOUT = 10000;
    public static final int AJAX_WAIT = 3;
	public static long sleepBetweenClicks;

    public static void configureBrowser(long sleepBetweenClicks) {
        System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir")
				+ "\\common\\src\\resources\\chromedriver.exe");
        System.setProperty("selenide.browser", "chrome"); //-Dbrowser=chrome
        System.setProperty("selenide.timeout", Long.toString(TIMEOUT));
        System.setProperty("selenide.start-maximized", "false");
        System.setProperty("selenide.screenshots", "false");
        System.setProperty("selenide.holdBrowserOpen", "false");
		SelenideUtils.sleepBetweenClicks = sleepBetweenClicks;
	}

	protected void waitForPageLoad(int timeout) {
		Wait<WebDriver> wait = new WebDriverWait(WebDriverRunner.getWebDriver(), timeout);
		wait.until(driver -> {
				String state = String.valueOf(((JavascriptExecutor) driver).executeScript("return document.readyState"));
				System.out.printf("current window state=%s\n", state);
				return "complete".equals(state);
		});
	}

	public static void humanWait() throws InterruptedException {
		humanWait(sleepBetweenClicks);
	}

	public static void humanWait(long timeoutMillis) throws InterruptedException {
		long duration = Math.max(0, timeoutMillis - 1000) + (long) (Math.random() * 2000);
		Thread.sleep(duration);
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