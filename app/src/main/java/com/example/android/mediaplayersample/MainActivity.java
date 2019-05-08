/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.mediaplayersample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Allows playback of a single MP3 file via the UI. It contains a {@link MediaPlayerHolder}
 * which implements the {@link PlayerAdapter} interface that the activity uses to control
 * audio playback.
 */
public final class MainActivity extends AppCompatActivity implements SensorEventListener{

    public static final String TAG = "MainActivity";
    public static final int MEDIA_RES_ID = R.raw.jazz_in_paris;
    public static final int MEDIA_RES_ID2 = R.raw.with_u;

    public int curr = 1;
    private TextView mTextDebug;
    private SeekBar mSeekbarAudio;
    private ScrollView mScrollContainer;
    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;


    //sensor variables
    private boolean playing = false;
    private SensorManager sensorManager;

    //proximity gesture variables
    private Sensor      proximitySensor;
    private final int   tapGestureDuration      = 2000;
    private final int   numOfTapsNeeded         = 2;
    private long        lastTapGestureTime      = 0;
    private float       lastProximitySensorValue= 0;
    private int         numOfTapGesturesDetected= 0;

    //accelerometer gesture variables
    private Sensor      accelerometerSensor;
    private final int   shakeGestureDuration        = 500;
    private final int   shakeGestureThreshold       = 500;
    private final int   detectShakeEvery            = 1000;
    private long        lastShakeGestureTime        = 0;
    private long        lastShakeGestureDetectedTime= 0;
    private float []    currentAccelerometerValues  = {0,0,0};
    private float []    prevAccelerometerValues     = {0,0,0};

    //rotation vector gesture variables
    private Sensor rotationVectorSensor;
    private int[]  rotationTriggerValue = {-45, 45, -45, 70};
    private long   lastRotationDetected = 0;
    private long   detectRotateEvery = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeUI();
        initializeSeekbar();
        initializePlaybackController();
        Log.d(TAG, "onCreate: finished");

        /*sensor code*/
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPlayerAdapter.loadMedia(MEDIA_RES_ID);
        Log.d(TAG, "onStart: create MediaPlayer");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isChangingConfigurations() && mPlayerAdapter.isPlaying()) {
            Log.d(TAG, "onStop: don't release MediaPlayer as screen is rotating & playing");
        } else {
            mPlayerAdapter.release();
            Log.d(TAG, "onStop: release MediaPlayer");
        }
    }

    private void initializeUI() {
        mTextDebug = (TextView) findViewById(R.id.text_debug);
        Button mPlayButton = (Button) findViewById(R.id.button_play);
        Button mPauseButton = (Button) findViewById(R.id.button_pause);
        Button mResetButton = (Button) findViewById(R.id.button_reset);
        mSeekbarAudio = (SeekBar) findViewById(R.id.seekbar_audio);
        mScrollContainer = (ScrollView) findViewById(R.id.scroll_container);

        mPauseButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.pause();
                        playing = false;
                    }
                });
        mPlayButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.play();
                        playing = true;
                    }
                });
        mResetButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.reset();
                        playing = false;
                    }
                });
    }

    private void initializePlaybackController() {
        MediaPlayerHolder mMediaPlayerHolder = new MediaPlayerHolder(this);
        Log.d(TAG, "initializePlaybackController: created MediaPlayerHolder");
        mMediaPlayerHolder.setPlaybackInfoListener(new PlaybackListener());
        mPlayerAdapter = mMediaPlayerHolder;
        Log.d(TAG, "initializePlaybackController: MediaPlayerHolder progress callback set");
    }

    private void initializeSeekbar() {
        mSeekbarAudio.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    int userSelectedPosition = 0;

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = true;
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            userSelectedPosition = progress;
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = false;
                        mPlayerAdapter.seekTo(userSelectedPosition);
                    }
                });
    }

    public class PlaybackListener extends PlaybackInfoListener {

        @Override
        public void onDurationChanged(int duration) {
            mSeekbarAudio.setMax(duration);
            Log.d(TAG, String.format("setPlaybackDuration: setMax(%d)", duration));
        }

        @Override
        public void onPositionChanged(int position) {
            if (!mUserIsSeeking) {
                mSeekbarAudio.setProgress(position, true);
                Log.d(TAG, String.format("setPlaybackPosition: setProgress(%d)", position));
            }
        }

        @Override
        public void onStateChanged(@State int state) {
            String stateToString = PlaybackInfoListener.convertStateToString(state);
            onLogUpdated(String.format("onStateChanged(%s)", stateToString));
        }

        @Override
        public void onPlaybackCompleted() {
        }

        @Override
        public void onLogUpdated(String message) {
            if (mTextDebug != null) {
                mTextDebug.append(message);
                mTextDebug.append("\n");
                // Moves the scrollContainer focus to the end.
                mScrollContainer.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                mScrollContainer.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        });
            }
        }
    }

