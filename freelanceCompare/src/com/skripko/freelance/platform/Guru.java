package com.skripko.freelance.platform;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.skripko.freelance.Job;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Skripko Sergey on 29.06.2015.
 */
public class Guru implements FlProcessor {
	public static final FlProcessor instance = new Guru();
	public static final String URL_DOMAIN = "http://www.guru.com/";
	public static final String URL_DOMAIN_SUFFIX = URL_DOMAIN + "d/jobs/q/";
	private static Set<SearchQuery> searchQueries = new LinkedHashSet<>();

	static {
		searchQueries.add(new SearchQuery("documentum", URL_DOMAIN_SUFFIX + "documentum/", URL_DOMAIN_SUFFIX + "dql/"));
		searchQueries.add(new SearchQuery("shiny", URL_DOMAIN_SUFFIX + "shiny/"));
		searchQueries.add(new SearchQuery("hadoop", URL_DOMAIN_SUFFIX + "hadoop/", URL_DOMAIN_SUFFIX + "spark/"));
		searchQueries.add(new SearchQuery("selenium", URL_DOMAIN_SUFFIX + "scrapy/"));

		searchQueries.add(new SearchQuery("data science",
				URL_DOMAIN_SUFFIX + "%22data-science%22/", URL_DOMAIN_SUFFIX + "%22data-analysis%22/",
				URL_DOMAIN_SUFFIX + "%22big-data%22/", URL_DOMAIN_SUFFIX + "%22machine-learning%22/"));
		searchQueries.add(new SearchQuery("mining",
				URL_DOMAIN_SUFFIX + "%22data-mining%22/", URL_DOMAIN_SUFFIX + "parse/",
				URL_DOMAIN_SUFFIX + "crawl", URL_DOMAIN_SUFFIX + "scrap/"));
	}

	private Guru() {
	}

	@Override
	public Set<SearchQuery> getSearchQueries() {
		return searchQueries;
	}

	@Override
	public boolean clickNextPage() {
		SelenideElement nextButton = $(JobListSelectors.PAGER_ROOT);
		if (!nextButton.exists()) {
			return false;
		}
		ElementsCollection lis = nextButton.$$("li");
		SelenideElement lastLiElement = lis.get(lis.size() - 1);
		if (lastLiElement.has(Condition.hasClass(JobListSelectors.ACTIVE_BUTTON_CLASS_NAME))
				|| lastLiElement.has(Condition.hasClass(JobListSelectors.DISABLED_BUTTON_CLASS_NAME))) {
			return false;
		}
		ListIterator<SelenideElement> iterator = lis.listIterator();
		while (iterator.hasNext()) {
			SelenideElement el = iterator.next();
			if (el.has(Condition.hasClass(JobListSelectors.ACTIVE_BUTTON_CLASS_NAME))) {
				iterator.next().$("a").click();
				return true;
			}
		}
		throw new IllegalStateException();
	}

	@Override
	public ElementsCollection getJobElements() {
		return $(JobListSelectors.ROOT).$$(JobListSelectors.JOB_CLASS);
	}

	@Override
	public Job parseElement(String queryName, SelenideElement el) {
		String idUrl = el.$(JobSelectors.TITLE).$("a").attr("href");

		String timePostedStr = el.$(JobSelectors.TIME_POSTED).getAttribute("data-date");
		long lifeDayDuration = extractJobDuration(timePostedStr);

		String[] attrText = $(JobSelectors.BID).getText().split("\\|");
		int proposalCount = Integer.parseInt(attrText[attrText.length - 1].split(" ")[0]);
		return new Job(getClass().getSimpleName(), queryName, idUrl, proposalCount, lifeDayDuration);
	}

	private long extractJobDuration(String strDate) {
		if (strDate == null || strDate.isEmpty()) {
			throw new IllegalArgumentException();
		}
		try {
			Date date = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss", Locale.US).parse(strDate);
			return (System.currentTimeMillis() - date.getTime()) / (1000 * 60 * 60 * 24);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Date parse exception. Wrong date pattern for:" + strDate);
		}
	}

	static class JobListSelectors {
		public static final String ROOT = "#serviceList";
		public static final String JOB_CLASS = ".serviceItem";
		public static final String PAGER_ROOT = "#ctl00_guB_ulpaginate";
		public static final String ACTIVE_BUTTON_CLASS_NAME = "active";
		public static final String DISABLED_BUTTON_CLASS_NAME = "disabled";
	}

	static class JobSelectors {
		public static final String TITLE = ".servTitle";
		public static final String TIME_POSTED = ".reltime_new.dt-style1";
		public static final String BID = ".projAttributes";
	}
}
