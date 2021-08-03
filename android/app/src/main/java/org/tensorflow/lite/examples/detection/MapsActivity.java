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
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.tensorflow.lite.examples.detection.databinding.ActivityMapsBinding;
import org.tensorflow.lite.examples.detection.env.Utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener,
        GoogleMap.OnPolygonClickListener, GoogleMap.OnMarkerClickListener{

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private LatLng current;
    private double latitude;
    private double longitude;
    private BroadcastReceiver mReceiver;
    private ArrayList<String> objects;
    private ArrayList<String> latitudes = new ArrayList<>();
    private ArrayList<String> longitudes = new ArrayList<>();
    private ArrayList<String> types = new ArrayList<>();
    private Handler myhandler = new Handler();

    private static final int COLOR_WHITE_ARGB = 0xffffffff;
    private static final int COLOR_GREEN_ARGB = 0xff388E3C;
    private static final int COLOR_PURPLE_ARGB = 0xff81C784;
    private static final int COLOR_ORANGE_ARGB = 0xffF57F17;
    private static final int COLOR_BLUE_ARGB = 0xffF9A825;

    private static final int POLYGON_STROKE_WIDTH_PX = 8;
    private static final int COLOR_BLACK_ARGB = 0xff000000;
    private static final int POLYLINE_STROKE_WIDTH_PX = 12;
    private static final int PATTERN_DASH_LENGTH_PX = 20;
    private static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);
    private static final int PATTERN_GAP_LENGTH_PX = 20;
    private static final PatternItem DOT = new Dot();
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);

    // Create a stroke pattern of a gap followed by a dot.
    private static final List<PatternItem> PATTERN_POLYLINE_DOTTED = Arrays.asList(GAP, DOT);


    // Create a stroke pattern of a gap followed by a dash.
    private static final List<PatternItem> PATTERN_POLYGON_ALPHA = Arrays.asList(GAP, DASH);

    // Create a stroke pattern of a dot followed by a gap, a dash, and another gap.
    private static final List<PatternItem> PATTERN_POLYGON_BETA =
            Arrays.asList(DOT, GAP, DASH, GAP);

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

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        myRunnable runnable = new myRunnable();
        new Thread(runnable).start();


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
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current,16));

        //mMap.setMinZoomPreference(6);

    }
    public void startThread (View view){

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




    @Override
    public void onPolylineClick(Polyline polyline) {
        // Flip from solid stroke to dotted stroke pattern.
        if ((polyline.getPattern() == null) || (!polyline.getPattern().contains(DOT))) {
            polyline.setPattern(PATTERN_POLYLINE_DOTTED);
        } else {
            // The default pattern is a solid stroke.
            polyline.setPattern(null);
        }

        Toast.makeText(this, "Route type " + polyline.getTag().toString(),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPolygonClick(Polygon polygon) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }
    private void stylePolyline(Polyline polyline) {
        String type = "";
        // Get the data object stored with the polyline.
        if (polyline.getTag() != null) {
            type = polyline.getTag().toString();
        }

        switch (type) {
            // If no type is given, allow the API to use the default.
            case "A":
                // Use a custom bitmap as the cap at the start of the line.
                polyline.setStartCap(
                        new CustomCap(
                                BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow), 10));
                break;
            case "B":
                // Use a round cap at the start of the line.
                polyline.setStartCap(new RoundCap());
                break;
        }

        polyline.setEndCap(new RoundCap());
        polyline.setWidth(POLYLINE_STROKE_WIDTH_PX);
        polyline.setColor(getApplicationContext().getResources().getColor(R.color.BLUE));
        polyline.setJointType(JointType.ROUND);
    }

    class myRunnable implements Runnable{
        private ArrayList<Double> customLat = new ArrayList<Double>();
        private ArrayList<Double> customLong = new ArrayList<Double>();

        @Override
        public void run() {
           myhandler.post(new Runnable() {
               private static final String TAG = "thread";

               @Override
               public void run() {
                   for (int i =0 ; i<10 ; i++){
                       Log.d(TAG, "Started" + i);
                   }
                   addCustomMarker();
               }
           });

        }
        public void addCustomMarker(){
            String filePath = Environment.getExternalStorageDirectory()+ "/object_detections.txt";
            String all = "";
            PolylineOptions polylineOptions = new PolylineOptions().clickable(true).color(getApplicationContext().getResources().getColor(R.color.BLUE));
            int prevIndex = 0;
            int i = 0;
            int numPeople = 1;
            double prevLat = 0;
            double prevLong = 0 ;
            String prevtype = "";

            try {
                BufferedReader br = new BufferedReader(new FileReader(filePath));
                String strLine;
                LatLng prevLane = null;
                while ((strLine = br.readLine()) != null){
                    String[] object = strLine.split(" ");
                    String type = object[0];
                    String latitude = object[1];
                    String longitude = object[2];
                    latitudes.add(latitude);
                    longitudes.add(longitude);
                    types.add(type);
                    LatLng latLng = new LatLng(Double.parseDouble(latitude),Double.parseDouble(longitude));
                    switch (type){
                        case "Lane":
                            if (markerExist(prevLat,prevLong)) {
                                Random random = new Random();
                                double temp1 = random.nextDouble();
                                temp1 = temp1 * 0.0001;
                                double temp2 = random.nextDouble();
                                temp2 = temp2 * 0.0001;
                                double newLat = Double.parseDouble(latitude) + temp1;
                                double newLon = Double.parseDouble(longitude) + temp2;
                                while (true) {
                                    if (markerExist(newLat, newLon)) {
                                        temp1 = random.nextDouble();
                                        temp1 = temp1 * 0.0001;
                                        temp2 = random.nextDouble();
                                        temp2 = temp2 * 0.0001;
                                        newLat = Double.parseDouble(latitude) + temp1;
                                        newLon = Double.parseDouble(longitude) + temp2;
                                    } else {
                                        break;
                                    }
                                }
                                if (prevLane!=null){
                                    double distance = distance(prevLane.latitude,prevLane.longitude,newLat,newLon,'K');
                                    if (distance<.8){
                                        polylineOptions.add(new LatLng(newLat,newLon));
                                        prevLat=newLat;
                                        prevLong=newLon;
                                        prevLane=new LatLng(newLat,newLon);
                                    }else{
                                        Polyline polyline = mMap.addPolyline(polylineOptions);
                                        polyline.setTag("B");
                                        stylePolyline(polyline);
                                        polylineOptions = new PolylineOptions().clickable(true);
                                        polylineOptions.add(new LatLng(newLat,newLon));
                                        prevLat=newLat;
                                        prevLong=newLon;
                                        prevLane=new LatLng(newLat,newLon);
                                    }
                                }else
                                {
                                    polylineOptions.add(new LatLng(newLat,newLon));
                                    prevLat=newLat;
                                    prevLong=newLon;
                                    prevLane=new LatLng(newLat,newLon);
                                }
                            }else{
                                if (prevLane!=null){
                                    double distance = distance(prevLane.latitude,prevLane.longitude,latLng.latitude,latLng.longitude,'K');
                                    if (distance<.8){
                                        polylineOptions.add(latLng);
                                        prevLat=Double.parseDouble(latitude);
                                        prevLong=Double.parseDouble(longitude);
                                        prevLane=latLng;
                                    }else{
                                        Polyline polyline = mMap.addPolyline(polylineOptions);
                                        polyline.setTag("B");
                                        stylePolyline(polyline);
                                        polylineOptions = new PolylineOptions().clickable(true);
                                        polylineOptions.add(latLng);
                                        prevLat=Double.parseDouble(latitude);
                                        prevLong=Double.parseDouble(longitude);
                                        prevLane=latLng;
                                    }
                                }else {
                                    polylineOptions.add(latLng);
                                    prevLat=Double.parseDouble(latitude);
                                    prevLong=Double.parseDouble(longitude);
                                    prevLane=latLng;
                                }
                            }

                            break;
                        case "PotHole":
                            if (markerExist(prevLat,prevLong)){
                                Random random = new Random();
                                double temp1 = random.nextDouble();
                                temp1 = temp1*0.0001;
                                double temp2 = random.nextDouble();
                                temp2 = temp2*0.0001;
                                double newLat = Double.parseDouble(latitude)+temp1;
                                double newLon = Double.parseDouble(longitude)+temp2;
                                while(true){
                                    if (markerExist(newLat,newLon)) {
                                        temp1 = random.nextDouble();
                                        temp1 = temp1 * 0.0001;
                                        temp2 = random.nextDouble();
                                        temp2 = temp2 * 0.0001;
                                        newLat = Double.parseDouble(latitude) + temp1;
                                        newLon = Double.parseDouble(longitude) + temp2;
                                    }else{
                                        break;
                                    }
                                }
                                CircleOptions circleOptions = new CircleOptions()
                                        .center(new LatLng(newLat,newLon))
                                        .radius(2); // In meters
                                Circle circle = mMap.addCircle(circleOptions);
                                prevLat = newLat;
                                prevLong=newLon;
                            }else{

                                CircleOptions circleOptions = new CircleOptions()
                                        .center(latLng)
                                        .radius(2); // In meters
                                Circle circle = mMap.addCircle(circleOptions);
                                prevLat=Double.parseDouble(latitude);
                                prevLong=Double.parseDouble(longitude);
                            }

                            break;
                        case "Person":
                            if (markerExist(prevLat,prevLong)){
                                Random random = new Random();
                                double temp1 = random.nextDouble();
                                temp1 = temp1*0.0001;
                                double temp2 = random.nextDouble();
                                temp2 = temp2*0.0001;
                                double newLat = Double.parseDouble(latitude)+temp1;
                                double newLon = Double.parseDouble(longitude)+temp2;
                                while(true){
                                    if (markerExist(newLat,newLon)) {
                                        temp1 = random.nextDouble();
                                        temp1 = temp1 * 0.0001;
                                        temp2 = random.nextDouble();
                                        temp2 = temp2 * 0.0001;
                                        newLat = Double.parseDouble(latitude) + temp1;
                                        newLon = Double.parseDouble(longitude) + temp2;
                                    }else{
                                        break;
                                    }

                                }
                                Marker person = mMap.addMarker(
                                        new MarkerOptions()
                                                .position(new LatLng(newLat,newLon))
                                                .title("person")
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.person)));
                                customLat.add(newLat);
                                customLong.add(newLon);
                                prevLat = newLat;
                                prevLong=newLon;
                            }else{
                               Marker person = mMap.addMarker(
                                        new MarkerOptions()
                                                .position(latLng)
                                                .title("person")
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.person)));
                                prevLat=Double.parseDouble(latitude);
                                prevLong=Double.parseDouble(longitude);
                                customLat.add(Double.parseDouble(latitude));
                                customLong.add(Double.parseDouble(longitude));

                            }


                            break;
                    }
                    i++;
                }
                Polyline polyline = mMap.addPolyline(polylineOptions);
                polyline.setTag("B");
                stylePolyline(polyline);
                Log.d(Utils.TAG, all);
            } catch (IOException e) {
                Log.e("notes_err", e.getLocalizedMessage());
            }
        }

        public boolean markerExist(Double lat, Double lon){
            if (customLat.contains(lat) && customLong.contains(lon))
                return true;
            return false;
        }
        private  double distance(double lat1, double lon1, double lat2, double lon2, char unit) {
            double theta = lon1 - lon2;
            double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
            dist = Math.acos(dist);
            dist = rad2deg(dist);
            dist = dist * 60 * 1.1515;
            if (unit == 'K') {
                dist = dist * 1.609344;
            } else if (unit == 'N') {
                dist = dist * 0.8684;
            }
            return (dist);
        }

        /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
        /*::  This function converts decimal degrees to radians             :*/
        /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
        private  double deg2rad(double deg) {
            return (deg * Math.PI / 180.0);
        }

        /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
        /*::  This function converts radians to decimal degrees             :*/
        /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
        private  double rad2deg(double rad) {
            return (rad * 180.0 / Math.PI);
        }


    }

}

