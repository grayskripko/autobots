package com.skripko;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

public class User {
    private static final User ANON = new User("","anon");
    private String link;
    private String name;

    public User(String link, String name) {
        this.link = link;
        this.name = name;
    }

    public User(SelenideElement adminEl) {
        if (!adminEl.has(Condition./*attribute("class",*/hasClass("gedit_user_lnk"))
                && !adminEl.has(Condition.hasClass("wall_signed_by"))
                && !adminEl.has(Condition.hasClass("wpe_auth_lnk"))) {
            throw new RuntimeException("Wrong user constructor");
        }
        link = adminEl.getAttribute("href");
        name = adminEl.getText();
    }

    public static User getAnon() {
        return ANON;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        if (!link.equals(user.link)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return link.hashCode();
    }
}