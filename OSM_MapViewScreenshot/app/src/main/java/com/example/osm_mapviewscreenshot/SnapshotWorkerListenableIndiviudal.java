package com.example.osm_mapviewscreenshot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.ticofab.androidgpxparser.parser.GPXParser;
import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;


// Using this class causes outofmeemero error after loading about 176 points, doesnt save
// Too  manu receivers when gpxPoints is 1000


public class SnapshotWorkerListenableIndiviudal extends ListenableWorker {

    List<TrackPoint> gpxTrackPoints;
    Iterator<TrackPoint> trackPointsIterator;

    List<GeoPoint> pts = new ArrayList<>();
    File path;

    public SnapshotWorkerListenableIndiviudal(Context context, WorkerParameters params) {
        super(context, params);
    }


    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        SettableFuture<Result> future = SettableFuture.create();

            // Do stuff
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

            path = ctx.getFilesDir();
            path = new File(path, "GPS_PICS");
            path.mkdirs();  // make sure the folder exists.

            MapView map = new MapView(ctx);
            MapTileProviderBase mapTileProviderBase = new MapTileProviderBasic(ctx);

            final int i = getInputData().getInt("index", 0);


            // Schedule for each location separealty
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
                    future.set(Result.success());
                    Log.d("Ex","Saved "+ Integer.toString(i));
                }
            }, MapSnapshot.INCLUDE_FLAG_UPTODATE, mapTileProviderBase, mOverlay, mProjection);
            getBackgroundExecutor().execute(mapSnapshot);
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

}