package com.rmnoon.workflowy.client;

import com.google.common.collect.ImmutableList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the workflowy client.  They're essentially integration tests so they require
 * workflowy.com to be available and require a test account (whose content will be cleared,
 * overridden, and cleared again during the tests).
 *
 * Created by rmnoon on 5/9/16.
 */
public class WFClientTest {

    private WFClient client;

    @Before
    public void before() throws IOException, BadLoginException {
        client = new WFClient();
        client.login(WFTestUtil.getTestUser(), WFTestUtil.getTestPassword());
        assertEquals(client.getUsername(), WFTestUtil.getTestUser());
        WFTestUtil.clearLists(client);
        client.refresh();
        assertEquals(client.getRootLists().size(), 0);
    }

    @After
    public void after() throws IOException {
        if (client.isLoggedIn()) {
            client.refresh();
            WFTestUtil.clearLists(client);
            client.logout();
        }
    }

    @Test(expected = BadLoginException.class)
    public void testBadLogin() throws IOException, BadLoginException {
        client.logout();
        assertFalse(client.isLoggedIn());
        String badUser = "nonexistent_" + UUID.randomUUID().toString();
        String badPass = "badpass_" + UUID.randomUUID().toString();
        client.login(badUser, badPass);
    }

    @Test
    public void testCreateList() throws IOException {
        assertTrue(client.getRootLists().isEmpty());

        String rootName = "createList root name " + UUID.randomUUID().toString();
        String rootDesc = "createList root desc " + UUID.randomUUID().toString();

        WFList root = client.createRootList(0, rootName, rootDesc);
        List<WFList> lists = client.getRootLists();
        String rootId = root.getId();

        assertNotNull(root);
        assertTrue(client.hasList(root));
        assertTrue(lists.contains(root));

        client.refresh();
        lists = client.getRootLists();
        assertTrue("objects are reused after refresh", root == lists.get(0));
        assertEquals(lists.size(), 1);
        assertEquals(rootName, root.getName());
        assertEquals(rootDesc, root.getDescription());
        assertEquals(rootId, root.getId());

        client.createList(root, 0, root.getName() + "_1", null);
        client.createList(root, 0, root.getName() + "_0", null);
        client.createList(root, 2, root.getName() + "_2", null);
        client.refresh();

        lists = client.getRootLists();
        assertTrue("objects are reused after refresh", root == lists.get(0));
        List<WFList> rootChildren = root.getChildren();

        assertEquals(lists.size(), 1);
        assertEquals(rootName, root.getName());
        assertEquals(rootDesc, root.getDescription());
        assertEquals(root, client.getListById(rootId));

        assertEquals(rootChildren.size(), 3);
        for (int i = 0; i < 3; i++) {
            assertEquals(rootChildren.get(i).getName(), root.getName() + "_" + i);
            assertNull(rootChildren.get(i).getDescription());
            assertNull(rootChildren.get(i).getChildren());
        }

        WFList prepended = client.createRootList(0, "prepend_" + root.getName(), "prepend_desc_" + root.getDescription());
        client.refresh();
        lists = client.getRootLists();
        assertEquals(lists.size(), 2);
        assertTrue(prepended == lists.get(0));
        assertTrue(root == lists.get(1));
        assertEquals(prepended.getName(), "prepend_" + root.getName());
        assertEquals(prepended.getDescription(), "prepend_desc_" + root.getDescription());
        assertEquals(root.getChildren().size(), 3);
        assertEquals(root.getChildren().get(2), client.getListById(root.getChildren().get(2).getId()));
    }

