package org.tensorflow.lite.examples.detection;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.tensorflow.lite.examples.detection.databinding.ActivityMapsBinding;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private LatLng current;
    private double latitude;
    private double longitude;
    private BroadcastReceiver mReceiver;
    private ArrayList<String> objects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                longitude= 0;
                latitude=0;
            } else {
                latitude= Double.parseDouble(extras.getString("Latitude"));
                longitude = Double.parseDouble(extras.getString("Longitude"));
            }
        } else {
            latitude= Long.parseLong((String) savedInstanceState.getSerializable("Latitude"));
            longitude = Long.parseLong((String) savedInstanceState.getSerializable("Longitude"));

        }
        current = new LatLng(latitude,longitude);
        objects = new ArrayList<String>();

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("Flags"));
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();

                int i=0;
                if (bundle!=null){
                    for (String key : bundle.keySet()){
                        objects.add(intent.getStringExtra("object "+i));
                        i++;
                    }
                }


            }
        };

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);




    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        // Add a marker in Sydney and move the camera
        mMap.addMarker(new MarkerOptions().position(current).title("Marker in current location")
        .icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.ic_baseline_flag_24)));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(current));
        addCustomMarker();
        mMap.setMinZoomPreference(6);
    }


    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId){
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0,0,vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public void addCustomMarker(){
        for (String object : objects){
            mMap.addMarker(new MarkerOptions().position(current).title(object)
                    .icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.ic_baseline_flag_24)));

        }
    }

}