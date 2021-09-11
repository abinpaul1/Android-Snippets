package com.example.osm_mapviewscreenshot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

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


// Using this class does not work

//java.util.concurrent.ExecutionException: java.lang.RuntimeException:
// Can't create handler inside thread Thread[pool-2-thread-1,5,main] that has not called Looper.prepare()

public class SnapshotWorker extends Worker {
    public SnapshotWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    List<TrackPoint> gpxTrackPoints;
    Iterator<TrackPoint> trackPointsIterator;

    List<GeoPoint> pts = new ArrayList<>();
    ExecutorService executorService = Executors.newCachedThreadPool();
    File path;
    int count = 0;
    CountDownLatch countDownLatch;

    @Override
    public Result doWork() {

        // Do the work here--in this case, upload the images.
        take_snapshots();

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Indicate whether the work finished successfully with the Result
        return Result.success();
    }


    @SuppressLint("RestrictedApi")
    public void take_snapshots(){
        Context ctx = getApplicationContext();

        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        org.osmdroid.config.IConfigurationProvider osmConf = org.osmdroid.config.Configuration.getInstance();
        File basePath = new File(ctx.getFilesDir().getAbsolutePath()+"/osmdroid");
        osmConf.setOsmdroidBasePath(basePath);
        File tileCache = new File(ctx.getFilesDir().getAbsolutePath()+"/osmdroid/tiles");
        osmConf.setOsmdroidTileCache(tileCache);


        // Open gpx file and draw Polyline of all points
        if (open_gpx_file()) {
            load_all_points();
        }

        // Set coutdown latch
        countDownLatch = new CountDownLatch(gpxTrackPoints.size()-1);


        path = ctx.getFilesDir();
        path = new File(path, "GPS_PICS");
        path.mkdirs();  // make sure the folder exists.

        // Do the work here--in this case, upload the images.
        MapView map = new MapView(ctx);
        MapTileProviderBase mapTileProviderBase = new MapTileProviderBasic(ctx);


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
                    countDownLatch.countDown();
                    Log.d("Ex","Saved "+ Integer.toString(count));
                }
            }, MapSnapshot.INCLUDE_FLAG_UPTODATE, mapTileProviderBase, mOverlay, mProjection);
//            getBackgroundExecutor().execute(mapSnapshot);
            mapSnapshot.run();
        }
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
