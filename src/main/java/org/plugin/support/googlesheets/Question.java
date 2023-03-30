package org.plugin.support.googlesheets;

public class Question {

    private String ques;
    private Integer priority;

    public Question(String ques, Integer priority) {
        this.ques = ques;
        this.priority = priority;
    }

    public String getQues() {
        return ques;
    }

    public Integer getPriority() {
        return priority;
    }
}
