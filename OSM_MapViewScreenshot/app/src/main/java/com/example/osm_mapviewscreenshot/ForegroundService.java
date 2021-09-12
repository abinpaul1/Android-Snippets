package com.example.osm_mapviewscreenshot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.drawing.MapSnapshot;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import io.ticofab.androidgpxparser.parser.GPXParser;
import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;

// Freezes UI, takes 800 snaphsots and crashes
// OR Long monitor contention with owner and stops

// Refer : https://heartbeat.fritz.ai/using-foreground-services-for-executing-long-running-processes-in-android-fac0b8585c3a

// Recursive works fine, UI doesnt freeze
// OS optimizes and kills process, so try acquiring wakelock to keep running

public class ForegroundService extends Service {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    int notificationID = 123;
    String input = "Snapshotting";
    Intent notificationIntent;
    PendingIntent pendingIntent ;
    NotificationManager manager;

    List<TrackPoint> gpxTrackPoints;
    Iterator<TrackPoint> trackPointsIterator;

    List<GeoPoint> pts = new ArrayList<>();
    ExecutorService executorService = Executors.newCachedThreadPool();
    File path;
    HashMap<MapSnapshot, Integer> mapping = new HashMap<>();
    AtomicInteger count = new AtomicInteger(0);

    Context ctx;
    MapView map = null;
    MapTileProviderBase mapTileProviderBase = null;

    @Override
    public void onCreate() {
        super.onCreate();
        this.ctx = getApplicationContext();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Setting notification
        manager = getSystemService(NotificationManager.class);
        createNotificationChannel();
        notificationIntent = new Intent(this, MainActivity.class);
        pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText(input)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(notificationID, notification);


        //do heavy work on a background thread
        take_snapshots();


        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateNotification(int current){
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Index" + Integer.toString(current))
                .setContentText(input)
                .setSmallIcon(R.drawable.person)
                .setContentIntent(pendingIntent)
                .build();
        manager.notify(notificationID, notification);
    }


    public void take_snapshots(){
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        org.osmdroid.config.IConfigurationProvider osmConf = org.osmdroid.config.Configuration.getInstance();
        File basePath = new File(ctx.getFilesDir().getAbsolutePath()+"/osmdroid");
        osmConf.setOsmdroidBasePath(basePath);
        File tileCache = new File(ctx.getFilesDir().getAbsolutePath()+"/osmdroid/tiles");
        osmConf.setOsmdroidTileCache(tileCache);

        // Open gpx file and loads all points
        if (open_gpx_file()) {
            load_all_points();
        }

        path = ctx.getFilesDir();
        path = new File(path, "GPS_PICS");
        path.mkdirs();  // make sure the folder exists.

        // Do the work here.
        map = new MapView(ctx);
        mapTileProviderBase = new MapTileProviderBasic(ctx);


        // Method 1 : Recursive snapshot
        recursive_snapshot(trackPointsIterator.next(),0);


//        // Method 2: Iterative snapshot
//        iterative_snapshot();
    }

    private void recursive_snapshot(TrackPoint point, int i){
        if (point==null)
            return;

        Double lat = point.getLatitude();
        Double lon = point.getLongitude();

        GeoPoint pt = new GeoPoint(lat, lon);


        Polyline line = new Polyline();
        line.setPoints(pts);

        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setDefaultIcon();

        List<Overlay> mOverlay = new ArrayList<>();
        mOverlay.add(line);
        mOverlay.add(marker);


        Projection mProjection = new Projection(15.00, 800, 800, pt, 0, true, true, 0, 0);

        final MapSnapshot mapSnapshot = new MapSnapshot(new MapSnapshot.MapSnapshotable() {
            @Override
            public void callback(final MapSnapshot pMapSnapshot) {
                // Save the current snapshot
                Log.d("Thread", Thread.currentThread().getName());

                if (pMapSnapshot.getStatus() != MapSnapshot.Status.CANVAS_OK) {
                    return;
                }

                String img_name = "map-" + mapping.get(pMapSnapshot) + ".png";
                File file = new File(path, img_name);

                pMapSnapshot.save(file); // Saves to specified file on storage
                Log.d("Ex", "Saved " + Integer.toString(mapping.get(pMapSnapshot)));

                // Update notification
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (i%16==0){
                        updateNotification(i);
                    }
                }


                if(trackPointsIterator.hasNext())
                    recursive_snapshot(trackPointsIterator.next(),i+1);
                else{
                    stopForeground(true);
                }
            }
        }, MapSnapshot.INCLUDE_FLAG_UPTODATE, mapTileProviderBase, mOverlay, mProjection);

        // Keep all Mapsnapshots in Hashmap
        mapping.put(mapSnapshot, i);
        mapSnapshot.run();
    }

    private void iterative_snapshot(){
        // Schedule for each location separealty
        for (int i =0; i< gpxTrackPoints.size(); ++i) {
            Log.d("Ex", "Point " + Integer.toString(i));
            Double lat = gpxTrackPoints.get(i).getLatitude();
            Double lon = gpxTrackPoints.get(i).getLongitude();
            GeoPoint pt = new GeoPoint(lat, lon);


            Polyline line = new Polyline();
            line.setPoints(pts);

            Marker marker = new Marker(map);
            marker.setPosition(new GeoPoint(lat, lon));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setDefaultIcon();

            List<Overlay> mOverlay = new ArrayList<>();
            mOverlay.add(line);
            mOverlay.add(marker);


            Projection mProjection = new Projection(15.00, 800, 800, pt, 0, true, true, 0, 0);

            // Todo : Extend Mapsnapshot to take in paramter and pass to callback
            final MapSnapshot mapSnapshot = new MapSnapshot(new MapSnapshot.MapSnapshotable() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void callback(final MapSnapshot pMapSnapshot) {
                    // Save the current snapshot
                    Log.d("Thread", Thread.currentThread().getName());

                    if (pMapSnapshot.getStatus() != MapSnapshot.Status.CANVAS_OK) {
                        return;
                    }

                    String img_name = "map-" + mapping.get(pMapSnapshot) + ".png";
                    File file = new File(path, img_name);

                    pMapSnapshot.save(file); // Saves to specified file on storage

                    int temp = count.getAndIncrement();

//                    // Log progress to notification bar
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        updateNotification(temp);
//                    }

                    Log.d("Ex", "Saved " + Integer.toString(mapping.get(pMapSnapshot)));
                }
            }, MapSnapshot.INCLUDE_FLAG_UPTODATE, mapTileProviderBase, mOverlay, mProjection);

            // Keep all Mapsnapshots in Hashmap
            mapping.put(mapSnapshot, i);

            executorService.submit(mapSnapshot);
        }
    }

    // Loads all the Points into an ArrayList
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