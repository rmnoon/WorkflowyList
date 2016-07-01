package com.rmnoon.workflowy.app.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.rmnoon.workflowy.app.AppWidgetUtils;
import com.rmnoon.workflowy.app.R;
import com.rmnoon.workflowy.app.WFModel;
import com.rmnoon.workflowy.app.WorkflowyListWidget;
import com.rmnoon.workflowy.client.WFList;

import static com.rmnoon.workflowy.app.AppWidgetUtils.fireEvent;

/**
 * Configures the widget for a given list.
 *
 * Created by rmnoon on 6/13/2016.
 */
public class ListSettingsDialogActivity extends Activity {

    private WFModel model;
    private int appWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_settings);

        appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        model = WFModel.getInstance(this);

        setupHeader();
        setupRefresh();
        setupShowCompleted();
        setupChooseList();
        setupEnableVoice();
        setupLogout();
    }

    private void setupHeader() {
        WFList toConfigure = model.getWidgetParentList(appWidgetId);
        TextView settingsHeader = (TextView) findViewById(R.id.settings_header);
        TextView settingsBreadcrumbs = (TextView) findViewById(R.id.settings_breadcrumbs);

        if (toConfigure == null) { // we're showing the root list
            settingsBreadcrumbs.setVisibility(View.GONE); // no breadcrumbs leading to it
            settingsBreadcrumbs.setText("");
            settingsHeader.setText(getText(R.string.root_list)); // explain to the user what they're looking at
        } else {
            DialogUtils.Breadcrumbs crumbs = DialogUtils.getBreadcrumbs(model, toConfigure, this);
            settingsBreadcrumbs.setVisibility(crumbs.breadcrumbs.isEmpty() ? View.GONE : View.VISIBLE);
            settingsBreadcrumbs.setText(Html.fromHtml(crumbs.breadcrumbLabel));
            settingsHeader.setText(Html.fromHtml(crumbs.selectedListLabel));
        }
    }

    private void setupRefresh() {
        findViewById(R.id.refresh_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListSettingsDialogActivity.this.finish();
                AppWidgetUtils.fireEvent(
                        getApplicationContext(),
                        appWidgetId,
                        WorkflowyListWidget.REFRESH_EVENT,
                        WorkflowyListWidget.class,
                        null,
                        null
                );
            }
        });
    }

    private void setupChooseList() {
        findViewById(R.id.choose_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListSettingsDialogActivity.this.finish();
                AppWidgetUtils.showDialog(
                        ListSettingsDialogActivity.this,
                        appWidgetId,
                        ListPickerDialogActivity.class,
                        null, null
                );
            }
        });
    }

    private void setupShowCompleted() {
        Button showCompletedButton = (Button) findViewById(R.id.show_completed_button);
        showCompletedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListSettingsDialogActivity.this.finish();
                model.setShowCompleted(appWidgetId, !model.isShowCompleted(appWidgetId));
                fireEvent(
                        getApplicationContext(),
                        appWidgetId,
                        WorkflowyListWidget.REDRAW_EVENT,
                        WorkflowyListWidget.class,
                        null,
                        null
                );
            }
        });
        showCompletedButton.setText(model.isShowCompleted(appWidgetId) ? R.string.hide_completed : R.string.show_completed);
    }

    private void setupEnableVoice() {
        Button enableVoiceButton = (Button) findViewById(R.id.voice_command_button);
        enableVoiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListSettingsDialogActivity.this.finish();
                model.setWidgetForVoiceCommands(appWidgetId);
            }
        });
        enableVoiceButton.setVisibility(model.getWidgetForVoiceCommands() == appWidgetId ? View.GONE : View.VISIBLE);
    }

    private void setupLogout() {
        findViewById(R.id.logout_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ListSettingsDialogActivity.this)
                        .setTitle(R.string.logout)
                        .setMessage(R.string.logout_confirm)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                ListSettingsDialogActivity.this.finish();
                                fireEvent(
                                        getApplicationContext(),
                                        appWidgetId,
                                        WorkflowyListWidget.LOGOUT_EVENT,
                                        WorkflowyListWidget.class,
                                        null,
                                        null
                                );
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
    }




}
