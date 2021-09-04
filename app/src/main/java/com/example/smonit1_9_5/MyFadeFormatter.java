package com.example.smonit1_9_5;

import android.graphics.Paint;

import com.androidplot.xy.AdvancedLineAndPointRenderer;

/**
 * Special {@link AdvancedLineAndPointRenderer.Formatter} that draws a line
 * that fades over time.  Designed to be used in conjunction with a circular buffer model.
 */
public class MyFadeFormatter extends AdvancedLineAndPointRenderer.Formatter {

    //ZADEKLAROWANA DŁUGOŚĆ "OGONA" SYGNAŁU - CZĘŚCI WIDOCZNEJ
    private int trailSize;

    //KONSTRUKTOR MYFADEFORMATTERA Z JEDNYM PARAMETREM - DŁUGOŚCIĄ OGONA
    MyFadeFormatter(int trailSize) {
        this.trailSize = trailSize;
    }

    //NADPISANA METODA GETLINEPAINT Z TRZEMA PARAMETRAMI - OBECNY INDEKS, OSTATNI I ROZMIAR SERII?
    @Override
    public Paint getLinePaint(int thisIndex, int latestIndex, int seriesSize) {
        //JAKIŚ OFFSET OD OSTATNIEGO INDEKSU - O CO CHODZI?
        // offset from the latest index:
        int offset;
        //WARUNEK - JEŚLI OBECNY INDEKS JEST WIĘKSZY OD OSTATNIEGO INDEKSU, TO WTEDY
        // USTAL OFFSET JAKO WARTOŚC OSTATNIEGO INDEKSU + ROZMIAR SERII - OBECNY INDEKS
        //W OBU PRZYPADKACH WARTOŚĆ OFFSETU JEST TAKA SAMA!
        if (thisIndex > latestIndex) {
            offset = latestIndex + (seriesSize - thisIndex);
        } else {
            offset =  latestIndex - thisIndex;
        }

        //255f - CHODZI PO PROSTU O ZWYKŁE 255 TYLKO W FORMACIE FLOAT
        float scale = 255f / trailSize;
        //NADANIE WARTOŚCI PARAMETROWI ALFA W ZALEŻNOŚCI OD SKALI (JAK DŁUGI MA BYĆ WIDOCZNY OGON SYGNAŁU)
        //ORAZ OFSETU - ZALEŻNEGO OD OBECNEGO POŁOŻENIA I OSTATNIEGO INDEKSU
        //MOŻNA WYWNIOSKOWAĆ ŻE 255 TO WARTOŚĆ MAX
        //JEŚLI TU ZMIENIMY WE WZORZE NA SAM OFSET (ZAMIAST ILOCZYNU), TO WTEDY SAM CIEŃ SIĘ PRZESUWA
        int alpha = (int) (255 - (offset * scale));
//            getLinePaint().setAlpha(alpha > 0 ? alpha : 0);

        //USTAWIENIE WARTOŚCI ALFA KOLORU - OKREŚLAJĄCEGO PRZEZROCZYSTOŚĆ
        if (alpha > 0) {
            getLinePaint().setAlpha(alpha);
        } else {
            getLinePaint().setAlpha(0);
        }
        return getLinePaint();
    }
}
