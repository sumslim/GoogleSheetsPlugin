package org.plugin.support.googlesheets;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.cloud.spring.SpringAppContextProvider;
import org.springframework.context.ApplicationContext;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class GoogleSheetsService {

    public static void main(String[] args){
        final ApplicationContext ctx = SpringAppContextProvider.getApplicationContext("plugin-support.xml");
        final GoogleSheetsService service = ctx.getBean(GoogleSheetsService.class);
        service.run();

    }

    private void run(){
        int nThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        Cluster cluster = Cluster.builder()
                .addContactPoint("seed")
                .withPort(9042)
                .build();

        Session session = cluster.connect("my_keyspace");
        ResultSet rs = session.execute("SELECT * FROM table_name WHERE timestamp > dateOf(now() - 86400000)");
        List<CassDataPOJO> orgsWithUpdatedForms = GoogleSheetsUtil.getRowsUpdated(rs);
        for(CassDataPOJO orgWithUpdatedForms : orgsWithUpdatedForms){
            executor.submit(() -> {
                try {
                    process(orgWithUpdatedForms);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void process(final CassDataPOJO orgWithUpdatedForms) throws IOException {
        /* Load pre-authorized user credentials from the environment.
           TODO(developer) - See https://developers.google.com/identity for
            guides on implementing OAuth2 for your application. */
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(
                credentials);
        // Create the sheets API client
        Sheets service = new Sheets.Builder(new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName("Sheets samples")
                .build();
        String spreadsheetId = orgWithUpdatedForms.getSpreadsheetId();
        Spreadsheet spreadsheet = null;
        final String orgName = orgWithUpdatedForms.getOrgName();
        final String formName = orgWithUpdatedForms.getFormName();
        if(spreadsheetId == null){
            spreadsheet = GoogleSheetsUtil.createSpreadsheet(orgName + "_" + formName, service);
            spreadsheetId = spreadsheet.getSpreadsheetId();
        }
        List<Map<Question, String>> rows = orgWithUpdatedForms.getForms();
        if(rows == null || rows.isEmpty())
            return;
        List<Question> uniqueQues = new ArrayList<>();
        for(Map.Entry<Question, String> ques : rows.get(0).entrySet()){
            if(!uniqueQues.contains(ques.getKey())){
                uniqueQues.add(ques.getKey());
            }
        }
        //Sorting the question according to priority for getting actual view in google sheets
        uniqueQues.sort(Comparator.comparingInt(Question::getPriority));
        String range = GoogleSheetsUtil.getRangeofValuesToBeFilled(rows.size(), uniqueQues.size());
        List<List<Object>> values = new ArrayList<>();
        List<Object> row = new ArrayList<>();
        for(Question question : uniqueQues){
            row.add(question.getQues());
        }
        values.add(row);
        for(Map<Question, String> nrow : rows){
            row = new ArrayList<>();
            for(Question ques : uniqueQues){
                row.add(nrow.get(ques));
            }
            values.add(row);
        }
        ValueRange body = new ValueRange().setValues(values);

        //updating the spreadsheet with values in order
        UpdateValuesResponse result = service.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("RAW")
                .execute();
        System.out.printf("%d cells updated.", result.getUpdatedCells());

        //set permission to view file
        //standard code to set up the permission for view only access to selected users
        GoogleSheetsUtil.setPermission(credentials, spreadsheetId);
        String spreadsheetUrl = "https://docs.google.com/spreadsheets/d/" + spreadsheetId;

        //persist this url and meta data details in a mysql table for reference.

    }

}
