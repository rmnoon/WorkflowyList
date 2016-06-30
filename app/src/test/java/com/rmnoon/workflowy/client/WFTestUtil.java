package com.rmnoon.workflowy.client;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Created by rmnoon on 6/16/2016.
 */
public class WFTestUtil {

    public static void clearLists(WFClient client) throws IOException {
        List<WFList> lists = ImmutableList.copyOf(client.getRootLists());
        for (WFList l : lists) {
            client.deleteList(l);
        }
    }

    public static String getTestUser() {
        return checkPlaceholder(getTestCreds().username, PLACEHOLDER_USERNAME);
    }

    public static String getTestPassword() {
        return checkPlaceholder(getTestCreds().password, PLACEHOLDER_PASSWORD);
    }

    private static final String CREDS_FILENAME = "test-credentials.json";
    private static final String PLACEHOLDER_USERNAME = "YOUR_TEST_ACCOUNT_USERNAME";
    private static final String PLACEHOLDER_PASSWORD = "YOUR_TEST_ACCOUNT_PASSWORD";

    private static String checkPlaceholder(String read, String placeholder) {
        if (Objects.equals(read, placeholder)) throw new RuntimeException("You need to supply your own Workflowy test account credentials in " + CREDS_FILENAME);
        else return read;
    }

    private static TestCredentials getTestCreds() {
        try {
            return new Gson().fromJson(Resources.toString(Resources.getResource(CREDS_FILENAME), Charsets.UTF_8), TestCredentials.class);
        } catch (Throwable t) {
            throw new RuntimeException("Couldn't load test credentials: you must place a test Workflowy account in " + CREDS_FILENAME + ".", t);
        }
    }

    public static class TestCredentials {
        public String username;
        public String password;
    }
}
