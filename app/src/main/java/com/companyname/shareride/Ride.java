package com.companyname.shareride;

public class Ride {
    private String route;
    private String fare;
    private String passengers;
    private String time;
    private String names;

    public Ride(String route, String fare, String passengers, String time, String names) {
        this.route = route;
        this.fare = fare;
        this.passengers = passengers;
        this.time = time;
        this.names = names;
    }

    // Getters
    public String getRoute() { return route; }
    public String getFare() { return fare; }
    public String getPassengers() { return passengers; }
    public String getTime() { return time; }
    public String getNames() { return names; }
}