package com.example.osm_mapviewscreenshot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.drawing.MapSnapshot;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.ticofab.androidgpxparser.parser.GPXParser;
import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;

public class MainActivity extends AppCompatActivity {

    List<TrackPoint> gpxTrackPoints;
    Iterator<TrackPoint> trackPointsIterator;

    MapView map = null;
    IMapController mapController;
    Marker prev_marker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*  Load/initialize the osmdroid configuration
        This can be done by setting this before the layout is inflated
        it 'should' ensure that the map has a writable location for the map cache, even without permissions
        if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        Note, the load method also sets the HTTP User Agent to your application's package name */
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        org.osmdroid.config.IConfigurationProvider osmConf = org.osmdroid.config.Configuration.getInstance();
        File basePath = new File(getFilesDir().getAbsolutePath()+"/osmdroid");
        osmConf.setOsmdroidBasePath(basePath);
        File tileCache = new File(getFilesDir().getAbsolutePath()+"/osmdroid/tiles");
        osmConf.setOsmdroidTileCache(tileCache);

        setContentView(R.layout.activity_main);

        //Configuring map display
        map = findViewById(R.id.exportmap);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setDrawingCacheEnabled(true);


        mapController = map.getController();
        mapController.setZoom(16.0);

        // Open gpx file and draw Polyline of all points
        if (open_gpx_file()) {
            draw_polyline_with_all_points();
        }


        Button mbtn = findViewById(R.id.screnbtn);
        // We start taking map snapshots when the button is clicked for first time
        mbtn.setOnClickListener(view -> {
            TrackPoint t = trackPointsIterator.next();
            Double lat = t.getLatitude();
            Double lon = t.getLongitude();
            schedule_snapshot(new GeoPoint(lat,lon));
        });
    }

    // Removes previous marker from mapview
    // Adds new marker with current point
    // Calls capture_snapshot
    private void schedule_snapshot(GeoPoint point) {
        if (point==null){
            return;
        }
        mapController.setCenter(point);

        if (prev_marker!=null){
            map.getOverlays().remove(prev_marker);
        }
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marker);
        marker.setDefaultIcon();

        map.postInvalidate();
        prev_marker = marker;
        capture_snapshot();
    }

    // Creates and starts a thread which gets called when snapshot is ready 
    private void capture_snapshot() {
        final MapSnapshot mapSnapshot = new MapSnapshot(new MapSnapshot.MapSnapshotable() {
            @Override
            public void callback(final MapSnapshot pMapSnapshot) {
                // Save the current snapshot
                // Schedule snapshot for next point (taken from iterator)

                if (pMapSnapshot.getStatus() != MapSnapshot.Status.CANVAS_OK) {
                    return;
                }
                final Bitmap bitmap = Bitmap.createBitmap(pMapSnapshot.getBitmap());
                save_snapshot(bitmap); //    Saved in 'GPS_PICS" folder in externalStorage

                // get next point and schedule snapshot for the same
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(trackPointsIterator.hasNext()) {
                            TrackPoint t = trackPointsIterator.next();
                            Double lat = t.getLatitude();
                            Double lon = t.getLongitude();
                            schedule_snapshot(new GeoPoint(lat, lon));
                        }
                    }
                });
            }
        }, MapSnapshot.INCLUDE_FLAG_UPTODATE, map);
        new Thread(mapSnapshot).start();
    }
    

    // Utility function to saves the bitmap to a file
    private void save_snapshot(Bitmap bm){
        // All images are saved to 'GPS_PICS" folder in app's internal storage

        String img_name = "map-" + System.currentTimeMillis() + ".png";

        /* now you've got the bitmap - go save it */
        File path = getFilesDir();
        path = new File(path, "GPS_PICS");
        path.mkdirs();  // make sure the folder exists.
        File file = new File(path, img_name);
        try {
            BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(file));
            boolean success = bm.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Uses all the Points and creates Polyline and adds it to map
    private void draw_polyline_with_all_points(){
        Polyline line = new Polyline(map);
        List<GeoPoint> pts = new ArrayList<>();
        for (int i =0; i< gpxTrackPoints.size(); ++i){
            Double lat = gpxTrackPoints.get(i).getLatitude();
            Double lon = gpxTrackPoints.get(i).getLongitude();
            pts.add(new GeoPoint(lat,lon));
        }
        Log.d("Points", "Successfully Loaded " + gpxTrackPoints.size() + " Points");
        line.setPoints(pts);
        map.getOverlayManager().add(line);
    }


    // Opens the gpx file and Loads all Points into an iterator
    private boolean open_gpx_file(){

        GPXParser parser;
        Gpx gpx_parsed;
        parser = new GPXParser();

        String gpx_filename = "Sample.gpx";

        try {
            gpx_parsed = parser.parse(getAssets().open(gpx_filename));
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