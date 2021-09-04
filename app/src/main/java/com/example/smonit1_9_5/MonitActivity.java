package com.example.smonit1_9_5;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import static android.os.Environment.DIRECTORY_DOWNLOADS;


public class MonitActivity extends Activity {

    private static final String TAG = "SMonit";
    private int mMaxChars = 50000;
    private UUID mDeviceUUID;
    private BluetoothSocket mmSocket;
    //private ReadInput mReadThread = null;

    private TextView mTxtReceive;
    private ScrollView scrollView;

    private boolean mIsBluetoothConnected = false;

    private BluetoothDevice mDevice;

    private ProgressDialog progressDialog;

    private SQLiteDatabase db;
    public Cursor resultsCursor;

    int theLastOne;

    private XYPlot mySimpleXYPlot0;
//    private XYPlot mySimpleXYPlot1;
//    private XYPlot mySimpleXYPlot2;

    private ListView listResults;



    //"Obserwator" - klasa do update'owania implementująca interfejs Observer (do obserwowania zmieniających się obiektów)
    // redraws a plot whenever an update is received:
    private class MyPlotUpdater implements Observer {
        Plot plot;

        public MyPlotUpdater(Plot plot) {
            this.plot = plot;
        }

        //metoda update - ponawia rysowanie wykresu (redraw)
        @Override
        public void update(Observable o, Object arg) {
            //Bez tej funkcji jest wykres stały - nieruchomy
            plot.redraw();
        }
    }

    //obiekt dla wykresu
    private XYPlot dynamicPlot;

    //obiekt Updater - do aktualizowania widoku wykresu
    private MyPlotUpdater plotUpdater;
    private MyPlotUpdater plotUpdater0;

    //klasa z danymi dynamicznymi (implementowana w dalszej częśći kodu)
    SampleDynamicXYDatasource data;
    SampleDynamicXYDatasource data0;


    //private Thread t;

    //wątek roboczy
    private Thread myThread;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monit);
        mTxtReceive = findViewById(R.id.txtReceive);
        scrollView = findViewById(R.id.viewScroll);
        mySimpleXYPlot0 = (XYPlot) findViewById(R.id.plot0);
//        mySimpleXYPlot1 = (XYPlot) findViewById(R.id.plot1);
//        mySimpleXYPlot2 = (XYPlot) findViewById(R.id.plot2);


        //odniesienie obiektu do rysowania dynamicPlot z polem graficznym
        // get handles to our View defined in layout.xml:
//        dynamicPlot = (XYPlot) findViewById(R.id.dynamicXYPlot);

        //utworzenie Updatera do aktualizowania wykresu dynamicPlot
//        plotUpdater = new MyPlotUpdater(dynamicPlot);
        plotUpdater0 = new MyPlotUpdater(mySimpleXYPlot0);

        //wyświetlanie etykiety dolnej osi jako LICZB CAŁKOWITYCH!
        // only display whole numbers in domain labels
//        dynamicPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).
//                setFormat(new DecimalFormat("0"));
        mySimpleXYPlot0.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).
                setFormat(new DecimalFormat("0"));

        //utworzenie instancji zbioru danych
        // getInstance and position datasets:
//        data = new SampleDynamicXYDatasource();
        data0 = new SampleDynamicXYDatasource();
        //utworzenie dwóch serii dynamicznych danych
//        SampleDynamicSeries sine1Series = new SampleDynamicSeries(data, 1, "Mój Wykres 1");
        SampleDynamicSeries ecgSeries = new SampleDynamicSeries(data0, 0, "Mój Wykres 0");

        //Formatter dla rysowanej linii - wykres 1 (kolor wykresu)
//        LineAndPointFormatter dynformatter1 = new LineAndPointFormatter(
//                Color.rgb(0, 200, 0), null, null, null);
        LineAndPointFormatter series0Format = new LineAndPointFormatter(
                Color.rgb(0, 255, 0), null, null, null);



        //ustawienie grubości linii
//        dynformatter1.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
//        dynformatter1.getLinePaint().setStrokeWidth(2); //domyślnie było 10, 2 jest optymalnie
        series0Format.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
        series0Format.getLinePaint().setStrokeWidth(2); //domyślnie było 10, 2 jest optymalnie
        series0Format.setLegendIconEnabled(false);
        //ustawienie na wykresie1 serii i formatu danych
//        dynamicPlot.addSeries(sine1Series,
//                dynformatter1);
        mySimpleXYPlot0.addSeries(ecgSeries,
                series0Format);


        //dodanie Obserwatora do danych i na wykres (inaczej wykres stoi w miejscu)
        // hook up the plotUpdater to the data model:
//        data.addObserver(plotUpdater);
//        data0.addObserver(plotUpdater0);
        data0.addObserver(plotUpdater0);

        //ustawienie kroku na etykietach osi - poziomej
        // thin out domain tick labels so they dont overlap each other:
//        dynamicPlot.setDomainStepMode(StepMode.INCREMENT_BY_VAL);
//        dynamicPlot.setDomainStepValue(50);
//        //ustawienie kroku na etykietach osi - pionowej
//        dynamicPlot.setRangeStepMode(StepMode.INCREMENT_BY_VAL);
//        dynamicPlot.setRangeStepValue(20);
        mySimpleXYPlot0.setDomainStepMode(StepMode.INCREMENT_BY_VAL);
        mySimpleXYPlot0.setDomainStepValue(1);
        mySimpleXYPlot0.setRangeStepMode(StepMode.INCREMENT_BY_VAL);
        mySimpleXYPlot0.setRangeStepValue(200);


        //ustawienie zakresu wartości wykresu - na sztywno
        // uncomment this line to freeze the range boundaries:
