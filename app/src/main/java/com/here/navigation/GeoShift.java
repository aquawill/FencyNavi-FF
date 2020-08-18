package com.here.navigation;

import com.here.sdk.core.GeoCoordinates;

class GeoShift {
    static GeoCoordinates getShiftedGeoLocation(GeoCoordinates geoCoordinates, double distanceInMeters, double bearing) {
        double bearingRadians = Math.toRadians(bearing);
        double latitudeRadians = Math.toRadians(geoCoordinates.latitude);
        double longitudeRadians = Math.toRadians(geoCoordinates.longitude);
        int earthRadiusInMetres = 6371000;
        double distanceRadio = distanceInMeters / earthRadiusInMetres;
        double latitudeResult = Math.asin(Math.sin(latitudeRadians) * Math.cos(distanceRadio) + Math.cos(latitudeRadians) * Math.sin(distanceRadio) * Math.cos(bearingRadians));
        double a = Math.atan2(Math.sin(bearingRadians) * Math.sin(distanceRadio) * Math.cos(latitudeRadians), Math.cos(distanceRadio) - Math.sin(latitudeRadians) * Math.sin(latitudeResult));
        double longitudeResult = (longitudeRadians + a + 3 * Math.PI) % (2 * Math.PI) - Math.PI;
        DataHolder.setGeoShifted(true);
        return new GeoCoordinates(Math.toDegrees(latitudeResult), Math.toDegrees(longitudeResult));
    }
}
