package com.rmnoon.workflowy.client;

import java.util.List;

/**
 * Created by rmnoon on 5/18/16.
 */
public class SessionData {
    public String sessionId;
    public String clientId;
    public String username;
    public String userId;
    public String initialTransactionId;
    public String curTransactionId;
    public long dateJoinedTimestampInSeconds;
    public List<WFList> rootLists;
    public List<List<PushPoll.Operation>> unconfirmedOps;
}
