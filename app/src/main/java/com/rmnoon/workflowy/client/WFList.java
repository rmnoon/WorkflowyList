package com.rmnoon.workflowy.client;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.util.List;
import java.util.Objects;

/**
 * Created by rmnoon on 5/11/16.
 */
public class WFList {
    String id; // uuid
    String nm; // name (text)
    String no; // note (description)
    Long cp; // complete (non-null / present if complete),
    long lm; // last modified (in terms of seconds since account creation)
    List<WFList> ch; // children

    public String getId() {
        return id;
    }

    public String getName() {
        return nm;
    }

    public String getDescription() {
        return no;
    }

    public boolean isComplete() {
        return cp != null;
    }

    public List<WFList> getChildren() {
        return ch;
    }

    public WFList getChild(int idx) {
        if (ch == null) return null;
        if (idx < 0 || idx >= ch.size()) return null;
        return ch.get(idx);
    }

    void copyIntoFrom(WFList other) {
        Preconditions.checkArgument(id.equals(other.id));
        this.nm = other.nm;
        this.no = other.no;
        this.cp = other.cp;
        this.lm = other.lm;
        this.ch = other.ch;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("nm", nm)
                .add("no", no)
                .add("cp", cp)
                .add("lm", lm)
                .add("ch", ch)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WFList wfList = (WFList) o;
        return lm == wfList.lm &&
                Objects.equals(id, wfList.id) &&
                Objects.equals(nm, wfList.nm) &&
                Objects.equals(no, wfList.no) &&
                Objects.equals(cp, wfList.cp) &&
                Objects.equals(ch, wfList.ch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nm, no, cp, lm, ch);
    }
}
