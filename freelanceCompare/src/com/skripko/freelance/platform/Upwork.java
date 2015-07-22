package com.skripko.freelance.platform;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.skripko.freelance.Job;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

//missed "need to hire 3 freelancers"

/**
 * Created by Skripko Sergey on 29.06.2015.
 */
public class Upwork implements FlProcessor {
	public static final FlProcessor instance = new Upwork();
	public static final String URL_DOMAIN = "https://www.upwork.com/";
	public static final String URL_DOMAIN_SUFFIX = URL_DOMAIN + "o/jobs/browse/";
	private static Set<SearchQuery> searchQueries = new LinkedHashSet<>();

	static {
		searchQueries.add(new SearchQuery("data science", URL_DOMAIN_SUFFIX +
				"jobs/?q=skills%3A(data-science)%20OR%20skills%3A(data-analysis)%20OR%20skills%3A(big-data)" +
				"%20OR%20skills%3A(machine-learning)&highlight=1&sortBy=s_ctime+desc",
				URL_DOMAIN_SUFFIX + "jobs/?q=skills%3A(r)&highlight=1&sortBy=s_ctime+desc"));
		searchQueries.add(new SearchQuery("mining", URL_DOMAIN_SUFFIX + "jobs/?q=skills%3A(data-mining)" +
				"%20OR%20skills%3A(parse)%20OR%20skills%3A(crawlers)%20OR%20skills%3A(web-crawler)&highlight=1&sortBy=s_ctime+desc",
				URL_DOMAIN_SUFFIX + "jobs/?q=skills%3A(data-scraping)%20OR%20skills%3A(web-scraping)&highlight=1&sortBy=s_ctime+desc"));

		searchQueries.add(new SearchQuery("hadoop", URL_DOMAIN_SUFFIX + "?q=skills%3A(hadoop)%20OR%20skills%3A(r-hadoop)%20OR%20skills%3A(apache-spark)&sortBy=s_ctime+desc"));
		searchQueries.add(new SearchQuery("shiny", URL_DOMAIN_SUFFIX + "?q=shiny&cn1%5B%5D=Data%20Science%20%26%20Analytics&sortBy=s_ctime+desc"));
		searchQueries.add(new SearchQuery("selenium", URL_DOMAIN_SUFFIX + "?q=skills%3A(selenium)" +
				"%20OR%20skills%3A(selenium-webdriver)%20OR%20skills%3A(scrapy-framework)&sortBy=s_ctime+desc"));
		searchQueries.add(new SearchQuery("documentum", URL_DOMAIN_SUFFIX + "?q=documentum&sort=create_time+desc",
				URL_DOMAIN_SUFFIX + "?q=dql&sort=create_time+desc"));
	}

	private Upwork() {
	}

	@Override
	public Set<SearchQuery> getSearchQueries() {
		return searchQueries;
	}

	@Override
	public boolean clickNextPage() {
		SelenideElement jobListPager = $(JobListSelectors.ROOT).$(JobListSelectors.PAGER);
		if (!jobListPager.exists()) {
			return false;
		}
		SelenideElement nextButton = jobListPager.$$("li").filter(Condition.hasText("Next")).get(0);
		if (!nextButton.exists() || nextButton.has(Condition.hasClass("disabled"))) {
			return false;
		}
		nextButton.$("a").click();
		return true;
	}

	@Override
	public ElementsCollection getJobElements() {
		return $(JobListSelectors.ROOT).$$(JobListSelectors.JOB_CLASS);
	}

	@Override
	public Job parseElement(String queryName, SelenideElement el) {
		SelenideElement titleEl = el.$(JobSelectors.TITLE).shouldHave(Condition.attribute("itemprop", "url"));
		String idUrl = titleEl.attr("href");

		String timePostedStr = el.$(JobSelectors.TIME_POSTED).
				shouldHave(Condition.attribute("itemprop", "datePosted")).getAttribute("datetime").substring(0, 10);
		long lifeDayDuration = extractJobDuration(timePostedStr);

		new Actions(getWebDriver()).keyDown(Keys.CONTROL).click(titleEl).keyUp(Keys.CONTROL).perform();
		WebDriver secondDriver = switchTo().window(1);
		String applicantsStr = $$(".ng-binding").find(Condition.attribute("ng-bind", "data.applicantsCount")).getText();
		secondDriver.close();
		switchTo().window(0);
		int proposalCount = Integer.parseInt(applicantsStr);
		return new Job(getClass().getSimpleName(), queryName, idUrl, proposalCount, lifeDayDuration);
	}

	private long extractJobDuration(String strDate) {
		if (strDate == null || strDate.isEmpty()) {
			throw new IllegalArgumentException();
		}

		try {
			Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(strDate);
			return (System.currentTimeMillis() - date.getTime()) / (1000 * 60 * 60 * 24);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Date parse exception. Wrong date pattern for:" + strDate);
		}
	}

	static class JobListSelectors {
		public static final String ROOT = ".js-search-results";//".jobs-list";
		public static final String JOB_CLASS = ".job-tile";
		public static final String PAGER = ".pagination";
	}

	static class JobSelectors {
		public static final String TITLE = "header a";
		public static final String TIME_POSTED = ".js-posted time";
	}
}
