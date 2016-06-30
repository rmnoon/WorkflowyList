package com.rmnoon.workflowy.app.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.rmnoon.workflowy.app.AppWidgetUtils;
import com.rmnoon.workflowy.app.R;
import com.rmnoon.workflowy.app.WFModel;
import com.rmnoon.workflowy.app.WFService;
import com.rmnoon.workflowy.app.WorkflowyListWidget;
import com.rmnoon.workflowy.client.WFList;

import java.util.List;

import static com.rmnoon.workflowy.app.AppWidgetUtils.enqueueForService;
import static com.rmnoon.workflowy.app.AppWidgetUtils.fireEvent;

/**
 * Created by rmnoon on 6/13/2016.
 */
public class ItemActionDialogActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_item_actions);

        final int appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        final int itemIndex = getIntent().getIntExtra(AppWidgetUtils.EXTRA_LIST_INDEX, -1);

        Preconditions.checkState(appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID, "Must have a valid app widget id");
        Preconditions.checkState(itemIndex != -1, "Must have a a valid item index, had " + itemIndex);

        WFModel model = WFModel.getInstance(getApplicationContext());

        final WFList item = model.getListsForWidget(appWidgetId).get(itemIndex);
        final boolean isComplete = item.isComplete(); // save here to avoid confusing the user in case some background refresh changes it

        TextView itemBreadcrumbs = ((TextView)findViewById(R.id.item_breadcrumbs));
        TextView itemHeader = ((TextView)findViewById(R.id.item_name_header));
        DialogUtils.Breadcrumbs crumbs = DialogUtils.getBreadcrumbs(model, item, this);

        itemHeader.setText(Html.fromHtml(crumbs.selectedListLabel));
        if (isComplete) {
            itemHeader.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        } else {
            itemHeader.setPaintFlags(Paint.ANTI_ALIAS_FLAG);
        }

        itemBreadcrumbs.setVisibility(crumbs.breadcrumbs.isEmpty() ? View.GONE : View.VISIBLE);
        itemBreadcrumbs.setText(Html.fromHtml(crumbs.breadcrumbLabel));

        TextView itemDesc = ((TextView)findViewById(R.id.item_desc));
        itemDesc.setVisibility(item.getDescription() == null || item.getDescription().isEmpty() ? View.GONE : View.VISIBLE);
        itemDesc.setText(item.getDescription() == null ? "" : Html.fromHtml(item.getDescription()));

        ((Button) findViewById(R.id.complete_button)).setText(item.isComplete() ? R.string.uncomplete : R.string.complete);

        findViewById(R.id.edit_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ItemActionDialogActivity.this.finish();
                fireEvent(
                        getApplicationContext(),
                        appWidgetId,
                        WorkflowyListWidget.EDIT_ITEM_EVENT,
                        WorkflowyListWidget.class,
                        ImmutableMap.of(WorkflowyListWidget.EDIT_ITEM_EXTRA_LISTID, item.getId()),
                        null
                );
            }
        });

        findViewById(R.id.complete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ItemActionDialogActivity.this.finish();
                enqueueForService(
                        getApplicationContext(),
                        WFService.class,
                        appWidgetId,
                        WFService.COMPLETE_ITEM_ACTION,
                        ImmutableMap.of(
                                WFService.COMPLETE_ITEM_EXTRA_LISTID, item.getId(),
                                WFService.COMPLETE_ITEM_EXTRA_STATE, isComplete ? WFService.COMPLETE_ITEM_FALSE : WFService.COMPLETE_ITEM_TRUE
                        )
                );
            }
        });

        findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ItemActionDialogActivity.this)
                        .setTitle(R.string.delete)
                        .setMessage(R.string.delete_confirm)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                ItemActionDialogActivity.this.finish();
                                enqueueForService(
                                        getApplicationContext(),
                                        WFService.class,
                                        appWidgetId,
                                        WFService.DELETE_ITEM_ACTION,
                                        ImmutableMap.of(WFService.DELETE_ITEM_EXTRA_LISTID, item.getId())
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
