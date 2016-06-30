package com.rmnoon.workflowy.client;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rmnoon on 5/11/16.
 */
public class PushPoll {

    public static Operation buildCreateOp(String newId, String parentId, int insertIndex, long time) {
        return new PushPoll.Operation()
                .setType("create")
                .setData(new PushPoll.OperationData()
                        .setProjectid(newId)
                        .setParentid(parentId)
                        .setPriority(insertIndex)
                )
                .setClient_timestamp(time)
                .setUndo_data(new PushPoll.OperationUndoData());
    }

    public static Operation buildEditOp(String listId, String name, String description, Long lastModified, long time) {
        if (name == null && description == null) return null;

        PushPoll.OperationData opData = new PushPoll.OperationData()
                .setProjectid(listId);
        if (name != null) opData.setName(name);
        if (description != null) opData.setDescription(description);

        PushPoll.OperationUndoData opUndoData = new PushPoll.OperationUndoData()
                .setPrevious_last_modified(lastModified)
                .setPrevious_last_modified_by(null);
        //if (name != null) opUndoData.setPrevious_name("");
        //if (description != null) opUndoData.setPrevious_description("");

        return new PushPoll.Operation()
                .setType("edit")
                .setData(opData)
                .setClient_timestamp(time)
                .setUndo_data(opUndoData);
    }

    public static Operation buildCompleteOp(String listId, boolean isComplete, Long listCp, Long lastModified, long time) {
        PushPoll.OperationData opData = new PushPoll.OperationData()
                .setProjectid(listId);

        PushPoll.OperationUndoData opUndoData = new PushPoll.OperationUndoData()
                .setPrevious_last_modified(lastModified)
                .setPrevious_last_modified_by(null)
                .setPrevious_completed(isComplete ? false : listCp);

        return new PushPoll.Operation()
                .setType(isComplete ? "complete" : "uncomplete")
                .setData(opData)
                .setClient_timestamp(time)
                .setUndo_data(opUndoData);
    }

    public static Operation buildDeleteOp(String listId, long lastModified, long time) {
        PushPoll.OperationData opData = new PushPoll.OperationData()
                .setProjectid(listId);

        PushPoll.OperationUndoData opUndoData = new PushPoll.OperationUndoData()
                .setPrevious_last_modified(lastModified)
                .setPrevious_last_modified_by(null);

        // TODO: parent id

        return new PushPoll.Operation()
                .setType("delete")
                .setData(opData)
                .setClient_timestamp(time)
                .setUndo_data(opUndoData);
    }

    public static class Data extends ArrayList<Transaction> {}

    public static class Transaction {
        public String most_recent_operation_transaction_id;
        public List<Operation> operations;
    }

    public static class Operation {
        public String type;
        public long client_timestamp;
        public OperationData data;
        public OperationUndoData undo_data;

        public Operation setType(String type) {
            this.type = type;
            return this;
        }

        public Operation setClient_timestamp(long client_timestamp) {
            this.client_timestamp = client_timestamp;
            return this;
        }

        public Operation setData(OperationData data) {
            this.data = data;
            return this;
        }

        public Operation setUndo_data(OperationUndoData undo_data) {
            this.undo_data = undo_data;
            return this;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("type", type)
                    .add("client_timestamp", client_timestamp)
                    .add("data", data)
                    .add("undo_data", undo_data)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Operation operation = (Operation) o;
            return client_timestamp == operation.client_timestamp &&
                    Objects.equal(type, operation.type) &&
                    Objects.equal(data, operation.data) &&
                    Objects.equal(undo_data, operation.undo_data);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(type, client_timestamp, data, undo_data);
        }
    }

    public static class OperationData {
        public String projectid;
        public String parentid;
        public Integer priority;
        public String name;
        public String description;

        public OperationData setProjectid(String projectid) {
            this.projectid = projectid;
            return this;
        }

        public OperationData setParentid(String parentid) {
            this.parentid = parentid;
            return this;
        }

        public OperationData setPriority(Integer priority) {
            this.priority = priority;
            return this;
        }

        public OperationData setName(String name) {
            this.name = name;
            return this;
        }

        public OperationData setDescription(String description) {
            this.description = description;
            return this;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("projectid", projectid)
                    .add("parentid", parentid)
                    .add("priority", priority)
                    .add("name", name)
                    .add("description", description)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OperationData that = (OperationData) o;
            return Objects.equal(projectid, that.projectid) &&
                    Objects.equal(parentid, that.parentid) &&
                    Objects.equal(priority, that.priority) &&
                    Objects.equal(name, that.name) &&
                    Objects.equal(description, that.description);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(projectid, parentid, priority, name, description);
        }
    }

    public static class OperationUndoData {
        public Long previous_last_modified;
        public String previous_last_modified_by;
        public String previous_name;
        public String previous_description;
        public Object previous_completed; // long or boolean (depending on completing or uncompleting)
        public String parentid;
        public Integer priority;

        public OperationUndoData setPrevious_last_modified(Long previous_last_modified) {
            this.previous_last_modified = previous_last_modified;
            return this;
        }

        public OperationUndoData setPrevious_last_modified_by(String previous_last_modified_by) {
            this.previous_last_modified_by = previous_last_modified_by;
            return this;
        }

        public OperationUndoData setPrevious_name(String previous_name) {
            this.previous_name = previous_name;
            return this;
        }

        public OperationUndoData setParentid(String parentid) {
            this.parentid = parentid;
            return this;
        }

        public OperationUndoData setPriority(Integer priority) {
            this.priority = priority;
            return this;
        }

        public OperationUndoData setPrevious_description(String previous_description) {
            this.previous_description = previous_description;
            return this;
        }

        public OperationUndoData setPrevious_completed(Object previous_completed) {
            this.previous_completed = previous_completed;
            return this;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("previous_last_modified", previous_last_modified)
                    .add("previous_last_modified_by", previous_last_modified_by)
                    .add("previous_name", previous_name)
                    .add("previous_description", previous_description)
                    .add("previous_completed", previous_completed)
                    .add("parentid", parentid)
                    .add("priority", priority)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OperationUndoData that = (OperationUndoData) o;
            return Objects.equal(previous_last_modified, that.previous_last_modified) &&
                    Objects.equal(previous_last_modified_by, that.previous_last_modified_by) &&
                    Objects.equal(previous_name, that.previous_name) &&
                    Objects.equal(previous_description, that.previous_description) &&
                    Objects.equal(previous_completed, that.previous_completed) &&
                    Objects.equal(parentid, that.parentid) &&
                    Objects.equal(priority, that.priority);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(previous_last_modified, previous_last_modified_by, previous_name, previous_description, previous_completed, parentid, priority);
        }
    }

    public static class Response {
        public List<ResponseResult> results;
    }

    public static class ResponseResult {
        public Integer monthly_item_quota;
        public Integer items_created_in_current_month;
        public Long new_polling_interval_in_ms;
        public List<Object> concurrent_remote_operation_transactions;
        public Boolean error_encountered_in_remote_operations;
        public String new_most_recent_operation_transaction_id;
        public String server_run_operation_transaction_json;
    }
}
