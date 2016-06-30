package com.rmnoon.workflowy.app.dialog;

import android.content.Context;
import android.view.View;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.rmnoon.workflowy.app.R;
import com.rmnoon.workflowy.app.WFModel;
import com.rmnoon.workflowy.client.WFList;

import java.util.List;

/**
 * Helpers for Dialog formatting!
 *
 * Created by rmnoon on 6/16/2016.
 */
public class DialogUtils {

    public static boolean isRtl(Context ctx) {
        return ctx.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    public static String getBreadCrumbSeparator(Context ctx) {
        return " " + ctx.getString(isRtl(ctx) ? R.string.path_separator_rtl : R.string.path_separator_ltr) + " ";
    }

    public static String getEmptyListName(Context ctx) {
        return "<em>" + ctx.getString(R.string.empty) + "</em>";
    }

    public static Breadcrumbs getBreadcrumbs(WFModel model, WFList selected, Context ctx) {
        List<WFList> ancestryPath = model.getAncestryPath(selected);
        List<String> ancestorNames = Lists.newArrayListWithCapacity(ancestryPath.size());
        String emptyListName = getEmptyListName(ctx);
        String breadCrumbSep = getBreadCrumbSeparator(ctx);

        for (WFList ancestor : ancestryPath) {
            ancestorNames.add(ancestor.getName() == null || ancestor.getName().isEmpty() ? emptyListName : ancestor.getName());
        }
        List<String> breadCrumbs = ancestorNames.subList(0, ancestorNames.size() - 1);
        String pickedName = ancestorNames.get(ancestorNames.size() - 1);
        String breadcrumbLabel = Joiner.on(breadCrumbSep).join(breadCrumbs) + breadCrumbSep;
        return new Breadcrumbs(breadCrumbs, breadcrumbLabel, pickedName);
    }

    public static class Breadcrumbs {
        public List<String> breadcrumbs;
        public String breadcrumbLabel;
        public String selectedListLabel;

        public Breadcrumbs(List<String> breadcrumbs, String breadcrumbLabel, String selectedListLabel) {
            this.breadcrumbs = breadcrumbs;
            this.breadcrumbLabel = breadcrumbLabel;
            this.selectedListLabel = selectedListLabel;
        }
    }

}
