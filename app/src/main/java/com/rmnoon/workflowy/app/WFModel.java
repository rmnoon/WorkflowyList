package com.rmnoon.workflowy.app;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rmnoon.workflowy.client.BadLoginException;
import com.rmnoon.workflowy.client.WFClient;
import com.rmnoon.workflowy.client.WFList;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private static final String WIDGET_PREFS_FILE = "WidgetPrefs";
    private static final String WIDGET_PREF_PREFIX = "WIDGET_";

    private static final String GLOBAL_PREFS_FILE = "GlobalPrefs";
    private static final String USERNAME_PREF = "USER";
    private static final String PW_PREF = "PW";
    private static final String VOICE_RECEIVER_PREF = "VOICE_RECEIVER";

    private static final String SESSION_FILE = "WorkflowyListSession";
    private static final String SESSION_PREF = "SESSION";

    private WFClient client;
    private SharedPreferences widgetPrefs, sessionPrefs, globalPrefs;
    private Gson gson;


    private WFModel(Context context) {
        Log.i(TAG, "WFModel created: " + UUID.randomUUID().toString());
        this.client = new WFClient();
        this.gson = new GsonBuilder().create();

        this.widgetPrefs = context.getSharedPreferences(WIDGET_PREFS_FILE, Context.MODE_PRIVATE);
        this.sessionPrefs = context.getSharedPreferences(SESSION_FILE, Context.MODE_PRIVATE);
        this.globalPrefs = context.getSharedPreferences(GLOBAL_PREFS_FILE, Context.MODE_PRIVATE);

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
        return globalPrefs.getString(USERNAME_PREF, "");
    }

    private String getConfiguredPassword() {
        return globalPrefs.getString(PW_PREF, "");
    }

    public boolean isConfigured() {
        return !getConfiguredUsername().isEmpty() && !getConfiguredPassword().isEmpty();
    }

    public void setConfiguredCredentials(String username, String password) {
        globalPrefs.edit()
                .putString(USERNAME_PREF, username == null ? "" : username)
                .putString(PW_PREF, password == null ? "" : password)
                .commit();
    }

    public void ensureAppWidgets(Collection<Integer> widgetIds) {
        if (widgetIds == null) return;

        Map<Integer, WidgetPreferences> toPut = Maps.newHashMap();
        for (Integer appWidgetId : widgetIds) {
            WidgetPreferences prefs = getPrefsForWidget(appWidgetId);
            if (prefs == null) {
                toPut.put(appWidgetId, new WidgetPreferences());
            }
        }

        if (!toPut.isEmpty()) {
            SharedPreferences.Editor editor = widgetPrefs.edit();
            for (Map.Entry<Integer, WidgetPreferences> e : toPut.entrySet()) {
                setPrefsForWidget(e.getKey(), e.getValue(), editor);
            }
            editor.commit();
        }
    }

    public void forgetAppWidgets(Collection<Integer> widgetIds) {
        if (widgetIds == null) return;

        int voiceCommandWidget = getWidgetForVoiceCommands();

        SharedPreferences.Editor editor = widgetPrefs.edit();
        for (Integer appWidgetId : widgetIds) {
            setPrefsForWidget(appWidgetId, null, editor);
            if (appWidgetId.equals(voiceCommandWidget)) {
                setWidgetForVoiceCommands(null);
            }
        }
        editor.commit();
    }

    public void setWidgetList(int appWidgetId, String listId) {
        WidgetPreferences prefs = getPrefsForWidget(appWidgetId);
        prefs.listId = listId == null ? "" : listId; // null or empty listid implies root
        setPrefsForWidget(appWidgetId, prefs, null);
    }

    public WFList getWidgetParentList(int appWidgetId) {
        String listId = getPrefsForWidget(appWidgetId).listId;
        if (listId == null || listId.isEmpty()) { // no setting or empty string implies root lists
            return null;
        }
        return client.isLoggedIn() ? client.getListById(listId) : null;
    }

    public boolean isShowCompleted(int appWidgetId) {
        Boolean showCompletedItems = getPrefsForWidget(appWidgetId).showCompletedItems;
        return Objects.equals(showCompletedItems, Boolean.TRUE);
    }

    public void setShowCompleted(int appWidgetId, boolean showCompleted) {
        WidgetPreferences prefs = getPrefsForWidget(appWidgetId);
        prefs.showCompletedItems = showCompleted;
        setPrefsForWidget(appWidgetId, prefs, null);
        return;
    }

    public int getWidgetForVoiceCommands() {
        // if we don't have an explicit one set, just take the first one
        int voiceReceiver = globalPrefs.getInt(VOICE_RECEIVER_PREF, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (voiceReceiver != AppWidgetManager.INVALID_APPWIDGET_ID) return voiceReceiver;
        for (String prefKey : widgetPrefs.getAll().keySet()) {
            return Integer.parseInt(prefKey.replace(WIDGET_PREF_PREFIX, ""));
        }
        return AppWidgetManager.INVALID_APPWIDGET_ID;
    }

    public void setWidgetForVoiceCommands(@Nullable Integer appWidgetId) {
        if (appWidgetId == null || appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            globalPrefs.edit().remove(VOICE_RECEIVER_PREF).commit();
        } else {
            globalPrefs.edit().putInt(VOICE_RECEIVER_PREF, appWidgetId).commit();
        }
    }

    public List<WFList> getListsForWidget(int appWidgetId) {
        if (!client.isLoggedIn()) return Collections.emptyList();
        WFList parent = getWidgetParentList(appWidgetId);
        boolean showCompleted = isShowCompleted(appWidgetId);
        // null parent implies root lists (or not logged in)
        List<WFList> lists = parent == null ? getRootLists() : parent.getChildren();
        if (lists == null) lists = Collections.emptyList();

        List<WFList> toReturn = Lists.newArrayList();
        for (WFList l : lists) {
            if (showCompleted || !l.isComplete()) {
                toReturn.add(l);
            }
        }
        return toReturn;
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

    private WidgetPreferences getPrefsForWidget(int appWidgetId) {
        String serialized = widgetPrefs.getString(WIDGET_PREF_PREFIX + appWidgetId, null);
        if (serialized == null) return null;
        return gson.fromJson(serialized, WidgetPreferences.class);
    }

    private void setPrefsForWidget(int appWidgetId, @Nullable WidgetPreferences prefs, @Nullable SharedPreferences.Editor openEdits) {
        SharedPreferences.Editor edits = openEdits == null ? widgetPrefs.edit() : openEdits;
        if (prefs == null) {
            edits.remove(WIDGET_PREF_PREFIX + appWidgetId);
        } else {
            edits.putString(WIDGET_PREF_PREFIX + appWidgetId, gson.toJson(prefs));
        }
        if (openEdits == null) edits.commit();
    }

    public static class WidgetPreferences {
        public String listId; // no setting or empty string implies root lists
        public Boolean showCompletedItems;
        public Boolean disableVoiceCommands;
    }
}
