package com.rmnoon.workflowy.client;


import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A client for Workflowy.
 *
 * Created by rmnoon on 5/9/16.
 */
public class WFClient {

    public static final Logger log = Logger.getLogger(WFClient.class.getName());

    public static final String LOGIN_URL = "https://workflowy.com/accounts/login/";
    public static final String API_URL = "https://workflowy.com/%s";
    public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.94 Safari/537.36";
    public static final int TIMEOUT_MS = 2500;
    public static final String CLIENT_VERSION = "16";

    private Gson gson;
    private OkHttpClient client;
    private SessionData session;


    public WFClient() {
        gson = new GsonBuilder().create();
        client = new OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build();
        session = null;
    }

    /**
     * Try logging in to Workflowy
     *
     * @param username the user's username
     * @param password the user's password (not stored permanently but unfortunately still insecure in lieu of a proper API)
     * @throws IOException if there's a network problem
     * @throws BadLoginException if there's a problem with credentials
     */
    public void login(String username, String password) throws IOException, BadLoginException {
        if (isLoggedIn()) {
            logout();
        }

        this.session = new SessionData();

        try {
            doLoginRequest(username, password);
            doInitialLoad();
        } catch (IOException | BadLoginException e) {
            this.session = null;
            throw e;
        }
    }

    /**
     * Returns whether or not the user is logged in
     * @return is the user logged in
     */
    public boolean isLoggedIn() {
        return session != null;
    }

    /**
     * Log the user out of Workflowy
     */
    public void logout() {
        checkLoggedIn();
        session = null;
    }

    /**
     * Sets this client to use a previously established session (as returned by {@link WFClient#getSession()}
     * @param session the session string
     * @throws IOException
     */
    public void setSession(String session) throws IOException {
        this.session = gson.fromJson(session, SessionData.class);
    }

    /**
     * Returns a serialized version of this client's session which can be saved and set later on
     * a new instance of {@link WFClient}
     * @return the session string
     */
    public String getSession() {
        return this.session == null ? null : gson.toJson(this.session);
    }

    /**
     * Returns the username of the currently logged in user
     * @return username (which is by convention an email address)
     */
    public String getUsername() {
        return checkLoggedIn(session.username);
    }

    /**
     * Do a hard-refresh of all Workflowy data from the server (where the "true copy" resides)
     * @throws IOException
     */
    public void refresh() throws IOException {
        checkLoggedIn();
        doInitialLoad();
    }

    /**
     * Get the Lists present in the root of the users Workflowy
     * @return the root lists
     */
    public List<WFList> getRootLists() {
        return checkLoggedIn(session.rootLists);
    }

    /**
     * Create a list as a root list on this account, inserted into the supplied index with the supplied
     * name and description.  Will block until the list has been created on the server.
     * @param insertIndex the indext to insert into (null leaves it up to the client)
     * @param name the desired name, null for no name
     * @param description the desired description, null for no description
     * @return the list that was created
     * @throws IOException
     */
    public WFList createRootList(Integer insertIndex, String name, String description) throws IOException {
        return createList(null, insertIndex, name, description);
    }

    /**
     * Create a list as a child of the given list, inserted into the supplied index with the supplied
     * name and description.  Will block until the list has been created on the server.
     *
     * @param parent the list to insert into (null inserts as a root list)
     * @param insertIndex the indext to insert into (null leaves it up to the client)
     * @param name the desired name, null for no name
     * @param description the desired description, null for no description
     * @return the list that was created
     * @throws IOException
     */
    public WFList createList(WFList parent, Integer insertIndex, String name, String description) throws IOException {
        checkLoggedIn();

        long time = getClientTimeInSeconds();
        String newId = UUID.randomUUID().toString();
        String parentId = parent == null ? "None" : parent.id;
        int idx = insertIndex == null ? 0 : insertIndex;

        List<PushPoll.Operation> ops;
        WFList proxy;

        synchronized (this) {
            // create a proxy object to hand back to the client in place of a full refresh
            proxy = new WFList();
            proxy.id = newId;
            proxy.ch = null;
            proxy.cp = null;
            proxy.lm = time;
            proxy.nm = name;
            proxy.no = description;
            // if we have a parent wire the proxy into it (otherwise to the root lists)
            List<WFList> toAddProxyTo = session.rootLists;
            if (parent != null) {
                if (parent.ch == null) {
                    parent.ch = Lists.newArrayListWithCapacity(1);
                }
                toAddProxyTo = parent.ch;
            }
            if (idx >= toAddProxyTo.size()) {
                toAddProxyTo.add(proxy);
            } else if (idx < 0) {
                toAddProxyTo.add(0, proxy);
            } else {
                toAddProxyTo.add(idx, proxy);
            }

            PushPoll.Operation createOp = PushPoll.buildCreateOp(newId, parentId, idx, time);
            PushPoll.Operation editOp = PushPoll.buildEditOp(newId, name, description, time + 1, time);

            ops = Lists.newArrayList(createOp);
            if (editOp != null) ops.add(editOp);
        }

        executePushPoll(ops);

        return proxy;
    }

