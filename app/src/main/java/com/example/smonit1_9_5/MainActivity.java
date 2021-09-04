package com.example.smonit1_9_5;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.androidplot.Plot;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;


public class MainActivity extends Activity {
    private Button connect;
    private ListView deviceListView;
    private BluetoothAdapter myBTAdapter;
    private static final int BT_ENABLE_REQUEST = 94;
    private UUID mDeviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final String DEVICE_EXTRA = "SOCKET";
    public static final String DEVICE_UUID = "uuid";
    private static final String DEVICE_LIST = "devicelist";
    private static final String DEVICE_LIST_SELECTED = "devicelistselected";
    private static final String TAG = "SMonit";


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

    //klasa z danymi dynamicznymi (implementowana w dalszej częśći kodu)
    SampleDynamicXYDatasource data;
    //wątek roboczy
    private Thread myThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button search = findViewById(R.id.search);
        connect = findViewById(R.id.connect);
        deviceListView = findViewById(R.id.devicelistview);

        dynamicPlot = (XYPlot) findViewById(R.id.dynamicXYPlot);


        //odniesienie obiektu do rysowania dynamicPlot z polem graficznym
        // get handles to our View defined in layout.xml:
        dynamicPlot = (XYPlot) findViewById(R.id.dynamicXYPlot);

        //utworzenie Updatera do aktualizowania wykresu dynamicPlot
        plotUpdater = new MyPlotUpdater(dynamicPlot);

