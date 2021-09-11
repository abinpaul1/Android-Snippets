package com.example.osm_mapviewscreenshot;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.ListenableWorker;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.ticofab.androidgpxparser.parser.GPXParser;
import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;


// Using this class causes app main ui to freeze and app closes after about 800 images

// Todo : Try Change ListenableFuture callback to ListeningExecutorService
// Refer : https://guava.dev/releases/19.0/api/docs/com/google/common/util/concurrent/ListeningExecutorService.html

// Sample ListenableWorker with future
// https://newbedev.com/asynchronous-worker-in-android-workmanager


// Set foregound Async to do as foreground service
// https://medium.com/androiddevelopers/use-workmanager-for-immediate-background-execution-a57db502603d


public class SnapshotWorkerListenable extends ListenableWorker {

    List<TrackPoint> gpxTrackPoints;
    Iterator<TrackPoint> trackPointsIterator;

    List<GeoPoint> pts = new ArrayList<>();
    ExecutorService executorService = Executors.newCachedThreadPool();
    File path;
    int count = 0;
    Context context;

    CountDownLatch countDownLatch;
    public static final String CHANNEL_ID = "ListenableWorkerChannel";


    public SnapshotWorkerListenable(Context context, WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        
        // Creating notification and making foreground task
        int notificationId = 4631;
        setForegroundAsync(new ForegroundInfo(notificationId, create_notification()));
        
        
        @SuppressLint("RestrictedApi") SettableFuture<Result> future = SettableFuture.<Result>create();

        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));

        org.osmdroid.config.IConfigurationProvider osmConf = org.osmdroid.config.Configuration.getInstance();
        File basePath = new File(context.getFilesDir().getAbsolutePath()+"/osmdroid");
        osmConf.setOsmdroidBasePath(basePath);
        File tileCache = new File(context.getFilesDir().getAbsolutePath()+"/osmdroid/tiles");
        osmConf.setOsmdroidTileCache(tileCache);


        // Open gpx file and draw Polyline of all points
        if (open_gpx_file()) {
            load_all_points();
        }

        countDownLatch = new CountDownLatch(gpxTrackPoints.size()-1);

        path = context.getFilesDir();
        path = new File(path, "GPS_PICS");
        path.mkdirs();  // make sure the folder exists.

        // Do the work here--in this case, upload the images.
        MapView map = new MapView(context);
        MapTileProviderBase mapTileProviderBase = new MapTileProviderBasic(context);


        // Schedule for each location separealty
        for (int i =0; i< gpxTrackPoints.size(); ++i){
            Log.d("Ex","Point "+ Integer.toString(i));
            Double lat = gpxTrackPoints.get(i).getLatitude();
            Double lon = gpxTrackPoints.get(i).getLongitude();
            GeoPoint pt = new GeoPoint(lat,lon);


            Polyline line = new Polyline();
            line.setPoints(pts);

            Marker marker = new Marker(map);
            marker.setPosition(new GeoPoint(lat,lon));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setDefaultIcon();

            List<Overlay> mOverlay = new ArrayList<>();
            mOverlay.add(line);
            mOverlay.add(marker);


            Projection mProjection = new Projection(15.00, 800, 800, pt, 0, true, true, 0, 0);

            // Todo : Extend Mapsnapshot to take in paramter and pass to callback
            final MapSnapshot mapSnapshot = new MapSnapshot(new MapSnapshot.MapSnapshotable() {
                @Override
                public void callback(final MapSnapshot pMapSnapshot) {
                    // Save the current snapshot
                    Log.d("Thread", Thread.currentThread().getName());

                    if (pMapSnapshot.getStatus() != MapSnapshot.Status.CANVAS_OK) {
                        return;
                    }

                    String img_name = "map-" + System.currentTimeMillis() + ".png";
                    File file = new File(path, img_name);

                    pMapSnapshot.save(file); // Saves to specified file on storage
                    count++;
                    Log.d("Ex","Saved "+ Integer.toString(count));
                    countDownLatch.countDown();
                }
            }, MapSnapshot.INCLUDE_FLAG_UPTODATE, mapTileProviderBase, mOverlay, mProjection);

            executorService.submit(mapSnapshot);
//            mapSnapshot.run();
        }

//            // Countdown latch is usually for sync like Workers to wait for completion
//        try {
//            countDownLatch.await();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//            future.set(Result.failure());
//        }

        future.set(Result.success());

        return future;
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
    
    private Notification create_notification(){
        String input = "Snapshotting";
        PendingIntent intent = WorkManager.getInstance(getApplicationContext()).createCancelPendingIntent(getId());
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setSmallIcon(R.drawable.person)
                .setContentText(input)
                .setContentIntent(intent)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(serviceChannel);
        }
        return notification;
    }

}