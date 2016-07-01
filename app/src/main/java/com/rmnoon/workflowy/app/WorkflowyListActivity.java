package com.rmnoon.workflowy.app;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

import static com.rmnoon.workflowy.app.AppWidgetUtils.enqueueForService;


public class WorkflowyListActivity extends Activity {

    private static final String TAG = WorkflowyListActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "=========== " + WorkflowyListActivity.class.getSimpleName() + " STARTED ===========");
        Toast.makeText(getApplicationContext(), R.string.help_text, Toast.LENGTH_LONG).show();
        finish();
    }

}
