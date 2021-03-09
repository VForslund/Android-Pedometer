package victor.pedometer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.util.Locale;

import victor.pedometer.util.Logger;
import victor.pedometer.util.Util;

import static victor.pedometer.Activity_Main.DEFAULT_GOAL;

public class Activity_SplitStep extends AppCompatActivity implements SensorEventListener {
    private int todayOffset, since_boot, new_steps, old_steps, split_steps;
    private TextView split_stepsView;
    public final static NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splitstep);
        updateData();

        SharedPreferences prefs =
                getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        old_steps = prefs.getInt("old_steps", 0);

        split_stepsView = (TextView) findViewById(R.id.split_steps);

        new_steps = Math.max(todayOffset + since_boot, 0);
        split_steps = (new_steps - old_steps);

        split_stepsView.setText(formatter.format(split_steps));
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.settings:
                Intent intent = new Intent(this, Activity_Settings.class);
                startActivity(intent);
                break;
            case R.id.about:
                Intent intent2 = new Intent(this, Activity_About.class);
                startActivity(intent2);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    public void backToMain(View view) {
        finish();
    }
    private void updateData(){
        Database db = Database.getInstance(this);
        since_boot = db.getCurrentSteps();
        todayOffset = db.getSteps(Util.getToday());
        db.close();
    }



    public void Split(View view) {
        updateData();


        old_steps = Math.max(todayOffset + since_boot, 0);

        SharedPreferences prefs =
                getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("old_steps", old_steps);
        editor.commit();

        split_stepsView.setText(formatter.format(0));

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        updateData();
        new_steps = Math.max(todayOffset + since_boot, 0);
        SharedPreferences prefs =
                getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        old_steps = prefs.getInt("old_steps", 0);


        split_steps = (new_steps - old_steps);
        split_stepsView.setText(formatter.format(split_steps));

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onResume() {
        super.onResume();


        Database db = Database.getInstance(this);

        if (BuildConfig.DEBUG) db.logState();
        // read todays offset
        todayOffset = db.getSteps(Util.getToday());

        SharedPreferences prefs =
                getSharedPreferences("pedometer", Context.MODE_PRIVATE);

        since_boot = db.getCurrentSteps();
        int pauseDifference = since_boot - prefs.getInt("pauseCount", since_boot);

        // register a sensorlistener to live update the UI if a step is taken
        SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (sensor == null) {
            new AlertDialog.Builder(this).setTitle(R.string.no_sensor)
                    .setMessage(R.string.no_sensor_explain)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(final DialogInterface dialogInterface) {
                            finish();
                        }
                    }).setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).create().show();
        } else {
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 0);
        }

        since_boot -= pauseDifference;
        db.close();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            SensorManager sm =
                    (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            sm.unregisterListener(this);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Logger.log(e);
        }
        Database db = Database.getInstance(this);
        db.saveCurrentSteps(since_boot);
        db.close();
    }
}