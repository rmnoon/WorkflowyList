package com.rmnoon.workflowy.client;

/**
 * Created by rmnoon on 5/10/16.
 */
public class BadLoginException extends Exception {
    public BadLoginException() {
        super("Invalid login credentials!");
    }
}
