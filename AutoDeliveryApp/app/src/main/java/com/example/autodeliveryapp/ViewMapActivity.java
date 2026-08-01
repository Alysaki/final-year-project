package com.example.autodeliveryapp;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;

public class ViewMapActivity extends AppCompatActivity {

    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Khởi tạo MapLibre
        MapLibre.getInstance(this);

        setContentView(R.layout.activity_view_map);
        EdgeToEdgeHelper.setLightStatusBar(this, true);
        
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        ImageView btnBack = findViewById(R.id.btnBack);
        EdgeToEdgeHelper.applySystemBarsPadding(btnBack);
        btnBack.setOnClickListener(v -> finish());

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapLibreMap mapLibreMap) {
                // Tải style từ Constants
                mapLibreMap.setStyle(new Style.Builder().fromUri(Constants.MAP_STYLE_URL), new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(Style style) {
                        // Focus vào khu vực trung tâm Đà Nẵng (ví dụ Cầu Rồng)
                        double[] center = Constants.DA_NANG_LOCATIONS.get("dragon bridge");
                        if (center != null) {
                            CameraPosition position = new CameraPosition.Builder()
                                    .target(new LatLng(center[0], center[1]))
                                    .zoom(14.0)
                                    .build();
                            mapLibreMap.setCameraPosition(position);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) {
            mapView.onStop();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }
}
