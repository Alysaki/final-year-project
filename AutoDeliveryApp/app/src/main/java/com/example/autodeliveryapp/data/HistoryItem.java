package com.example.autodeliveryapp.data;

public class HistoryItem { // "Done" | "On Mission" | "Canceled"
    private String orderId;
    private String pickup;
    private String dropoff;
    private String status;
    private String date;
    private String price;

    public HistoryItem(String orderId, String pickup, String dropoff,
                       String status, String date, String price) {
        this.orderId = orderId;
        this.pickup  = pickup;
        this.dropoff = dropoff;
        this.status  = status;
        this.date    = date;
        this.price   = price;
    }

    public String getOrderId()  { return orderId; }
    public String getPickup()   { return pickup; }
    public String getDropoff()  { return dropoff; }
    public String getStatus()   { return status; }
    public String getDate()     { return date; }
    public String getPrice()    { return price; }
}