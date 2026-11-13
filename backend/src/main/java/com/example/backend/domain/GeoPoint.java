package com.example.backend.domain;

/** 위도·경도. 앱의 GeoPoint와 같은 필드 이름(latitude, longitude)을 쓴다. */
public record GeoPoint(double latitude, double longitude) {

    public double distanceKmTo(GeoPoint o) {
        double r = 6371.0;
        double dLat = Math.toRadians(o.latitude - latitude);
        double dLon = Math.toRadians(o.longitude - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(o.latitude))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    public GeoPoint offsetKm(double northKm, double eastKm) {
        return new GeoPoint(latitude + northKm / 111.0,
                longitude + eastKm / (111.0 * Math.cos(Math.toRadians(latitude))));
    }

    /** 카카오 API 형식 "경도,위도". */
    public String toKakao() {
        return longitude + "," + latitude;
    }
}
