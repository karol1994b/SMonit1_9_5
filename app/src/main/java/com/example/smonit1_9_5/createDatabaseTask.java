package com.example.smonit1_9_5;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class createDatabaseTask extends AsyncTask<Context, Void, CursorAdapter> {

    private static final String TAG = "SMonit";
    private Cursor resultsCursor;
    //WPROWADZONO ZMIENNĄ KONTEKST, KTÓRA JEST POBIERANA JAKO PARAMETR NA WEJŚCIU ASYNCTASKU
    private Context context;

    private static Application instance;

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    @Override
    protected CursorAdapter doInBackground(Context... cont) {

        this.context=cont[0];

        CursorAdapter resultsAdapter = null;
        try {
            SQLiteOpenHelper ecgDatabaseHelper = new ECGDatabaseHelper(this.context);
            SQLiteDatabase db = ecgDatabaseHelper.getWritableDatabase();
            resultsCursor = db.query("RESULTS",
                    new String[] { "_id", "DATA0", "DATA1", "DATA2"},
                    null,
                    null,
                    null, null, null);
            resultsAdapter = new SimpleCursorAdapter(this.context,
                    android.R.layout.simple_list_item_1,
                    resultsCursor,
                    new String[]{"DATA0"},
                    new int[]{android.R.id.text1}, 0);

            Log.d(TAG, "createDatabaseTask się melduje");
        }
        catch (SQLiteException e) {
            Toast.makeText(this.context, "Baza danych jest niedostępna", Toast.LENGTH_SHORT).show();
            Log.d("TAG", "Problem z bazą");
        }
        return resultsAdapter;
    }

    @Override
    protected void onPostExecute(CursorAdapter resultsAdapter) {
        //listResults.setAdapter(resultsAdapter);
    }

}
