package com.here.navigation;

import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.mapview.MapMarker;
import com.here.sdk.mapview.MapPolyline;
import com.here.sdk.mapview.MapView;
import com.here.sdk.routing.Route;

public class DataHolder {

    static final String TAG = "SDK";

    public static final GeoCoordinates DEFAULT_MAP_CENTER = new GeoCoordinates(25.0612415, 121.5507537);
    public static final int DEFAULT_DISTANCE_IN_METERS = 1000 * 2;

    public static void setGeoShifted(boolean geoShifted) {
        DataHolder.geoShifted = geoShifted;
    }

    private static Route calculatedResultRoute;
    private static MapPolyline routeOnMapView;

    public void setRouteOnMapView(MapPolyline routeOnMapView) {
        routeOnMapView = routeOnMapView;
    }

    public static MapPolyline getRouteOnMapView() {
        return routeOnMapView;
    }

    public static Route getCalculatedResultRoute() {
        return calculatedResultRoute;
    }

    public static void setCalculatedResultRoute(Route calculatedResultRoute) {
        DataHolder.calculatedResultRoute = calculatedResultRoute;
    }

    static boolean geoShifted = false;
    static boolean satelliteMapOn = false;
    static boolean trafficMapOn = false;
    static boolean isDragged = false;
    static GeoCoordinates currentPositionGeoCoordinates;
    static MapMarker navigationPositionMapMarker;
    static MapMarker trackingPositionMapMarker;
    static NavigationExample navigationExample;
    static MapView mapView;

    static MainActivity mainActivity;

    public static void setMainActivity(MainActivity mainActivity) {
        DataHolder.mainActivity = mainActivity;
    }

    public static MainActivity getMainActivity() {
        return mainActivity;
    }

    public static boolean isGeoShifted() {
        return geoShifted;
    }

    public static MapView getMapView() {
        return mapView;
    }

    public static void setMapView(MapView mapView) {
        DataHolder.mapView = mapView;
    }

    public static void setNavigationExample(NavigationExample navigationExample) {
        DataHolder.navigationExample = navigationExample;
    }

    public static NavigationExample getNavigationExample() {
        return navigationExample;
    }

    public static GeoCoordinates getDefaultMapCenter() {
        return DEFAULT_MAP_CENTER;
    }

    public static void setNavigationPositionMapMarker(MapMarker navigationPositionMapMarker) {
        DataHolder.navigationPositionMapMarker = navigationPositionMapMarker;
    }

    public static void setTrackingPositionMapMarker(MapMarker trackingPositionMapMarker) {
        DataHolder.trackingPositionMapMarker = trackingPositionMapMarker;
    }

    public static MapMarker getNavigationPositionMapMarker() {
        return navigationPositionMapMarker;
    }

    public static MapMarker getTrackingPositionMapMarker() {
        return trackingPositionMapMarker;
    }

    public static void setCurrentPositionGeoCoordinates(GeoCoordinates currentPositionGeoCoordinates) {
        DataHolder.currentPositionGeoCoordinates = currentPositionGeoCoordinates;
    }

    public static GeoCoordinates getCurrentPositionGeoCoordinates() {
        return currentPositionGeoCoordinates;
    }

    public static void setIsDragged(boolean isDragged) {
        DataHolder.isDragged = isDragged;
    }

    public static boolean isIsDragged() {
        return isDragged;
    }

    public static boolean isSatelliteMapOn() {
        return satelliteMapOn;
    }

    public static boolean isTrafficMapOn() {
        return trafficMapOn;
    }

    public static void setSatelliteMapOn(boolean satelliteMapOn) {
        DataHolder.satelliteMapOn = satelliteMapOn;
    }

    public static void setTrafficMapOn(boolean trafficMapOn) {
        DataHolder.trafficMapOn = trafficMapOn;
    }


}
