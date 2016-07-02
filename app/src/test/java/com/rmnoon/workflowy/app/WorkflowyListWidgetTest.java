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
        WFList blueMilk = client.createList(todo, 0, "buy blue milk", null);
        WFList protocolDroid = client.createList(todo, 1, "find a new protocol droid", "must be able to speak Bocce");
        client.completeList(protocolDroid, true);
        client.createList(todo, 2, "pick up power converters", "at toshe station");
        client.createList(todo, 3, "study for the Academy entrance test", null);
        client.createList(todo, 4, "investigate womp rat infestation", null);

        WFList ideas = client.createRootList(1, "ideas", "my greatest ideas");
        WFList podracing = client.createList(ideas, 0, "podracing: is it still a thing?", null);
        WFList mosEisley = client.createList(ideas, 1, "mos eisley is a wretched hive of scum and villainy but its really quite livable and the bars are great.", null);
        WFList sandPeople = client.createList(ideas, 2, "why do sand people always walk single file?", null);
        client.completeList(sandPeople, true);
        WFList biggs = client.createList(ideas, 3, "biggs darklighter", "i wonder how he's doing these days");

        assertEquals(client.getRootLists().size(), 2);

        assertEquals(client.getRootLists().get(0), todo);
        assertEquals(todo.getChild(0), blueMilk);
        assertEquals(todo.getChild(1), protocolDroid);
        assertTrue(protocolDroid.isComplete());

        assertEquals(client.getRootLists().get(1), ideas);
        assertEquals(ideas.getChild(0), podracing);
        assertEquals(ideas.getChild(1), mosEisley);
        assertTrue(sandPeople.isComplete());
    }

}