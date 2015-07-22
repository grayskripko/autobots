package com.skripko;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.skripko.common.SelenideUtils;

import javax.swing.*;
import java.util.Set;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
/*вести учет только постов, написанных админами. не репосты и не посты из доставки*/

public class VkMain extends SelenideUtils {
	public static final String LOGIN = "grayskripko@gmail.com";
	public static final String PASS_KEY = "logv";
	public static final int POSTS_MONITORING_COUNT = 50;
	public static final int EXCEL_SCANNED_DAYS = 7;
	public static final String[] PUBLIC_URLS = {"https://vk.com/vk.atheism"};

	static {
		SelenideUtils.configureBrowser(2000);
	}

	public static void main(String[] args) {
		String urlForException = null;
		try {
			for (String publicUrl : PUBLIC_URLS) {
				urlForException = publicUrl;
				new VkMain().execute(publicUrl);
			}
		} catch (Exception | Error e) {
			String type = (e instanceof Exception) ? "Exception" : "Error";
			JFrame frame = new JFrame();
			String message = String.format("%s:\n%s", urlForException, e.getMessage().replace("Screenshot: \n", ""));
			JOptionPane.showMessageDialog(frame, message, type, JOptionPane.WARNING_MESSAGE);
		}
		System.exit(0);
	}

	private void execute(String publicUrl) {
		String adminTabUrlSuffix = "?act=users&tab=admins";
		logInAndGoAdminTab(publicUrl + adminTabUrlSuffix, LOGIN, System.getenv(PASS_KEY));
		Set<User> admins = getAdmins();
		//Set<Post> excelPosts = getExcelPosts(EXCEL_SCANNED_DAYS);
		Set<Post> newPosts = getPostedPosts(publicUrl, POSTS_MONITORING_COUNT); //todo reminder - posponed part was removed
		/*if ($("#wall_postponed").isDisplayed()) {
            newPosts.addAll(processPostponed());
        }*/
	}

	private void logInAndGoAdminTab(String url, String login, String pass) {
		open(url);
		$("#email").setValue(login);
		$("#pass").setValue(pass);
		$("#login").submit();
//		waitForPageLoad(20);
		$("#email").should(Condition.disappears);
		/*SelenideElement probablyWarnMessage = $("#message");//.waitUntil(disappears, 10000);
		if (probablyWarnMessage.exists() && probablyWarnMessage.text() != null
				&& probablyWarnMessage.text().contains("Не удается войти")) {
			throw new RuntimeException("Wrong login/password");
		}*/
	}

	private Set<User> getAdmins() {
        /*int dryShots = 0;
        while (!$("gedit_users_more_admins").isDisplayed()) { //todo test block
            if (dryShots > 10) {
                terminateProcess("2114090914 id=..");
            }
            ((JavascriptExecutor) getWebDriver()).executeScript("GroupsEdit.uShowMore()");
        }*/
		ElementsCollection adminEls = $("#gedit_users_rows_admins").findAll(".gedit_user_lnk");
		Set<User> admins = adminEls.stream().map(User::new).collect(Collectors.toSet());
		return admins;
	}

	private Set<Post> getExcelPosts(int excelScannedDays) {
		return null;
	}

	private Set<Post> getPostedPosts(String url, int count) {
        /*int dryShots = 0; //el.scrollTo
        while (!$("gedit_users_more_admins").isDisplayed()) { //todo test block
            if (dryShots > 10) {
                terminateProcess("2114090914 id=..");
            }
            ((JavascriptExecutor) getWebDriver()).executeScript("GroupsEdit.uShowMore()");
        }*/
		open(url);
		ElementsCollection postEls = $("#page_wall_posts").findAll(".gedit_user_lnk");
		Set<Post> posts = postEls.stream().map(Post::new).collect(Collectors.toSet());
		return posts;
	}

    /*private Set<Post> processPostponed() {
        $("#wall_postponed_link").click();
        SelenideElement parent = $("#wall_postponed_posts");
        ElementsCollection allSuggestions = $$("#page_suggestions > div")*//*.shouldHaveSize(10)*//*;
        System.out.println(">> " + allSuggestions.size());
        $("#page_suggest_more").shouldBe(exist, visible).click();
        return null;
    }*/

}