    /**
     * Edit the given list to have the given name and description.  Will block until the list has
     * been successfully created on the server.
     *
     * @param toEdit the list to edit
     * @param newName the desired name (null for no change)
     * @param newDescription the desired description (null for no change)
     * @throws IOException
     */
    public void editList(WFList toEdit, String newName, String newDescription) throws IOException {
        checkLoggedIn();
        long time = getClientTimeInSeconds();
        Long prevLm = toEdit.lm;
        PushPoll.Operation editOp;
        synchronized (this) {
            // set the proxy too
            if (newName != null) toEdit.nm = newName;
            if (newDescription != null) {
                toEdit.no = newDescription.isEmpty() ? null : newDescription; // a workflowy backend behavior
            }
            toEdit.lm = time;

            editOp = PushPoll.buildEditOp(toEdit.id, newName, newDescription, prevLm, time);
        }

        executePushPoll(ImmutableList.of(editOp));
    }

    /**
     * Set the given list to the supplied completion state.  Will block until it's confirmed by the server.
     * @param toComplete
     * @param isComplete
     * @throws IOException
     */
    public void completeList(WFList toComplete, boolean isComplete) throws IOException {
        checkLoggedIn();
        long time = getClientTimeInSeconds();

        PushPoll.Operation completeOp;
        synchronized (this) {
            Long prevLm = toComplete.lm;
            // set the proxy too
            toComplete.cp = isComplete ? time : null;
            toComplete.lm = time;
            completeOp = PushPoll.buildCompleteOp(toComplete.id, isComplete, toComplete.cp, prevLm, time);
        }

        executePushPoll(ImmutableList.of(completeOp));
    }

    /**
     * Deletes the given list from this workflowy account.  Will block until it's confirmed by the server.
     * @param toDelete
     * @throws IOException
     */
    public void deleteList(WFList toDelete) throws IOException {
        checkLoggedIn();
        long time = getClientTimeInSeconds();
        Long prevLm = toDelete.lm;
        PushPoll.Operation deleteOp;

        synchronized (this) {
            // set the proxy (and delete it from parent)
            toDelete.lm = time;
            List<WFList> toDeleteFrom = null;
            if (isRootList(toDelete)) {
                toDeleteFrom = session.rootLists;
            } else {
                WFList parent = getParentList(toDelete);
                if (parent != null) {
                    toDeleteFrom = parent.ch;
                }
            }
            if (toDeleteFrom != null) {
                toDeleteFrom.remove(Utils.getIndexOfListWithId(toDeleteFrom, toDelete));
            }
            deleteOp = PushPoll.buildDeleteOp(toDelete.id, prevLm, time);
        }

        executePushPoll(ImmutableList.of(deleteOp));
    }


    public synchronized boolean isRootList(WFList list) {
        return Utils.hasListWithId(session.rootLists, list);
    }

    public synchronized boolean hasList(WFList list) {
        return isRootList(list) || getParentList(list) != null;
    }

    public synchronized WFList getParentList(WFList child) {
        if (isRootList(child)) return null;
        return Utils.getParentList(session.rootLists, child);
    }

    public synchronized WFList getListById(final String listId) {
        final WFList[] found = new WFList[] { null };
        Utils.bfsLists(session.rootLists, new Function<WFList, Boolean>() {
            @Override
            public Boolean apply(WFList input) {
                if (input.getId().equals(listId)) {
                    found[0] = input;
                    return true;
                }
                return null;
            }
        });
        return found[0];
    }

    public synchronized List<WFList> getAncestryPath(WFList list) {
        List<WFList> result = Lists.newArrayList();
        while (list != null) {
            result.add(list);
            list = getParentList(list);
        }
        return Lists.reverse(result);
    }

    /* Private methods */

    private void doLoginRequest(String username, String password) throws IOException, BadLoginException {
        Request req = buildRequest(LOGIN_URL)
                .header("Referer", LOGIN_URL)
                .post(new FormBody.Builder()
                        .add("username", username)
                        .add("password", password)
                        .add("next", "")
                        .build()
                )
                .build();
        Response res = executeRequest(req);

        int code = res.code();
        Map<String, String> cookie = Utils.parseCookie(res.header("Set-Cookie"));
        String locationHeader = res.header("Location");
        String sessionId = cookie.get("sessionid");

        if (code != 302 || cookie.isEmpty() || !String.format(API_URL, "").equals(locationHeader) || sessionId == null) {
            throw new BadLoginException();
        }
        synchronized (this) {
            if (session != null) {
                session.sessionId = sessionId;
            }
        }
    }

