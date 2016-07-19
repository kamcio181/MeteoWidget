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
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetProvider extends AppWidgetProvider {
    public static final String NEXT_DAY = "android.appwidget.action.NEXT_DAY";
    public static final String PREVIOUS_DAY = "android.appwidget.action.PREVIOUS_DAY";
    public static final String NEXT_CITY = "android.appwidget.action.NEXT_CITY";
    public static final String PREVIOUS_CITY = "android.appwidget.action.PREVIOUS_CITY";
    public static final String REFRESH = "android.appwidget.action.REFRESH"; //TODO
    public static final String SHOW_DATE = "android.appwidget.action.SHOW_DATE";

    private static final String TAG = "WidgetProvider";

    private SharedPreferences preferences;

    private String lastUpdate;
    private int currentDay;
    private int currentCity;
    private boolean[] enabledParts;
    private boolean showLegend;

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

                updateWidget(context, appWidgetId);
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
                //TODO refresh
                Utils.showToast(context, "Refresh button clicked");
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
        lastUpdate = preferences.getString(Constants.LAST_UPDATE, "unknown");
        currentDay = preferences.getInt(Constants.DAY + widgetId, Constants.MIN_DAY_VALUE);
        currentCity = preferences.getInt(Constants.CITY + widgetId, Constants.MIN_CITY_VALUE);
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

    private Bitmap mergeBitmap(Context context){
        int height = 0;
        int width = Constants.DAY_WIDTHS[currentDay];
        if(showLegend)
            width += Constants.DAY_WIDTHS[0];
        for(int i = 0; i<enabledParts.length; i++){
            if(enabledParts[i]) {
                Log.e(TAG, "HEIGHT " + i + " " + height);
                height += Constants.GRAPHS_HEIGHTS[i];
            }
        }

        if(height == 0 || width == 0)
            return null; //TODO empty view

        Bitmap resultGraph = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultGraph);

        float currentX = 0;
        for(int i = showLegend? 0 : 1; i < 2; i++) {
            float currentY = 0;
            int column = i == 0? 0: currentDay;
            for (int j = 0; j < enabledParts.length; j++) {
                if (enabledParts[j]) {
                    canvas.drawBitmap(loadBitmapFromFile(context, "" + currentCity + column + j), currentX,
                            currentY, null);
                    currentY += Constants.GRAPHS_HEIGHTS[j];
                }
            }
            currentX += Constants.DAY_WIDTHS[0];
        }
        return resultGraph;
    }

    private Bitmap loadBitmapFromFile(Context context, String name){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Log.e(TAG, "loadPath " + context.getCacheDir().getPath() +"/" + name + ".jpg");
        return BitmapFactory.decodeFile(context.getCacheDir().getPath() + "/" + name + ".jpg", options);
    }

    private RemoteViews updateWidgetListView(Context context,
                                             int appWidgetId) {
        if(enabledParts == null)
            getConfiguration(context, appWidgetId);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

        views.setTextViewText(R.id.textView4, context.getResources().getStringArray(R.array.cities)[currentCity]);
        views.setTextViewText(R.id.textView5, context.getResources().getStringArray(R.array.days)[currentDay-1]); // -1 because legend has index 0

        views.setImageViewBitmap(R.id.imageView5, mergeBitmap(context));

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

        Log.v(TAG, "updateWidgetListView");

        return views;
    }
}
