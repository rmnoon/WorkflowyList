package com.rmnoon.workflowy.app.dialog;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.rmnoon.workflowy.app.R;
import com.rmnoon.workflowy.app.WFModel;
import com.rmnoon.workflowy.app.WFService;
import com.rmnoon.workflowy.client.WFList;

import java.util.Map;
import java.util.Objects;

import static com.rmnoon.workflowy.app.AppWidgetUtils.enqueueForService;

/**
 * Created by rmnoon on 6/13/2016.
 */
public class ItemAddEditDialogActivity extends Activity {

    private static final String TAG = ItemAddEditDialogActivity.class.getName();

    public static final String EDIT_ITEM_EXTRA_LISTID = "EDIT_ITEM_EXTRA_LISTID";

    private EditText itemName, itemNote;
    private CheckBox addNote;

    private String startName, startDesc, editListId;
    private int appWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_item_add_edit);

        itemName = (EditText) findViewById(R.id.item_name_text);
        itemNote = (EditText) findViewById(R.id.item_note_text);
        addNote = (CheckBox) findViewById(R.id.add_note_checkbox);

        Intent intent = getIntent();
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

        // if present we're editing instead of adding
        editListId = intent.getStringExtra(EDIT_ITEM_EXTRA_LISTID);

        if (editListId != null) {
            WFModel model = WFModel.getInstance(this);
            WFList toEdit = model.getListById(editListId);
            if (toEdit == null) {
                Log.e(TAG, "Can't edit unknown listId: " + editListId);
                finish();
                return;
            }
            startName = toEdit.getName();
            startDesc = toEdit.getDescription();
            if (startName != null) {
                itemName.setText(Html.fromHtml(startName));
            }
            if (startDesc != null) {
                itemNote.setText(Html.fromHtml(startDesc));
            }

            boolean shouldBeVisible = startDesc != null && !startDesc.isEmpty();
            itemNote.setVisibility(shouldBeVisible ? View.VISIBLE : View.GONE);
            addNote.setChecked(shouldBeVisible);
        }

        addNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemNote.setVisibility(addNote.isChecked() ? View.VISIBLE : View.GONE);
            }
        });

        findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = itemName.getText().toString();
                String desc = itemNote.getText().toString();
                if (itemNote.getVisibility() == View.GONE) {
                    desc = null; // if they toggled it off after writing text don't save it
                }

                ItemAddEditDialogActivity.this.finish();

                if (editListId != null) {  // we're saving edits
                    if (Objects.equals(name, startName) && Objects.equals(desc, startDesc)) {
                        return; // no change, don't make a request
                    }
                    Map<String, String> extras = ImmutableMap.of(
                            WFService.EDIT_ITEM_EXTRA_LISTID, editListId,
                            WFService.EDIT_ITEM_EXTRA_NAME, name,
                            WFService.EDIT_ITEM_EXTRA_DESC, desc == null ? "" : desc
                    );
                    enqueueForService(
                            getApplicationContext(),
                            WFService.class,
                            appWidgetId,
                            WFService.EDIT_ITEM_ACTION,
                            extras
                    );
                } else { // we're adding
                    Map<String, String> extras = Maps.newHashMapWithExpectedSize(2);
                    extras.put(WFService.ADD_ITEM_EXTRA_NAME, name);
                    if (desc != null) extras.put(WFService.ADD_ITEM_EXTRA_DESC, desc);
                    enqueueForService(
                            getApplicationContext(),
                            WFService.class,
                            appWidgetId,
                            WFService.ADD_ITEM_ACTION,
                            extras
                    );
                }
            }
        });

        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ItemAddEditDialogActivity.this.finish();
            }
        });
    }
}
