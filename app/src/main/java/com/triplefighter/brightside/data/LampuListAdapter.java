package com.triplefighter.brightside.data;

import android.content.Context;
import android.support.annotation.IdRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.philips.lighting.hue.listener.PHScheduleListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import com.philips.lighting.model.PHSchedule;
import com.triplefighter.brightside.MainActivity;
import com.triplefighter.brightside.R;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class LampuListAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private List<PHLight> lampuList;

    private PHLight light;
    private PHLightState state = new PHLightState();
    private PHHueSDK phHueSDK = PHHueSDK.create();
    private PHBridge bridge = phHueSDK.getSelectedBridge();

    private Boolean kondisiLampu;
    private Boolean adaLampu;
    private Boolean status = false;
    private int intensitas;

    class LampuItem{
        private TextView namaLampu;
        private ToggleButton power_but;
        private SeekBar brightness;
        private TextView nama_lampu,alarm_time,brightness_num;
        private ImageView repeat_mode;
        private RadioGroup mode_container;
        private RadioButton eco_mode,night_mode,none_mode;

    }

    public LampuListAdapter(Context context, List<PHLight> lampuList) {
        inflater = LayoutInflater.from(context);
        this.lampuList = lampuList;
    }

    @Override
    public int getCount() {
        return lampuList.size();
    }

    @Override
    public Object getItem(int i) {
        return lampuList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int position, View view, ViewGroup viewGroup) {

        LampuItem item = new LampuItem();

        if (view == null) {
            view = inflater.inflate(R.layout.list_home, null);

            item.namaLampu = (TextView) view.findViewById(R.id.nama_lampu);
            item.power_but=(ToggleButton) view.findViewById(R.id.power_but);
            item.brightness=(SeekBar) view.findViewById(R.id.brightness);
            item.brightness_num = (TextView) view.findViewById(R.id.brightness_num);
            item.alarm_time = (TextView) view.findViewById(R.id.alarm_time);
            item.repeat_mode= (ImageView) view.findViewById(R.id.repeat_mode);
            item.mode_container= (RadioGroup) view.findViewById(R.id.mode_container);
            item.eco_mode= (RadioButton) view.findViewById(R.id.eco_mode);
            item.night_mode= (RadioButton) view.findViewById(R.id.night_mode);
            item.none_mode= (RadioButton) view.findViewById(R.id.none_mode);

            view.setTag(item);
        } else {
            item = (LampuItem) view.getTag();
        }
        light = lampuList.get(position);
        final String lampuId = light.getName();
        item.namaLampu.setText(lampuId);

        kondisiLampu = light.getLastKnownLightState().isOn();
        adaLampu = light.getLastKnownLightState().isReachable();
        intensitas = light.getLastKnownLightState().getBrightness();

        if(kondisiLampu == true){
            item.power_but.setChecked(true);
            item.brightness.setProgress(intensitas);
            int persen = (intensitas*100/254);
            item.brightness_num.setText(persen +"%");
        }else {
            item.power_but.setChecked(false);
            item.brightness.setProgress(0);
            item.brightness_num.setText("0%");
        }

        Log.v("status", String.valueOf(status));


        if (item.night_mode.isChecked()){
            item.night_mode.setAlpha(1);
        }

        item.brightness.setMax(254);
        final LampuItem finalItem = item;
        item.brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                light = lampuList.get(position);
                int persen = (i*100/254);
                finalItem.brightness_num.setText(String.valueOf(persen)+"%");
                state.setBrightness(i);
                bridge.updateLightState(light,state);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        item.power_but.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                light = lampuList.get(position);
                if(b){
                    if(adaLampu == true || kondisiLampu == false){
                        state.setOn(true);
                        bridge.updateLightState(light,state);
                        status = state.isOn();
                        Log.v("coba","status " +status);
                    }
                }else {
                    state.setOn(false);
                    bridge.updateLightState(light,state);
                    status = state.isOn();
                    Log.v("coba","status " +status);
                }

            }
        });

        final LampuItem finalItem1 = item;
        item.mode_container.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                light = lampuList.get(position);
                String id = light.getIdentifier();
                if(i == R.id.night_mode){
                    int bright = 75;
                    finalItem1.brightness.setProgress(bright);
                    int persen = (bright*100/254);
                    finalItem1.brightness_num.setText(persen +"%");
                    finalItem1.brightness.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            return true;
                        }
                    });
                    finalItem1.power_but.setChecked(true);
                    state.setOn(true);
                    state.setBrightness(bright);
                    bridge.updateLightState(light,state);
                }else if(i == R.id.none_mode){
                    finalItem1.brightness.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            return false;
                        }
                    });
                    finalItem1.power_but.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            return false;
                        }
                    });
                    finalItem1.power_but.setChecked(true);
                    state.setOn(true);
                    bridge.updateLightState(light,state);
                }else if(i == R.id.eco_mode){
                    PHSchedule schedule = new PHSchedule(String.valueOf(R.string.eco_mode));
                    Calendar cal = Calendar.getInstance();
                    if(Calendar.HOUR_OF_DAY == 18 && Calendar.HOUR_OF_DAY < 7){
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.HOUR_OF_DAY,18);

                        state.setOn(true);
                        finalItem1.power_but.setChecked(true);
                        finalItem1.power_but.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View view, MotionEvent motionEvent) {
                                return true;
                            }
                        });

                        schedule.setLightState(state);
                        schedule.setLightIdentifier(id);
                        schedule.setLocalTime(true);
                        schedule.setDate(cal.getTime());

                        bridge.createSchedule(schedule,listener);
                    }
                    if(Calendar.HOUR_OF_DAY == 6 && Calendar.HOUR_OF_DAY < 19){
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.HOUR_OF_DAY,6);

                        state.setOn(false);
                        finalItem1.power_but.setChecked(false);
                        finalItem1.power_but.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View view, MotionEvent motionEvent) {
                                return true;
                            }
                        });

                        schedule.setLightState(state);
                        schedule.setLightIdentifier(id);
                        schedule.setLocalTime(true);
                        schedule.setDate(cal.getTime());

                        bridge.createSchedule(schedule,listener);
                    }
                }
            }
        });

        return view;
    }

    PHScheduleListener listener = new PHScheduleListener() {
        @Override
        public void onCreated(PHSchedule phSchedule) {

        }

        @Override
        public void onSuccess() {

        }

        @Override
        public void onError(int i, String s) {

        }

        @Override
        public void onStateUpdate(Map<String, String> map, List<PHHueError> list) {

        }
    };

}