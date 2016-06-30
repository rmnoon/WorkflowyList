package com.rmnoon.workflowy.app.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.rmnoon.workflowy.app.R;
import com.rmnoon.workflowy.app.WFModel;
import com.rmnoon.workflowy.app.WorkflowyListWidget;
import com.rmnoon.workflowy.client.WFList;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.rmnoon.workflowy.app.AppWidgetUtils.fireEvent;

/**
 * Created by rmnoon on 6/13/2016.
 */
public class ListPickerDialogActivity extends Activity {

    private WFModel model;
    private WFList picked;
    private int appWidgetId;

    private TextView pickedItemHeader;
    private TextView pickedItemBreadcrumbs;
    private ImageButton upButton;

    private ListView listPickerItems;
    private WFListAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_list_picker);

        Intent intent = getIntent();
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        model = WFModel.getInstance(this);

        pickedItemHeader = (TextView) findViewById(R.id.picked_item_header);
        pickedItemBreadcrumbs = (TextView) findViewById(R.id.picked_item_breadcrumbs);
        upButton = (ImageButton) findViewById(R.id.up_button);

        listAdapter = new WFListAdapter();
        listPickerItems = (ListView) findViewById(R.id.list_picker_items);
        listPickerItems.setAdapter(listAdapter);
        listPickerItems.setEmptyView(findViewById(R.id.empty_view));

        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (picked == null) return; // root has no parents
                setPickedList(model.getParentList(picked));
            }
        });

        findViewById(R.id.logout_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ListPickerDialogActivity.this)
                        .setTitle(R.string.logout)
                        .setMessage(R.string.logout_confirm)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                ListPickerDialogActivity.this.finish();
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

        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListPickerDialogActivity.this.finish();
            }
        });

        findViewById(R.id.choose_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListPickerDialogActivity.this.finish();
                Map<String, String> extras = Maps.newHashMap();
                if (picked != null) {
                    extras.put(WorkflowyListWidget.LIST_PICKED_EXTRA_LISTID, picked.getId());
                }
                fireEvent(
                        getApplicationContext(),
                        appWidgetId,
                        WorkflowyListWidget.LIST_PICKED_EVENT,
                        WorkflowyListWidget.class,
                        extras,
                        null
                );
            }
        });

        setPickedList(model.getWidgetParentList(appWidgetId));
    }

    private void setPickedList(WFList list) {
        picked = list; // null implies picking root

        if (picked == null) { // we're showing the root list
            upButton.setVisibility(View.GONE); // can't go up

            pickedItemBreadcrumbs.setVisibility(View.GONE); // no breadcrumbs leading to it
            pickedItemBreadcrumbs.setText("");

            pickedItemHeader.setText(getText(R.string.root_list)); // explain to the user what they're looking at
        } else {
            upButton.setVisibility(View.VISIBLE);
            DialogUtils.Breadcrumbs crumbs = DialogUtils.getBreadcrumbs(model, picked, this);
            pickedItemBreadcrumbs.setVisibility(crumbs.breadcrumbs.isEmpty() ? View.GONE : View.VISIBLE);
            pickedItemBreadcrumbs.setText(Html.fromHtml(crumbs.breadcrumbLabel));
            pickedItemHeader.setText(Html.fromHtml(crumbs.selectedListLabel));
        }

        listAdapter.notifyDataSetChanged();
    }

    private List<WFList> getChoices() {
        if (picked == null) return model.getRootLists();
        return picked.getChildren() == null ? Collections.<WFList>emptyList() : picked.getChildren();
    }

    private class WFListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return getChoices().size();
        }

        @Override
        public Object getItem(int position) {
            return getChoices().get(position);
        }

        @Override
        public long getItemId(int position) {
            return getChoices().get(position).getId().hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final WFList list = getChoices().get(position);

            View toUse = convertView != null ? convertView : getLayoutInflater().inflate(R.layout.list_item, parent, false); // false because we can't append to a listview

            String name = list.getName();
            TextView itemNameView = (TextView) toUse.findViewById(R.id.item_name);
            itemNameView.setText(Html.fromHtml(name == null ? "" : name));

            String desc = list.getDescription();
            TextView itemDescView = (TextView) toUse.findViewById(R.id.item_desc);
            if (desc == null || desc.isEmpty()) {
                itemDescView.setText("");
                itemDescView.setVisibility(View.GONE);
            } else {
                itemDescView.setText(Html.fromHtml(desc));
                itemDescView.setVisibility(View.VISIBLE);
            }

            if (list.isComplete()) {
                itemNameView.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
            } else {
                itemNameView.setPaintFlags(Paint.ANTI_ALIAS_FLAG);
            }

            toUse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setPickedList(list);
                }
            });

            return toUse;
        }
    }

}