//        dynamicPlot.setRangeBoundaries(0, 200, BoundaryMode.FIXED);
        mySimpleXYPlot0.setRangeBoundaries( 100, 700, BoundaryMode.FIXED);

        //dzięki temu nie ma przecinków na etykietach osi pionowej
//        dynamicPlot.getGraph().getLineLabelStyle(
//                XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("###.#"));
        mySimpleXYPlot0.getGraph().getLineLabelStyle(
                XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("###.#"));





        //tworzy przerywaną siatkę w tle - Grid zamiast linii ciągłej
        // create a dash effect for domain and range grid lines:
        DashPathEffect dashFx = new DashPathEffect(
                new float[] {PixelUtils.dpToPix(3), PixelUtils.dpToPix(3)}, 0);
//        dynamicPlot.getGraph().getDomainGridLinePaint().setPathEffect(dashFx);
//        dynamicPlot.getGraph().getRangeGridLinePaint().setPathEffect(dashFx);
        mySimpleXYPlot0.getGraph().getDomainGridLinePaint().setPathEffect(dashFx);
        mySimpleXYPlot0.getGraph().getRangeGridLinePaint().setPathEffect(dashFx);



        Bundle b = getIntent().getExtras();
        assert b != null;
        mDevice = b.getParcelable(MainActivity.DEVICE_EXTRA);
        mDeviceUUID = UUID.fromString(b.getString(MainActivity.DEVICE_UUID));

        mTxtReceive.setMovementMethod(new ScrollingMovementMethod());

        if (mmSocket == null || !mIsBluetoothConnected) {
            new ConnectBT().execute();
        }
        new createDatabaseTask().execute(MonitActivity.this);
    }


    @Override
    protected void onResume() {

        //new updateResultTask().execute();

        Log.d(TAG, "Resumed");
        super.onResume();
    }

    @Override
    protected void onPause() {

        //uruchomienie metody stopThread, która zmienia flagę keepRunning na false
        // (w efekcie chodzi CHYBA o zatrzymanie wątku z danymi?)
        data.stopThread();
        data0.stopThread();

        if (mmSocket != null && mIsBluetoothConnected) {
            new DisConnectBT().execute();
        }
        Log.d(TAG, "Paused");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Stopped");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resultsCursor.close();
        db.close();
    }

    //klasa - źródło danych dynamicznych która implementuje interfejs Runnable
    class SampleDynamicXYDatasource implements Runnable {

        private boolean bStop = false;

        public boolean isRunning() {
            return myThread.isAlive();
        }

        //"hermetyzacja" zarządzania Obserwatorem obserwującym nasze źródło danych w celu aktualizacji zdarzeń
        //dziedziczy po klasie Observable - "obiekcie obserwowanym - podglądanym"
        // encapsulates management of the observers watching this datasource for update events:
        class MyObservable extends Observable {
            @Override
            //metoda notifyObservers - powiadamia Obserwatora że coś się zmieniło...
            public void notifyObservers() {
                //...na podstawie metody setChanged - która oznacza obiekt obserwowany jako zmieniony (zaktualizowany)
                setChanged();
                //dziedziczenie metod - powiadom Obserwatora że coś się zmieniło
                super.notifyObservers();
            }
        }

        //DALEJ JESTEŚMY W KLASIE SampleDynamicXYDatasource

        //PARAMETRY GENEROWANEGO SYGNAŁU

        //rozmiar - ile danych mamy mieć na osi poziomej (jak np. 15 to jest wąski zakresik, dla 100 już jest dużo więcej sygnału
        //powyżej 100 praktycznie traci sens (nachodzą się na siebie próbki sygnału i staje się nieczytelny). domyślnie było 31
        private static final int SAMPLE_SIZE = 1000;

        Number[] liczby = new Number[SAMPLE_SIZE];
        LinkedBlockingQueue<Number> lbq = new LinkedBlockingQueue<>(SAMPLE_SIZE);
        LinkedBlockingQueue<Number> lbq0 = new LinkedBlockingQueue<>(SAMPLE_SIZE);
        Number[] kontener0 = new Number[SAMPLE_SIZE];
        Number[] kontenerY = new Number[SAMPLE_SIZE];

        void uzupelnij() {
            for (int i = 0;i < SAMPLE_SIZE; i++) {
//                liczby[i] = (int) (Math.random()*10);
//                lbq.add(i);
                lbq.add((Math.random() * Math.random() * 200));
                liczby[i] = (lbq.remove());
            }
        }

        Number[] uzupelnij2(String[][] expected) {

            for (int j = 0; j < expected.length; j++) {

                if (Integer.parseInt(expected[j][2]) < 1000 && Integer.parseInt(expected[j][2]) > 10) {
                    lbq0.add(Integer.parseInt(expected[j][2]));
//                if (Integer.parseInt(expected[j][0]) < 1000 && Integer.parseInt(expected[j][0]) > 10) {
//                    lbq0.add(Integer.parseInt(expected[j][0]));
                }
            }

            for (int z = 0; z < lbq0.size(); z++) {
                kontener0[z] = lbq0.remove();
            }

                return kontener0;



//            for (int i = 0;i < SAMPLE_SIZE; i++) {
////                liczby[i] = (int) (Math.random()*10);
////                lbq.add(i);
//                lbq.add((Math.random() * Math.random() * 200));
//                liczby[i] = (lbq.remove());
//            }
        }


        //amplituda która jest potem poddawana operacjom matematycznym i służy potem do ustalania czy amplituda ma rosnąć czy maleć
        //private int sinAmp = (int) (Math.random() * 10);
        //obiekt "obserwowany?" - "ogłaszacz" do upewnienia się że to tak działa!
        //private MyObservable notifier;
        private MyObservable notifier0;
        //zmienna określająca czy wątek żyje. Początkowo ustalana jako false, zmieniana na true na początku metody run,
        // w metodzie stopThread również ustalana jako false
        private boolean keepRunning = false;

        //te nawiasy klamrowe są potrzebne - nie wiadomo czemu?! bez nich wyrzuca błąd
//        {
//            //powołanie do życia obiektu - instancji klasy MyObservable (obiektu obserwowanego?)
//            notifier = new MyObservable();
//        }
        {
            //powołanie do życia obiektu - instancji klasy MyObservable (obiektu obserwowanego?)
            notifier0 = new MyObservable();
        }

        //metoda wstrzymująca wątek (poprzez zmianę flagi keepRunning)
        void stopThread() {
            //ustawienie flagi keepRunning na false (powodującej wstrzymanie wątku)
            keepRunning = false;
        }

        public void stop2() {
            bStop = true;
        }

        //metoda run - tu zmieniają się parametry sygnału -> aktualizowane sa wartości będące danymi do wykresu
        //@Override
        public void run() {


            //blok try-catch do łapania wyjątków w razie czego
//            try {
//                //przestawienie flagi keepRunning na true - "wątek żyje"
//                keepRunning = true;
//                //flaga określająca czy amplituda rośnie - początkowo ustawion na true (czyli rośnie)
//                //pętla while dziejąca się dopóki wątek żyje - czyli dopóki flaga keepRunning jest true
//                int k = 0;
//                while (k<20) {
//                    //delay wstrzymujący na chwilę wątek - dla rzadszego/częstszego odświeżania danych
//                    //dla sinusoidy przy 100 jest ślimak, przy 10 (domyślnym) dość szybko, zmienione na 20
//                    //dla Randoma przy ok 150 jest raczej optymalnie
//                    Thread.sleep(150); // decrease or remove to speed up the refresh rate.
//
//                    //uzupelnij();
//
//                    //sinAmp++;
//
//                    //"ogłaszacz" - obiekt obserwowany powiadamia - informuje że coś się zmieniło
//                    //notifier.notifyObservers();
//                    k++;
//                }
//                //obsługa wyjątku
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }



            InputStream mmInStream;
            byte[] buffer;

            try {
                mmInStream = mmSocket.getInputStream();

                int a = 0;

                while (a<1000) {

                    buffer = new byte[SAMPLE_SIZE];
//                    LinkedBlockingQueue<Number> lbq0 = new LinkedBlockingQueue<>(SAMPLE_SIZE);
//                    Number[] kontener0 = new Number[SAMPLE_SIZE];


                    if (mmInStream.available() > 0) {
                        mmInStream.read(buffer);


                        int i;
                        for (i = 0; i < buffer.length && buffer[i] != 0; i++) {

                        }
                        final String strInput = new String(buffer, 0, i);

                        try {
                            String[] splitted_data = strInput.split("\\r?\\n");
                            String[][] splitted_channels = new String[splitted_data.length][3];

                            for (int z = 0; z < splitted_data.length; z++) {
                                if(splitted_data[z].contains(",") && (splitted_data[z].length()>8) && (splitted_data[z].length()<12)){
                                    splitted_channels[z] = splitted_data[z].split(",", 3);
                                }

                                else if (!splitted_data[z].contains(",") || (splitted_data[z].length() <= 8)) {

                                    //  Log.d(TAG, "DANA NIEPODZIELNA");
                                }
                            }

                            kontenerY = uzupelnij2(splitted_channels);
                            Thread.sleep(150);
                            notifier0.notifyObservers();

//                            for (int j = 0; j < splitted_data.length; j++) {
//
//                                if (Integer.parseInt(splitted_channels[j][0]) < 1000 && Integer.parseInt(splitted_channels[j][0]) > 10) {
//                                    lbq0.add(Integer.parseInt(splitted_channels[j][0]));
//                                    notifier0.notifyObservers();
//                                }
//
//                            }

//                            mySimpleXYPlot0.clear();

                        }catch (NumberFormatException e) {
                            System.err.println("This is not a number!");
                            Log.d(TAG, "wyjątek związany z NFE");
                        }




                        //ustawienie kroku na etykietach osi - pionowej i poziomej
//                        mySimpleXYPlot0.setDomainStepMode(StepMode.INCREMENT_BY_VAL);
//                        mySimpleXYPlot0.setDomainStepValue(5);
//                        mySimpleXYPlot0.setRangeStepMode(StepMode.INCREMENT_BY_VAL);
//                        mySimpleXYPlot0.setRangeStepValue(100);
//
//                        mySimpleXYPlot0.setRangeBoundaries( 0, 400, BoundaryMode.FIXED);
//                        FastLineAndPointRenderer.Formatter series0Format = new FastLineAndPointRenderer.Formatter(
//                                Color.rgb(0, 255, 0),
//                                null,
//                                null);
//                        mySimpleXYPlot0.addSeries(series0, series0Format);
//                        mySimpleXYPlot0.getBackgroundPaint().setAlpha(0);
//
//                        mySimpleXYPlot0.setLinesPerRangeLabel(3);

//                        List<Plot> lista_wykresow = new ArrayList<>(3);
//
//                        lista_wykresow.add(mySimpleXYPlot0);


//                        redrawer_ultra = new Redrawer(lista_wykresow, 20, true);
//                        mySimpleXYPlot0.redraw();

                        mTxtReceive.post(new Runnable() {
                            @Override
                            public void run() {
                                mTxtReceive.append(strInput);
                                int txtLength = mTxtReceive.getEditableText().length();

                                if(txtLength > mMaxChars){
                                    mTxtReceive.getEditableText().delete(0, txtLength - mMaxChars);
                                }


                                scrollView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        scrollView.fullScroll(View.FOCUS_DOWN);
                                    }
                                });

                            }
                        });



                    }
                    a++;
                    Thread.sleep(10);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