    private void doInitialLoad() throws IOException {
        String initUrl = String.format(API_URL, "get_initialization_data?client_version=16");
        Request req = buildRequest(initUrl)
                .get()
                .build();
        Response res = executeRequest(req);
        Initialization.Response parsed = gson.fromJson(res.body().charStream(), Initialization.Response.class);

        synchronized (this) {
            session.clientId = Preconditions.checkNotNull(parsed.projectTreeData.clientId);
            session.username = Preconditions.checkNotNull(parsed.settings.username);
            session.initialTransactionId = Preconditions.checkNotNull(parsed.projectTreeData.mainProjectTreeInfo.initialMostRecentOperationTransactionId);
            session.curTransactionId = session.initialTransactionId;
            session.dateJoinedTimestampInSeconds = Preconditions.checkNotNull(parsed.projectTreeData.mainProjectTreeInfo.dateJoinedTimestampInSeconds);

            // reuse any existing WFList objects that we have to preserve referential equality
            List<WFList> newRootLists = Preconditions.checkNotNull(parsed.projectTreeData.mainProjectTreeInfo.rootProjectChildren);
            session.rootLists = Utils.getReusedLists(session.rootLists, newRootLists);


            if (parsed.globals == null) parsed.globals = Collections.emptyList();
            for (List<Object> varPair : parsed.globals) {
                if (varPair.size() != 2) continue;
                String varName = varPair.get(0) == null ? null : varPair.get(0).toString();
                String varVal = varPair.get(1) == null ? null : varPair.get(1).toString();
                if ("USER_ID".equals(varName)) {
                    session.userId = varVal;
                }
            }

            // try and replay any unconfirmed operations that the user wanted (which hopefully are still valid)
            // IDEA: could try to introspect here and see which ones are still relevant
            if (session.unconfirmedOps == null) {
                session.unconfirmedOps = Lists.newArrayList();
            }
            int numReplaysSucceeded = 0;
            for (List<PushPoll.Operation> ops : ImmutableList.copyOf(session.unconfirmedOps)) {
                executePushPoll(ops);
                numReplaysSucceeded++;
            }
            if (numReplaysSucceeded > 0) { // resync to load the results of our late pushes
                doInitialLoad();
            }
        }
    }

    private void executePushPoll(List<PushPoll.Operation> ops) throws IOException {
        Request req;
        synchronized (this) {
            String transactionId = session.curTransactionId;

            PushPoll.Transaction txn = new PushPoll.Transaction();
            txn.most_recent_operation_transaction_id = transactionId;
            txn.operations = ops;

            PushPoll.Data data = new PushPoll.Data();
            data.add(txn);

            req = buildPushPoll(data);
            if (session.unconfirmedOps == null) {
                session.unconfirmedOps = Lists.newArrayList();
            }
            if (!session.unconfirmedOps.contains(ops)) { // might be a retry (in which case it'll already be there)
                session.unconfirmedOps.add(ops);
            }
        }
        Response res = executeRequest(req);
        PushPoll.Response ppRes = gson.fromJson(res.body().charStream(), PushPoll.Response.class);
        synchronized (this) {
            // TODO: might need to get introspective on the HTTP code of the response to know whether we should remove the unconfirmed ops
            session.unconfirmedOps.remove(ops);
            session.curTransactionId = Preconditions.checkNotNull(ppRes.results.get(0).new_most_recent_operation_transaction_id);
        }
    }

    private Request buildPushPoll(PushPoll.Data data) {
        String pushPollId = "WB79Gp0T"; // TODO: Should we generate this somehow?

        return buildRequest(String.format(API_URL, "push_and_poll"))
                .post(new FormBody.Builder()
                        .add("client_id", session.clientId)
                        .add("client_version", CLIENT_VERSION)
                        .add("push_poll_id", pushPollId)
                        .add("push_poll_data", gson.toJson(data))
                        .add("crosscheck_user_id", session.userId)
                        .build()
                )
                .build();
    }

    private Request.Builder buildRequest(String url) {
        return buildRequest(url, null);
    }

    private Request.Builder buildRequest(String url, Map<String, String> headers) {
        if (headers == null) headers = Collections.emptyMap();

        Request.Builder rb = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT);

        for (Map.Entry<String, String> e : headers.entrySet()) {
            rb.header(e.getKey(), e.getValue());
        }
        if (session.sessionId != null) {
            rb.header("Cookie", "sessionid=" + session.sessionId);
        }

        return rb;
    }

    private Response executeRequest(Request request) throws IOException {
        log.info("" + request);
        Response response = client.newCall(request).execute();
        log.info("" + response);
        return response;
    }

    private void checkLoggedIn() {
        checkLoggedIn(null);
    }

    private <T> T checkLoggedIn(T retVal) {
        if (!isLoggedIn()) {
            log.severe("Not logged in (but we need to be!)");
            throw new IllegalStateException("Not logged in!");
        }
        return retVal;
    }

    private synchronized long getClientTimeInSeconds() {
        return System.currentTimeMillis() / 1000 - session.dateJoinedTimestampInSeconds;
    }

}
