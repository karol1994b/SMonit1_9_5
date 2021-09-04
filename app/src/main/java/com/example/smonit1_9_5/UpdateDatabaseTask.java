//package com.example.smonit1_8_4;
//
//import android.app.Application;
//import android.content.ContentValues;
//import android.content.Context;
//import android.database.Cursor;
//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteException;
//import android.database.sqlite.SQLiteOpenHelper;
//import android.os.AsyncTask;
//import android.util.Log;
//import android.widget.CursorAdapter;
//import android.widget.SimpleCursorAdapter;
//import android.widget.Toast;
//
//public class UpdateDatabaseTask extends AsyncTask<String, Void, CursorAdapter> {
//
//    private static final String TAG = "SMonit";
//    ContentValues resultValues;
//    int ostatniMohikanin;
//    String results_db;
//    int theLastOne;
//    private Cursor resultsCursor;
//    private SQLiteDatabase db;
//
//    //Wstawka z kontekstem - dotychczas jako kontekst była podawana aktywność MonitActivity.this, a teraz tak już się nie da
//    // (chyba że parametrem wejściowym jak w createDatabaseTask
//    //Rozwiązanie znalezione na Stack'u
////    private static Application instance;
////
////    public static Context getContext() {
////        return instance.context;
////    }
//
//
//    private int insertResults(SQLiteDatabase db) {
//        String[] splitted_results_db = results_db.split("\\r?\\n");
//        String[][] splitted_channels_db = new String[splitted_results_db.length][3];
//
//        Log.d(TAG, "Split po raz pierwszy się melduje");
//
//        for (int z = 0; z < splitted_channels_db.length; z++) {
//            if (splitted_results_db[z].contains(",") && (splitted_results_db[z].length() > 8) && (splitted_results_db[z].length() < 12)) {
//                splitted_channels_db[z] = splitted_results_db[z].split(",",3);
//            }
//        }
//
//
//
//        for (int i = 0; i < splitted_channels_db.length; i++) {
//            resultValues.put("DATA0", splitted_channels_db[i][0]);
//            resultValues.put("DATA1", splitted_channels_db[i][1]);
//            resultValues.put("DATA2", splitted_channels_db[i][2]);
//            db.insert("RESULTS", null, resultValues);
//        }
//
//        Log.d(TAG, "Dodanie do bazy się melduje");
//        ostatniMohikanin = splitted_results_db.length;
//        return ostatniMohikanin;
//    }
//
//
//    private int insertResultsAgain(SQLiteDatabase db, Integer the_last_element) {
//        String[] splitted_results_db = results_db.split("\\r?\\n");
//        String[][] splitted_channels_db = new String[splitted_results_db.length][3];
//
//        for (int z = the_last_element; z < splitted_channels_db.length; z++) {
//            splitted_channels_db[z] = splitted_results_db[z].split(",",3);
//        }
//
//        for (int i = the_last_element; i < splitted_channels_db.length; i++) {
//
//            resultValues.put("DATA0", splitted_channels_db[i][0]);
//            resultValues.put("DATA1", splitted_channels_db[i][1]);
//            resultValues.put("DATA2", splitted_channels_db[i][2]);
//            db.insert("RESULTS", null, resultValues);
//        }
//
//        ostatniMohikanin = splitted_results_db.length;
//        return ostatniMohikanin;
//    }
//
//    protected void onPreExecute() {
//    }
//
//    protected CursorAdapter doInBackground(String... abc) {
//
//        resultValues = new ContentValues();
//        CursorAdapter resultsAdapter = null;
//
//        results_db = abc[0];
//
//        try {
//            SQLiteOpenHelper ECGDatabaseHelper = new ECGDatabaseHelper(getContext());
//            SQLiteDatabase db = ECGDatabaseHelper.getWritableDatabase();
//
//            if (!resultsCursor.moveToFirst() && !resultsCursor.moveToLast()) {
//                ostatniMohikanin = insertResults(db);
//                String t = Integer.toString(ostatniMohikanin);
//                Log.d(TAG, "Po raz pierwszy: Brak pierwszego punktu bazy, wrzuć dane");
//            } else {
//                String g = Integer.toString(theLastOne);
//                ostatniMohikanin = insertResultsAgain(db, theLastOne);
//                Log.d(TAG, "Po raz kolejny: Mamy pierwszy punkt bazy, nic nie dodawaj ze starych danych");
//            }
//
//            resultsAdapter = new SimpleCursorAdapter(getContext(),
//                    android.R.layout.simple_list_item_1,
//                    resultsCursor,
//                    new String[]{"DATA0"},
//                    new int[]{android.R.id.text1}, 0);
//
//        } catch (SQLiteException e) {
//            Toast.makeText(getContext(), "Baza danych jest niedostępna", Toast.LENGTH_SHORT).show();
//            Log.d("TAG", "Problem z bazą 2");
//        }
//
//        return resultsAdapter;
//    }
//
//    protected void onPostExecute(CursorAdapter resultsAdapter) {
//        theLastOne = ostatniMohikanin;
//
//        try {
//            ECGDatabaseHelper ecg1DatabaseHelper = new ECGDatabaseHelper(getContext());
//            db = ecg1DatabaseHelper.getWritableDatabase();
//            Cursor newCursor = db.query("RESULTS",
//                    new String[] { "_id", "DATA0", "DATA1", "DATA2"}, null, null, null, null, null);
//
//            resultsCursor = newCursor;
//
//        } catch (SQLiteException e) {
//            Toast.makeText(getContext(), "Baza danych jest niedostępna", Toast.LENGTH_SHORT).show();
//            Log.d("TAG", "Problem z bazą 3");
//        }
//    }
//}