//            //blok try-catch do łapania wyjątków w razie czego
//            try {
//                //przestawienie flagi keepRunning na true - "wątek żyje"
//                keepRunning = true;
//                //flaga określająca czy amplituda rośnie - początkowo ustawion na true (czyli rośnie)
//                //pętla while dziejąca się dopóki wątek żyje - czyli dopóki flaga keepRunning jest true
//                while (keepRunning) {
//                    //delay wstrzymujący na chwilę wątek - dla rzadszego/częstszego odświeżania danych
//                    //dla sinusoidy przy 100 jest ślimak, przy 10 (domyślnym) dość szybko, zmienione na 20
//                    //dla Randoma przy ok 150 jest raczej optymalnie
//                    Thread.sleep(150); // decrease or remove to speed up the refresh rate.
//                    //zmiana fazy z każdym kolejnym krokiem o +1
//                    //jeśli amplituda sinusa osiągnie wartość równą maksymalnej dopuszczonej (zadanej),
//                    //to wtedy flaga isRising zmienia się na false.
//
//                    uzupelnij();
//
//
//                    //sinAmp++;
//
//
//                    //"ogłaszacz" - obiekt obserwowany powiadamia - informuje że coś się zmieniło
//                    notifier.notifyObservers();
//                }
//                //obsługa wyjątku
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }

        //metoda zwracająca ilośc próbek
        int getItemCount(int series) {
            return SAMPLE_SIZE;
        }

        //metoda zwracająca indeks (danego elementu?) z serii
        //w razie jak indeks byłby większy od rozmiaru próbki - wyrzuć wyjątek że nieprawidłowy argument
        Number getX(int series, int index) {
            if (index >= SAMPLE_SIZE) {
                throw new IllegalArgumentException();
            }
            //zwraca indeks
            return index;
        }

        //metoda zwracająca wartość danej próbki z serii o wybranym (danym) indeksie
        Number getY(int series, int index) {
            //jak wyżej - w razie jak indeks byłby większy od rozmiaru próbki - wyrzuć wyjątek że nieprawidłowy argument
            if (index >= SAMPLE_SIZE) {
                throw new IllegalArgumentException();
            }
            //zmienna kąt jako argument funkcji sinusoidalnej - obliczana na podstawie aktualnego indeksu poprawiona o fazę dzieloną przez częstotliwość
            //double angle = (index + (phase))/FREQUENCY;
            //amplituda amp - jako zdefiniowana na wstępie amplituda sygnału * wartość sinusa od danego kąta
            //double amp = sinAmp;

            return kontenerY[index];
        }

        //metoda umożliwiająca dodanie Obserwatora
        void addObserver(Observer observer) {
//            notifier.addObserver(observer);
            notifier0.addObserver(observer);
        }

        //metoda pozwalająca na usunięcie Obserwatora - nieużywana
        public void removeObserver(Observer observer) {
//            notifier.deleteObserver(observer);
            notifier0.deleteObserver(observer);
        }


        //DOPIERO TU KOŃCZY SIĘ KLASA SampleDynamicXYDatasource
    }


    //klasa SampleDynamicSeries (bezpośrednio używana do narysowania wykresu) implementująca serie "dwuwymiarowe" XY
    class SampleDynamicSeries implements XYSeries {
        //pola tej klasy: źródło danych, numer serii (0 lub 1), tytuł serii
        private SampleDynamicXYDatasource datasource;
        private int seriesIndex;
        private String title;

        //konstruktor klasy SampleDynamicSeries z trzema parametrami - źródło danych,
        SampleDynamicSeries(SampleDynamicXYDatasource datasource, int seriesIndex, String title) {
            //pobrane jako argumenty funkcji parametry wejściowe potraktuj jako swoje dane - przypisz je do swoich pól
            this.datasource = datasource;
            this.seriesIndex = seriesIndex;
            this.title = title;
        }

        //obowiązkowa metoda klasy SampleDynamicSeries zwracająca tytuł
        @Override
        public String getTitle() {
            return title;
        }

        //obowiązkowa metoda klasy SampleDynamicSeries zwracająca rozmiar danych - na podstawie tego jak została określona z góry liczba próbek
        @Override
        public int size() {
            return datasource.getItemCount(seriesIndex);
        }

        //obowiązkowa metoda klasy SampleDynamicSeries zwracająca indeks X (elementu?) z danej serii (seria o numerze 0 lub 1)
        @Override
        public Number getX(int index) {
            return datasource.getX(seriesIndex, index);
        }

        //obowiązkowa metoda klasy SampleDynamicSeries zwracająca wartość Y elementu dla danego indeksu (elementu?) z danej serii (seria o numerze 0 lub 1)
        @Override
        public Number getY(int index) {
            return datasource.getY(seriesIndex, index);
        }
    }










