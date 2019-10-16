package com.example.solartime;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.widget.TextView;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;

import android.content.Context;
import android.content.res.Resources;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {
    private FusedLocationProviderClient fusedLocationClient;
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    private GestureDetectorCompat mDetector;
    solar solarThread;
    public boolean doubletap = false;
    private static final String DEBUG_TAG = "Gestures";
    final Context context = this;
    private String timezone;
    private double tzshift;
    final boolean daylight = false;
    private double latitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocationRequest lc = createLocationRequest();
        getCurrentLocationSettings(lc);

        setLocation();

        solarThread = new solar(this);
        setContentView(solarThread);
        mDetector = new GestureDetectorCompat(this,this);
        // Set the gesture detector as the double tap
        // listener.
        mDetector.setOnDoubleTapListener(this);
    }

    public void setTimezone() {
        tzshift = latitude / 15.0;
        //Log.d("Timezone 1", "" + tzshift);
    }

    public class solar extends SurfaceView implements Runnable {
        Thread gameThread = null;
        volatile boolean playing;
        SurfaceHolder ourHolder;
        Canvas canvas;
        Paint paint;

        // When the we initialize (call new()) on solarThread
        // This special constructor method runs
        public solar(Context context) {
            // asks SurfaceView class to set up our object.
            super(context);
            // Initialize ourHolder and paint objects
            ourHolder = getHolder();
            paint = new Paint();

            // Set our boolean to true - game on!
            playing = true;

        }

        @Override
        public void run() {
            Looper.prepare();
            while(playing) {
                if(timezone == null) setTimezone();
                draw();
            }
        }

        public void draw() {
            String s = updateView();
            // Make sure our drawing surface is valid or we crash
            if (ourHolder.getSurface().isValid()) {
                // Lock the canvas ready to draw
                canvas = ourHolder.lockCanvas();

                canvas.drawColor(Color.argb(255,  123, 157, 200));
                Bitmap sun = BitmapFactory.decodeResource(this.getResources(), R.drawable.solar);
                int height = sun.getHeight(); int width = sun.getWidth();
                canvas.drawBitmap(sun, getScreenWidth() / 2 - width / 2, getScreenHeight() / 2 - height / 2 - 150, paint);
                paint.setTextSize(200);
                //Log.d("Latitude", "" + latitude);
                //Log.d("Timezone", "" + tzshift);
                canvas.drawText(s, getScreenWidth() / 2 - 450, 300, paint);

                // Draw everything to the screen
                ourHolder.unlockCanvasAndPost(canvas);
            }

        }

        public int getScreenWidth() {
            return Resources.getSystem().getDisplayMetrics().widthPixels;
        }

        public int getScreenHeight() {
            return Resources.getSystem().getDisplayMetrics().heightPixels;
        }

        public String updateView() {
            long t = System.currentTimeMillis();
            String s = solarString(t);
            return s;
        }

        public String solarString(long t) {
            String s = "";
            int ms = removeCalendar(t);
            double p = ((double) ms) / (13.0 / 10.0 + 8.0 / 90.0) / 1000;
            //System.out.println(p);
            double ss = p / 12; p %= 12; p = (int) p;
            //System.out.println(ss + "\t" + p);
            double sm = ss / 12; ss %= 12; ss = (int) ss;
            //System.out.println(sm + "\t" + ss);
            double sh = sm / 12; sm %= 12; sm = (int) sm;
            //System.out.println(sh + "\t" + sm);
            int sq = (int) sh / 12; sh %= 12; sh = (int) sh;
            //System.out.println(sq + "\t" + sh);

            String sqc = "";
            switch(sq) {
                case 0:
                    sqc = "MS";
                    break;
                case 1:
                    sqc = "ES";
                    break;
                case 2:
                    sqc = "NS";
                    break;
            }
            s += sqc + " " + a(sh) + ":" + a(sm) + ":" + a(ss) + ":" + a(p);
            return s;
        }

        public int removeCalendar(long t) {
            int a = (int) ((t + (tzshift*1000*60*60)) % (1000*60*60*24));
            //int b = (a - (8*1000*60*60));
            int b = a;
            int c = (b - (6*1000*60*60));
            if( c < 0 ) {
                c += 24*60*60*1000;
            }
            //System.out.println(a);
            //System.out.println(b);
            //System.out.println(c);
            return c;
        }

        public String a(double a) {
            return base12((int) a);
        }

        public String base12(int a) {
            if( a < 10 )
                return a + "";
            else {
                switch(a) {
                    case 10:
                        return "a";
                    case 11:
                        return "b";
                    default:
                        return "-";
                }
            }
        }

        // If activity is paused/stopped
        // shutdown our thread.
        public void pause() {
            playing = false;
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Log.e("Error:", "joining thread");
            }

        }

        // If activity is started then
        // start our thread.
        public void resume() {
            playing = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

    }

    // This method executes when the player starts the game
    @Override
    protected void onResume() {
        super.onResume();

        // Tell the solar resume method to execute
        solarThread.resume();
    }

    // This method executes when the player quits the game
    @Override
    protected void onPause() {
        super.onPause();

        // Tell the solar pause method to execute
        solarThread.pause();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (this.mDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event) {
        Log.d(DEBUG_TAG,"onDown: " + event.toString());
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
        Log.d(DEBUG_TAG, "onFling: " + event1.toString() + event2.toString());
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        Log.d(DEBUG_TAG, "onLongPress: " + event.toString());
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                            float distanceY) {
        Log.d(DEBUG_TAG, "onScroll: " + event1.toString() + event2.toString());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        Log.d(DEBUG_TAG, "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());
        doubletap = true;
        Log.d(DEBUG_TAG, "Setting up messagebox");
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.dialogbox, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.editTextDialogUserInput);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // get user input and set it to result
                                // edit text
                                timezone = (userInput.getText()) + "";
                                tzshift = interpretTimezone(timezone);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                setTimezone();
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
        return true;
    }

    private double interpretTimezone(String t) {
        double res = 0;
        boolean daylightused = false;
        String[] timezones = {"EST", "ACST", "ACT", "ACWST", "AST", "AST", "AEST", "AFT", "AKST", "AMT", "AMT", "ART", "AWST", "AZT", "BDT", "BIOT",
                "BIT", "BOT", "BRT", "BST", "BST", "BTT", "CAT", "CCT",
                "CST", "BT", "CET", "CHAST", "CHOT", "CHUT", "CIT", "CLT", "COT", "DFT",
                "EET", "EGT", "EIT", "FET", "FKT", "GALT", "GYT", "HKT", "IST", "KST",
                "KRAT", "MET", "MSK", "MST", "OMST", "PETT", "PHT", "PST", "UZT", "YEKT"};
        boolean[] d = {true, true, false, false, false, true, true, false, true, true, true, false, true, false, false, false,
                false, false, true, false, false, false, false, false,
                true, false, true, true, true, false, false, true, true, false,
                true, true, false, false, true, false, false, false, true, false,
                true, true, false, true, false, false, false, true, false, false};
        double[] shifts = {-5, 9.5, -5, 8.75, 3, -4, 11, 4.5, -9, 4, -4, -3, 8, 4, 8, 6,
                -12, -4, -3, 6, 11, 6, 2, 6.5,
                -6, 8, 1, 12.75, 8, 10, 8, -4, -5, 1,
                2, -1, 9, 3, -4, -6, -4, 8, 2, 9,
                7, 1, 3, -7, 6, 12, 8, -8, 5, 5};

        for(int i = 0; i < timezones.length; i++) {
            if(t.equals(timezones[i])) {
                res = shifts[i];
                daylightused = d[i];
                break;
            }
        }

        if(daylight & daylightused) {
            res++;
        }
        return res;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDoubleTapEvent: " + event.toString());
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
        return true;
    }

    protected void setLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            latitude = location.getLongitude();
                            Log.d("Latitude 1", "" + latitude);
                        }
                        else {
                            Log.d("NO PERMISSION", "Location");
                        }
                    }
                });
    }

    protected void getCurrentLocationSettings(LocationRequest locationRequest) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        // ...

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                        Log.d("ERROR", "Failed to raise dialog");
                    }
                }
            }
        });

    }

    protected LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

}
