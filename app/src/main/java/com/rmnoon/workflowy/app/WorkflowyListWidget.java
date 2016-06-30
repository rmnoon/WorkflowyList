package com.rmnoon.workflowy.app;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import com.rmnoon.workflowy.app.dialog.ItemActionDialogActivity;
import com.rmnoon.workflowy.app.dialog.ItemAddEditDialogActivity;
import com.rmnoon.workflowy.app.dialog.ListPickerDialogActivity;
import com.rmnoon.workflowy.app.dialog.LoginDialogActivity;
import com.rmnoon.workflowy.client.WFList;

import java.util.Arrays;

import static com.rmnoon.workflowy.app.AppWidgetUtils.*;

/**
 * The main class for the workflowy widget.  Describes to android how to make one and wire it up.
 *
 * Created by rmnoon on 5/15/16.
 */
public class WorkflowyListWidget extends AppWidgetProvider {

    private static final String TAG = WorkflowyListWidget.class.getName();

    public static final String REDRAW_EVENT = "REDRAW_EVENT";

    public static final String REFRESH_EVENT = "REFRESH_EVENT";

    public static final String ADD_ITEM_EVENT = "ADD_ITEM_EVENT";

    public static final String LIST_ITEM_PRESS_EVENT = "LIST_ITEM_PRESS_EVENT";

    public static final String LOGOUT_EVENT = "LOGOUT_EVENT";

    public static final String LOGIN_EVENT = "LOGIN_EVENT";

    public static final String EDIT_ITEM_EVENT = "EDIT_ITEM_EVENT";
    public static final String EDIT_ITEM_EXTRA_LISTID = "EDIT_ITEM_EXTRA_LISTID";

    public static final String PICK_LIST_EVENT = "PICK_LIST_EVENT";

    public static final String LIST_PICKED_EVENT = "LIST_PICKED_EVENT";
    public static final String LIST_PICKED_EXTRA_LISTID = "LIST_PICKED_EXTRA_LISTID";

    private static void drawWidget(Context context, int appWidgetId) {
        Log.i(TAG, "Drawing widget: " + appWidgetId);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
        WFModel model = WFModel.getInstance(context);
        WFList parentList = model.getWidgetParentList(appWidgetId);

        String listName = parentList == null ? model.getConfiguredUsername() : parentList.getName();
        rv.setTextViewText(R.id.list_name_button, Html.fromHtml(listName == null ? "" : listName));

        addAdapter(context, rv, appWidgetId, R.id.list_items, R.id.empty_view, WFListViewService.class);
        addListListener(context, rv, appWidgetId, R.id.list_items, LIST_ITEM_PRESS_EVENT, WorkflowyListWidget.class);
        addListener(context, rv, appWidgetId, R.id.refresh_button, REFRESH_EVENT, WorkflowyListWidget.class);
        addListener(context, rv, appWidgetId, R.id.add_item_button, ADD_ITEM_EVENT, WorkflowyListWidget.class);
        addListener(context, rv, appWidgetId, R.id.list_name_button, PICK_LIST_EVENT, WorkflowyListWidget.class);
        addListener(context, rv, appWidgetId, R.id.log_in_button, LOGIN_EVENT, WorkflowyListWidget.class);

        boolean isConfigured = model.isConfigured();
        rv.setViewVisibility(R.id.button_bar, isConfigured ? View.VISIBLE : View.GONE);
        rv.setViewVisibility(R.id.list_items, isConfigured ? View.VISIBLE : View.GONE);
        rv.setViewVisibility(R.id.login_panel, isConfigured ? View.GONE : View.VISIBLE);

        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }

    private static void doSilentRefresh(Context context) {
        enqueueForService(
                context,
                WFService.class,
                AppWidgetManager.INVALID_APPWIDGET_ID,
                WFService.REFRESH_ACTION,
                ImmutableMap.of(WFService.REFRESH_EXTRA_MODE, WFService.REFRESH_MODE_SILENT)
        );
    }

