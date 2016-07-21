package com.kaszubski.kamil.meteowidget;

public interface Constants {

    int MIN_CITY_VALUE = 0;
    int WARSAW = 0;
    int LODZ = 1;

//    int LEGEND = 0;
//    int VALUES = 1;
    int MIN_DAY_VALUE = 2;
//    int TODAY = 2;
//    int TOMORROW = 3;
//    int THE_NEXT_DAY = 4;
    int MAX_DAY_VALUE = 4;

    String URL_CONST = "http://www.meteo.pl/um/metco/mgram_pict.php?ntype=0u&fdate=";
    String[] CITY_URL = new String[]{
            "00&row=406&col=250&lang=pl", //WARSAW
            "00&row=418&col=223&lang=pl"  //LODZ
    };

    String PREFS_NAME = "MyPrefs";
    String SHOW_LEGEND = "Legend";
    String TEMPERATURE = "Temperature";
    String FALL = "Fall";
    String WIND = "Wind";
    String CITY = "City";
    String DAY = "Day";
    String LAST_UPDATE = "LastUpdate";

    int[] COLUMN_WIDTHS = new int[]{48, 20, 156, 155, 156};
    int[] GRAPHS_HEIGHTS = new int[]{25, 85, 89, 88};
}
