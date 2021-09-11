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


        Button mbtn = findViewById(R.id.screnbtn);
        // We start taking map snapshots when the button is clicked for first time
        mbtn.setOnClickListener(view -> {

            //  Use foreground service to do snapshoting
            Intent serviceIntent = new Intent(this, ForegroundService.class);
            ContextCompat.startForegroundService(this, serviceIntent);


        });
    }
}

