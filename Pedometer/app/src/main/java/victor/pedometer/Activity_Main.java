package victor.pedometer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.PieModel;

import victor.pedometer.Database;
import victor.pedometer.R;
import victor.pedometer.SensorListener;
import victor.pedometer.util.API26Wrapper;
import victor.pedometer.util.Logger;
import victor.pedometer.util.Util;
import java.text.NumberFormat;
import java.util.Locale;

import static victor.pedometer.Activity_Graph.DEFAULT_STEP_SIZE;

public class Activity_Main extends AppCompatActivity implements SensorEventListener {

    private int todayOffset;
    private int total_start;
    private static int goal;
    private final static long MICROSECONDS_IN_ONE_MINUTE = 60000000;
    private int since_boot;
    private int total_days;

    private static int Old_steps;
    static int DEFAULT_GOAL = 10000;
    final static float DEFAULT_STEP_SIZE = Locale.getDefault() == Locale.US ? 2.5f : 75f;
    final static String DEFAULT_STEP_UNIT = Locale.getDefault() == Locale.US ? "ft" : "cm";
    private PieModel sliceGoal, sliceCurrent;
    private TextView stepsView;
    private TextView totalView;
    private TextView averageView;
    private static TextView goalView;
    public final static NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
    private SensorManager sm;
    private boolean showSteps = true;
    private PieChart pg;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stepsView = (TextView) findViewById(R.id.steps);
        totalView = (TextView) findViewById(R.id.total);
        SharedPreferences prefs =
                getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        DEFAULT_GOAL  = prefs.getInt("goal", DEFAULT_GOAL);
        averageView = (TextView) findViewById(R.id.average);
        goalView = (TextView) findViewById(R.id.goal);
        pg = (PieChart) findViewById(R.id.graph);
        sliceCurrent = new PieModel("", 3000, Color.parseColor("#99CC00"));
        sliceGoal = new PieModel("", DEFAULT_GOAL, Color.parseColor("#2d2d2d"));
        pg.addPieSlice(sliceCurrent);
        pg.addPieSlice(sliceGoal);
        pg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                showSteps = !showSteps;
                stepsDistanceChanged();
            }
        });
        pg.setDrawValueInPie(false);
        pg.setUsePieRotation(true);
        pg.startAnimation();
        if (Build.VERSION.SDK_INT >= 26) {
            API26Wrapper.startForegroundService(this,
                    new Intent(this, SensorListener.class));
        } else {
            startService(new Intent(this, SensorListener.class));
        }
        updatePie();
    }





    private void stepsDistanceChanged() {
        if (showSteps) {
            ((TextView) findViewById(R.id.unit)).setText(getString(R.string.steps));
        } else {
            String unit = getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                    .getString("stepsize_unit", DEFAULT_STEP_UNIT);
            if (unit.equals("cm")) {
                unit = "km";
            } else {
                unit = "mi";
            }
            ((TextView) findViewById(R.id.unit)).setText(unit);
        }

        updatePie();

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



    private void updateData(){
        goal = DEFAULT_GOAL;
        Database db = Database.getInstance(this);
        since_boot = db.getCurrentSteps();
        todayOffset = db.getSteps(Util.getToday());
        total_start = db.getTotalWithoutToday();
        total_days = db.getDays();
        db.close();
    }

    private void updatePie() {
        if (BuildConfig.DEBUG) Logger.log("UI - update steps: " + since_boot);
        // todayOffset might still be Integer.MIN_VALUE on first start

        updateData();
        int steps_today = Math.max(todayOffset + since_boot, 0);
        sliceCurrent.setValue(steps_today);
        if (goal - steps_today > 0) {
            // goal not reached yet
            if (pg.getData().size() == 1) {
                // can happen if the goal value was changed: old goal value was
                // reached but now there are some steps missing for the new goal
                pg.addPieSlice(sliceGoal);
            }
            sliceGoal.setValue(goal - steps_today);
        } else {
            // goal reached
            pg.clearChart();
            pg.addPieSlice(sliceCurrent);
        }
        pg.update();
        if (showSteps) {
            stepsView.setText(formatter.format(steps_today));
            totalView.setText(formatter.format(total_start + steps_today));
            averageView.setText(formatter.format((total_start + steps_today) / total_days));
            goalView.setText(formatter.format(goal));
        } else {
            // update only every 10 steps when displaying distance
            SharedPreferences prefs =
                    getSharedPreferences("pedometer", Context.MODE_PRIVATE);
            float stepsize = prefs.getFloat("stepsize_value", DEFAULT_STEP_SIZE);
            float distance_today = steps_today * stepsize;
            float distance_total = (total_start + steps_today) * stepsize;
            if (prefs.getString("stepsize_unit", DEFAULT_STEP_UNIT)
                    .equals("cm")) {
                distance_today /= 100000;
                distance_total /= 100000;
            } else {
                distance_today /= 5280;
                distance_total /= 5280;
            }
            stepsView.setText(formatter.format(distance_today));
            totalView.setText(formatter.format(distance_total));
            averageView.setText(formatter.format(distance_total / total_days));
            goalView.setText(formatter.format(goal));
        }
    }

    public void updateMain(){

        SharedPreferences prefs =
                getSharedPreferences("pedometer", Context.MODE_PRIVATE);

        goal = prefs.getInt("goal", DEFAULT_GOAL);
        goalView.setText(formatter.format(goal));
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (BuildConfig.DEBUG) Logger.log(
                "UI - sensorChanged | todayOffset: " + todayOffset + " since boot: " +
                        event.values[0]);
        if (event.values[0] > Integer.MAX_VALUE || event.values[0] == 0) {
            return;
        }
        if (todayOffset == Integer.MIN_VALUE) {
            // no values for today
            // we dont know when the reboot was, so set todays steps to 0 by
            // initializing them with -STEPS_SINCE_BOOT
            todayOffset = -(int) event.values[0];
            Database db = Database.getInstance(this);
            db.insertNewDay(Util.getToday(), (int) event.values[0]);
            db.close();
        }
        since_boot = (int) event.values[0];
        updatePie();
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

        goal = prefs.getInt("goal", DEFAULT_GOAL);
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

        total_start = db.getTotalWithoutToday();
        total_days = db.getDays();

        db.close();

        stepsDistanceChanged();
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


    public void toSettings(View view) {
        Intent intent = new Intent(this, Activity_Settings.class);
        startActivity(intent);
    }

    public void toGraph(View view) {
        Intent intent = new Intent(this, Activity_Graph.class);
        startActivity(intent);
    }

    public void toSplitStep(View view) {
        Intent intent = new Intent(this, Activity_SplitStep.class);
        startActivity(intent);
    }


    public static int getOld_steps() {
        return Old_steps;
    }

    public static void setOld_steps(int old_steps) {
        Old_steps = old_steps;
    }
}