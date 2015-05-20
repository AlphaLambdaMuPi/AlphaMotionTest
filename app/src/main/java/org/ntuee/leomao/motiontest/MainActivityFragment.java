package org.ntuee.leomao.motiontest;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private SensorManager smanager;
    private Sensor ameter;
    private Sensor gyroscope;
//    private Sensor gmeter;
//    private Sensor mmeter;

    private Camera camera = null;
    private SurfaceTexture prevsurf;

    private TextureView campreview;

    private View rootview;
    private TextView cnttext = null;

    private Toast toast;

    private Button startbtn;
    private Button stopbtn;

    private EditText hosttext;
    private EditText porttext;

//    private double alpha = 0.2;
//    private float [] acc = new float[3];
//    private float [] omega = new float[3];
//    private float [] orientation = new float[3];
//    private float [] gravity = new float[3];
//    private float [] magnetic = new float[3];
//    private float [] rotationMatrix = new float[16];
//    private long lastacctime = 0;
    private int datacnt = 0;

    private OutputStreamWriter writer = null;
    private InputStreamReader reader = null;
    private HandlerThread socketThread;
    private HandlerThread cameraThread;
    private Handler UIHandler = new Handler();
    private Handler cameraHandler;
    private Handler socketHandler;
    private Socket socket = null;
    private boolean connected = false;
    private boolean started = false;

    private String datahost = "";
    private int port = 0;

//    private final int DELTA_T = 5;

    private SensorEventListener ahandler = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (started) {
                socketHandler.post(new DataSender("accel", getFloatData(event.values),
                        event.timestamp));
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private SensorEventListener rhandler = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (started) {
                socketHandler.post(new DataSender("gyroscope", getFloatData(event.values),
                        event.timestamp));
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

//    private SensorEventListener ghandler = new SensorEventListener() {
//        @Override
//        public void onSensorChanged(SensorEvent event) {
//            if (started) {
//
//            }
//            lastacctime = event.timestamp;
//            System.arraycopy(event.values, 0, gravity, 0, 3);
//            getOrientation();
//        }
//
//        @Override
//        public void onAccuracyChanged(Sensor sensor, int accuracy) {
//
//        }
//    };
//
//    private SensorEventListener mhandler = new SensorEventListener() {
//        @Override
//        public void onSensorChanged(SensorEvent event) {
//            lastacctime = event.timestamp;
//            System.arraycopy(event.values, 0, magnetic, 0, 3);
//            getOrientation();
//        }
//
//        @Override
//        public void onAccuracyChanged(Sensor sensor, int accuracy) {
//
//        }
//    };
//
//    private void getOrientation() {
//        SensorManager.getRotationMatrix(rotationMatrix, null, gravity, magnetic);
//        SensorManager.getOrientation(rotationMatrix, orientation);
//    }

    private TextureView.SurfaceTextureListener camhandler = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d("DEBUG", "texture done");
            initCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            camera.release();
            camera = null;
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private Runnable starter = new Runnable() {
        @Override
        public void run() {
            if (datahost.equals("") || port == 0)
                return;
            try {
                socket = new Socket(datahost, port);
                writer = new OutputStreamWriter(socket.getOutputStream());
                reader = new InputStreamReader(socket.getInputStream());
                if (camera != null) {
                    camera.setPreviewCallback(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] data, Camera camera) {
                            long now = System.currentTimeMillis();
                            if (now - cameratime < 500)
                                return;
                            cameratime = now;
                            String imgstr = Base64.encodeToString(data, Base64.NO_WRAP);
                            Log.d("DEBUG", "data length: " + imgstr.length());
                            //Log.d("DEBUG", "data: " + imgstr);
                            for (int i = 0; i < (imgstr.length()-1)/8192+1; i++) {
                                int end = Math.min(imgstr.length(), (i + 1) * 8192);
                                socketHandler.post(new DataSender("camera", imgstr.substring(i * 8192, end), cameratime));
                            }
                            //socketHandler.post(stoper);
                        }
                    });
                    camera.startPreview();
                }
                connected = true;
                started = true;
                Log.d("DEBUG", "connected");
            }
            catch (Exception e) {
                connected = false;
                started = false;
                Log.d("DEBUG", "connection failed");
                Log.d("DEBUG", e.getMessage());
                return;
            }
            datacnt = 0;
            JSONObject data = new JSONObject();
            try {
                data.put("name", android.os.Build.MODEL);
                String s = data.toString() + "\n";
                writer.write(s);
            }
            catch (JSONException e) {
                Log.d("DEBUG", "JSON object error");
            }
            catch (IOException e) {
                Log.d("DEBUG", "socket write error");
                if (socket.isInputShutdown())
                    socketHandler.post(stoper);
            }
        }
    };

