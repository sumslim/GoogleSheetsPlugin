package org.plugin.support.googlesheets;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.api.services.drive.model.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GoogleSheetsUtil {

    private static final String ORG_NAME = "org_name";
    private static final String FORM_NAME = "form_name";
    private static final String SPREADSHEET_ID = "spreadsheet_id";
    private static final String FORM = "form";
    private static final String USER_EMAIL = "user-email";
    private static final String ROLE = "reader"; // View-only access

    public static List<CassDataPOJO> getRowsUpdated(ResultSet rs){
        List<CassDataPOJO> formList = new ArrayList<>();
        Gson gson = new Gson();
        final Type resultType = new TypeToken<List<Map<Question, String>>>() {}.getType();
        final Iterator<Row> iterator = rs.iterator();
        while (iterator.hasNext()) {
            final Row row = iterator.next();
            final CassDataPOJO dataPOJO = new CassDataPOJO(row.getString(ORG_NAME), row.getString(FORM_NAME), row.getString(SPREADSHEET_ID), gson.fromJson(row.getString(FORM),resultType));
            formList.add(dataPOJO);
        }
        return formList;
    }

    public static String getRangeofValuesToBeFilled(int row, int columns){
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dateString = currentDate.format(formatter);
        String columnName = getColumnName(columns);
        return dateString + "!A1:" + columnName + row;
    }

    private static String getColumnName(int columns){
        int div = (columns-1) / 26;
        int rem = (columns-1) % 26;
        StringBuilder colName = new StringBuilder();
        while(div>1){
            colName.append("A");
            div--;
        }
        char ch = (char) (65+rem);
        colName.append(ch);
        return colName.toString();
    }
    public static Spreadsheet createSpreadsheet(String title, Sheets service) throws IOException {

        // Create new spreadsheet with a title
        Spreadsheet spreadsheet = new Spreadsheet()
                .setProperties(new SpreadsheetProperties()
                        .setTitle(title));
        spreadsheet = service.spreadsheets().create(spreadsheet)
                .setFields("spreadsheetId")
                .execute();
        // Prints the new spreadsheet id
        System.out.println("Spreadsheet ID: " + spreadsheet.getSpreadsheetId());
        return spreadsheet;
    }

    public static void setPermission(GoogleCredentials credentials, String spreadsheetId){
        //create the Google Drive client with appropriate credentials
        Drive driveService = new Drive();
        // Retrieve the existing permissions for the file
        PermissionList permissions = driveService.permissions().list(spreadsheetId).execute();

        // Check if the specified user already has permission to view the file
        boolean hasPermission = false;
        for (Permission permission : permissions.getPermissions()) {
            if (permission.getEmailAddress().equals(USER_EMAIL) && permission.getRole().equals(ROLE)) {
                hasPermission = true;
                break;
            }
        }

        Permission clientPermission = new Permission();
        clientPermission.setEmailAddress(USER_EMAIL);
        clientPermission.setRole("writer");
        clientPermission.setType("user");
        // If the user doesn't have permission yet, add the new permission
        if (!hasPermission) {
            driveService.permissions().create(spreadsheetId, clientPermission).execute();
            System.out.println("View-only permission set for user " + USER_EMAIL);
        } else {
            System.out.println("User " + USER_EMAIL + " already has view-only permission");
        }
    }
}
