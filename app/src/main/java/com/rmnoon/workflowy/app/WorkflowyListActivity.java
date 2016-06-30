package com.rmnoon.workflowy.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;


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