//    private class ReadInput extends Thread {
//
////        private boolean bStop = false;
////        private Thread t;
////
////        //wątek roboczy
////        private Thread myThread;
//
////        public ReadInput() {
////            t = new Thread(this, "Input Thread");
////            t.start();
////            //wystartowanie wątku z danymi
////            // kick off the data generating thread:
////            myThread = new Thread(data);
////            myThread.start();
////        }
//
//
////        public boolean isRunning() {
////            return t.isAlive();
////        }
//
//        @Override
//        public void run() {
////            InputStream mmInStream;
////            byte[] buffer;
////
//            try {
////                mmInStream = mmSocket.getInputStream();
//
//                while (true) {
//
////                    buffer = new byte[1024];
////                    buffer = new byte[4096];
//
////                    LinkedBlockingQueue<Number> lbq0 = new LinkedBlockingQueue<>(1024);
//////                    LinkedBlockingQueue<Number> lbq1 = new LinkedBlockingQueue<>(1024);
//////                    LinkedBlockingQueue<Number> lbq2 = new LinkedBlockingQueue<>(1024);
////
//////                    Number[] kontener0 = new Number[4096];
////                    Number[] kontener0 = new Number[16384];
////                    Number[] kontener1 = new Number[4096];
////                    Number[] kontener2 = new Number[4096];
//
//
//                    if (mmInStream.available() > 0) {
////                        mmInStream.read(buffer);
////
////                        int i;
////
////                        for (i = 0; i < buffer.length && buffer[i] != 0; i++) {
////
////                        }
////
////                        final String strInput = new String(buffer, 0, i);
//
//                        try {
////                            String[] splitted_data = strInput.split("\\r?\\n");
////                            String[][] splitted_channels = new String[splitted_data.length][3];
////
////                            for (int z = 0; z < splitted_data.length; z++) {
////                                if(splitted_data[z].contains(",") && (splitted_data[z].length()>8) && (splitted_data[z].length()<12)){
////                                    splitted_channels[z] = splitted_data[z].split(",", 3);
////                                }
////
////                                else if (!splitted_data[z].contains(",") || (splitted_data[z].length() <= 8)) {
////
////                                  //  Log.d(TAG, "DANA NIEPODZIELNA");
////                                }
////                            }
//
//
//
////                            for (int j = 0; j < splitted_data.length; j++) {
////
////                                if (Integer.parseInt(splitted_channels[j][0]) < 1000 && Integer.parseInt(splitted_channels[j][0]) > 10) {
////                                    lbq0.add(Integer.parseInt(splitted_channels[j][0]));
////                                    //kontener0[k] = Integer.parseInt(splitted_channels[j][0]);
////                                   // Log.d(TAG, kontener0[k].toString());
////                                    //k++;
////                                }
////
//////                                if (Integer.parseInt(splitted_channels[j][1]) < 1000 && Integer.parseInt(splitted_channels[j][1]) > 10) {
//////                                    lbq1.add(Integer.parseInt(splitted_channels[j][1]));
////////                                    kontener1[k] = Integer.parseInt(splitted_channels[j][1]);
//////                                    //Log.d(TAG, kontener1[k].toString());
//////                                    //k++;
//////                                }
////////
//////                                if (Integer.parseInt(splitted_channels[j][2]) < 1000 && Integer.parseInt(splitted_channels[j][2]) > 10) {
//////                                    lbq2.add(Integer.parseInt(splitted_channels[j][2]));
////////                                    kontener2[k] = Integer.parseInt(splitted_channels[j][2]);
////////                                    Log.d(TAG, kontener2[k].toString());
//////                                    //k++;
//////                                }
////
////                            }
////
////                            mySimpleXYPlot0.clear();
//////                            mySimpleXYPlot1.clear();
//////                            mySimpleXYPlot2.clear();
//
//                        } catch (NumberFormatException e) {
//                            System.err.println("This is not a number!");
//                            Log.d(TAG, "wyjątek związany z NFE");
//                        }
//
//
////                        for (int z = 0; z < lbq0.size(); z++) {
////                            kontener0[z] = lbq0.remove();
//////                            kontener1[z] = lbq1.remove();
//////                            kontener2[z] = lbq2.remove();
////                        }
////                        for (int z = 0; z < lbq1.size(); z++) {
////
////                        }
////                        for (int z = 0; z < lbq2.size(); z++) {
////
////                        }
//
//
////                        XYSeries series0 = new SimpleXYSeries(
////                                Arrays.asList(kontener0),
////                                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
////                                "Dane - Seria0");
//
////                        XYSeries series1 = new SimpleXYSeries(
////                                Arrays.asList(kontener1),
////                                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
////                                "Dane - Seria1");
////
////                        XYSeries series2 = new SimpleXYSeries(
////                                Arrays.asList(kontener2),
////                                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
////                                "Dane - Seria2");
//
//
//                        //etykiety osi poziomej powinny być w liczbach całkowitych!
////                        mySimpleXYPlot0.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).
////                                setFormat(new DecimalFormat("0"));
////                        mySimpleXYPlot1.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).
////                                setFormat(new DecimalFormat("0"));
////                        mySimpleXYPlot2.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).
////                                setFormat(new DecimalFormat("0"));
//
////                        //dzięki temu nie powinno być przecinków na etykietach osi pionowej
////                        mySimpleXYPlot0.getGraph().getLineLabelStyle(
////                                XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("###.#"));
//////
//////                        mySimpleXYPlot1.getGraph().getLineLabelStyle(
//////                                XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("###.#"));
//////
//////                        mySimpleXYPlot2.getGraph().getLineLabelStyle(
//////                                XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("###.#"));
////
////                        //ustawienie kroku na etykietach osi - pionowej i poziomej
////                        mySimpleXYPlot0.setDomainStepMode(StepMode.INCREMENT_BY_VAL);
////                        mySimpleXYPlot0.setDomainStepValue(5);
////                        mySimpleXYPlot0.setRangeStepMode(StepMode.INCREMENT_BY_VAL);
////                        mySimpleXYPlot0.setRangeStepValue(10);
//
//
////
////                        mySimpleXYPlot1.setDomainStepMode(StepMode.INCREMENT_BY_VAL);
////                        mySimpleXYPlot1.setDomainStepValue(5);
////                        mySimpleXYPlot1.setRangeStepMode(StepMode.INCREMENT_BY_VAL);
////                        mySimpleXYPlot1.setRangeStepValue(10);
////
////                        mySimpleXYPlot2.setDomainStepMode(StepMode.INCREMENT_BY_VAL);
////                        mySimpleXYPlot2.setDomainStepValue(5);
////                        mySimpleXYPlot2.setRangeStepMode(StepMode.INCREMENT_BY_VAL);
////                        mySimpleXYPlot2.setRangeStepValue(10);
//
//
////                        FastLineAndPointRenderer.Formatter series0Format = new FastLineAndPointRenderer.Formatter(
////                                Color.rgb(0, 255, 0),
////                                null,
////                                null);
//
////                        FastLineAndPointRenderer.Formatter series1Format = new FastLineAndPointRenderer.Formatter(
////                                Color.rgb(0, 255, 0),
////                                null,
////                                null);
////
////                        FastLineAndPointRenderer.Formatter series2Format = new FastLineAndPointRenderer.Formatter(
////                                Color.rgb(0, 255, 0),
////                                null,
////                                null);
//
//
////                        series0Formatseries0Format.setLegendIconEnabled(false);
////                        series1Format.setLegendIconEnabled(false);
////                        series2Format.setLegendIconEnabled(false);
//
////                        mySimpleXYPlot0.setRangeBoundaries( 0, 700, BoundaryMode.FIXED);
//////                        mySimpleXYPlot0.addSeries(series0, series0Format);
////                        mySimpleXYPlot0.getBackgroundPaint().setAlpha(0);
//
////                        mySimpleXYPlot1.setRangeBoundaries( 0, 700, BoundaryMode.FIXED);
////                        mySimpleXYPlot1.addSeries(series1, series1Format);
////                        mySimpleXYPlot1.getBackgroundPaint().setAlpha(0);
////
////                        mySimpleXYPlot2.setRangeBoundaries( 0, 700, BoundaryMode.FIXED);
////                        mySimpleXYPlot2.addSeries(series2, series2Format);
////                        mySimpleXYPlot2.getBackgroundPaint().setAlpha(0);
//
//
////                        mySimpleXYPlot0.setLinesPerRangeLabel(3);
//////                        mySimpleXYPlot1.setLinesPerRangeLabel(3);
//////                        mySimpleXYPlot2.setLinesPerRangeLabel(3);
////
////                        List<Plot> lista_wykresow = new ArrayList<>(3);
////
////                        lista_wykresow.add(mySimpleXYPlot0);
//////                        lista_wykresow.add(mySimpleXYPlot1);
//////                        lista_wykresow.add(mySimpleXYPlot2);
//
//
////                        redrawer_ultra = new Redrawer(lista_wykresow, 20, true);
////                        mySimpleXYPlot0.redraw();
////                        mySimpleXYPlot1.redraw();
////                        mySimpleXYPlot2.redraw();
//
//
////                        mTxtReceive.post(new Runnable() {
////                            @Override
////                            public void run() {
////                                mTxtReceive.append(strInput);
////                                int txtLength = mTxtReceive.getEditableText().length();
////
////                                if(txtLength > mMaxChars){
////                                    mTxtReceive.getEditableText().delete(0, txtLength - mMaxChars);
////                                }
////
////
////                                scrollView.post(new Runnable() {
////                                    @Override
////                                    public void run() {
////                                        scrollView.fullScroll(View.FOCUS_DOWN);
////                                    }
////                                });
////
////                            }
////                        });
//
//                    }
//                    //Thread.sleep(500);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        }
//
////        public void stop2() {
////            bStop = true;
////        }
//
//    }




    private class ConnectBT extends AsyncTask<Void, Void, Void> {

        private boolean mConnectSuccessful = true;

        @Override
        protected void onPreExecute() {

            progressDialog = ProgressDialog.show(MonitActivity.this, "Please wait", "Connecting");
        }
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (mmSocket == null || !mIsBluetoothConnected) {
                    mmSocket = mDevice.createRfcommSocketToServiceRecord(mDeviceUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    mmSocket.connect();
                }
            } catch (IOException e) {
                e.printStackTrace();
                mConnectSuccessful = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!mConnectSuccessful) {
                Toast.makeText(getApplicationContext(), "Could not connect to device. Is it a Serial device? Also check if the UUID is correct in the settings", Toast.LENGTH_LONG).show();
                //finish();
            } else {
                Toast.makeText(getApplicationContext(), "Connected to device", Toast.LENGTH_SHORT).show();
                mIsBluetoothConnected = true;
                //mReadThread = new ReadInput();

                //wystartowanie wątku z danymi
                // kick off the data generating thread:
                myThread = new Thread(data0);
                myThread.start();



            }
            progressDialog.dismiss();
        }
    }


    private class DisConnectBT extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
