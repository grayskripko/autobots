package com.skripko.common;

/**
 * Created by Skripko Sergey on 26.06.2015.
 */
public class Login {
	private String url;
	private String login;
	private boolean appendix;

	public Login(String url, String login) {
		this.login = login;
		this.url = url;
	}

	public String getLogin() {
		return login;
	}

	public String getUrl() {
		return url;
	}

	public boolean isAppendix() {
		return appendix;
	}

	public void setAppendix(boolean appendix) {
		this.appendix = appendix;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Login login = (Login) o;

		return url.equals(login.url);

	}

	@Override
	public int hashCode() {
		return url.hashCode();
	}
}
