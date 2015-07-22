package com.skripko.freelance;

import com.codeborne.selenide.SelenideElement;
import com.skripko.common.ExcelIO;
import com.skripko.common.SelenideUtils;
import com.skripko.freelance.platform.FlProcessor;
import com.skripko.freelance.platform.Guru;
import com.skripko.freelance.platform.SearchQuery;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Selenide.open;

public class MainFreelanceComp {
	public static final int JOBS_OF_PERIOD = 30;
	public static final String OUTPUT_XLSX = "currentJob.xlsx";
	private static String passk = "psprt";
	private static final Class<? extends FlProcessor> CURRENT_SITE_CLASS = Guru.class;

	static {
		SelenideUtils.configureBrowser(1000);
	}

	public static void main(String[] args) throws IOException, IllegalAccessException {
		long startTime = System.currentTimeMillis();
		Reflections reflections = new Reflections(FlProcessor.class.getPackage().getName());
		Set<Class<? extends FlProcessor>> sites = reflections.getSubTypesOf(FlProcessor.class);
		Set<Job> jobs = new LinkedHashSet<>();
		for (Class<? extends FlProcessor> siteProcessorClass : sites) {
			try {
				if (siteProcessorClass == CURRENT_SITE_CLASS) {
					jobs.addAll(grabSiteJobs(siteProcessorClass));
				}
			} catch (Exception | Error e) {
				e.printStackTrace();
				System.out.printf("duration=%s\n", System.currentTimeMillis() - startTime);
				return;
			}
		}

		ExcelIO excelIO = new ExcelIO(OUTPUT_XLSX, ExcelIO.WRITE_MODE);
		Job instance = jobs.iterator().next();
		excelIO.createSheet(instance.getSite());
		List<String> columnNames = Arrays.asList(instance.getClass().getDeclaredFields()).stream()
				.map(Field::getName).collect(Collectors.toList());
		excelIO.printFirstRow(columnNames.toArray(new String[columnNames.size()]));
		for (Job job : jobs) {
			excelIO.printRow(job.getSite(), job.getQueryName(), job.getUrl(), job.getProposalCount(), job.getLifeDuration());
		}
		excelIO.close();
		System.out.printf("Program execution finished, duration=%s\n", System.currentTimeMillis() - startTime);
//		System.exit(0);
	}

	public static Set<Job> grabSiteJobs(Class<? extends FlProcessor> siteProcessorClass) throws NoSuchFieldException, IllegalAccessException {
		FlProcessor siteProcessor = (FlProcessor) siteProcessorClass.getField("instance").get(null);
		Set<Job> jobs = new LinkedHashSet<>();
		for (SearchQuery queryObj : siteProcessor.getSearchQueries()) {

			String siteName = siteProcessorClass.getSimpleName();
			for (String url : queryObj.getQueryUrls()) {
				String queryName = queryObj.getName();
				open(url);

				int pageNumber = 1;
				outer:
				do {
					System.out.printf("site=%s query=[%s] pageNumber=%s\n", siteName, queryName, pageNumber++);
					List<SelenideElement> jobElements = siteProcessor.getJobElements();
					if (jobElements == null || jobElements.size() == 0) {
						break; //necessarily
					}
					for (SelenideElement jobElement : jobElements) {
						Job job = siteProcessor.parseElement(queryName, jobElement);
						if (job.getLifeDuration() > MainFreelanceComp.JOBS_OF_PERIOD) {
							break outer;
						}
						jobs.add(job);
					}
				} while (siteProcessor.clickNextPage());
			}
		}
		return jobs;
	}
}
