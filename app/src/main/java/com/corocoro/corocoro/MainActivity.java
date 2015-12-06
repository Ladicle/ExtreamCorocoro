package com.corocoro.corocoro;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.view.animation.CycleInterpolator;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;

public class MainActivity extends Activity implements NfcAdapter.ReaderCallback { //SensorEventListener,

    private final static String EOL = "\r\n";
    final int SOUND_POOL_MAX = 6;

    //private SensorManager mManager;
    private NfcAdapter mNfcAdapter;
    private WebSocketClient mSocket;
    private RelativeLayout mBackground;
    private static final Handler mHandler = new Handler();
    private SoundPool mSound;
    private int mSoundId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep Screen On
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Get background view
        mBackground = (RelativeLayout) findViewById(R.id.background);

        // Audio volume
        setupAudio();

        // Setup sensor manager
        //mManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Setup NFC adaptor
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Load Sound
        mSound = buildSoundPool(SOUND_POOL_MAX);
        mSoundId = mSound.load(this, R.raw.coin_1, 0);

        // Enable WebSocket
        setupSocket(Context.SOCKET_URL);
        connect2WebSocket();

        // Enable NFC Reader
        mNfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A, null);

        // Enable Sensor
        //List<Sensor> sensors = mManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        //if (sensors.size() > 0) {
        //    Sensor s = sensors.get(0);
        //    mManager.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
        //}
    }

    @Override
    protected void onStop() {
        super.onStop();
        //mManager.unregisterListener(this);
        mSound.release();
        mSocket.close();
    }

//    @Override
//    public void onSensorChanged(SensorEvent event) {
//        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ) {
//            float x = event.values[0];
//            float y = event.values[1];
//            float z = event.values[2];
//
//            // Send to WebSocket Server
//            try {
//                mSocket.send(x + "," + y + "z");
//            } catch (NotYetConnectedException e) {
//                Toast.makeText(this, "まだ接続できてないですね", Toast.LENGTH_LONG);
//            }
//        }
//    }

//    @Override
//    public void onAccuracyChanged(Sensor sensor, int accuracy) {
//    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        final StringBuffer buff = new StringBuffer();
        byte[] arr = tag.getId();
        int i = 0;
        int size = arr.length;
        for (i=0; i<size; ++i) {
            buff.append(Integer.toHexString(arr[i]));
        }

        // Send to WebSocket Server
        try {
            mSocket.send(buff.toString());
        } catch (NotYetConnectedException e) {
            Toast.makeText(this, "まだ接続できてないですね", Toast.LENGTH_LONG);
        }

        mSound.play(mSoundId, 1.0F, 1.0F, 0, 0, 1.0F);

        // Flash Animation
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                ValueAnimator aview = ValueAnimator.ofObject(new ArgbEvaluator(), 0xFF000000, 0xFFFFFFFF);
                aview.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mBackground.setBackgroundColor((Integer) animation.getAnimatedValue());
                    }
                });

                aview.setDuration(500);
                aview.setInterpolator(new CycleInterpolator(1));
                aview.start();
            }
        });
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private SoundPool buildSoundPool(int poolMax)
    {
        SoundPool pool = null;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            pool = new SoundPool(poolMax, AudioManager.STREAM_MUSIC, 0);
        }
        else {
            AudioAttributes attr = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            pool = new SoundPool.Builder()
                    .setAudioAttributes(attr)
                    .setMaxStreams(poolMax)
                    .build();
        }

        return pool;
    }

    private void setupAudio() {
        AudioManager manager = (AudioManager)getSystemService(AUDIO_SERVICE);
        int maxVol = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0);
    }

    private void setupSocket(String URL) {
        try {
            mSocket = new CoroWebSocket(new URI(URL));
        } catch (URISyntaxException e) {
            Toast.makeText(this, "WebSocketのURLが間違っています", Toast.LENGTH_LONG);
            e.printStackTrace();
        }
    }

    private void connect2WebSocket() {
        try {
            mSocket.connect();
            Toast.makeText(this, "Connected WebSocket Server!", Toast.LENGTH_LONG);
        } catch (IllegalStateException e) {
            Toast.makeText(this, "WebSocketServerに接続できませんでした。\nNodeRed動いてます？\n1s後にリトライします", Toast.LENGTH_LONG);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.fillInStackTrace();
            }
            connect2WebSocket();
        }
    }
}