    private static void redrawWidgets(Context context) {
        WFModel model = WFModel.getInstance(context);
        Log.i(TAG, "redrawWidgets: " + model.toString());
        model.ensureAppWidgets(Ints.asList(getAppWidgetIds(context)));
        notifyListItemsChanged(context);

        for (int appWidgetId : getAppWidgetIds(context)) {
            drawWidget(context, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // the widget id that sent us this intent (if one sent it, if not it'll be the invalid sentinel)
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

        Log.i(TAG, "Received: " + intent.getAction());

        switch(intent.getAction()) {
            case ConnectivityManager.CONNECTIVITY_ACTION:
                // the network became available so we should do a refresh
                ConnectivityManager cMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo nInfo = cMgr.getActiveNetworkInfo();
                if (nInfo != null && nInfo.isConnected()) {
                    doSilentRefresh(context);
                }
                break;
            case REDRAW_EVENT:
                // do nothing since we redraw on all events anyway
                break;
            case REFRESH_EVENT:
                enqueueForService(
                        context,
                        WFService.class,
                        AppWidgetManager.INVALID_APPWIDGET_ID,
                        WFService.REFRESH_ACTION,
                        null
                );
                break;
            case ADD_ITEM_EVENT:
                showDialog(
                        context,
                        appWidgetId,
                        ItemAddEditDialogActivity.class,
                        null, null
                );
                break;
            case EDIT_ITEM_EVENT:
                showDialog(
                        context,
                        appWidgetId,
                        ItemAddEditDialogActivity.class,
                        ImmutableMap.of(
                                ItemAddEditDialogActivity.EDIT_ITEM_EXTRA_LISTID,
                                intent.getStringExtra(EDIT_ITEM_EXTRA_LISTID)
                        ),
                        null
                );
                break;
            case LIST_ITEM_PRESS_EVENT:
                showDialog(
                        context,
                        appWidgetId,
                        ItemActionDialogActivity.class,
                        null,
                        ImmutableMap.of(
                                AppWidgetUtils.EXTRA_LIST_INDEX,
                                intent.getIntExtra(AppWidgetUtils.EXTRA_LIST_INDEX, -1)
                        )
                );
                break;
            case LOGOUT_EVENT:
                WFModel.getInstance(context).clearSession();
                break;
            case LOGIN_EVENT:
                showDialog(
                        context,
                        appWidgetId,
                        LoginDialogActivity.class,
                        null, null
                );
                break;
            case PICK_LIST_EVENT:
                showDialog(
                        context,
                        appWidgetId,
                        ListPickerDialogActivity.class,
                        null, null
                );
                break;
            case LIST_PICKED_EVENT:
                String listPickedId = intent.getStringExtra(LIST_PICKED_EXTRA_LISTID);
                WFModel.getInstance(context).setWidgetList(appWidgetId, listPickedId == null || listPickedId.isEmpty() ? null : listPickedId);
                break;
            default:
                Log.i(TAG, "No action taken on received intent: " + intent.getAction());
                return;
        }

        redrawWidgets(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.i(TAG, "onUpdate (periodic): " + Arrays.toString(appWidgetIds));
        doSilentRefresh(context);
        redrawWidgets(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.i(TAG, "Enabled WF widget, model is: " + WFModel.getInstance(context).toString());
        doSilentRefresh(context);
        redrawWidgets(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        Log.i(TAG, "onDeleted: " + Arrays.toString(appWidgetIds));
        WFModel.getInstance(context).forgetAppWidgets(Ints.asList(appWidgetIds));
        redrawWidgets(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.i(TAG, "onDisabled");
        WFModel.getInstance(context).backupSession();
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        super.onRestored(context, oldWidgetIds, newWidgetIds);
        redrawWidgets(context);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        Log.i(TAG, "onAppWidgetOptionsChanged " + appWidgetId + " : " + newOptions);
        redrawWidgets(context);
    }

}
