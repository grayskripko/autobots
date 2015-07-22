package com.skripko.freelance.platform;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.skripko.common.SelenideUtils;
import com.skripko.freelance.Job;
import com.sun.istack.internal.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Skripko Sergey on 29.06.2015.
 */
public class Freelancer implements FlProcessor {
	public static final FlProcessor instance = new Freelancer();
	public static final String URL_DOMAIN = "https://www.freelancer.com/";
	private static Set<SearchQuery> searchQueries = new LinkedHashSet<>();

	static {
		searchQueries.add(new SearchQuery("hadoop", URL_DOMAIN + "jobs/s-hadoop-Spark/?cl=l-en"));

		searchQueries.add(new SearchQuery("data science", URL_DOMAIN +
				"jobs/s-bigdata-data_science-Machine_Learning-r/?cl=l-en"));
		searchQueries.add(new SearchQuery("mining", URL_DOMAIN + "jobs/s-Data_Mining-Web_Scraping/?cl=l-en"));
	}

	private Freelancer() {
	}

	@Override
	public Set<SearchQuery> getSearchQueries() {
		return searchQueries;
	}

	@Override
	public boolean clickNextPage() {
		SelenideElement nextButton = $(JobListSelectors.NEXT_BUTTON);
		if (!nextButton.exists() || nextButton.has(Condition.hasClass(JobListSelectors.DISABLED_CLASS.substring(1)))) {
			return false;
		}
		nextButton.click();
		return true;
	}

	@Override
	public ElementsCollection getJobElements() {
		try {
			SelenideUtils.humanWait(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return $(JobListSelectors.ROOT).$$(JobListSelectors.JOB_CLASS);
	}

	@Override
	public Job parseElement(String queryName, SelenideElement el) {
		String idUrl;
		try {
			idUrl = el.$(JobSelectors.TITLE).$("a").attr("href");
		} catch (Exception | Error e) {
			throw e;
		}

		String timePostedStr = el.$(JobSelectors.TIME_POSTED).getText();
		long lifeDayDuration = extractJobDuration(timePostedStr);

		String applicantsStr = el.$(JobSelectors.BID).getText();
		int proposalCount = -1;
		if (isInteger(applicantsStr)) {
			proposalCount = Integer.parseInt(applicantsStr);
		}
		return new Job(getClass().getSimpleName(), queryName, idUrl, proposalCount, lifeDayDuration);
	}

	private static boolean isInteger(String s) {
		Scanner sc = new Scanner(s.trim());
		if (!sc.hasNextInt()) {
			return false; // we know it starts with a valid int, now make sure there's nothing left!
		}
		sc.nextInt();
		return !sc.hasNext();
	}

	private long extractJobDuration(@NotNull String strDate) {
		if (strDate == null || strDate.isEmpty()) {
			throw new IllegalArgumentException();
		}

		if (strDate.toLowerCase().contains("сегодня")) {
			return 0;
		}
		try {
			Date date = new SimpleDateFormat("MMM dd, yyyy", Locale.US).parse(strDate);
			return (System.currentTimeMillis() - date.getTime()) / (1000 * 60 * 60 * 24);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Date parse exception. Wrong date pattern for:" + strDate);
		}
	}

	static class JobListSelectors {
		public static final String ROOT = "#browse-project-table";
		public static final String JOB_CLASS = ".project-details";
		public static final String NEXT_BUTTON = "#pagination_top_next";
		public static final String DISABLED_CLASS = ".disabled";
	}

	static class JobSelectors {
		public static final String TITLE = ".title-col";
		public static final String TIME_POSTED = ".started-col";
		public static final String BID = ".bids-col-inner";
	}
}
