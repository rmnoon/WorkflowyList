package com.rmnoon.workflowy.client;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Created by rmnoon on 5/11/16.
 */
public class Utils {

    public static Map<String, String> parseCookie(String cookieStr) {
        if (cookieStr == null) return Collections.emptyMap();
        Map<String, String> result = Maps.newLinkedHashMap();
        for (String part : Splitter.on(";").trimResults().omitEmptyStrings().split(cookieStr)) {
            String[] parts = part.split("=");
            if (parts.length == 2) {
                result.put(parts[0], parts[1]);
            } else {
                result.put(parts[0], "");
            }
        }
        return result;
    }

    /**
     * Get the list (from the given collection of lists) who is the direct parent of the
     * supplied list.
     * @param lists
     * @param child
     * @return
     */
    public static WFList getParentList(Collection<WFList> lists, WFList child) {
        if (lists == null || child == null) return null;
        for (WFList possibleParent : lists) {
            // this possible parent is the actual parent
            if (hasListWithId(possibleParent.ch, child)) return possibleParent;

            // recurse to children
            WFList parent = getParentList(possibleParent.ch, child);
            if (parent != null) return parent;
        }
        return null;
    }

    /**
     * Returns true if the given list is in the supplied collection (we need this because the POJO
     * should have every field but the ID is fine for a lot of cases.  Returns false if either arg
     * is null.
     *
     * @param lists
     * @param list
     * @return
     */
    public static boolean hasListWithId(Collection<WFList> lists, WFList list) {
        return getIndexOfListWithId(lists, list) != -1;
    }

    /**
     * Returns the index in the given collection of lists of the list whose id matches the supplied
     * list.
     * @param lists
     * @param list
     * @return
     */
    public static int getIndexOfListWithId(Collection<WFList> lists, WFList list) {
        if (list == null || lists == null) return -1;
        int i = 0;
        for (WFList item : lists) {
            if (item.id == list.id) return i;
            i++;
        }
        return -1;
    }

    /**
     * Return a new set of Workflowy lists that are exactly equal to the provided canonical set
     * but that reuse existing object references from the other provided set.
     * @param reuseLists
     * @param canonicalLists
     * @return
     */
    public static List<WFList> getReusedLists(List<WFList> reuseLists, List<WFList> canonicalLists) {
        if (reuseLists == null) reuseLists = Collections.emptyList();

        final Map<String, WFList> fromMap = Maps.newHashMap();
        bfsLists(reuseLists, new Function<WFList, Boolean>() {
            @Override
            public Boolean apply(WFList input) {
                fromMap.put(input.getId(), input);
                return null;
            }
        });

        List<WFList> result = getReusedLists(fromMap, canonicalLists);
        Preconditions.checkState(result.equals(canonicalLists));
        return result;
    }

    private static List<WFList> getReusedLists(Map<String, WFList> reuseMap, List<WFList> canonicalList) {
        if (canonicalList == null) return null;

        List<WFList> result = Lists.newArrayListWithCapacity(canonicalList.size());

        for (WFList canonical : canonicalList) {
            WFList newCanonical;
            if (reuseMap.containsKey(canonical.id)) {
                WFList toReuse = reuseMap.get(canonical.id);
                toReuse.copyIntoFrom(canonical);
                newCanonical = toReuse;
            } else {
                newCanonical = canonical;
            }
            result.add(newCanonical);
            newCanonical.ch = getReusedLists(reuseMap, newCanonical.ch);
        }

        return result;
    }

    /**
     * Do a breadth-first search over the given collection of lists, applying the supplied function
     * to each item in the list.  If the function returns an explicit true (not null or false)
     * the search will abort.
     *
     * @param toTraverse the lists to traverse (breadth-first)
     * @param toApply the function to apply (if it returns true the search will abort)
     * @return the number of lists visited (inclusive)
     */
    public static int bfsLists(Collection<WFList> toTraverse, Function<WFList, Boolean> toApply) {
        if (toTraverse == null) return 0;

        int numVisited = 0;
        Queue<WFList> visitQueue = Queues.newArrayDeque(toTraverse);
        while (!visitQueue.isEmpty()) {
            numVisited++;
            WFList cur = visitQueue.remove();
            Boolean abort = toApply.apply(cur);
            if (abort != null && abort.equals(true)) {
                break;
            }
            if (cur.getChildren() != null) {
                visitQueue.addAll(cur.getChildren());
            }
        }
        return numVisited;
    }

}
