/*
 * Copyright (C) 2019-2020 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.here.navigation;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.here.sdk.core.Anchor2D;
import com.here.sdk.core.Angle;
import com.here.sdk.core.Color;
import com.here.sdk.core.GeoBox;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.LanguageCode;
import com.here.sdk.core.Metadata;
import com.here.sdk.core.Point2D;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.gestures.GestureState;
import com.here.sdk.gestures.PanListener;
import com.here.sdk.gestures.PinchRotateListener;
import com.here.sdk.gestures.TapListener;
import com.here.sdk.gestures.TwoFingerPanListener;
import com.here.sdk.mapview.MapCamera;
import com.here.sdk.mapview.MapCameraObserver;
import com.here.sdk.mapview.MapError;
import com.here.sdk.mapview.MapImage;
import com.here.sdk.mapview.MapImageFactory;
import com.here.sdk.mapview.MapMarker;
import com.here.sdk.mapview.MapPolyline;
import com.here.sdk.mapview.MapScene;
import com.here.sdk.mapview.MapScheme;
import com.here.sdk.mapview.MapView;
import com.here.sdk.mapview.MapViewBase;
import com.here.sdk.mapview.PickMapItemsResult;
import com.here.sdk.routing.CarOptions;
import com.here.sdk.routing.PedestrianOptions;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.TruckOptions;
import com.here.sdk.routing.Waypoint;
import com.here.sdk.search.Place;
import com.here.sdk.search.SearchCallback;
import com.here.sdk.search.SearchEngine;
import com.here.sdk.search.SearchError;
import com.here.sdk.search.SearchOptions;
import com.here.sdk.search.TextQuery;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    String TAG = DataHolder.TAG;

    private PermissionsRequestor permissionsRequestor;
    private MapView mapView;
    private Snackbar searchResultSnackbar;
    private SearchEngine searchEngine;
    private MapMarker searchResultMapMarker;
    private MapMarker selectedMapMarker;
    private List<MapMarker> searchResultMapMarkerList = new ArrayList<>();
    private List<MapMarker> waypointMapMarkerList = new ArrayList<>();
    private List<Waypoint> wayPointList = new ArrayList<>();
    private List<MapMarker> selectedMapMarkerList = new ArrayList<>();
    private Button navigationControlButton;
    private Button resetMapButton;
    private MapPolyline routeOnMapView;
    private double selectedFeatureBoundingBoxMinLat = 999;
    private double selectedFeatureBoundingBoxMinLng = 999;
    private double selectedFeatureBoundingBoxMaxLat = 999;
    private double selectedFeatureBoundingBoxMaxLng = 999;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        // Get a MapView instance from layout.
        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        DataHolder.setMainActivity(this);
        DataHolder.setNavigationExample(new NavigationExample(this, mapView));
        DataHolder.getNavigationExample().startTracking();

        handleAndroidPermissions();
    }

    private void handleAndroidPermissions() {
        permissionsRequestor = new PermissionsRequestor(this);
        permissionsRequestor.request(new PermissionsRequestor.ResultListener() {

            @Override
            public void permissionsGranted() {
                loadMapScene();
            }

            @Override
            public void permissionsDenied() {
                Log.e(TAG, "Permissions denied by user.");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsRequestor.onRequestPermissionsResult(requestCode, grantResults);
    }

    void satelliteMapSwitch(boolean on) {
        if (on) {
            mapView.getMapScene().loadScene(MapScheme.HYBRID_DAY, new MapScene.LoadSceneCallback() {
                @Override
                public void onLoadScene(MapError mapError) {

                }
            });
        } else {
            mapView.getMapScene().loadScene(MapScheme.NORMAL_DAY, new MapScene.LoadSceneCallback() {
                @Override
                public void onLoadScene(MapError mapError) {

                }
            });
        }
    }

    void trafficMapSwitch(boolean on) {
        if (on) {
            mapView.getMapScene().setLayerState(MapScene.Layers.TRAFFIC_FLOW, MapScene.LayerState.VISIBLE);
            mapView.getMapScene().setLayerState(MapScene.Layers.TRAFFIC_INCIDENTS, MapScene.LayerState.VISIBLE);
        } else {
            mapView.getMapScene().setLayerState(MapScene.Layers.TRAFFIC_FLOW, MapScene.LayerState.HIDDEN);
            mapView.getMapScene().setLayerState(MapScene.Layers.TRAFFIC_INCIDENTS, MapScene.LayerState.HIDDEN);
        }
    }

    void zoomMapByButton(boolean zoomIn) {
        double currentDistance = mapView.getCamera().getState().distanceToTargetInMeters;
        Log.d(TAG, "currentDistance: " + currentDistance);
        if (zoomIn) {
            mapView.getCamera().setDistanceToTarget(currentDistance * 0.8 + 1);
        } else {
            mapView.getCamera().setDistanceToTarget(currentDistance * 1.125 + 1);
        }
    }

    private void showSearchResult(GeoCoordinates geoCoordinates, String s1, String s2, String s3) {
        MapImage mapImage = MapImageFactory.fromResource(getResources(), R.drawable.ic_map_pin_red);
        searchResultMapMarker = new MapMarker(geoCoordinates, mapImage, new Anchor2D(0.5F, 1));
        Metadata selectedFeaturemetadata = new Metadata();
        selectedFeaturemetadata.setString("primary", s1);
        selectedFeaturemetadata.setString("secondary", s2);
        selectedFeaturemetadata.setString("description", s3);
        searchResultMapMarker.setMetadata(selectedFeaturemetadata);
        searchResultMapMarkerList.add(searchResultMapMarker);
        mapView.getMapScene().addMapMarker(searchResultMapMarker);
    }

    private void loadMapScene() {

        mapView.getMapScene().loadScene(MapScheme.NORMAL_DAY, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(@Nullable MapError mapError) {
                if (mapError == null) {
                    DataHolder.setMapView(mapView);
                    mapView.getCamera().lookAt(DataHolder.getDefaultMapCenter(), 1000);
                    DataHolder.setCurrentPositionGeoCoordinates(DataHolder.getDefaultMapCenter());

                    mapView.getGestures().setPanListener(new PanListener() {
                        @Override
                        public void onPan(GestureState gestureState, Point2D point2D, Point2D point2D1, double v) {
                            DataHolder.setIsDragged(true);
                        }
                    });

                    mapView.getGestures().setTwoFingerPanListener(new TwoFingerPanListener() {
                        @Override
                        public void onTwoFingerPan(GestureState gestureState, Point2D point2D, Point2D point2D1, double v) {
                            DataHolder.setIsDragged(true);
                        }
                    });

                    mapView.getGestures().setPinchRotateListener(new PinchRotateListener() {
                        @Override
                        public void onPinchRotate(GestureState gestureState, Point2D point2D, Point2D point2D1, double v, Angle angle) {
                            DataHolder.setIsDragged(true);
                        }
                    });

                    mapView.getGestures().setTapListener(new TapListener() {
                        @Override
                        public void onTap(Point2D point2D) {
                            findViewById(R.id.search_input_text).clearFocus();
                            findViewById(R.id.search_bar_linear_layout).setVisibility(View.GONE);
                            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputMethodManager.hideSoftInputFromWindow(findViewById(R.id.search_input_text).getWindowToken(), 0);
                            if (selectedMapMarker != null) {
                                mapView.getMapScene().removeMapMarker(selectedMapMarker);
                                selectedMapMarker = null;
                            }
                            mapView.pickMapItems(point2D, 30, new MapViewBase.PickMapItemsCallback() {
                                @Override
                                public void onPickMapItems(PickMapItemsResult pickMapItemsResult) {
                                    if (pickMapItemsResult.getMarkers().size() > 0) {
                                        MapMarker pickedMapMarker = pickMapItemsResult.getMarkers().get(0);
                                        if (searchResultMapMarkerList.contains(pickedMapMarker)) {
                                            if (selectedMapMarker == null) {
                                                MapImage mapImage = MapImageFactory.fromResource(getResources(), R.drawable.ic_map_pin_yellow);
                                                selectedMapMarker = new MapMarker(pickedMapMarker.getCoordinates(), mapImage, new Anchor2D(0.5F, 1));
                                                selectedMapMarker.setDrawOrder(1000);
                                                mapView.getMapScene().addMapMarker(selectedMapMarker);
                                            }
                                            if (pickedMapMarker.getMetadata() != null) {
                                                String primaryText = pickedMapMarker.getMetadata().getString("primary");
                                                String secondaryText = pickedMapMarker.getMetadata().getString("secondary");
//                                String description = pickedMapMarker.getMetadata().getString("description");
                                                showResultSnackbar(pickedMapMarker.getCoordinates(), primaryText + "\n" + secondaryText, mapView, Snackbar.LENGTH_INDEFINITE);
                                            }
                                        } else {
                                            if (searchResultSnackbar != null) {
                                                searchResultSnackbar.dismiss();
                                            }
                                        }
                                    } else {
                                        if (searchResultSnackbar != null) {
                                            searchResultSnackbar.dismiss();
                                        }
                                    }
                                }
                            });
                        }
                    });

                    findViewById(R.id.zoom_in).setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            zoomMapByButton(true);
                            return false;
                        }
                    });

                    findViewById(R.id.zoom_out).setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            zoomMapByButton(false);
                            return false;
                        }
                    });

                    findViewById(R.id.sat_map_button).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.d(TAG, "DataHolder.isSatelliteMapOn():" + DataHolder.isSatelliteMapOn());
                            if (!DataHolder.isSatelliteMapOn()) {
                                satelliteMapSwitch(true);
                                findViewById(R.id.sat_map_button).setBackgroundResource(R.drawable.round_button_on);
                                DataHolder.setSatelliteMapOn(true);
                                trafficMapSwitch(false);
                                findViewById(R.id.traffic_button).setBackgroundResource(R.drawable.round_button_off);
                                DataHolder.setTrafficMapOn(false);
                            } else {
                                satelliteMapSwitch(false);
                                findViewById(R.id.sat_map_button).setBackgroundResource(R.drawable.round_button_off);
                                DataHolder.setSatelliteMapOn(false);
                            }
                        }
                    });

                    findViewById(R.id.traffic_button).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.d(TAG, "DataHolder.isTrafficMapOn():" + DataHolder.isTrafficMapOn());
                            if (!DataHolder.isTrafficMapOn()) {
                                trafficMapSwitch(true);
                                findViewById(R.id.traffic_button).setBackgroundResource(R.drawable.round_button_on);
                                DataHolder.setTrafficMapOn(true);
                                satelliteMapSwitch(false);
                                findViewById(R.id.sat_map_button).setBackgroundResource(R.drawable.round_button_off);
                                DataHolder.setSatelliteMapOn(false);
                            } else {
                                trafficMapSwitch(false);
                                findViewById(R.id.traffic_button).setBackgroundResource(R.drawable.round_button_off);
                                DataHolder.setTrafficMapOn(false);
                            }
                        }
                    });

                    Button northUpButton = findViewById(R.id.north_up);
                    northUpButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            DataHolder.setIsDragged(false);
                            MapCamera.OrientationUpdate orientation = new MapCamera.OrientationUpdate();
                            orientation.bearing = 0.0;
                            orientation.tilt = 45.0;
                            mapView.getCamera().lookAt(GeoShift.getShiftedGeoLocation(DataHolder.getCurrentPositionGeoCoordinates(), mapView.getCamera().getState().distanceToTargetInMeters / 4, orientation.bearing), orientation, mapView.getCamera().getState().distanceToTargetInMeters);
                            DataHolder.getNavigationPositionMapMarker().setCoordinates(DataHolder.getCurrentPositionGeoCoordinates());
                            DataHolder.getTrackingPositionMapMarker().setCoordinates(DataHolder.getCurrentPositionGeoCoordinates());
                        }
                    });

                    mapView.getCamera().addObserver(new MapCameraObserver() {
                        @Override
                        public void onCameraUpdated(MapCamera.State state) {
                            northUpButton.setRotation((float) -state.targetOrientation.bearing);
                        }
                    });

                    Button searchButton = findViewById(R.id.search_button);
                    EditText searchTextBar = findViewById(R.id.search_input_text);
                    LinearLayout searchBarLinearLayout = findViewById(R.id.search_bar_linear_layout);

                    searchButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (searchResultSnackbar != null) {
                                searchResultSnackbar.dismiss();
                            }
                            searchTextBar.setText("");
                            searchBarLinearLayout.setVisibility(View.VISIBLE);
                            searchTextBar.requestFocus();
                            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputMethodManager.showSoftInput(searchTextBar, InputMethodManager.SHOW_IMPLICIT);
                        }
                    });

                    searchTextBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                                searchTextBar.clearFocus();
                                searchBarLinearLayout.setVisibility(View.GONE);
                                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                inputMethodManager.hideSoftInputFromWindow(searchTextBar.getWindowToken(), 0);
                                if (searchResultMapMarkerList.size() > 0) {
                                    for (MapMarker m : searchResultMapMarkerList) {
                                        mapView.getMapScene().removeMapMarker(m);
                                    }
                                }
                                selectedFeatureBoundingBoxMinLat = 999;
                                selectedFeatureBoundingBoxMinLng = 999;
                                selectedFeatureBoundingBoxMaxLat = 999;
                                selectedFeatureBoundingBoxMaxLng = 999;
                                String inputString = searchTextBar.getText().toString();
                                if (inputString.matches("^(-?\\d+(\\.\\d+)?),\\s*(-?\\d+(\\.\\d+)?)$")) {
                                    double inputLatitude = Double.parseDouble(inputString.split(",")[0]);
                                    double inputLongitude = Double.parseDouble(inputString.split(",")[1]);
                                    GeoCoordinates inputGeoCoordinates = new GeoCoordinates(inputLatitude, inputLongitude);
                                    mapView.getCamera().lookAt(inputGeoCoordinates);
                                    DataHolder.setIsDragged(true);

                                    try {
                                        searchEngine = new SearchEngine();
                                    } catch (InstantiationErrorException e) {
                                        e.printStackTrace();
                                    }
                                    int maxItems = 1;
                                    SearchOptions reverseGeocodingOptions = new SearchOptions(LanguageCode.ZH_TW, maxItems);
                                    searchEngine.search(inputGeoCoordinates, reverseGeocodingOptions, new SearchCallback() {
                                        @Override
                                        public void onSearchCompleted(@Nullable SearchError searchError, @Nullable List<Place> list) {
                                            if (searchError == null) {
                                                showSearchResult(inputGeoCoordinates, list.get(0).getTitle(), list.get(0).getAddress().addressText, "");
                                            }
                                        }
                                    });
                                } else {
                                    try {
                                        searchEngine = new SearchEngine();
                                    } catch (InstantiationErrorException e) {
                                        e.printStackTrace();
                                    }
                                    int maxItems = 30;
                                    SearchOptions searchOptions = new SearchOptions(LanguageCode.ZH_TW, maxItems);
                                    TextQuery query = new TextQuery(inputString, mapView.getCamera().getState().targetCoordinates);
                                    searchEngine.search(query, searchOptions, new SearchCallback() {
                                        @Override
                                        public void onSearchCompleted(@Nullable SearchError searchError, @Nullable List<Place> list) {
                                            if (searchError == null) {
                                                for (Place place : list) {
                                                    String placeTitle = place.getTitle();
                                                    double placeLat = place.getGeoCoordinates().latitude;
                                                    double placeLng = place.getGeoCoordinates().longitude;
                                                    if (selectedFeatureBoundingBoxMaxLat == 999 || placeLat > selectedFeatureBoundingBoxMaxLat) {
                                                        selectedFeatureBoundingBoxMaxLat = placeLat;
                                                    }
                                                    if (selectedFeatureBoundingBoxMaxLng == 999 || placeLng > selectedFeatureBoundingBoxMaxLng) {
                                                        selectedFeatureBoundingBoxMaxLng = placeLng;
                                                    }
                                                    if (selectedFeatureBoundingBoxMinLat == 999 || placeLat < selectedFeatureBoundingBoxMinLat) {
                                                        selectedFeatureBoundingBoxMinLat = placeLat;
                                                    }
                                                    if (selectedFeatureBoundingBoxMinLng == 999 || placeLng < selectedFeatureBoundingBoxMinLng) {
                                                        selectedFeatureBoundingBoxMinLng = placeLng;
                                                    }
                                                    GeoCoordinates placeGeocoordinates = place.getGeoCoordinates();
                                                    String title = place.getTitle();
                                                    String placeAddress = place.getAddress().addressText;
                                                    showSearchResult(placeGeocoordinates, title, placeAddress, "");
                                                }
                                                GeoBox searchResultsGeoBox = new GeoBox(new GeoCoordinates(selectedFeatureBoundingBoxMinLat, selectedFeatureBoundingBoxMinLng), new GeoCoordinates(selectedFeatureBoundingBoxMaxLat, selectedFeatureBoundingBoxMaxLng));
                                                double d = searchResultsGeoBox.northEastCorner.distanceTo(searchResultsGeoBox.southWestCorner);
                                                try {
                                                    searchResultsGeoBox = searchResultsGeoBox.expandedBy(d / 4, d / 4, d / 3, d / 4);
                                                } catch (InstantiationErrorException e) {
                                                    e.printStackTrace();
                                                }
                                                MapCamera.OrientationUpdate orientationUpdate = new MapCamera.OrientationUpdate(0.0, 0.0);
                                                mapView.getCamera().lookAt(searchResultsGeoBox, orientationUpdate);
                                                DataHolder.setIsDragged(true);
                                            }
                                        }
                                    });
                                    return true;
                                }
                                return false;
                            }
                            return false;
                        }
                    });

                } else {
                    Log.d(TAG, "Loading map failed: " + mapError.name());
                }
            }
        });
    }

    void switchRoutingControllers(boolean on) {
        if (on) {
            findViewById(R.id.clear).setVisibility(View.VISIBLE);
            findViewById(R.id.startGuidance).setVisibility(View.VISIBLE);
            findViewById(R.id.vehicleTypeTableLayout).setVisibility(View.VISIBLE);
            findViewById(R.id.car_route).setVisibility(View.VISIBLE);
            findViewById(R.id.bike_route).setVisibility(View.VISIBLE);
            findViewById(R.id.truck_route).setVisibility(View.VISIBLE);
            findViewById(R.id.scooter_route).setVisibility(View.VISIBLE);
            findViewById(R.id.peds_route).setVisibility(View.VISIBLE);
            findViewById(R.id.search_input_text).setVisibility(View.GONE);
            findViewById(R.id.search_button).setVisibility(View.GONE);
        } else {
            findViewById(R.id.clear).setVisibility(View.GONE);
            findViewById(R.id.startGuidance).setVisibility(View.GONE);
            findViewById(R.id.vehicleTypeTableLayout).setVisibility(View.GONE);
            findViewById(R.id.car_route).setVisibility(View.GONE);
            findViewById(R.id.bike_route).setVisibility(View.GONE);
            findViewById(R.id.truck_route).setVisibility(View.GONE);
            findViewById(R.id.scooter_route).setVisibility(View.GONE);
            findViewById(R.id.peds_route).setVisibility(View.GONE);
            findViewById(R.id.search_input_text).setVisibility(View.VISIBLE);
            findViewById(R.id.search_button).setVisibility(View.VISIBLE);
        }
    }

    private void showRouteOnMapView(Route route) {
        DataHolder.setCalculatedResultRoute(route);
        if (routeOnMapView != null) {
            mapView.getMapScene().removeMapPolyline(routeOnMapView);
            routeOnMapView = null;
        }
        try {
            routeOnMapView = new MapPolyline(new GeoPolyline(route.getPolyline()),
                    16,
                    new Color((short) 255, (short) 119, (short) 3, (short) 255));
            mapView.getMapScene().addMapPolyline(routeOnMapView);
            GeoBox routeGeoBox = route.getBoundingBox();
            double d = routeGeoBox.northEastCorner.distanceTo(routeGeoBox.southWestCorner);
            try {
                routeGeoBox = routeGeoBox.expandedBy(d / 4, d / 4, d / 0.9, d / 4);
            } catch (InstantiationErrorException e) {
                e.printStackTrace();
            }
            mapView.getCamera().lookAt(routeGeoBox, new MapCamera.OrientationUpdate(0.0,0.0));
            navigationControlButton.setText("Simulate Guidance");
            navigationControlButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DataHolder.getNavigationExample().startNavigation(DataHolder.getCalculatedResultRoute(), true);
                }
            });
        } catch (InstantiationErrorException e) {
            e.printStackTrace();
        }
    }

    private void showResultSnackbar(GeoCoordinates waypointMapMakerGeoCoordinate, String stringToShow, View view, int duration) {
        searchResultSnackbar = Snackbar.make(view, stringToShow, duration);
        searchResultSnackbar.setAction(R.string.add_waypoint, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (searchResultMapMarkerList.size() > 0) {
                    for (MapMarker m : searchResultMapMarkerList) {
                        mapView.getMapScene().removeMapMarker(m);
                    }
                }
                wayPointList.add(new Waypoint(waypointMapMakerGeoCoordinate));
                MapImage mapImage = MapImageFactory.fromResource(getResources(), R.drawable.ic_map_pin_green);
                MapMarker waypointMapMarker = new MapMarker(waypointMapMakerGeoCoordinate, mapImage, new Anchor2D(0.5F, 1));
                mapView.getMapScene().addMapMarker(waypointMapMarker);
                waypointMapMarkerList.add(waypointMapMarker);

                MapCamera.OrientationUpdate orientation = new MapCamera.OrientationUpdate();
                orientation.bearing = 0.0;
                orientation.tilt = 0.0;
                mapView.getCamera().lookAt(GeoShift.getShiftedGeoLocation(waypointMapMakerGeoCoordinate, mapView.getCamera().getState().distanceToTargetInMeters / 5, orientation.bearing), orientation, mapView.getCamera().getState().distanceToTargetInMeters);
                if (selectedMapMarker != null) {
                    mapView.getMapScene().removeMapMarker(selectedMapMarker);
                    selectedMapMarker = null;
                }

                DataHolder.isDragged = true;
                findViewById(R.id.search_input_text).clearFocus();
                findViewById(R.id.search_input_text).setVisibility(View.GONE);
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(findViewById(R.id.search_input_text).getWindowToken(), 0);
                switchRoutingControllers(true);
                navigationControlButton = findViewById(R.id.startGuidance);
                navigationControlButton.setText("Create Route");
                Router router = new Router();

                resetMapButton = findViewById(R.id.clear);
                resetMapButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mapView.getCamera().lookAt(DataHolder.getCurrentPositionGeoCoordinates());
                        if (routeOnMapView != null) {
                            mapView.getMapScene().removeMapPolyline(routeOnMapView);
                            routeOnMapView = null;
                        }
                        if (waypointMapMarkerList.size() > 0) {
                            for (MapMarker m : waypointMapMarkerList) {
                                mapView.getMapScene().removeMapMarker(m);
                            }
                        }
                        if (searchResultMapMarkerList.size() > 0) {
                            for (MapMarker m : searchResultMapMarkerList) {
                                mapView.getMapScene().removeMapMarker(m);
                            }
                        }
                        switchRoutingControllers(false);
                        DataHolder.getNavigationExample().stopNavigation();
                    }
                });


                navigationControlButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        CarOptions carOptions = new CarOptions();
                        router.calculateRoute(wayPointList, VehicleType.CAR, carOptions);
                        router.setOnRouteCalculationFinishedListener(new Router.OnRouteCalculationFinishedListener() {
                            @Override
                            public void onFinished() {
                                if (router.getRoute() != null) {
                                    showRouteOnMapView(router.getRoute());
                                }
                            }
                        });

                    }
                });
                findViewById(R.id.car_route).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        CarOptions carOptions = new CarOptions();
                        router.calculateRoute(wayPointList, VehicleType.CAR, carOptions);
                        router.setOnRouteCalculationFinishedListener(new Router.OnRouteCalculationFinishedListener() {
                            @Override
                            public void onFinished() {
                                if (router.getRoute() != null) {
                                    showRouteOnMapView(router.getRoute());
                                }
                            }
                        });
                    }
                });
                findViewById(R.id.truck_route).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        TruckOptions truckOptions = new TruckOptions();
                        router.calculateRoute(wayPointList, VehicleType.TRUCK, truckOptions);
                        router.setOnRouteCalculationFinishedListener(new Router.OnRouteCalculationFinishedListener() {
                            @Override
                            public void onFinished() {
                                if (router.getRoute() != null) {
                                    showRouteOnMapView(router.getRoute());
                                }
                            }
                        });
                    }
                });
                findViewById(R.id.peds_route).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PedestrianOptions pedestrianOptions = new PedestrianOptions();
                        router.calculateRoute(wayPointList, VehicleType.PEDESTRIAN, pedestrianOptions);
                        router.setOnRouteCalculationFinishedListener(new Router.OnRouteCalculationFinishedListener() {
                            @Override
                            public void onFinished() {
                                if (router.getRoute() != null) {
                                    showRouteOnMapView(router.getRoute());
                                }
                            }
                        });
                    }
                });
                findViewById(R.id.scooter_route).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar.make(mapView, "Scooter Routing is not supported.", Snackbar.LENGTH_SHORT);
                    }
                });
                findViewById(R.id.bike_route).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar.make(mapView, "Bicycle Routing is not supported.", Snackbar.LENGTH_SHORT);
                    }
                });
            }
        });
        searchResultSnackbar.show();
    }

//    public void addRouteSimulatedLocationButtonClicked(View view) {
//        routingExample.addRouteSimulatedLocation();
//    }
//
//    public void addRouteDeviceLocationButtonClicked(View view) {
//        routingExample.addRouteDeviceLocation();
//    }

//    public void clearMapButtonClicked(View view) {
//        routingExample.clearMapButtonPressed();
//    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}
