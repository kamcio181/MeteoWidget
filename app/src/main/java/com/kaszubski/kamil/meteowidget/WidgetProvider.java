package com.kaszubski.kamil.meteowidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;

public class WidgetProvider extends AppWidgetProvider {
    public static final String NEXT_DAY = "android.appwidget.action.NEXT_DAY";
    public static final String PREVIOUS_DAY = "android.appwidget.action.PREVIOUS_DAY";
    public static final String NEXT_CITY = "android.appwidget.action.NEXT_CITY";
    public static final String PREVIOUS_CITY = "android.appwidget.action.PREVIOUS_CITY";
    public static final String REFRESH = "android.appwidget.action.REFRESH";
    public static final String SHOW_DATE = "android.appwidget.action.SHOW_DATE";

    private static final String TAG = "WidgetProvider";

    private SharedPreferences preferences;

    private String lastUpdate;
    private int currentDay;
    private int currentCity;
    private boolean[] enabledParts;
    private boolean showLegend;

    private Bitmap[][] bitmaps = new Bitmap[3][4];

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive");

        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        getConfiguration(context, appWidgetId);

        switch (intent.getAction()) {
            case NEXT_CITY:
                Log.v(TAG, "next city");
                currentCity = currentCity == Constants.MAX_CITY_VALUE? Constants.MIN_CITY_VALUE : currentCity + 1;

                preferences.edit().putInt(Constants.CITY + appWidgetId, currentCity).apply();

                updateWidget(context, appWidgetId);
                break;

            case PREVIOUS_CITY:
                Log.v(TAG, "previous city");
                currentCity = currentCity == Constants.MIN_CITY_VALUE? Constants.MAX_CITY_VALUE : currentCity - 1;

                preferences.edit().putInt(Constants.CITY + appWidgetId, currentCity).apply();

                updateWidget(context, appWidgetId);
                break;

            case NEXT_DAY:
                Log.v(TAG, "next day");
                currentDay = currentDay == Constants.MAX_DAY_VALUE? Constants.MIN_DAY_VALUE : currentDay + 1;

                preferences.edit().putInt(Constants.DAY + appWidgetId, currentDay).apply();

                //updateWidget(context, appWidgetId);
                Utils.updateAllWidgets(context);
                break;

            case PREVIOUS_DAY:
                Log.v(TAG, "previous day");
                currentDay = currentDay == Constants.MIN_DAY_VALUE? Constants.MAX_DAY_VALUE : currentDay - 1;

                preferences.edit().putInt(Constants.DAY + appWidgetId, currentDay).apply();

                updateWidget(context, appWidgetId);
                break;

            case SHOW_DATE:
                Utils.showToast(context, "Last updated: " + lastUpdate);
                break;

            case REFRESH:
                refresh(context);
                break;
        }
        super.onReceive(context, intent);
    }

    private void updateWidget(Context context, int appWidgetId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        onUpdate(context, appWidgetManager, new int[]{appWidgetId});
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.v(TAG, "onUpdate");
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = updateWidgetListView(context, appWidgetId);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private void refresh(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isConnected()) {
            Utils.downloadGraph(context, Constants.WARSAW, null);
            Utils.downloadGraph(context, Constants.LODZ, null);
        } else
            Utils.showToast(context, context.getString(R.string.check_internet_connection));
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        Log.v(TAG, "onDeleted");
        if (preferences == null)
            preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        for (int i : appWidgetIds) {
            editor.remove(Constants.DAY + i).remove(Constants.CITY + i).apply();
        }
    }

    private void getConfiguration(final Context context, int widgetId){
        if (preferences == null)
            preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        if(enabledParts == null)
            enabledParts = new boolean[4];
        showLegend = preferences.getBoolean(Constants.SHOW_LEGEND, true);
        lastUpdate = preferences.getString(Constants.LAST_UPDATE, context.getString(R.string.unknown));
        currentDay = preferences.getInt(Constants.DAY + widgetId, Constants.MIN_DAY_VALUE);
        currentCity = preferences.getInt(Constants.CITY + widgetId, Constants.MIN_CITY_VALUE);
        Log.v(TAG, "widget " + widgetId + " day " + currentDay + " city " +currentCity);
        enabledParts[0] = true;
        enabledParts[1] = preferences.getBoolean(Constants.TEMPERATURE, true);
        enabledParts[2] = preferences.getBoolean(Constants.FALL, true);
        enabledParts[3] = preferences.getBoolean(Constants.WIND, true);
    }

    private PendingIntent getPendingIntentWithAction(Context context, Intent intent, int appWidgetId, String action){
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getOpenAppPendingIntent(Context context, int appWidgetId){
        Intent configIntent = new Intent(context, MainActivity.class);
        configIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        return PendingIntent.getActivity(context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Bitmap mergeBitmap(){
        int height = 0;
        int width = Constants.COLUMN_WIDTHS[1] + Constants.COLUMN_WIDTHS[currentDay]; //Values + day
        if(showLegend)
            width += Constants.COLUMN_WIDTHS[0]; // Legend is optional
        for(int i = 0; i<enabledParts.length; i++){
            if(enabledParts[i]) {
                Log.e(TAG, "HEIGHT " + i + " " + height);
                height += Constants.GRAPHS_HEIGHTS[i];
            }
        }

        if(height == 0 || width == 0)
            return null;

        Bitmap resultGraph = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultGraph);

        float currentX = 0;
        for(int i = showLegend? 0 : 1; i < 3; i++) {
            float currentY = 0;
            for (int j = 0; j < enabledParts.length; j++) {
                if (enabledParts[j]) {
                    if((i != 0 || j != 0) && (i != 1 || j != 0))
                        canvas.drawBitmap(bitmaps[i][j], currentX, currentY, null);
                    currentY += Constants.GRAPHS_HEIGHTS[j];
                }
            }
            currentX += Constants.COLUMN_WIDTHS[i];
        }
        return resultGraph;
    }

    private boolean loadBitmapsFromFile(Context context){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        for(int i = showLegend? 0 : 1; i < 3; i++) {
            int column = i < Constants.MIN_DAY_VALUE? i: currentDay;
            for (int j = 0; j <bitmaps[i].length; j++){
                if(!(new File(context.getCacheDir(), "" + currentCity + "" + column + "" + j + ".jpg").exists())){
                    Log.e(TAG, "FILE NOT EXISTS");
                    return false;
                } else {
                    if((i != 0 || j != 0) && (i != 1 || j != 0))
                        bitmaps[i][j] = BitmapFactory.decodeFile(context.getCacheDir().getPath() + "/"
                                + currentCity + column + j + ".jpg", options);
                }
            }
        }
        return true;
    }

    private RemoteViews updateWidgetListView(Context context,
                                             int appWidgetId) {
        RemoteViews views;

        getConfiguration(context, appWidgetId);

        if(!loadBitmapsFromFile(context)){ //empty view
            views = new RemoteViews(context.getPackageName(), R.layout.empty_view);

            views.setOnClickPendingIntent(R.id.textView6, getPendingIntentWithAction(context,
                    new Intent(context, WidgetProvider.class), appWidgetId, REFRESH));
        } else {

            views = new RemoteViews(context.getPackageName(), R.layout.widget);

            views.setTextViewText(R.id.textView4, context.getResources().getStringArray(R.array.cities)[currentCity]);
            views.setTextViewText(R.id.textView5, context.getResources().getStringArray(R.array.days)[currentDay - 2]); // -2 because legend has index 0 and values has 1

            views.setImageViewBitmap(R.id.imageView5, mergeBitmap());

            views.setOnClickPendingIntent(R.id.textView4, getPendingIntentWithAction(context,
                    new Intent(context, WidgetProvider.class), appWidgetId, REFRESH));
            views.setOnClickPendingIntent(R.id.textView5, getPendingIntentWithAction(context,
                    new Intent(context, WidgetProvider.class), appWidgetId, SHOW_DATE));

            views.setOnClickPendingIntent(R.id.imageView3, getPendingIntentWithAction(context,
                    new Intent(context, WidgetProvider.class), appWidgetId, PREVIOUS_CITY));
            views.setOnClickPendingIntent(R.id.imageView4, getPendingIntentWithAction(context,
                    new Intent(context, WidgetProvider.class), appWidgetId, NEXT_CITY));
            views.setOnClickPendingIntent(R.id.imageView6, getPendingIntentWithAction(context,
                    new Intent(context, WidgetProvider.class), appWidgetId, PREVIOUS_DAY));
            views.setOnClickPendingIntent(R.id.imageView7, getPendingIntentWithAction(context,
                    new Intent(context, WidgetProvider.class), appWidgetId, NEXT_DAY));

            views.setOnClickPendingIntent(R.id.imageView5, getOpenAppPendingIntent(context, appWidgetId));
        }
        Log.v(TAG, "updateWidgetListView");

        return views;
    }
}
