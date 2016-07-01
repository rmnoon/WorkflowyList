package com.rmnoon.workflowy.app;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.common.collect.ImmutableMap;

import static com.rmnoon.workflowy.app.AppWidgetUtils.enqueueForService;

/**
 * Created by rmnoon on 7/1/2016.
 */
public class VoiceIntentActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addToList(getIntent().getStringExtra(Intent.EXTRA_TEXT));
        finish();
    }

    private void addToList(String textToAdd) {
        WFModel model = WFModel.getInstance(this);

        if (!model.isConfigured()) {
            Toast.makeText(this, R.string.not_logged_in, Toast.LENGTH_LONG).show();
            return;
        }

        int widgetToAddTo = model.getWidgetForVoiceCommands();

        if (widgetToAddTo == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Toast.makeText(this, R.string.no_widgets, Toast.LENGTH_LONG).show();
            return;
        }

        enqueueForService(
                getApplicationContext(),
                WFService.class,
                widgetToAddTo,
                WFService.ADD_ITEM_ACTION,
                ImmutableMap.of(WFService.ADD_ITEM_EXTRA_NAME, textToAdd)
        );
    }
}
