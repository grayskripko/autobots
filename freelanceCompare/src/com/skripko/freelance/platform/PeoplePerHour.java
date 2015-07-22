/*package com.skripko.freelance.platform;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.skripko.freelance.Job;
import org.openqa.selenium.Keys;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.codeborne.selenide.Selenide.$$;


public class PeoplePerHour implements FlProcessor {
	public static final FlProcessor instance = new PeoplePerHour();
	public static final String CONST_JOB_URL_PART = "https://www.upwork.com/jobs/";
	private static Set<SearchQuery> searchQueries = new LinkedHashSet<>();

	static {
		searchQueries.add(new SearchQuery("data science", CONST_JOB_URL_PART +
				"#filter/?q=skills%3A(data-science)%20OR%20skills%3A(data-analysis)%20OR%20skills%3A(big-data)" +
				"%20OR%20skills%3A(machine-learning)%20OR%20skills%3A(r)&sortBy=s_ctime+desc"));//&spellcheck=1&highlight=1&sortBy=s_ctime+desc
		searchQueries.add(new SearchQuery("mining", CONST_JOB_URL_PART + "?q=skills%3A(data-mining)" +
				"%20OR%20skills%3A(parse)%20OR%20skills%3A(crawlers)%20OR%20skills%3A(web-crawler)" +
				"%20OR%20skills%3A(data-scraping)%20OR%20skills%3A(web-scraping)&sortBy=s_ctime+desc"));

		searchQueries.add(new SearchQuery("selenium", CONST_JOB_URL_PART + "?q=skills%3A(selenium)" +
				"%20OR%20skills%3A(selenium-webdriver)%20OR%20skills%3A(scrapy-framework)&sortBy=s_ctime+desc"));
		searchQueries.add(new SearchQuery("hadoop", CONST_JOB_URL_PART + "?q=skills%3A(hadoop)%20OR%20skills%3A(r-hadoop)%20OR%20skills%3A(apache-spark)&sortBy=s_ctime+desc"));
		searchQueries.add(new SearchQuery("shiny", CONST_JOB_URL_PART + "?q=shiny&cn1%5B%5D=Data%20Science%20%26%20Analytics&sortBy=s_ctime+desc"));
		searchQueries.add(new SearchQuery("documentum", CONST_JOB_URL_PART + "?or_terms=documentum+dql&cn1%5B%5D=IT+%26+Networking", "?or_terms=documentum+dql&cn1%5B%5D=Web%2C+Mobile+%26+Software+Dev&sortBy=s_ctime+desc"));
	}

	private PeoplePerHour() {
	}

	@Override
	public Set<SearchQuery> getSearchQueries() {
		return searchQueries;
	}

	@Override
	public boolean clickNextPage() {
		SelenideElement jobListFooter = getJobListFooter();
		if (!jobListFooter.$("nav").exists()) {
			return false;
		}
		ElementsCollection nextButtonCol = jobListFooter.$$(JobListSelectors.PAGER_CLASS).filter(Condition.hasText("Next"));
		if (nextButtonCol == null || JobListSelectors.NEXT_DISABLED_CLASS.equals(nextButtonCol.get(0).attr("class"))) {
			return false;
		}
		nextButtonCol.get(0).click();
		return true;
	}

	private SelenideElement getJobListFooter() {
		return $(JobListSelectors.JOB_LIST_ROOT).$("footer");
	}

	@Override
	public ElementsCollection getJobElements() {
		return $(JobListSelectors.JOB_LIST_ROOT).$$(JobListSelectors.JOB_CLASS);
	}

	@Override
	public Job parseElement(SelenideElement el) {
		String idUrl = el.$(JobSelectors.JOB_TITLE_CLASS).attr("href").replace(CONST_JOB_URL_PART, "");

		String timePostedStr = el.$(JobSelectors.JOBS_TIME_POSTED_CLASS).
				shouldHave(Condition.matchText(".*" + JobSelectors.JOB_DURATION_PATTERN + ".*")).getText();
		long lifeDayDuration = extractJobDuration(timePostedStr);

		el.$(JobSelectors.JOB_LINK).sendKeys(Keys.CONTROL + "t");
		el.sendKeys(Keys.CONTROL + "\t");
		String applicantsStr = $$("span").find(Condition.attribute("ng-bind", "data.applicantsCount")).getText();
		el.sendKeys(Keys.CONTROL + "\t");
		int proposalCount = Integer.parseInt(applicantsStr);
		return new Job(idUrl, lifeDayDuration, proposalCount);
	}

	private long extractJobDuration(String strDate) {
		if (strDate == null || strDate.isEmpty()) {
			throw new IllegalArgumentException();
		}

		if (strDate.contains("hour")) {
			return 0;
		} else if (strDate.contains("day")) {
			return Long.parseLong(strDate.split(" ")[1]);
		} else if (strDate.contains("month")) {
			return 30 * Long.parseLong(strDate.split(" ")[1]);
		}
		return 365;
	}

	static class JobListSelectors {
		public static final String JOB_LIST_ROOT = ".oListLite.jsSearchResults";
		public static final String JOB_CLASS = ".oMed.oJobTile.jsSimilarTile";
		public static final String PAGER_CLASS = ".oPager";
		public static final String NEXT_DISABLED_CLASS = "oPager.isDisabled";
	}

	static class JobSelectors {
		public static final String JOB_TITLE_CLASS = ".oVisitedLink";
		public static final String JOBS_TIME_POSTED_CLASS = ".jsPosted";
		public static final String JOB_DURATION_PATTERN = "Posted";
		public static final String JOB_LINK = ".oVisitedLink";
	}
}
*/