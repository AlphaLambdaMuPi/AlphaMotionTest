package org.ntuee.leomao.motiontest;

import android.content.Context;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private SensorManager smanager;
    private Sensor ameter;
    private Sensor gyroscope;
    private SensorEventListener ahandler;
    private SensorEventListener rhandler;

    private View rootview;
    private TextView aresult;
    private TextView rresult;

    private double alpha = 0.5;
    private double [] acc = {0, 0, 0};

    public MainActivityFragment() {
        ahandler = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                for (int i = 0; i < 3; i++)
                    acc[i] = acc[i] * alpha + event.values[i] * (1 - alpha);
                String r = "(" + String.valueOf(acc[0])
                        + ", " + String.valueOf(acc[1])
                        + ", " + String.valueOf(acc[2]) + ")";
                aresult.setText(r);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        rhandler = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                //String r = "(" + String.valueOf(event.values[0])
                //        + ", " + String.valueOf(event.values[1])
                //        + ", " + String.valueOf(event.values[1]) + ")";
                //rresult.setText(r);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.fragment_main, container, false);
        aresult = (TextView) rootview.findViewById(R.id.acctext);
        return rootview;
    }


    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        smanager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        ameter = smanager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroscope = smanager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        smanager.registerListener(ahandler, ameter, SensorManager.SENSOR_DELAY_FASTEST);
        smanager.registerListener(rhandler, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onResume() {
        super.onResume();
        smanager.registerListener(ahandler, ameter, SensorManager.SENSOR_DELAY_FASTEST);
        smanager.registerListener(rhandler, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onPause() {
        super.onPause();
        smanager.unregisterListener(ahandler);
        smanager.unregisterListener(rhandler);
    }

    @Override
    public void onStop() {
        super.onPause();
        smanager.unregisterListener(ahandler);
        smanager.unregisterListener(rhandler);
    }


}