//    private Runnable updater = new Runnable() {
//        @Override
//        public void run() {
//            if (socket == null || socket.isClosed())
//                return;
//
//            if (datacnt >= 5000) {
//                socketHandler.post(stoper);
//                return;
//            }
//
//            getOrientation();
//            datacnt += 1;
//            UIHandler.post(updateUI);
//            if (started)
//                socketHandler.postDelayed(updater, DELTA_T);
//        }
//    };

    private Runnable stoper = new Runnable() {
        @Override
        public void run() {
            if (camera != null)
                camera.stopPreview();
            started = false;
//            socketHandler.removeCallbacks(updater);
            JSONObject data = new JSONObject();
            try {
                data.put("stop", true);
                String s = data.toString() + "\n";
                writer.write(s);
                writer.flush();
                socket.close();
            }
            catch (JSONException e) {
                Log.d("DEBUG", "JSON object error");
            }
            catch (IOException e) {
                Log.d("DEBUG", "socket was already closed");
            }
            socket = null;
        }
    };

    private class DataSender implements Runnable {
        private String type;
        private String data = "";
        private long time;
        public DataSender(String type, String data, long time) {
            this.type = type;
            this.time = time;
            this.data = data;
        }

        @Override
        public void run() {
            if (socket == null || socket.isClosed() || !started)
                return;

            JSONObject data = new JSONObject();
            try {
                data.put("time", this.time);
                data.put("data", this.data);
                data.put("type", this.type);
                String s = data.toString() + "\n";
                writer.write(s);
                datacnt += 1;
                if (this.type.equals("camera"))
                    Log.d("DEBUG", "camera data sent");
                UIHandler.post(updateUI);
            }
            catch (JSONException e) {
                Log.d("DEBUG", "JSON object error");
            }
            catch (IOException e) {
                Log.d("DEBUG", "socket write error");
                if (socket.isInputShutdown())
                    socketHandler.post(stoper);
            }
        }

    }

    private Runnable updateUI = new Runnable() {
        @Override
        public void run() {
            if (cnttext != null)
                cnttext.setText(String.format("data count: %d", datacnt));
        }
    };

    public MainActivityFragment() {
        socketThread = new HandlerThread("socket");
        socketThread.start();
        socketHandler = new Handler(socketThread.getLooper());
        cameraThread = new HandlerThread("camera");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
        prevsurf = new SurfaceTexture(10);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.fragment_main, container, false);
        cnttext = (TextView) rootview.findViewById(R.id.acctext);
        hosttext = (EditText) rootview.findViewById(R.id.datahost);
        porttext = (EditText) rootview.findViewById(R.id.port);
        startbtn = (Button) rootview.findViewById(R.id.start);
        stopbtn = (Button) rootview.findViewById(R.id.stop);
        campreview = (TextureView) rootview.findViewById(R.id.camView);
        campreview.setSurfaceTextureListener(camhandler);
        startbtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                datahost = hosttext.getText().toString();
                port = Integer.parseInt(porttext.getText().toString());
                socketHandler.post(starter);
                // if (connected) {
                //     started = true;
                // }
                // else {
                //     String message = "connection wasn't established!";
                //     if (toast != null)
                //         toast.cancel();
                //     toast = Toast.makeText(getActivity().getApplicationContext(),
                //             message, Toast.LENGTH_SHORT);
                //     toast.show();
                // }
            }
        });
        stopbtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (started) {
                    socketHandler.post(stoper);
                }
            }
        });
        return rootview;
    }


    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        smanager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        ameter = smanager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroscope = smanager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
//        gmeter = smanager.getDefaultSensor(Sensor.TYPE_GRAVITY);
//        mmeter = smanager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        registerListener();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerListener();
        initCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        unRegisterListener();
        if (camera != null)
            camera.release();
    }

    @Override
    public void onStop() {
        super.onPause();
        unRegisterListener();
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    private void registerListener() {
        smanager.registerListener(ahandler, ameter, SensorManager.SENSOR_DELAY_FASTEST);
        smanager.registerListener(rhandler, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
//        smanager.registerListener(ghandler, gmeter, SensorManager.SENSOR_DELAY_FASTEST);
//        smanager.registerListener(mhandler, mmeter, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void unRegisterListener() {
        smanager.unregisterListener(ahandler);
        smanager.unregisterListener(rhandler);
//        smanager.unregisterListener(ghandler);
//        smanager.unregisterListener(mhandler);
    }


    private String getFloatData(float [] data) {
        String datastr = String.format("[%.4f", data[0]);
        for (int i = 1; i < data.length; i++)
            datastr += String.format(",%.4f", data[i]);
        datastr += "]";
        return datastr;
    }


    private long cameratime = 0;

    private void initCamera() {
        cameraHandler.post(new Runnable() {
            @Override
            public void run() {
                if (camera != null) {
                    camera.release();
                    camera = null;
                }
                camera = Camera.open();
                if (camera != null) {
                    try {
                        Camera.Parameters p = camera.getParameters();
//                        p.setPreviewFormat(ImageFormat.JPEG);
//                        ArrayList<Integer> ssf = (ArrayList<Integer>) p.getSupportedPreviewFormats();
//                        String loggg = "";
//                        for (int i = 0; i < ssf.size(); i++)
//                            loggg += ssf.get(i) + " ";
//                        Log.d("DEUBG", "formats: " + loggg);
                        p.setPreviewSize(640, 480);
                        p.set("orientation", "portrait");
                        camera.setParameters(p);
                        camera.setPreviewTexture(campreview.getSurfaceTexture());
                    }
                    catch (IOException e) {
                        Log.d("DEBUG", "camera concon");
                        camera.release();
                        camera = null;
                    }
                }
                else {
                    Log.d("DEBUG", "can't connect camera");
                }
            }
        });
    }


}
