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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener,
        GoogleMap.OnPolygonClickListener{

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private LatLng current;
    private double latitude;
    private double longitude;
    private BroadcastReceiver mReceiver;
    private ArrayList<String> objects;
    private ArrayList<String> latitudes;
    private ArrayList<String> longitudes;
    private ArrayList<String> types;

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
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current,16));
        addCustomMarker();
        //mMap.setMinZoomPreference(6);

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
        types = DataHolder.getInstance().getTypes();
        latitudes = DataHolder.getInstance().getLatitudes();
        longitudes = DataHolder.getInstance().getLongitudes();
        PolylineOptions polylineOptions = new PolylineOptions().clickable(true);
        int prevIndex = 0;
        int i = 0;
        for (String type: types){

            switch (type){
                case "Lane":
                    if (i<prevIndex+4){
                        LatLng latLng = new LatLng(Double.parseDouble(latitudes.get(i)),Double.parseDouble(longitudes.get(i)));
                        polylineOptions.add(latLng);
                        prevIndex = types.indexOf(type);
                    }
                    break;
                case "PotHole": CircleOptions circleOptions = new CircleOptions()
                        .center(new LatLng(Double.parseDouble(latitudes.get(i)),Double.parseDouble(longitudes.get(i))))
                        .radius(0.5); // In meters

                    Circle circle = mMap.addCircle(circleOptions);
                    break;
                case "Person":
                    LatLng personLocation = new LatLng(Double.parseDouble(latitudes.get(i)),Double.parseDouble(longitudes.get(i)));
                    Marker person = mMap.addMarker(
                            new MarkerOptions()
                                    .position(personLocation)
                                    .title("person")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.person)));
                    break;

            }

            Polyline polyline = mMap.addPolyline(polylineOptions);
            polyline.setTag("B");
            stylePolyline(polyline);
            polylineOptions = new PolylineOptions().clickable(true);
            i++;
        }
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
        polyline.setColor(COLOR_BLACK_ARGB);
        polyline.setJointType(JointType.ROUND);
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
}