package com.here.navigation;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Log;

import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.routing.CalculateRouteCallback;
import com.here.sdk.routing.CarOptions;
import com.here.sdk.routing.PedestrianOptions;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.RoutingError;
import com.here.sdk.routing.TruckOptions;
import com.here.sdk.routing.Waypoint;

import java.util.List;

import static com.here.navigation.DataHolder.TAG;

public class Router {

    interface OnRouteCalculationFinishedListener {
        void onFinished();
    }

    Route route;
    OnRouteCalculationFinishedListener onRouteCalculationFinishedListener;

    public void setOnRouteCalculationFinishedListener(OnRouteCalculationFinishedListener onRouteCalculationFinishedListener) {
        this.onRouteCalculationFinishedListener = onRouteCalculationFinishedListener;
    }

    public Route getRoute() {
        return route;
    }

    public void calculateRoute(List<Waypoint> waypointList, VehicleType vehicleType, Object vehicleOption) {
        Log.d(TAG, "start route calculation for " + vehicleType.name());
        if (waypointList.size() == 1) {
            Log.d(TAG, "adding waypoint of " + DataHolder.getCurrentPositionGeoCoordinates().toString());
            waypointList.add(0, new Waypoint(DataHolder.getCurrentPositionGeoCoordinates()));
        }

        switch (vehicleType) {
            case CAR:
                try {
                    RoutingEngine routingEngine = new RoutingEngine();
                    routingEngine.calculateRoute(
                            waypointList,
                            (CarOptions) vehicleOption,
                            new CalculateRouteCallback() {
                                @Override
                                public void onRouteCalculated(@Nullable RoutingError routingError, @Nullable List<Route> routes) {
                                    if (routingError == null) {
                                        route = routes.get(0);
                                        Log.d(TAG, "routes.size(): " + routes.size());
                                        onRouteCalculationFinishedListener.onFinished();
                                    } else {
                                        route = null;
                                        Snackbar.make(DataHolder.getMapView(), "Error: " + routingError.toString(), Snackbar.LENGTH_SHORT).show();
                                    }
                                }
                            });
                } catch (InstantiationErrorException e) {
                    e.printStackTrace();
                }
                break;
            case TRUCK:
                try {
                    RoutingEngine routingEngine = new RoutingEngine();
                    routingEngine.calculateRoute(
                            waypointList,
                            (TruckOptions) vehicleOption,
                            new CalculateRouteCallback() {
                                @Override
                                public void onRouteCalculated(@Nullable RoutingError routingError, @Nullable List<Route> routes) {
                                    if (routingError == null) {
                                        Log.d(TAG, "routes.size(): " + routes.size());
                                        route = routes.get(0);
                                        onRouteCalculationFinishedListener.onFinished();
                                    } else {
                                        route = null;
                                        Snackbar.make(DataHolder.getMapView(), "Error: " + routingError.toString(), Snackbar.LENGTH_SHORT).show();
                                    }
                                }
                            });
                } catch (InstantiationErrorException e) {
                    e.printStackTrace();
                }
                break;
            case BICYCLE:
                break;
            case SCOOTER:
                break;
            case PEDESTRIAN:
                try {
                    RoutingEngine routingEngine = new RoutingEngine();
                    routingEngine.calculateRoute(
                            waypointList,
                            (PedestrianOptions) vehicleOption,
                            new CalculateRouteCallback() {
                                @Override
                                public void onRouteCalculated(@Nullable RoutingError routingError, @Nullable List<Route> routes) {
                                    if (routingError == null) {
                                        Log.d(TAG, "routes.size(): " + routes.size());
                                        route = routes.get(0);
                                        onRouteCalculationFinishedListener.onFinished();
                                    } else {
                                        route = null;
                                        Snackbar.make(DataHolder.getMapView(), "Error: " + routingError.toString(), Snackbar.LENGTH_SHORT).show();
                                    }
                                }
                            });
                } catch (InstantiationErrorException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
