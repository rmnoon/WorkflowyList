package com.rmnoon.workflowy.app;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.rmnoon.workflowy.client.BadLoginException;
import com.rmnoon.workflowy.client.WFClient;
import com.rmnoon.workflowy.client.WFList;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The model for all shared instances of the workflowy widget.
 *
 * Created by rmnoon on 5/18/16.
 */
public class WFModel {

    private static WFModel instance = null;

    public static WFModel getInstance(Context context) {
        Preconditions.checkNotNull(context);

        if (instance == null) {
            instance = new WFModel(context);
        }
        return instance;
    }

    private static final String TAG = WFModel.class.getName();

    private static final String PREFS_FILE = "WorkflowyListPrefs";
    private static final String USERNAME_PREF = "WF_UN";
    private static final String PW_PREF = "WF_PW";
    private static final String WIDGET_LIST_PREFIX = "W_LST_";

    private static final String SESSION_FILE = "WorkflowyListSession";
    private static final String SESSION_PREF = "WF_SESSION";

    private WFClient client;
    private SharedPreferences prefs, sessionPrefs;


    private WFModel(Context context) {
        Log.i(TAG, "WFModel created: " + UUID.randomUUID().toString());
        this.client = new WFClient();
        this.prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        this.sessionPrefs = context.getSharedPreferences(SESSION_FILE, Context.MODE_PRIVATE);

        String savedSession = sessionPrefs.getString(SESSION_PREF, null);

        if (savedSession != null) {
            try {
                client.setSession(savedSession);
            } catch (IOException e) {
                Log.e(TAG, "Bad saved sessionPrefs: " + savedSession);
                e.printStackTrace();
            }
        }

        if (client.isLoggedIn()) {
            Log.i(TAG, "restored from saved session");
        } else {
            Log.i(TAG, "restoration from saved session unsuccessful");
        }
    }

    public void backupSession() {
        if (!client.isLoggedIn()) return;
        String session = client.getSession();
        if (session != null) sessionPrefs.edit().putString(SESSION_PREF, session).commit();
    }

    public void clearSession() {
        setConfiguredCredentials(null, null);
        sessionPrefs.edit().remove(SESSION_PREF).commit();
        if (client.isLoggedIn()) {
            client.logout();
        }
    }

    public void refresh() throws IOException, BadLoginException {
        if (!isConfigured()) {
            Log.e(TAG, "Got an refresh request when we're not configured, ignoring...");
            return;
        }
        try {
            if (!client.isLoggedIn()) {
                client.login(getConfiguredUsername(), getConfiguredPassword());
            }
            // TODO: maybe make refresh do the full login juju, or logout the client and log it in again here
            client.refresh();
        } finally {
            backupSession();
        }
    }

    public String getConfiguredUsername() {
        return prefs.getString(USERNAME_PREF, "");
    }

    private String getConfiguredPassword() {
        return prefs.getString(PW_PREF, "");
    }

    public boolean isConfigured() {
        return !getConfiguredUsername().isEmpty() && !getConfiguredPassword().isEmpty();
    }

    public void setConfiguredCredentials(String username, String password) {
        prefs.edit()
                .putString(USERNAME_PREF, username == null ? "" : username)
                .putString(PW_PREF, password == null ? "" : password)
                .commit();
    }

    public void ensureAppWidgets(Collection<Integer> widgetIds) {
        if (widgetIds == null) return;

        Map<String, String> toPut = Maps.newHashMap();
        for (Integer appWidgetId : widgetIds) {
            String key = WIDGET_LIST_PREFIX + appWidgetId;
            String list = prefs.getString(key, null);
            if (list == null || list.isEmpty()) {
                toPut.put(key, "");
            }
        }

        if (!toPut.isEmpty()) {
            SharedPreferences.Editor editor = prefs.edit();
            for (Map.Entry<String, String> e : toPut.entrySet()) {
                editor.putString(e.getKey(), e.getValue());
            }
            editor.commit();
        }
    }

    public void forgetAppWidgets(Collection<Integer> widgetIds) {
        if (widgetIds == null) return;

        SharedPreferences.Editor editor = prefs.edit();
        for (Integer appWidgetId : widgetIds) {
            String key = WIDGET_LIST_PREFIX + appWidgetId;
            editor.remove(key);
        }
        editor.commit();
    }

    public void setWidgetList(int appWidgetId, String listId) {
        prefs.edit() // null or empty listId implies root
                .putString(WIDGET_LIST_PREFIX + appWidgetId, listId == null ? "" : listId)
                .commit();
    }

    public WFList getWidgetParentList(int appWidgetId) {
        String listId = prefs.getString(WIDGET_LIST_PREFIX + appWidgetId, null);
        if (listId == null || listId.isEmpty()) { // no setting or empty string implies root lists
            return null;
        }
        return client.isLoggedIn() ? client.getListById(listId) : null;
    }

    public List<WFList> getListsForWidget(int appWidgetId) {
        if (!client.isLoggedIn()) return Collections.emptyList();
        WFList parent = getWidgetParentList(appWidgetId);
        // null parent implies root lists (or not logged in)
        List<WFList> rv = parent == null ? getRootLists() : parent.getChildren();
        return rv == null ? Collections.<WFList>emptyList() : rv;
    }

    public List<WFList> getRootLists() {
        return client.isLoggedIn() ? client.getRootLists() : Collections.<WFList>emptyList();
    }

    public WFList getListById(String listId) {
        return client.isLoggedIn() ? client.getListById(listId) : null;
    }

    public WFList getParentList(WFList child) {
        return client.isLoggedIn() ? client.getParentList(child) : null;
    }

    public List<WFList> getAncestryPath(WFList list) {
        return client.isLoggedIn() ? client.getAncestryPath(list) : null;
    }

    public void addItem(int appWidgetId, String name, String description) throws IOException {
        try {
            client.createList(getWidgetParentList(appWidgetId), 0, name, description);
        } finally {
            backupSession();
        }
    }

    public void editItem(String listId, String name, String description) throws IOException {
        try {
            client.editList(client.getListById(listId), name, description);
        } finally {
            backupSession();
        }
    }

    public void completeItem(String listId, boolean state) throws IOException {
        try {
            client.completeList(client.getListById(listId), state);
        } finally {
            backupSession();
        }

    }

    public void deleteItem(String listId) throws IOException {
        try {
            client.deleteList(client.getListById(listId));
        } finally {
            backupSession();
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("client logged in", client.isLoggedIn())
                .add("rootList count", getRootLists() == null ? 0 : getRootLists().size())
                .add("username", getConfiguredUsername())
                .toString();
    }
}
