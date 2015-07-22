package com.skripko.freelance;

import java.util.Collection;

/**
 * Created by Skripko Sergey on 28.06.2015.
 */
public class FreelanceStatistics implements Comparable<FreelanceStatistics> {
	private String siteName;
	private String queryName;
	private long jobsPerDay;
	private long candidatesPerJob;
	private long expectedSearchDurationDays;

	public FreelanceStatistics(String siteName, String queryName, Collection<Job> jobs, int monitorPeriodDuration) {
		this.siteName = siteName;
		this.queryName = queryName;
		jobsPerDay = jobs.size() / monitorPeriodDuration;
		candidatesPerJob = Math.round(jobs.stream().mapToInt(Job::getProposalCount).average().getAsDouble());
		expectedSearchDurationDays = candidatesPerJob * jobs.size() / monitorPeriodDuration;
	}

	public String getQueryName() {
		return queryName;
	}

	public String getSiteName() {
		return siteName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		FreelanceStatistics that = (FreelanceStatistics) o;

		if (!siteName.equals(that.siteName)) return false;
		return queryName.equals(that.queryName);

	}

	@Override
	public int hashCode() {
		int result = siteName.hashCode();
		result = 31 * result + queryName.hashCode();
		return result;
	}

	@Override
	public int compareTo(FreelanceStatistics o) {
		int bySite = siteName.compareTo(o.getSiteName());
		if (bySite != 0) {
			return bySite;
		}
		return queryName.compareTo(o.getQueryName());
	}
}

