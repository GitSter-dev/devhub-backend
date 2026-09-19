package com.application.devhub.mail;

public enum EmailTemplate {

    VERIFICATION("verification"),
    PASSWORD_RESET("password-reset"),
    PASSWORD_CHANGED("password-changed");

    private final String name;

    EmailTemplate(String name) {
        this.name = name;
    }

    public String templatePath() {
        return "email/" + name;
    }

    public String stylesheetPath() {
        return "stylesheets/email/" + name + ".css";
    }

    public String subjectKey() {
        return "email." + name + ".subject";
    }
}
