package org.plugin.support.googlesheets;

import java.util.List;
import java.util.Map;

public class CassDataPOJO {

    private String orgName;

    private String formName;
    private String spreadsheetId;
    private List<Map<Question, String>> forms;

    public CassDataPOJO(String orgName, String formName, String spreadsheetId, List<Map<Question, String>> forms) {
        this.orgName = orgName;
        this.formName = formName;
        this.spreadsheetId = spreadsheetId;
        this.forms = forms;
    }

    public String getOrgName() {
        return orgName;
    }

    public String getFormName() {
        return formName;
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public List<Map<Question, String>>  getForms() {
        return forms;
    }

}