//            if (mReadThread != null) {
//                mReadThread.stop2();
//                while (mReadThread.isRunning()) {
//                }
//                mReadThread = null;
//            }

            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void result) {
            //super.onPostExecute(result);
            mIsBluetoothConnected = false;
            //finish();
        }
    }


    public void onSaveClicked(View view) {

        new UpdateDatabaseTask().execute();
        saveToTxtfile();
    }


    void saveToTxtfile() {
        try {
            String root = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).toString();
            File myFile = new File(root, "ecgResults.txt");
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

            myOutWriter.append(mTxtReceive.getText());

            myOutWriter.close();
            fOut.close();
            Toast.makeText(getApplicationContext(), "Done writing to file 'ecgResults.txt' in the path Phone/Download",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),e.getMessage(), Toast.LENGTH_SHORT)
                    .show();
            Log.d(TAG, "Brak uprawnien - brak dostepu");
        }
    }


    public class createDatabaseTask extends AsyncTask<Context, Void, Cursor> {

        private static final String TAG = "SMonit";
        private Cursor resultsCursor;
        //WPROWADZONO ZMIENNĄ KONTEKST, KTÓRA JEST POBIERANA JAKO PARAMETR NA WEJŚCIU ASYNCTASKU
//        private Context context;

//        private static Application instance;
//
//        public static Context getContext() {
//            return instance.getApplicationContext();
//        }

        @Override
        protected Cursor doInBackground(Context... cont) {

//            this.context=cont[0];

            CursorAdapter resultsAdapter = null;
            try {
                SQLiteOpenHelper ecgDatabaseHelper = new ECGDatabaseHelper(MonitActivity.this);
                SQLiteDatabase db = ecgDatabaseHelper.getWritableDatabase();
                resultsCursor = db.query("RESULTS",
                        new String[] { "_id", "DATA0", "DATA1", "DATA2"},
                        null,
                        null,
                        null, null, null);
                resultsAdapter = new SimpleCursorAdapter(MonitActivity.this,
                        android.R.layout.simple_list_item_1,
                        resultsCursor,
                        new String[]{"DATA2"},
                        new int[]{android.R.id.text1}, 0);

                Log.d(TAG, "createDatabaseTask się melduje");
            }
            catch (SQLiteException e) {
                Toast.makeText(MonitActivity.this, "Baza danych jest niedostępna", Toast.LENGTH_SHORT).show();
                Log.d("TAG", "Problem z bazą");
            }
            return resultsCursor;
        }

        @Override
        protected void onPostExecute(Cursor resultsCursor) {
            //listResults.setAdapter(resultsAdapter);
        }

    }


    // Klasa wewnętrzna służąca do aktualizacji danych - wyników
