package io.ghap.activity.bannermanagement.model;


public class Error {

    private int code;
    private String msg;

    public Error(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