        //wyświetlanie etykiety dolnej osi jako LICZB CAŁKOWITYCH!
        // only display whole numbers in domain labels
        dynamicPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).
                setFormat(new DecimalFormat("0"));

        //utworzenie instancji zbioru danych
        // getInstance and position datasets:
        data = new SampleDynamicXYDatasource();
        //utworzenie dwóch serii dynamicznych danych
        SampleDynamicSeries sine1Series = new SampleDynamicSeries(data, 0, "Mój Wykres 1");

        //Formatter dla rysowanej linii - wykres 1 (kolor wykresu)
        LineAndPointFormatter formatter1 = new LineAndPointFormatter(
                Color.rgb(0, 200, 0), null, null, null);
        //ustawienie grubości linii
        formatter1.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
        formatter1.getLinePaint().setStrokeWidth(2); //domyślnie było 10
        //ustawienie na wykresie1 serii i formatu danych
        dynamicPlot.addSeries(sine1Series,
                formatter1);


        //dodanie Obserwatora do danych i na wykres (inaczej wykres stoi w miejscu)
        // hook up the plotUpdater to the data model:
        data.addObserver(plotUpdater);

        //ustawienie kroku na etykietach osi - poziomej
        // thin out domain tick labels so they dont overlap each other:
        dynamicPlot.setDomainStepMode(StepMode.INCREMENT_BY_VAL);
        dynamicPlot.setDomainStepValue(50);
        //ustawienie kroku na etykietach osi - pionowej
        dynamicPlot.setRangeStepMode(StepMode.INCREMENT_BY_VAL);
        dynamicPlot.setRangeStepValue(10);

        //dzięki temu nie ma przecinków na etykietach osi pionowej
        dynamicPlot.getGraph().getLineLabelStyle(
                XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("###.#"));


        //ustawienie zakresu wartości wykresu - na sztywno
        // uncomment this line to freeze the range boundaries:
        dynamicPlot.setRangeBoundaries(-10, 10, BoundaryMode.FIXED);


        //tworzy przerywaną siatkę w tle - Grid zamiast linii ciągłej
        // create a dash effect for domain and range grid lines:
        DashPathEffect dashFx = new DashPathEffect(
                new float[] {PixelUtils.dpToPix(3), PixelUtils.dpToPix(3)}, 0);
        dynamicPlot.getGraph().getDomainGridLinePaint().setPathEffect(dashFx);
        dynamicPlot.getGraph().getRangeGridLinePaint().setPathEffect(dashFx);



        int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 9;

        if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
            }
        }



        if (savedInstanceState != null) {
            ArrayList<BluetoothDevice> list = savedInstanceState.getParcelableArrayList(DEVICE_LIST);
            if (list != null) {
                initList(list);

                MyAdapter adapter = (MyAdapter) deviceListView.getAdapter();
                int selectedIndex = savedInstanceState.getInt(DEVICE_LIST_SELECTED);
                if (selectedIndex != -1) {
                    adapter.setSelectedIndex(selectedIndex);
                }
            } else {
                initList(new ArrayList<BluetoothDevice>());
            }
        } else {
            initList(new ArrayList<BluetoothDevice>());
        }

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBTAdapter = BluetoothAdapter.getDefaultAdapter();

                if (myBTAdapter == null) {
                    Toast.makeText(getApplicationContext(), "Bluetooth not found", Toast.LENGTH_SHORT).show();
                } else if (!myBTAdapter.isEnabled()) {
                    Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBT, BT_ENABLE_REQUEST);
                } else {
                    new SearchDevices().execute();
                }
            }
        });

        connect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                BluetoothDevice device = ((MyAdapter) (deviceListView.getAdapter())).getSelectedItem();
                Intent intent = new Intent(getApplicationContext(), MonitActivity.class);
                intent.putExtra(DEVICE_EXTRA, device);
                intent.putExtra(DEVICE_UUID, mDeviceUUID.toString());
                startActivity(intent);
            }
        });



    }

    @Override
    protected void onResume() {

        //wystartowanie wątku z danymi
        // kick off the data generating thread:
        myThread = new Thread(data);
        myThread.start();

        super.onResume();
    }


    @Override
    public void onPause() {
        //uruchomienie metody stopThread, która zmienia flagę keepRunning na false
        // (w efekcie chodzi CHYBA o zatrzymanie wątku z danymi?)
        data.stopThread();
        super.onPause();
    }


    //klasa - źródło danych dynamicznych która implementuje interfejs Runnable
    class SampleDynamicXYDatasource implements Runnable {

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
        private static final int SAMPLE_SIZE = 500;

        Number[] liczby = new Number[SAMPLE_SIZE];
        LinkedBlockingQueue<Number> lbq = new LinkedBlockingQueue<>(SAMPLE_SIZE);

        void uzupelnij() {
            for (int i = 0;i < SAMPLE_SIZE; i++) {
//                liczby[i] = (int) (Math.random()*10);
//                lbq.add(i);
                lbq.add((Math.random() * Math.random() * 10));
                liczby[i] = (lbq.remove());
            }
        }


        //amplituda która jest potem poddawana operacjom matematycznym i służy potem do ustalania czy amplituda ma rosnąć czy maleć
        //private int sinAmp = (int) (Math.random() * 10);
        //obiekt "obserwowany?" - "ogłaszacz" do upewnienia się że to tak działa!
        private MyObservable notifier;
        //zmienna określająca czy wątek żyje. Początkowo ustalana jako false, zmieniana na true na początku metody run,
        // w metodzie stopThread również ustalana jako false
        private boolean keepRunning = false;

        //te nawiasy klamrowe są potrzebne - nie wiadomo czemu?! bez nich wyrzuca błąd
        {
            //powołanie do życia obiektu - instancji klasy MyObservable (obiektu obserwowanego?)
            notifier = new MyObservable();
        }

        //metoda wstrzymująca wątek (poprzez zmianę flagi keepRunning)
        void stopThread() {
            //ustawienie flagi keepRunning na false (powodującej wstrzymanie wątku)
            keepRunning = false;
        }

        //metoda run - tu zmieniają się parametry sygnału -> aktualizowane sa wartości będące danymi do wykresu
        //@Override
        public void run() {
            //blok try-catch do łapania wyjątków w razie czego
            try {
                //przestawienie flagi keepRunning na true - "wątek żyje"
                keepRunning = true;
                //flaga określająca czy amplituda rośnie - początkowo ustawion na true (czyli rośnie)
                //pętla while dziejąca się dopóki wątek żyje - czyli dopóki flaga keepRunning jest true
                while (keepRunning) {
                    //delay wstrzymujący na chwilę wątek - dla rzadszego/częstszego odświeżania danych
                    //dla sinusoidy przy 100 jest ślimak, przy 10 (domyślnym) dość szybko, zmienione na 20
                    //dla Randoma przy ok 150 jest raczej optymalnie
                    Thread.sleep(150); // decrease or remove to speed up the refresh rate.
                    //zmiana fazy z każdym kolejnym krokiem o +1
                    //jeśli amplituda sinusa osiągnie wartość równą maksymalnej dopuszczonej (zadanej),
                    //to wtedy flaga isRising zmienia się na false.

                    uzupelnij();


                    //sinAmp++;


                    //"ogłaszacz" - obiekt obserwowany powiadamia - informuje że coś się zmieniło
                    notifier.notifyObservers();
                }
                //obsługa wyjątku
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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

            return liczby[index];
        }

        //metoda umożliwiająca dodanie Obserwatora
        void addObserver(Observer observer) {
            notifier.addObserver(observer);
        }

        //metoda pozwalająca na usunięcie Obserwatora - nieużywana
        public void removeObserver(Observer observer) {
            notifier.deleteObserver(observer);
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








    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BT_ENABLE_REQUEST:
                if (resultCode == RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "Bluetooth Enabled successfully", Toast.LENGTH_SHORT).show();
                    new SearchDevices().execute();
                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth couldn't be enabled", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }




    private void initList(List<BluetoothDevice> objects) {
        final MyAdapter adapter = new MyAdapter(getApplicationContext(), R.layout.list_item, R.id.listContent, objects);
        deviceListView.setAdapter(adapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapter.setSelectedIndex(position);
                connect.setText("CONNECT");
                connect.setBackgroundColor(getResources().getColor(R.color.blue));
                connect.setEnabled(true);
            }
        });
    }





    private class SearchDevices extends AsyncTask<Void, Void, List<BluetoothDevice>> {

        @Override
        protected List<BluetoothDevice> doInBackground(Void... params) {
            Set<BluetoothDevice> pairedDevices = myBTAdapter.getBondedDevices();
            return new ArrayList<>(pairedDevices);
        }

        @Override
        protected void onPostExecute(List<BluetoothDevice> listDevices) {
            super.onPostExecute(listDevices);
            if (listDevices.size() > 0) {
                MyAdapter adapter = (MyAdapter) deviceListView.getAdapter();
                adapter.replaceItems(listDevices);
            } else {
                Toast.makeText(getApplicationContext(), "No paired devices found, please pair your serial BT device and try again", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private class MyAdapter extends ArrayAdapter<BluetoothDevice> {
        private int selectedIndex;
        private Context context;
        private int selectedColor = getResources().getColor(R.color.blue);
        private List<BluetoothDevice> myList;

        public MyAdapter(Context ctx, int resource, int textViewResourceId, List<BluetoothDevice> objects) {
            super(ctx, resource, textViewResourceId, objects);
            context = ctx;
            myList = objects;
            selectedIndex = -1;
        }

        public void setSelectedIndex(int position) {
            selectedIndex = position;
            notifyDataSetChanged();
        }

        public BluetoothDevice getSelectedItem() {
            return myList.get(selectedIndex);
        }

        @Override
        public int getCount() {
            return myList.size();
        }

        @Override
        public BluetoothDevice getItem(int position) {
            return myList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private class ViewHolder {
            TextView tv;
        }

        public void replaceItems(List<BluetoothDevice> list) {
            myList = list;
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View vi = convertView;
            ViewHolder holder;
            if (convertView == null) {
                vi = LayoutInflater.from(context).inflate(R.layout.list_item, null);
                holder = new ViewHolder();
                holder.tv = vi.findViewById(R.id.listContent);
                vi.setTag(holder);
            } else {
                holder = (ViewHolder) vi.getTag();
            }

            if (selectedIndex != -1 && position == selectedIndex) {
                holder.tv.setBackgroundColor(selectedColor);
            } else {
                holder.tv.setBackgroundColor(Color.WHITE);
            }
            BluetoothDevice device = myList.get(position);
            holder.tv.setText(device.getName() + "\n " + device.getAddress());
            return vi;
        }
    }
}