//sensor code

    @Override
    public void onSensorChanged(SensorEvent event) {

        Sensor sensor = event.sensor;

        if(sensor.getType() == Sensor.TYPE_PROXIMITY){

            if( (System.currentTimeMillis() - lastTapGestureTime) >= tapGestureDuration  )
            {
                lastTapGestureTime = System.currentTimeMillis();
                numOfTapGesturesDetected = 0;
                lastProximitySensorValue = event.values[0];
            }
            else if((event.values[0] != lastProximitySensorValue ) ) {

                numOfTapGesturesDetected++;

                Log.d("Log", "Single Tap ");
                if (numOfTapGesturesDetected == numOfTapsNeeded)
                {

                    Log.d("Log", "Double Tap ");
                    lastProximitySensorValue = System.currentTimeMillis();
                    numOfTapGesturesDetected = 0;

                    if(!playing) {
                        playing = true;
                        mPlayerAdapter.play();
                    }else
                    {

                        playing = false;
                        mPlayerAdapter.pause();
                    }
                }
            }
        }

        else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER ) {

            long currentTime = System.currentTimeMillis();
            //reduce polling rate to 2 per second
            if ((currentTime - lastShakeGestureTime) > shakeGestureDuration) {

                long shakeTime = (currentTime - lastShakeGestureTime);
                lastShakeGestureTime = currentTime;

                currentAccelerometerValues[0] = event.values[0];
                currentAccelerometerValues[1] = event.values[1];
                currentAccelerometerValues[2] = event.values[2];

                float speed = Math.abs(currentAccelerometerValues[0]+ currentAccelerometerValues[1]+  currentAccelerometerValues[2] - prevAccelerometerValues[0] - prevAccelerometerValues[1] - prevAccelerometerValues[2]) / shakeTime * 10000;

                if (speed > shakeGestureThreshold && (currentTime-lastShakeGestureDetectedTime)>detectShakeEvery) {


                    Log.d("Log", "Shake detected w/ speed: " + speed );

                    mPlayerAdapter.reset();
                    playing = false;

                    lastShakeGestureDetectedTime = currentTime;
                }

                prevAccelerometerValues[0] = currentAccelerometerValues[0];
                prevAccelerometerValues[1] = currentAccelerometerValues[1];
                prevAccelerometerValues[2] = currentAccelerometerValues[2];
            }
        }
        else if (sensor.getType() == Sensor.TYPE_ROTATION_VECTOR ) {

            float[] rotationMatrix = new float[16];
            SensorManager.getRotationMatrixFromVector(
                    rotationMatrix, event.values);

            // Remap coordinate system
            float[] remappedRotationMatrix = new float[16];
            SensorManager.remapCoordinateSystem(rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Z,
                    remappedRotationMatrix);

            // Convert to orientations
            float[] orientations = new float[3];
            SensorManager.getOrientation(remappedRotationMatrix, orientations);

            for (int i = 0; i < 3; i++) {
                orientations[i] = (float) (Math.toDegrees(orientations[i]));
            }

            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastRotationDetected) > detectRotateEvery) {

                //rotate forward
                if (orientations[1] < rotationTriggerValue[2]) {

                    if (curr == 1) {
                        mPlayerAdapter.reset();
                        mPlayerAdapter.release();
                        mPlayerAdapter.loadMedia(MEDIA_RES_ID2);
                        mPlayerAdapter.play();
                        lastRotationDetected = currentTime;
                        curr=2;
                    }
                }
                //rotate back
                else if (orientations[1] > rotationTriggerValue[3]) {

                    if (curr == 2) {
                        mPlayerAdapter.reset();
                        mPlayerAdapter.release();
                        mPlayerAdapter.loadMedia(MEDIA_RES_ID);
                        mPlayerAdapter.play();
                        lastRotationDetected = currentTime;
                        curr =1;
                    }

                    lastRotationDetected = currentTime;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {

        super.onResume();
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onPause(){

        super.onPause();
        sensorManager.unregisterListener(this);
    }

}