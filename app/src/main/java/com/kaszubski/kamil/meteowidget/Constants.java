package com.kaszubski.kamil.meteowidget;

public interface Constants {

    int MIN_CITY_VALUE = 0;
    int WARSAW = 0;
    int LODZ = 1;
    int MAX_CITY_VALUE = 1;

//    int LEGEND = 0;
    int MIN_DAY_VALUE = 1;
//    int TODAY = 1;
//    int TOMORROW = 2;
//    int THE_NEXT_DAY = 3;
    int MAX_DAY_VALUE = 3;

    String PREFS_NAME = "MyPrefs";
    String SHOW_LEGEND = "Legend";
    String TEMPERATURE = "Temperature";
    String FALL = "Fall";
    String WIND = "Wind";
    String CITY = "City";
    String DAY = "Day";
    String LAST_UPDATE = "LastUpdate";

    int[] DAY_WIDTHS = new int[]{68, 156, 155, 156};
    int[] GRAPHS_HEIGHTS = new int[]{25, 85, 89, 88};
}
