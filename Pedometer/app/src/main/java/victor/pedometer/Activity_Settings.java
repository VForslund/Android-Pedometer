package victor.pedometer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.util.Locale;

import static victor.pedometer.Activity_Main.DEFAULT_STEP_SIZE;

public class Activity_Settings extends AppCompatActivity {
    public final static NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
    TextView currentGaol, currentStepLenght;
    private final boolean SWTICH_STATE = false;
    private Switch myswitch;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        currentGaol = findViewById(R.id.currentGoal);
        currentStepLenght = findViewById(R.id.currentStepLenght);
        myswitch = findViewById(R.id.unitSwitch);
        SharedPreferences prefs =
                getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        int goal = prefs.getInt("goal",0);
        boolean sState = prefs.getBoolean("switchstate", SWTICH_STATE);
        float lenght = prefs.getFloat("stepsize_value", DEFAULT_STEP_SIZE);
        myswitch.setChecked(sState);
        currentGaol.setText(formatter.format(goal));
        currentStepLenght.setText(formatter.format(lenght));


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
        Activity_Main am = new Activity_Main();
       am.updateMain();
        finish();
    }

    private void saveGoal(){

        TextView newGoal = findViewById(R.id.goaltext);


        String nGaol = newGoal.getText().toString();
        if (!nGaol.equals("")) {
            int numberGaol = Integer.parseInt(nGaol);

            SharedPreferences prefs =
                    getSharedPreferences("pedometer", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("goal", numberGaol);
            editor.commit();


            int goal = prefs.getInt("goal", 0);

            currentGaol.setText(formatter.format(goal));

        }
    }

    private void saveStepLenght(){
        TextView newStepLenght = findViewById(R.id.steplenght);
        String nstepLenght = newStepLenght.getText().toString();

        if (!nstepLenght.equals("")) {
            int numberStepLenght = Integer.parseInt(nstepLenght);

            SharedPreferences prefs =
                    getSharedPreferences("pedometer", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat("stepsize_value", numberStepLenght);
            editor.commit();


            float lenght = prefs.getFloat("stepsize_value", DEFAULT_STEP_SIZE);
            currentStepLenght.setText(formatter.format(lenght));

        }
    }

    private void saveUnit(){

        myswitch = (Switch) findViewById(R.id.unitSwitch);
        String unit;
        //off is cm , on is inch
       if(myswitch.isChecked()){
           unit = "inch";
       }else{
           unit = "cm";
       }

        SharedPreferences prefs =
                getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("stepsize_unit", unit);
        editor.putBoolean("switchstate", myswitch.isChecked());
        editor.commit();

    }

    public void save(View view) {
        saveGoal();
        saveStepLenght();
        saveUnit();

    }
}