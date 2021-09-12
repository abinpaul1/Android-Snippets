package com.example.osm_mapviewscreenshot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import org.osmdroid.util.GeoPoint;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.ticofab.androidgpxparser.parser.GPXParser;
import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;


// Running in background important takeaways

// For tasks that should be executed immediately and need continued processing,
// even if the user puts the application in background or the device restarts,
// we recommend using WorkManager and its support for long-running tasks.

// We recommend you use WorkManager to execute long running immediate tasks.

// ListenableWorker is a class that can perform work asynchronously in WorkManager.

public class MainActivity extends AppCompatActivity {

    List<TrackPoint> gpxTrackPoints;
    Iterator<TrackPoint> trackPointsIterator;

    List<GeoPoint> pts = new ArrayList<>();
    File path;
    int count = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // For testing screen hang
        Button inc_btn = findViewById(R.id.button);
        EditText num = findViewById(R.id.editTextNumber);
        num.setText("1");
        inc_btn.setOnClickListener(view -> {
            int i = Integer.valueOf(num.getText().toString()) + 1;
            num.setText(Integer.toString(i));
        });


        Button mbtn = findViewById(R.id.screnbtn);
        // We start taking map snapshots when the button is clicked for first time
        mbtn.setOnClickListener(view -> {

            // Method 1 : Create ListenableWorker/Worker to do snapshoting
            WorkRequest snapshotWorkRequest =
                    new OneTimeWorkRequest.Builder(SnapshotWorkerListenable.class)
                            .build();
            WorkManager
                    .getInstance(getApplicationContext())
                    .enqueue(snapshotWorkRequest);


//            // Method 2 : Use foreground service to do snapshoting
//            Intent serviceIntent = new Intent(this, ForegroundService.class);
//            ContextCompat.startForegroundService(this, serviceIntent);




//            // Method 3 : Create worker for each point separaltely and snapshot
//            if (open_gpx_file()) {
//                load_all_points();
//            }
//
//            for (int i =0; i< gpxTrackPoints.size(); ++i) {
//                // Passing index to work manager
//                Data myData = new Data.Builder().putInt("index", i).build();
//                WorkRequest snapshotWorkRequest =
//                    new OneTimeWorkRequest.Builder(SnapshotWorkerListenableIndiviudal.class).setInputData(myData)
//                            .build();
//                WorkManager
//                        .getInstance(getApplicationContext())
//                        .enqueue(snapshotWorkRequest);
//            }



        });
    }

    // Uses all the Points and creates Polyline and adds it to map
    private void load_all_points(){
        for (int i =0; i< gpxTrackPoints.size(); ++i){
            Double lat = gpxTrackPoints.get(i).getLatitude();
            Double lon = gpxTrackPoints.get(i).getLongitude();
            pts.add(new GeoPoint(lat,lon));
        }
        Log.d("Points", "Successfully Loaded " + gpxTrackPoints.size() + " Points");
    }


    // Opens the gpx file and Loads all Points into an iterator
    private boolean open_gpx_file(){

        GPXParser parser;
        Gpx gpx_parsed;
        parser = new GPXParser();

        String gpx_filename = "Sample.gpx";

        try {
            gpx_parsed = parser.parse(getApplicationContext().getAssets().open(gpx_filename));
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
            return false;
        }


        // We support playback of first segment of first track only
        if (gpx_parsed!=null){
            List<Track> tracks = gpx_parsed.getTracks();
            List<TrackSegment> trackSegments;
            if (tracks.size()>0) {
                trackSegments = tracks.get(0).getTrackSegments();
                if (trackSegments.size() > 0) {
                    gpxTrackPoints = trackSegments.get(0).getTrackPoints();
                    // Empty GPX file
                    if(gpxTrackPoints.size()==0) {
                        return false;
                    }
                    trackPointsIterator = gpxTrackPoints.iterator();
                    return true;
                }
            }
        }
        return false;
    }
}

