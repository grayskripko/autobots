package com.skripko;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.skripko.common.SelenideUtils;

import static com.codeborne.selenide.Condition.*;

public class Post {
    public final int SNIPPET_SIZE = 50;

    private String id;
    private String link;
    private String snippet;
    private VkDate date;
    private User user;

    public Post(SelenideElement postEl) {
        if (!postEl.has(Condition.hasClass("post"))) {
            throw new RuntimeException("Wrong post constructor");
        }
        id = postEl.getAttribute("id");
        snippet = postEl.$(".wall_post_text").getText().substring(0, SNIPPET_SIZE);
        SelenideElement footer = postEl.$(".replies").$("small");
        link = footer.$("a").getAttribute("href"); //link = publicUrl + id.replace("post", "?w=wall"); //"?w=wall-50702969_2768"  id="post-50702969_2748"
        date = new VkDate(footer.$(".rel_date").getText());
        user = getUserFromPostFooter(footer);
    }

    private User getUserFromPostFooter(SelenideElement footer) { //todo gap debug
        if (footer.$(".wall_signed_by").exists()) { //if user element is shown
            return new User(footer.$(".wall_signed_by"));
        } else if (footer.$(".post_edit_button").exists()) { //try to click an edit pencil
            SelenideElement pencil = footer.$(".post_edit_button");
            pencil.hover().waitUntil(present, SelenideUtils.AJAX_WAIT).click();
            /*SelenideElement userShowCheckbox = footer.$("#wpe_signed").waitUntil(appear, AJAX_WAIT);
            if (footer.has(Condition.hasClass("postponed"))) { //postponed hasn't grayUser. Posted posts must have
                checkbox.shouldBe(disabled).click();
                SelenideElement saveButton = footer.$(".button_blue");
                saveButton.click();
                saveButton.waitUntil(disappear, AJAX_WAIT);

                pencil.hover();
                User user = new User(footer.$(".wall_signed_by"));
                pencil.waitUntil(appear, AJAX_WAIT).click();
                checkbox.waitUntil(appear, AJAX_WAIT).click();
                saveButton.click();
                saveButton.waitUntil(disappear, AJAX_WAIT);
                return user;
            } else {*/
            SelenideElement grayUserEl = pencil.$(".wpe_auth_lnk").shouldBe(visible);
            User user = new User(grayUserEl);
            SelenideElement cancelButton = footer.$(".button_cancel").$(".button");
            cancelButton.click();
            cancelButton.waitUntil(disappear, SelenideUtils.AJAX_WAIT);
            return user;
            //}
        } else {
            return User.getAnon();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public VkDate getDate() {
        return date;
    }

    public void setDate(VkDate date) {
        this.date = date;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        if (id != null ? !id.equals(post.id) : post.id != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}