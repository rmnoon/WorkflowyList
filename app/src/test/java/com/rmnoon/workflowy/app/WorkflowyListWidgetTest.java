package com.rmnoon.workflowy.app;

import com.rmnoon.workflowy.client.BadLoginException;
import com.rmnoon.workflowy.client.WFClient;
import com.rmnoon.workflowy.client.WFList;
import com.rmnoon.workflowy.client.WFTestUtil;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Not actually a unit test, just basically a script for setting up the state of our test account for
 * manual testing.
 *
 * Created by rmnoon on 6/16/2016.
 */
public class WorkflowyListWidgetTest {

    @Test
    public void setupForManualTesting() throws IOException, BadLoginException {
        WFClient client = new WFClient();

        client.login(WFTestUtil.getTestUser(), WFTestUtil.getTestPassword());

        WFTestUtil.clearLists(client);

        WFList todo = client.createRootList(0, "todo", "my todo list");
        WFList buyMilk = client.createList(todo, 0, "buy milk", null);
        WFList buyCereal = client.createList(todo, 1, "buy cereal", null);

        WFList ideas = client.createRootList(1, "ideas", "where i keep ideas");
        WFList cureCancer = client.createList(ideas, 0, "cure cancer", "would totally save lives");
        WFList gotoSpace = client.createList(ideas, 1, "go to space", null);
        client.completeList(gotoSpace, true);

        assertEquals(client.getRootLists().size(), 2);

        assertEquals(client.getRootLists().get(0), todo);
        assertEquals(todo.getChild(0), buyMilk);
        assertEquals(todo.getChild(1), buyCereal);

        assertEquals(client.getRootLists().get(1), ideas);
        assertEquals(ideas.getChild(0), cureCancer);
        assertEquals(ideas.getChild(1), gotoSpace);
        assertTrue(gotoSpace.isComplete());
    }

}