//    private class updateResultTask extends AsyncTask<Void, Void, CursorAdapter> {
//
//
//        @Override
//        protected CursorAdapter doInBackground(Void... voids) {
//// Tworzymy kursor
//            CursorAdapter resultsAdapter = null;
//            try {
//                SQLiteOpenHelper smonitDatabaseHelper = new ECGDatabaseHelper(MonitActivity.this);
//                SQLiteDatabase db = smonitDatabaseHelper.getWritableDatabase();
//                resultsCursor = db.query("RESULTS",
//                        new String[] { "_id", "DATA0"},
//                        null,
//                        null,
//                        null, null, null);
//                resultsAdapter = new SimpleCursorAdapter(MonitActivity.this,
//                        android.R.layout.simple_list_item_1,
//                        resultsCursor,
//                        new String[]{"DATA0"},
//                        new int[]{android.R.id.text1}, 0);
//                //listResults.setAdapter(resultsAdapter);
//            }
//            catch (SQLiteException e) {
//                Toast.makeText(MonitActivity.this, "Baza danych jest niedostępna", Toast.LENGTH_SHORT).show();
//                Log.d("TAG", "Problem z bazą");
//            }
//            return resultsAdapter;
//        }
//
//        //protected void onPostExecute(List<BluetoothDevice> listDevices) {
//        protected void onPostExecute(CursorAdapter resultsAdapter) {
//            listResults.setAdapter(resultsAdapter);
//        }
//    }


    public class UpdateDatabaseTask extends AsyncTask<Void, Void, CursorAdapter> {

        private static final String TAG = "SMonit";
        ContentValues resultValues;
        int ostatniMohikanin;
        //String results_db;
        //int theLastOne;
//        private Cursor resultsCursor;
        //private SQLiteDatabase db;

        //cursorPlusString koszyk = new cursorPlusString(resultsCursor,results_db);




        //Wstawka z kontekstem - dotychczas jako kontekst była podawana aktywność MonitActivity.this, a teraz tak już się nie da
        // (chyba że parametrem wejściowym jak w createDatabaseTask
        //Rozwiązanie znalezione na Stack'u
//    private static Application instance;
//
//    public static Context getContext() {
//        return instance.context;
//    }


        private int insertResults(SQLiteDatabase db) {
            String results_db = mTxtReceive.getText().toString();
            String[] splitted_results_db = results_db.split("\\r?\\n");
            String[][] splitted_channels_db = new String[splitted_results_db.length][3];

            Log.d(TAG, "Split po raz pierwszy się melduje");

            for (int z = 0; z < splitted_channels_db.length; z++) {
                if (splitted_results_db[z].contains(",") && (splitted_results_db[z].length() > 8) && (splitted_results_db[z].length() < 12)) {
                    splitted_channels_db[z] = splitted_results_db[z].split(",",3);
                }
            }



            for (int i = 0; i < splitted_channels_db.length; i++) {
                resultValues.put("DATA0", splitted_channels_db[i][0]);
                resultValues.put("DATA1", splitted_channels_db[i][1]);
                resultValues.put("DATA2", splitted_channels_db[i][2]);
                db.insert("RESULTS", null, resultValues);
            }

            Log.d(TAG, "Dodanie do bazy się melduje");
            ostatniMohikanin = splitted_results_db.length;
            return ostatniMohikanin;
        }


        private int insertResultsAgain(SQLiteDatabase db, Integer the_last_element) {
            String results_db = mTxtReceive.getText().toString();
            String[] splitted_results_db = results_db.split("\\r?\\n");
            String[][] splitted_channels_db = new String[splitted_results_db.length][3];

            for (int z = the_last_element; z < splitted_channels_db.length; z++) {
                if (splitted_results_db[z].contains(",") && (splitted_results_db[z].length() > 8) && (splitted_results_db[z].length() < 12)) {
                    splitted_channels_db[z] = splitted_results_db[z].split(",",3);
                }
            }

            for (int i = the_last_element; i < splitted_channels_db.length; i++) {

                resultValues.put("DATA0", splitted_channels_db[i][0]);
                resultValues.put("DATA1", splitted_channels_db[i][1]);
                resultValues.put("DATA2", splitted_channels_db[i][2]);
                db.insert("RESULTS", null, resultValues);
            }

            ostatniMohikanin = splitted_results_db.length;
            return ostatniMohikanin;
        }

        protected void onPreExecute() {
        }

        protected CursorAdapter doInBackground(Void... voids) {

            resultValues = new ContentValues();
            CursorAdapter resultsAdapter = null;
            //public Cursor resultsCursor;
//            results_db = abc[0];
            //results_db = koszyczek.getSt;


            try {
                SQLiteOpenHelper ECGDatabaseHelper = new ECGDatabaseHelper(MonitActivity.this);
                SQLiteDatabase db = ECGDatabaseHelper.getWritableDatabase();


                resultsCursor = db.query("RESULTS",
                        new String[] { "_id", "DATA0", "DATA1", "DATA2"}, null, null, null, null, null);



                if (!resultsCursor.moveToFirst() && !resultsCursor.moveToLast()) {
                    ostatniMohikanin = insertResults(db);
                    String t = Integer.toString(ostatniMohikanin);
                    Log.d(TAG, "Po raz pierwszy: Brak pierwszego punktu bazy, wrzuć dane");
                } else {
                    String g = Integer.toString(theLastOne);
                    ostatniMohikanin = insertResultsAgain(db, theLastOne);
                    Log.d(TAG, "Po raz kolejny: Mamy pierwszy punkt bazy, nic nie dodawaj ze starych danych");
                }

                resultsAdapter = new SimpleCursorAdapter(MonitActivity.this,
                        android.R.layout.simple_list_item_1,
                        resultsCursor,
                        new String[]{"DATA2"},
                        new int[]{android.R.id.text1}, 0);

            } catch (SQLiteException e) {
                Toast.makeText(MonitActivity.this, "Baza danych jest niedostępna", Toast.LENGTH_SHORT).show();
                Log.d("TAG", "Problem z bazą 2");
            }

            return resultsAdapter;
        }

        protected void onPostExecute(CursorAdapter resultsAdapter) {
            theLastOne = ostatniMohikanin;

            try {
                ECGDatabaseHelper ecg1DatabaseHelper = new ECGDatabaseHelper(MonitActivity.this);
                db = ecg1DatabaseHelper.getWritableDatabase();
                Cursor newCursor = db.query("RESULTS",
                        new String[] { "_id", "DATA2"}, null, null, null, null, null);

                //w 1.2.16 jest tu kod z ListView i adapterem - tu niepotrzebny

                resultsCursor = newCursor;

            } catch (SQLiteException e) {
                Toast.makeText(MonitActivity.this, "Baza danych jest niedostępna", Toast.LENGTH_SHORT).show();
                Log.d("TAG", "Problem z bazą 3");
            }
        }
    }


    public class cursorPlusString {
        public Cursor cursorx;
        public String strx;


        cursorPlusString(Cursor cursor, String string) {
            this.cursorx = cursor;
            this.strx = string;
        }

        public String getStringa() {
            return strx;
        }
    }

}
