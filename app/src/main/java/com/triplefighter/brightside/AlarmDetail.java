package com.triplefighter.brightside;

import android.app.AlarmManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.philips.lighting.hue.listener.PHScheduleListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.utilities.PHDateTimePattern;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import com.philips.lighting.model.PHSchedule;
import com.triplefighter.brightside.data.ScheduleListAdapter;
import com.triplefighter.brightside.data.SpinnerListAdapter;

import static junit.runner.Version.id;

public class AlarmDetail extends AppCompatActivity {

    private PHHueSDK sdk;
    private PHBridge bridge;
    private PHLight light;
    private PHLightState state;
    private PHSchedule phSchedule;

    TimePicker time_pick;
    AlarmManager alarmManager;
    private Calendar calendar;
    Spinner lamp_name_spinner;
    TextView lamp_name_view;
    Switch repeat_alarm;
    Switch condition;
    Button submit_alarm;
    EditText alarm_name;
    CheckBox monday,tuesday,wednesday,thursday,friday,saturday,sunday;

    String choosen;
    int jam,menit;

    String idAlarm, namaAlarm;
    String nama;

    List<PHLight> lamp_name_arr;

    int a = 0;
    int lastPos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_detail);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sdk = PHHueSDK.create();
        bridge = sdk.getInstance().getSelectedBridge();

        if(bridge == null){
            AlertDialogWizard.showErrorDialog(this, "No Bridge Found", R.string.btn_ok);
        }else {
            Intent in = getIntent();
            idAlarm = in.getStringExtra("idAlarm");
            namaAlarm = in.getStringExtra("namaAlarm");

            if(idAlarm == null){
                getSupportActionBar().setTitle(getText(R.string.set_timer_title));
                invalidateOptionsMenu();
            }else {
                getSupportActionBar().setTitle(getText(R.string.delete_alarm_menu));
            }

            calendar = Calendar.getInstance();

            lastPos = MainActivity.halaman;

            time_pick=(TimePicker) findViewById(R.id.time_pick);
            lamp_name_view=(TextView) findViewById(R.id.lamp_name_view);
            lamp_name_spinner=(Spinner) findViewById(R.id.lamp_name);
            repeat_alarm=(Switch) findViewById(R.id.repeat_alarm);
            condition=(Switch) findViewById(R.id.lamp_condition);
            submit_alarm=(Button) findViewById(R.id.submit_alarm);
            monday=(CheckBox) findViewById(R.id.monday);
            tuesday=(CheckBox) findViewById(R.id.tuesday);
            wednesday=(CheckBox) findViewById(R.id.wednesday);
            thursday=(CheckBox) findViewById(R.id.thursday);
            friday=(CheckBox) findViewById(R.id.friday);
            saturday=(CheckBox) findViewById(R.id.saturday);
            sunday=(CheckBox) findViewById(R.id.sunday);
            submit_alarm=(Button) findViewById(R.id.submit_alarm);
            alarm_name = (EditText) findViewById(R.id.alarm_name);

            if(idAlarm != null){
                alarm_name.setText(namaAlarm);
                submit_alarm.setVisibility(View.INVISIBLE);
            }

            condition.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(condition.isChecked()){
                        condition.setText("On");
                    }else if(!condition.isChecked()){
                        condition.setText("Off");
                    }
                }
            });

            repeat_alarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if(b){
                        monday.setChecked(true);
                        tuesday.setChecked(true);
                        wednesday.setChecked(true);
                        thursday.setChecked(true);
                        friday.setChecked(true);
                        saturday.setChecked(true);
                        sunday.setChecked(true);
                        repeat_alarm.setText("On");
                    }else {
                        monday.setChecked(false);
                        tuesday.setChecked(false);
                        wednesday.setChecked(false);
                        thursday.setChecked(false);
                        friday.setChecked(false);
                        saturday.setChecked(false);
                        sunday.setChecked(false);
                        repeat_alarm.setText("Off");
                    }
                }
            });

            lamp_name_arr = bridge.getResourceCache().getAllLights();

            if(lamp_name_arr.isEmpty()){
                SpinnerListAdapter adapter = new SpinnerListAdapter(this,null);
                lamp_name_spinner.setAdapter(adapter);
            }else {
                SpinnerListAdapter adapter = new SpinnerListAdapter(this,lamp_name_arr);
                lamp_name_spinner.setAdapter(adapter);
                lamp_name_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        light = lamp_name_arr.get(i);
                        choosen=light.getIdentifier();
                        nama = light.getName();
                        //lamp_name_view.setText(choosen);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            }

            submit_alarm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    namaAlarm = alarm_name.getText().toString().trim();
                    if(TextUtils.isEmpty(namaAlarm)){
                        alarm_name.setError(getText(R.string.timer_name));
                    }else {
                        setAlarm();
                    }
                }
            });
        }
    }

    public void setAlarm(){
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        int cur_hour=calendar.get(Calendar.HOUR);
        int cur_minute=calendar.get(Calendar.MINUTE);
        if(currentApiVersion>android.os.Build.VERSION_CODES.LOLLIPOP_MR1){
            calendar.set(Calendar.HOUR_OF_DAY,time_pick.getHour());
            calendar.set(Calendar.MINUTE,time_pick.getMinute());
            calendar.set(Calendar.SECOND, 0);
            jam=time_pick.getHour();
            menit=time_pick.getMinute();
            if(jam<cur_hour && menit<cur_minute){
                calendar.add(Calendar.DATE,1);
                Log.d("def","besok2");
                if(time_pick.getMinute()<10){
                    addSchedule();
                    Toast.makeText(AlarmDetail.this,nama +" " +R.string.alarm_created_notif +" " +jam+":0"+menit,Toast.LENGTH_SHORT).show();
                }else{
                    addSchedule();
                    Toast.makeText(AlarmDetail.this,nama +" " +R.string.alarm_created_notif +" " +jam+":"+menit,Toast.LENGTH_SHORT).show();
                }
            }else{
                Log.d("asd","besok");
                if(time_pick.getMinute()<10){
                    addSchedule();
                    Toast.makeText(AlarmDetail.this,nama +" " +R.string.alarm_created_notif +" " +jam+":0"+menit,Toast.LENGTH_SHORT).show();
                }else{
                    addSchedule();
                    Toast.makeText(AlarmDetail.this,nama +" " +R.string.alarm_created_notif +" " +jam+":"+menit,Toast.LENGTH_SHORT).show();
                }
            }
        }else {
            calendar.set(Calendar.HOUR_OF_DAY,time_pick.getCurrentHour());
            calendar.set(Calendar.MINUTE,time_pick.getCurrentMinute());
            calendar.set(Calendar.SECOND, 0);
            jam=time_pick.getCurrentHour();
            menit=time_pick.getCurrentMinute();
            if(jam<=cur_hour && menit<=cur_minute){
                calendar.set(Calendar.DATE,1);
                if(time_pick.getCurrentMinute()<10){
                    addSchedule();
                    Toast.makeText(AlarmDetail.this,nama +" " +getText(R.string.alarm_created_notif) +" " +jam+":0"+menit,Toast.LENGTH_SHORT).show();
                }else{
                    addSchedule();
                    Toast.makeText(AlarmDetail.this,nama +" " +getText(R.string.alarm_created_notif) +" " +jam+":"+menit,Toast.LENGTH_SHORT).show();
                }
            }else{
                if(time_pick.getCurrentMinute()<10){
                    addSchedule();
                    Toast.makeText(AlarmDetail.this,nama +" " +getText(R.string.alarm_created_notif) +" " +jam+":0"+menit,Toast.LENGTH_SHORT).show();
                }else{
                    addSchedule();
                    Toast.makeText(AlarmDetail.this,nama +" " +getText(R.string.alarm_created_notif) +" " +jam+":"+menit,Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void addSchedule(){
        phSchedule = new PHSchedule(namaAlarm);
        state = new PHLightState();

        if(repeat_alarm.isChecked()){
            //phSchedule.setRecurringDays(PHSchedule.RecurringDay.RECURRING_ALL_DAY.getValue());
            a = 127;
            Log.d("selected","day : All " +a);
        }else{
            if(monday.isChecked()){
                a = 64;
                Log.d("selected","day : Monday " +a);
            }else if(tuesday.isChecked()){
                a = 32;
                Log.d("selected","day : Tuesday " +a);
            }
            else if(wednesday.isChecked()){
                a = 16;
                Log.d("selected","day : Wednesday " +a);
            }
            else if(thursday.isChecked()){
                a = 8;
                Log.d("selected","day : Thursday " +a);
            }
            else if(friday.isChecked()){
                a = 4;
                Log.d("selected","day : Friday " +a);
            }
            else if(saturday.isChecked()){
                a = 2;
                Log.d("selected","day : Saturday " +a);
            }
            else if(sunday.isChecked()){
                a = 1;
                Log.d("selected","day : Sunday " +a);
            }
        }

        if(condition.isChecked()){
            condition.setText("On");
            state.setOn(true);
            Log.d("selected","On");
        }else {
            condition.setText("Off");
            state.setOn(false);
            Log.d("selected","Off");
        }

        phSchedule.setStatus(PHSchedule.PHScheduleStatus.ENABLED);
        phSchedule.setLightState(state);
        phSchedule.setLightIdentifier(choosen);
        phSchedule.setRecurringDays(a);
        phSchedule.setLocalTime(true);
        phSchedule.setDate(calendar.getTime());

        if(idAlarm == null){
            bridge.createSchedule(phSchedule,listener);
        }else if(idAlarm != null) {
            bridge.updateSchedule(phSchedule,listener);
        }

        Log.d("selected","jam " +calendar.getTime());
        Log.d("selected","idLampu " +choosen);

        //finish();
        Intent in = new Intent(this,MainActivity.class);
        in.putExtra("halaman",lastPos);
        startActivity(in);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_alarm_detail, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if(idAlarm == null){
            MenuItem menuItem = menu.findItem(R.id.delete_alarm);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.delete_alarm){
            showDeleteConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void deleteAlarm(){
        if(idAlarm != null){
            bridge.removeSchedule(idAlarm,listener);
        }

        Intent in = new Intent(this,MainActivity.class);
        in.putExtra("halaman",1);
        startActivity(in);
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_alarm_confirm);
        builder.setPositiveButton(getText(R.string.delete_button), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteAlarm();
            }
        });
        builder.setNegativeButton(getText(R.string.cancel_button), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    PHScheduleListener listener = new PHScheduleListener() {
        @Override
        public void onCreated(PHSchedule phSchedule) {
            Log.d("onCreated","alarm has been created");
        }

        @Override
        public void onSuccess() {
            Log.d("onSuccess","alarm has been created");
        }

        @Override
        public void onError(int i, String s) {

        }

        @Override
        public void onStateUpdate(Map<String, String> map, List<PHHueError> list) {

        }
    };
}
