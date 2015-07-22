package com.skripko.freelance;

import com.skripko.freelance.platform.FlProcessor;
import org.reflections.Reflections;

import java.util.Set;

/**
 * Created by Skripko Sergey on 27.06.2015.
 */
public class Job {
	private String site;
	private String queryName;
	private String url;
	private int proposalCount;
	private long lifeDuration;

	static {
		Reflections reflections = new Reflections(FlProcessor.class.getPackage().getName());
		Set<Class<? extends FlProcessor>> subTypes = reflections.getSubTypesOf(FlProcessor.class);
	}

	public Job(String site, String queryName, String url, int proposalCount, long lifeDuration) {
		this.url = url;
		this.lifeDuration = lifeDuration;
		this.proposalCount = proposalCount;
		this.queryName = queryName;
		this.site = site;
	}

	public String getQueryName() {
		return queryName;
	}

	public String getSite() {
		return site;
	}

	public String getUrl() {
		return url;
	}

	public long getLifeDuration() {
		return lifeDuration;
	}

	public int getProposalCount() {
		return proposalCount;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Job job = (Job) o;

		return url.equals(job.url);

	}

	@Override
	public int hashCode() {
		return url.hashCode();
	}
}
