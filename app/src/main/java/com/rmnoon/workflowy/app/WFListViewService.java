package com.rmnoon.workflowy.app;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.rmnoon.workflowy.client.WFList;

import java.util.List;

/**
 * The service / factory required for populating a ListView in an AppWidget (remotely).
 *
 * Created by rmnoon on 5/19/16.
 */
public class WFListViewService extends RemoteViewsService {

    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WFListViewFactory(this.getApplicationContext(), intent);
    }

    public static class WFListViewFactory implements RemoteViewsService.RemoteViewsFactory {
        private Context context;
        private int appWidgetId;
        private WFModel model;

        private static final String TAG = WFListViewFactory.class.getName();

        public WFListViewFactory(Context context, Intent intent) {
            this.context = context;
            this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            this.model = WFModel.getInstance(context);
        }

        private List<WFList> getLists() {
            return model.getListsForWidget(appWidgetId);
        }

        @Override
        public int getCount() {
            return getLists().size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            WFList list = getLists().get(position);

            RemoteViews listItemLayout = new RemoteViews(context.getPackageName(), R.layout.list_item);

            String name = list.getName();
            listItemLayout.setTextViewText(R.id.item_name, Html.fromHtml(name == null ? "" : name));

            String desc = list.getDescription();
            if (desc == null || desc.isEmpty()) {
                listItemLayout.setTextViewText(R.id.item_desc, "");
                listItemLayout.setViewVisibility(R.id.item_desc, View.GONE);
            } else {
                listItemLayout.setTextViewText(R.id.item_desc, Html.fromHtml(desc));
                listItemLayout.setViewVisibility(R.id.item_desc, View.VISIBLE);
            }

            if (list.isComplete()) {
                listItemLayout.setInt(R.id.item_name, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
            } else {
                listItemLayout.setInt(R.id.item_name, "setPaintFlags", Paint.ANTI_ALIAS_FLAG);
            }

            AppWidgetUtils.setListItemClick(listItemLayout, R.id.list_item, position);

            return listItemLayout;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return getLists().get(position).getId().hashCode();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public void onCreate() {

        }

        @Override
        public void onDataSetChanged() {

        }

        @Override
        public void onDestroy() {

        }
    }


}
