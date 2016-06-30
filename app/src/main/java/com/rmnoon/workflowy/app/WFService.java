package com.rmnoon.workflowy.app;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.rmnoon.workflowy.client.BadLoginException;

import static com.rmnoon.workflowy.app.AppWidgetUtils.*;

import java.io.IOException;

/**
 * The worker service for the app.  Handles all long-running requests.  Use
 * {@link AppWidgetUtils#enqueueForService} to send it work to do (using the action names [and extra
 * fields if necessary] specified as constants in this class).
 *
 * Created by rmnoon on 5/25/16.
 */
public class WFService extends IntentService {

    private static final String TAG = WFService.class.getName();

    public static final String REFRESH_ACTION = "REFRESH_ACTION";
    public static final String REFRESH_EXTRA_MODE = "REFRESH_EXTRA_MODE";
    public static final String REFRESH_MODE_SILENT = "REFRESH_MODE_SILENT";

    public static final String ADD_ITEM_ACTION = "ADD_ITEM_ACTION";
    public static final String ADD_ITEM_EXTRA_NAME = "ADD_ITEM_EXTRA_NAME";
    public static final String ADD_ITEM_EXTRA_DESC = "ADD_ITEM_EXTRA_DESC";

    public static final String EDIT_ITEM_ACTION = "EDIT_ITEM_ACTION";
    public static final String EDIT_ITEM_EXTRA_LISTID = "EDIT_ITEM_EXTRA_LISTID";
    public static final String EDIT_ITEM_EXTRA_NAME = "EDIT_ITEM_EXTRA_NAME";
    public static final String EDIT_ITEM_EXTRA_DESC = "EDIT_ITEM_EXTRA_DESC";

    public static final String COMPLETE_ITEM_ACTION = "COMPLETE_ITEM_ACTION";
    public static final String COMPLETE_ITEM_EXTRA_LISTID = "COMPLETE_ITEM_EXTRA_LISTID";
    public static final String COMPLETE_ITEM_EXTRA_STATE = "COMPLETE_ITEM_EXTRA_STATE";
    public static final String COMPLETE_ITEM_TRUE = "COMPLETE";
    public static final String COMPLETE_ITEM_FALSE = "INCOMPLETE";

    public static final String DELETE_ITEM_ACTION = "DELETE_ITEM_ACTION";
    public static final String DELETE_ITEM_EXTRA_LISTID = "DELETE_ITEM_EXTRA_LISTID";

    private WFModel model;

    public WFService() {
        super(WFService.class.getName());
        model = WFModel.getInstance(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "action received: " + intent.getAction());
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

        switch (intent.getAction()) {
            case REFRESH_ACTION:
                String mode = intent.getStringExtra(REFRESH_EXTRA_MODE);
                doRefreshAction(REFRESH_MODE_SILENT.equals(mode));
                break;
            case ADD_ITEM_ACTION:
                String addItemName = intent.getStringExtra(ADD_ITEM_EXTRA_NAME);
                String addItemDesc = intent.getStringExtra(ADD_ITEM_EXTRA_DESC);
                doAddItemAction(appWidgetId, addItemName, addItemDesc);
                break;
            case EDIT_ITEM_ACTION:
                String editItemListId = intent.getStringExtra(EDIT_ITEM_EXTRA_LISTID);
                String editItemName = intent.getStringExtra(EDIT_ITEM_EXTRA_NAME);
                String editItemDesc = intent.getStringExtra(EDIT_ITEM_EXTRA_DESC);
                doEditItemAction(editItemListId, editItemName, editItemDesc);
                break;
            case COMPLETE_ITEM_ACTION:
                String completeItemListId = intent.getStringExtra(COMPLETE_ITEM_EXTRA_LISTID);
                String completeItemState = intent.getStringExtra(COMPLETE_ITEM_EXTRA_STATE);
                doCompleteItemAction(completeItemListId, COMPLETE_ITEM_TRUE.equals(completeItemState));
                break;
            case DELETE_ITEM_ACTION:
                String deleteItemListId = intent.getStringExtra(DELETE_ITEM_EXTRA_LISTID);
                doDeleteItemAction(deleteItemListId);
                break;
            default:
                Log.e(TAG, "unknown action: '" + intent.getAction() + "', ignoring...");
                return;
        }

        fireEvent(this, appWidgetId, WorkflowyListWidget.REDRAW_EVENT, WorkflowyListWidget.class, null, null);
    }

    private void toastUser(final int resId, final boolean important) {
        // create a handler to post messages to the main thread
        Handler mHandler = new Handler(getMainLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), resId, important ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doRefreshAction(boolean isSilent) {
        try {
            model.refresh();
            Log.i(TAG, "Successfully refreshed: " + model.getRootLists().size() + " root lists found...");
            if (!isSilent) toastUser(R.string.refresh_success, false);
        } catch (IOException e) {
            e.printStackTrace();
            if (!isSilent) toastUser(R.string.cant_refresh, true);
        } catch (BadLoginException e) {
            e.printStackTrace();
            if (!isSilent) toastUser(R.string.bad_login_on_refresh, true);
        }
    }

    private void doAddItemAction(int appWidgetId, String name, String desc) {
        try {
            model.addItem(appWidgetId, name, desc);
            toastUser(R.string.item_added, false);
        } catch (IOException e) {
            e.printStackTrace();
            toastUser(R.string.item_added_local, true);
        }
    }

    private void doEditItemAction(String listId, String name, String desc) {
        try {
            model.editItem(listId, name, desc);
            toastUser(R.string.item_edited, false);
        } catch (IOException e) {
            e.printStackTrace();
            toastUser(R.string.item_edited_local, true);
        }
    }

    private void doCompleteItemAction(String listId, boolean state) {
        try {
            model.completeItem(listId, state);
            toastUser(state ? R.string.item_completed : R.string.item_uncompleted, false);
        } catch (IOException e) {
            e.printStackTrace();
            toastUser(state ? R.string.item_completed_local : R.string.item_uncompleted_local, true);
        }
    }

    private void doDeleteItemAction(String listId) {
        try {
            model.deleteItem(listId);
            toastUser(R.string.item_deleted, false);
        } catch (IOException e) {
            e.printStackTrace();
            toastUser(R.string.item_deleted_local, true);
        }
    }
}
