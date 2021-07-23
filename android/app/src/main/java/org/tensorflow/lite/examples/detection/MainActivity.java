package org.tensorflow.lite.examples.detection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.jetbrains.annotations.NotNull;
import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.env.Utils;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.YoloV4Classifier;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener {

    public static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.2f;
    private Button mapButton;
    FusedLocationProviderClient fusedLocationProviderClient;
    private double latitude;
    private double longitude;
    private Button select;
    private Bitmap bitmapSelect;
    private Button usbCamera;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        cameraButton = findViewById(R.id.cameraButton);
        detectButton = findViewById(R.id.detectButton);
        imageView = findViewById(R.id.imageView);
        mapButton = findViewById(R.id.map);
        select = findViewById(R.id.select);
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.kite);


        this.sourceBitmap = bm;
        this.cropBitmap = Utils.processBitmap(sourceBitmap, TF_OD_API_INPUT_SIZE);
        imageView.setImageBitmap(cropBitmap);
        usbCamera = findViewById(R.id.usbCamera);

        cameraButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, DetectorActivity.class)));


        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*"); // change the image to the file you want.
                startActivityForResult(intent, 100 );
            }
        });

        detectButton.setOnClickListener(v -> {
            Handler handler = new Handler();

            new Thread(() -> {
                final List<Classifier.Recognition> results = detector.recognizeImage(cropBitmap);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        handleResult(cropBitmap, results);
                    }
                });
            }).start();

        });


        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                    getCurrentLocation();

                }else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION
                                    ,Manifest.permission.ACCESS_COARSE_LOCATION}
                            , 100);
                }


            }
        });
        usbCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, USBCameraActivity.class);
                startActivity(intent);
            }
        });


        initBox();
    }

    private static final Logger LOGGER = new Logger();

    public static final int TF_OD_API_INPUT_SIZE = 416;

    private static final boolean TF_OD_API_IS_QUANTIZED = false;

    private static final String TF_OD_API_MODEL_FILE = "custom-tiny-416.tflite";

    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/classes.txt";

    // Minimum detection confidence to track a detection.
    private static final boolean MAINTAIN_ASPECT = true;
    private Integer sensorOrientation = 90;

    private Classifier detector;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private MultiBoxTracker tracker;
    private OverlayView trackingOverlay;

    protected int previewWidth = 0;
    protected int previewHeight = 0;

    private Bitmap sourceBitmap;
    private Bitmap cropBitmap;

    private Button cameraButton, detectButton;
    private ImageView imageView;

    private void initBox() {
        previewHeight = TF_OD_API_INPUT_SIZE;
        previewWidth = TF_OD_API_INPUT_SIZE;
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        tracker = new MultiBoxTracker(this);
        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                canvas -> tracker.draw(canvas));

        tracker.setFrameConfiguration(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, sensorOrientation);

        try {
            detector =
                    YoloV4Classifier.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_IS_QUANTIZED);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
    }

    private void handleResult(Bitmap bitmap, List<Classifier.Recognition> results) {
        final Canvas canvas = new Canvas(bitmap);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<Classifier.Recognition>();

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                canvas.drawRect(location, paint);
//                cropToFrameTransform.mapRect(location);
//
//                result.setLocation(location);
//                mappedRecognitions.add(result);
            }
        }
//        tracker.trackResults(mappedRecognitions, new Random().nextInt());
//        trackingOverlay.postInvalidate();
        imageView.setImageBitmap(bitmap);
    }
    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    public void getCurrentLocation(){
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull @NotNull Task<Location> task) {
                    Location location = task.getResult();
                    if (location != null){
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();

                    }else{
                        LocationRequest locationRequest = new LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                                .setInterval(10000)
                                .setFastestInterval(1000)
                                .setNumUpdates(1);

                        LocationCallback locationCallback = new LocationCallback() {
                            @Override
                            public void onLocationResult(@NonNull @NotNull LocationResult locationResult) {
                                Location location1 = locationResult.getLastLocation();
                                latitude = location1.getLatitude();
                                longitude = location1.getLongitude();
                            }
                        };
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());

                    }
                    Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                    intent.putExtra("Latitude",""+latitude);
                    intent.putExtra("Longitude", ""+longitude);
                    startActivity(intent);
                }
            });
            }else{
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }



    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull  int[] grantResults) {
        if ( requestCode== 100 && grantResults.length>0 && (grantResults[0]+grantResults[1] == PackageManager.PERMISSION_GRANTED)){
            getCurrentLocation();
        }else{
            Toast.makeText(getApplicationContext(), "Permission denied",Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {


                Uri select = data.getData();
                InputStream inputStream = null;

                try {
                    assert select != null;
                    inputStream = getContentResolver().openInputStream(select);
                    bitmapSelect = BitmapFactory.decodeStream(inputStream);
                    this.sourceBitmap = bitmapSelect;

                    this.cropBitmap = Utils.processBitmap(sourceBitmap, TF_OD_API_INPUT_SIZE);

                    this.imageView.setImageBitmap(cropBitmap);
                    //imageView.setImageBitmap(bitmap);

                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        }
    }

}
