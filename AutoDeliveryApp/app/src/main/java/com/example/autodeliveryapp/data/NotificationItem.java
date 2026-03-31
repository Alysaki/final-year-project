package com.example.autodeliveryapp.data;

public class NotificationItem {
    private String icon;
    private String title;
    private String message;
    private String time;

    public NotificationItem(String icon, String title, String message, String time) {
        this.icon    = icon;
        this.title   = title;
        this.message = message;
        this.time    = time;
    }

    public String getIcon()    { return icon; }
    public String getTitle()   { return title; }
    public String getMessage() { return message; }
    public String getTime()    { return time; }
}