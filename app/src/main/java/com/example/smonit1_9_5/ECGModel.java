package com.example.smonit1_9_5;

import android.util.Log;

import com.androidplot.xy.AdvancedLineAndPointRenderer;
import com.androidplot.xy.XYSeries;

import java.lang.ref.WeakReference;

/**
 * Primitive simulation of some kind of signal.  For this example,
 * we'll pretend its an ecg.  This class represents the data as a circular buffer;
 * data is added sequentially from left to right.  When the end of the buffer is reached,
 * i is reset back to 0 and simulated sampling continues.
 */
//public static class ECGModel implements XYSeries {
public class ECGModel implements XYSeries {

    private static final String TAG = "SMonit";
    //zadeklarowane zmienne: numeryczny bufor na dane, wartość opóźnienia, poboczny wątek,
    // zmienna czy (wątek) nadal żyje i zmienna ostatni indeks
    private final Number[] data;
    private final long delayMs;
    private final Thread thread;
    private boolean keepRunning;
    private int latestIndex;

    //pole "słabej referencji"
    private WeakReference<AdvancedLineAndPointRenderer> rendererRef;

    //parametr size - rozmiar "Próbki danych" zawartej w tym modelu
    //paramter updateFreqHz - częstotliwość z jaką dane są dodawane do modelu
    /**
     * @param size Sample size contained within this model
     * @param updateFreqHz Frequency at which new samples are added to the model
     */
    //konstruktor klasy ECGModel z dwoma powyższymi parametrami: rozmiar, częstotliwość
    ECGModel(int size, int updateFreqHz, final String strumienInput) {
        //tworzymy nowy bufor numeryczny (tablicę) o nazwie data i rozmiarze takim, jaki podaliśmy w parametrze wejściowym
        data = new Number[size];
        //pętla - do każdego elementu tego bufora przypisujemy 0 (na start)
        for(int i = 0; i < data.length; i++) {
            data[i] = 0;
        }

        //przeliczenie częstotliwości odświeżania na czas (opóźnienie)
        // translate hz into delay (ms):
        delayMs = 1000 / updateFreqHz;

        final Number[] kontenerek = new Number[4096];

        //powołanie do życia wątku pobocznego do rysowania
        thread = new Thread(new Runnable() {
            @Override
            //metoda run wątku - tu dzieje się mięsko
            public void run() {
                try {
                    //pętla - wykonująca się dopóki wątek żywy
                    while (keepRunning) {
                        //warunek - jeśli ostatni indeks przekracza długość (rozmiar) bufora, to ustaw ostatni indeks jako 0
                        if (latestIndex >= data.length) {
                            latestIndex = 0;
                        }

//------------------------------MOJA WSTAWKA------------------------------------------------------
                        String[] splitted_data_strumien = strumienInput.split("\\r?\\n");
                        Log.d(TAG, splitted_data_strumien[0]);
                        Log.d(TAG, splitted_data_strumien[1]);
                        Log.d(TAG, splitted_data_strumien[2]);
//------------------------------MOJA WSTAWKA------------------------------------------------------

                        //element bufora o indeksie latestIndex to jest obecny indeks!
                        // insert a random sample:
                        //wstaw losową liczbę - symulacja EKG


                        for (int j = 0; j < splitted_data_strumien.length; j++) {
                            kontenerek[j] = Integer.parseInt(splitted_data_strumien[j]);
                        }

                        data[latestIndex] = kontenerek[0];
//                            data[latestIndex] = Math.random() * 17;

                        if(latestIndex < data.length - 1) {
                            // null out the point immediately following i, to disable
                            // connecting i and i+1 with a line:
                            //jakieś zabezpieczenie, żeby aktualny i następny element się nie łączyły automatycznie linią
                            // (albo po prostu chodzi o zerowanie pozostałości z poprzedniej serii danych)
                            data[latestIndex +1] = null;
                        }

                        //warunek - jeśli słaba referencja będzie różna od nulla, to ustaw ostatni indeks jako aktualny (?)
                        if(rendererRef.get() != null) {
                            rendererRef.get().setLatestIndex(latestIndex);
                            //odczekanie czasu w wątku - opóźnienia związanego z częstotliwością
                            Thread.sleep(delayMs);
                        } else {
                            //w innym przypadku - ustaw flagę na false
                            keepRunning = false;
                        }
                        //niezależnie od warunków, zwiększ numer aktualnego (ostatniego) o jeden
                        latestIndex++;
                    }
                    //złapanie wyjątku i też ustawienie flagi na false
                } catch (InterruptedException e) {
                    keepRunning = false;
                }
            }
        });
    }

    //start - uruchomienie wątku
    void start(final WeakReference<AdvancedLineAndPointRenderer> rendererRef) {
        this.rendererRef = rendererRef;
        keepRunning = true;
        thread.start();
    }

    //funkcja zwracająca rozmiar bufora - jego długość
    @Override
    public int size() {
        return data.length;
    }

    //funkcja zwracająca indeks x - numer elementu
    @Override
    public Number getX(int index) {
        return index;
    }

    //funkcja zwracająca wartość y - wartość elementu o indeksie x
    @Override
    public Number getY(int index) {
        return data[index];
    }

    //zwraca tytuł - niby zbędna metoda, ale bez niej nie uruchomi się serii...
    @Override
    public String getTitle() {
        return "Signal";
    }
}
