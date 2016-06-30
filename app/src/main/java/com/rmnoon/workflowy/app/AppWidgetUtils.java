package com.rmnoon.workflowy.app;

import android.app.Activity;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

import java.util.Collections;
import java.util.Map;

/**
 * Handy utility functions for repeated AppWidget boilerplate.
 *
 * Created by rmnoon on 5/24/16.
 */
public class AppWidgetUtils {

    public static final String EXTRA_LIST_INDEX = AppWidgetUtils.class.getName() + ".EXTRA_LIST_INDEX";

    public static void addListener(Context context, RemoteViews rv, int appWidgetId, int componentId, String action, Class<? extends AppWidgetProvider> widgetClass) {
        Intent intent = new Intent(context, widgetClass)
                .setAction(action)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(componentId, pendingIntent);
    }

    public static void fireEvent(Context context, int appWidgetId, String action, Class<? extends AppWidgetProvider> widgetClass, Map<String, String> extraStrings, Map<String, Integer> extraInts) {
        Intent intent = new Intent(context, widgetClass)
                .setAction(action)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        fillIntent(intent, extraStrings, extraInts);
        context.sendBroadcast(intent);
    }

    public static void addAdapter(Context context, RemoteViews rv, int appWidgetId, int listComponentId, Integer emptyViewId, Class<?> serviceClass) {
        Intent intent = new Intent(context, serviceClass);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        // When intents are compared, the extras are ignored, so we need to embed the extras
        // into the data so that the extras will not be ignored.
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        rv.setRemoteAdapter(listComponentId, intent);

        // The empty view is displayed when the collection has no items. It should be a sibling
        // of the collection view.
        if (emptyViewId != null) {
            rv.setEmptyView(listComponentId, emptyViewId);
        }
    }

    public static void addListListener(Context context, RemoteViews rv, int appWidgetId, int componentId, String action, Class<? extends AppWidgetProvider> widgetClass) {
        // Here we setup the a pending intent template. Individuals items of a collection
        // cannot setup their own pending intents, instead, the collection as a whole can
        // setup a pending intent template, and the individual items can set a fillInIntent
        // to create unique before on an item to item basis.
        Intent i = new Intent(context, widgetClass)
                    .setAction(action)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        i.setData(Uri.parse(i.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setPendingIntentTemplate(componentId, pi);
    }

    public static void setListItemClick(RemoteViews itemView, int itemViewId, int itemIndex) {
        // Next, we set a fill-intent which will be used to fill-in the pending intent template
        // which is set on the collection view in StackWidgetProvider.
        Bundle extras = new Bundle();
        extras.putInt(EXTRA_LIST_INDEX, itemIndex);
        Intent fillInIntent = new Intent().putExtras(extras);
        itemView.setOnClickFillInIntent(itemViewId, fillInIntent);
    }

    public static void enqueueForService(Context context, Class<? extends IntentService> clazz, int appWidgetId, String action, Map<String, String> extraStrings) {
        Intent intent = new Intent(context, clazz)
                .setAction(action)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        fillIntent(intent, extraStrings, null);
        context.startService(intent);
    }

    public static void showDialog(Context context, int appWidgetId, Class<? extends Activity> activityClass, Map<String, String> extraStrings, Map<String, Integer> extraInts) {
        Intent intent = new Intent(context, activityClass)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        fillIntent(intent, extraStrings, extraInts);
        context.startActivity(intent);
    }

    public static int[] getAppWidgetIds(Context context) {
        return AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WorkflowyListWidget.class));
    }

    public static void notifyListItemsChanged(Context context) {
        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(getAppWidgetIds(context), R.id.list_items);
    }

    private static void fillIntent(Intent intent, Map<String, String> extraStrings, Map<String, Integer> extraInts) {
        if (extraStrings == null) extraStrings = Collections.emptyMap();
        for (Map.Entry<String, String> e : extraStrings.entrySet()) {
            intent.putExtra(e.getKey(), e.getValue());
        }

        if (extraInts == null) extraInts = Collections.emptyMap();
        for (Map.Entry<String, Integer> e : extraInts.entrySet()) {
            intent.putExtra(e.getKey(), e.getValue());
        }
    }
}