    @Test
    public void testEditList() throws IOException {
        String rootName = "root_" + UUID.randomUUID().toString();
        WFList rootList = client.createRootList(0, rootName, null);
        String rootId = rootList.getId();
        String childName = rootName + "_child";
        WFList childList = client.createList(rootList, 0, childName, null);
        String childId = childList.getId();

        client.refresh();
        List<WFList> rootLists = client.getRootLists();
        rootList = rootLists.get(0);
        childList = rootList.getChildren().get(0);

        assertEquals(rootId, rootList.getId());
        assertEquals(rootName, rootList.getName());
        assertEquals(childId, childList.getId());
        assertEquals(childName, childList.getName());

        String newRootName = "new_" + rootName;
        String newRootDesc = "desc_" + newRootName;
        String newChildName = "new_" + childName;
        String newChildDesc = "new_" + newRootDesc;
        client.editList(rootList, newRootName, newRootDesc);
        client.editList(childList, newChildName, null);
        client.editList(childList, null, newChildDesc);

        client.refresh();
        List<WFList> newRootLists = client.getRootLists();
        WFList newRootList = newRootLists.get(0);
        WFList newChildList = newRootList.getChildren().get(0);

        assertEquals(newRootLists.size(), 1);
        assertEquals(rootId, newRootList.getId());
        assertEquals(childId, newChildList.getId());
        assertEquals(newRootName, newRootList.getName());
        assertEquals(newChildName, newChildList.getName());
        assertEquals(newRootDesc, newRootList.getDescription());
        assertEquals(newChildDesc, newChildList.getDescription());
        assertEquals(newRootList.getChildren().size(), 1);

        client.editList(newRootList, "", "");
        client.refresh();
        List<WFList> newNewRootLists = client.getRootLists();
        WFList newNewRootList = newNewRootLists.get(0);
        WFList newNewChildList = newNewRootList.getChildren().get(0);

        assertEquals(newNewRootLists.size(), 1);
        assertEquals(rootId, newNewRootList.getId());
        assertEquals(childId, newNewChildList.getId());
        assertEquals("", newNewRootList.getName());
        assertEquals(null, newNewRootList.getDescription());
    }

    @Test
    public void testCompleteList() throws IOException {
        String rootName = "root_" + UUID.randomUUID().toString();
        WFList rootList = client.createRootList(0, rootName, null);
        String childName = rootName + "_child";
        client.createList(rootList, 0, childName, null);

        client.refresh();
        rootList = client.getRootLists().get(0);
        WFList childList = rootList.getChildren().get(0);

        assertFalse(rootList.isComplete());
        assertFalse(childList.isComplete());

        client.completeList(rootList, true);

        client.refresh();
        rootList = client.getRootLists().get(0);
        childList = rootList.getChildren().get(0);

        assertTrue(rootList.isComplete());
        assertFalse(childList.isComplete());

        client.completeList(rootList, false);
        client.completeList(childList, true);

        client.refresh();
        rootList = client.getRootLists().get(0);
        childList = rootList.getChildren().get(0);

        assertFalse(rootList.isComplete());
        assertTrue(childList.isComplete());
    }

    @Test
    public void testDeleteList() throws IOException {
        String rootName = "root_" + UUID.randomUUID().toString();
        WFList root = client.createRootList(0, rootName, null);
        WFList child = client.createList(root, 0, "child_" + rootName, null);
        assertTrue(root.getChildren().contains(child));

        client.deleteList(child);
        assertFalse(root.getChildren().contains(child));

        client.refresh();
        root = client.getRootLists().get(0);
        assertNull(root.getChildren());
    }

    @Test
    public void testTreeMethods() throws IOException {
        String rootName = "root_" + UUID.randomUUID().toString();
        for (int i = 0; i < 2; i++) {
            WFList l = client.createRootList(null, rootName + "," + i, null);
            for (int j = 0; j < 3; j++) {
                WFList m = client.createList(l, null, l.getName() + "," + j, null);
                for (int k = 0; k < 4; k++) {
                    client.createList(m, null, m.getName() + "," + k, null);
                }
            }
        }

        List<WFList> rootLists = client.getRootLists();

        // has list
        WFList bogusList = new WFList();
        bogusList.id = "bogus";
        bogusList.nm = "bogus";
        assertFalse(client.hasList(bogusList));
        int levelsFound = 0;
        for (WFList l = rootLists.get(0); l != null;) {
            levelsFound++;
            assertTrue(client.hasList(l));
            l = l.getChild(0);
        }
        assertEquals(levelsFound, 3);

        // get parent list
        assertEquals(client.getParentList(rootLists.get(0)), null);
        assertEquals(client.getParentList(rootLists.get(1).getChild(1)), rootLists.get(1));
        assertEquals(client.getParentList(rootLists.get(1).getChild(2).getChild(3)), rootLists.get(1).getChild(2));

        // get list by id
        assertEquals(client.getListById(rootLists.get(1).getId()), rootLists.get(1));
        assertEquals(client.getListById("bogus_id"), null);
        assertEquals(client.getListById(rootLists.get(0).getChild(2).getId()), rootLists.get(0).getChild(2));

        // get ancestry path
        assertEquals(client.getAncestryPath(rootLists.get(0)), ImmutableList.of(rootLists.get(0)));
        assertEquals(client.getAncestryPath(rootLists.get(1).getChild(2).getChild(3)), ImmutableList.of(
                rootLists.get(1),
                rootLists.get(1).getChild(2),
                rootLists.get(1).getChild(2).getChild(3)
        ));
    }

}
