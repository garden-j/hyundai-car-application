package com.example.application.model;

import java.util.Locale;

/** 위도·경도 좌표. */
public class GeoPoint {
    public final double latitude;
    public final double longitude;

    public GeoPoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** 두 지점 사이 직선 거리(km, 하버사인). */
    public double distanceKmTo(GeoPoint other) {
        double r = 6371.0;
        double dLat = Math.toRadians(other.latitude - latitude);
        double dLon = Math.toRadians(other.longitude - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    /** 북쪽으로 northKm, 동쪽으로 eastKm 이동한 지점. */
    public GeoPoint offsetKm(double northKm, double eastKm) {
        double dLat = northKm / 111.0;
        double dLon = eastKm / (111.0 * Math.cos(Math.toRadians(latitude)));
        return new GeoPoint(latitude + dLat, longitude + dLon);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%.5f,%.5f", latitude, longitude);
    }
}
