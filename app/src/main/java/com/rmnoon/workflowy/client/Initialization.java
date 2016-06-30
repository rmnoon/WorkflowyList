package com.rmnoon.workflowy.client;

import java.util.List;

/**
 * Created by rmnoon on 5/11/16.
 */
public class Initialization {
    public static class Response {
        public ProjectTreeData projectTreeData;
        public List<List<Object>> globals;
        public Settings settings;
    }

    public static class ProjectTreeData {
        public List<Object> auxiliaryProjectTreeInfos;
        public ProjectTreeInfo mainProjectTreeInfo;
        public String clientId;
    }

    public static class ProjectTreeInfo {
        public String initialMostRecentOperationTransactionId;
        public List<String> serverExpandedProjectsList;
        public long dateJoinedTimestampInSeconds;
        public Object rootProject;
        public long initialPollingIntervalInMs;
        public int monthlyItemQuota;
        public boolean isReadOnly;
        public int ownerId;
        public int itemsCreatedInCurrentMonth;
        public List<WFList> rootProjectChildren;
    }

    public static class Settings {
        public String username;
        public String theme;
        public String last_seen_message_json_string;
        public String saved_views_json;
        public boolean auto_hide_left_bar;
        public boolean unsubscribe_from_summary_emails;
        public String font;
        public boolean backup_to_dropbox;
        public String email;
        public boolean show_keyboard_shortcuts;
    }
}
