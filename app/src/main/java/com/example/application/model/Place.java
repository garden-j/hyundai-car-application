package com.example.application.model;

/** 이름이 있는 장소(목적지·자주 가는 곳). */
public class Place {
    public final String name;
    public final String address;
    public final GeoPoint point;

    public Place(String name, String address, GeoPoint point) {
        this.name = name;
        this.address = address;
        this.point = point;
    }
}
