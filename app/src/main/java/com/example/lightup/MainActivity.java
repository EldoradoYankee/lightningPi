package com.example.lightup;


import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

import java.sql.Timestamp;
import java.util.ArrayList;


/**
 * MainActivity is already the whole App: simple design with intuitive ColorPicker, which continuously
 * updates the light and therefore sets a SSH session and connect tho the Pi
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Session instance to connect to the pi and
     */
    protected Session session;
    protected ColorPicker picker;

    private Button btn;
    private TextView colorView;
    private TextView commandView;
    private EditText plain_text_input;
    private final ArrayList<Integer> dark = new ArrayList<>();
    private int currentColor;
    private boolean off = false;

    private Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    private String piip;

    /**
     * SharedPrefernces
     */
    private SharedPreferences sharedPreferences;
    private static final String COLOR_SHARED_PREFS = "sharedColor";
    private static final String COLOR = "color";
    private static final String IP = "ip";

    /**
     * The MainActivity is created by this method (generated from a XML)
     * It displays a continuous ColorPicker wheel to set the color with a toggle
     * by releasing the wheel, the color will be set
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        colorView = findViewById(R.id.colorView);
        commandView = findViewById(R.id.commandView);

        picker = findViewById(R.id.picker);
        SaturationBar saturationBar = findViewById(R.id.saturationbar);
        ValueBar valueBar = findViewById(R.id.valuebar);


        plain_text_input = findViewById(R.id.plain_text_input);

        dark.add(0);
        dark.add(0);
        dark.add(0);


        // add a saturationBar & valueBar to the picker
        picker.addSaturationBar(saturationBar);
        picker.addValueBar(valueBar);

        valueBar.setValue(20);
        saturationBar.setSaturation(100);

        //to turn of showing the old color
        picker.setShowOldCenterColor(true);

        //To set the old selected color
        picker.setOldCenterColor(picker.getColor());

        btn = findViewById(R.id.powerButton);
        Button buttonIP = findViewById(R.id.buttonIP);



        // onColorChanged (which detects every slight deviation as a different number) calls the method to setUp a SSH connection and exec the handed command
         picker.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                // Method changes the Views in the App & calls the setUpCommand Method
                onColorChangedAction();
            }
        });



        // onSaturationChanged (which detects every slight deviation as a different number) calls the method to setUp a SSH connection and exec the handed command
        saturationBar.setOnSaturationChangedListener(new SaturationBar.OnSaturationChangedListener() {
            @Override
            public void onSaturationChanged(int saturation) {
                // Method changes the Views in the App & calls the setUpCommand Method
                onColorChangedAction();
            }
        });


        valueBar.setOnValueChangedListener(new ValueBar.OnValueChangedListener() {
            @Override
            public void onValueChanged(int value) {
                // Method changes the Views in the App & calls the setUpCommand Method
                onColorChangedAction();
            }
        });

        // when button is clicked, the color goes either dark or the current color on which the wheel is set
        btn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                if (!off) {
                    setUpCommand(dark);
                    btn.setText("On");
                    off = true;
                }
                else {
                    ArrayList<Integer> oldColor = aarrggbbConverter(currentColor);
                    setUpCommand(oldColor);
                    btn.setText("Off");
                    off = false;
                }
            }
        });

        buttonIP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPIIP();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        sharedPreferences = getSharedPreferences(COLOR_SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(COLOR, currentColor);
        editor.putString(IP, plain_text_input.getText().toString());
        editor.apply();
    }


    /**
     * sets the current color from the class variable everytime the activity gets displayed again
     */
    @SuppressLint("ShowToast")
    @Override
    public void onResume(){
        super.onResume();
        // picks up the currentColor again
        sharedPreferences = getSharedPreferences(COLOR_SHARED_PREFS, MODE_PRIVATE);
        currentColor = sharedPreferences.getInt(COLOR, -6000);
        plain_text_input.setText(sharedPreferences.getString(IP, "192.168.10.99"));
        picker.setColor(currentColor);
        setPIIP();
    }


    /**
     * setUpSSH tries to setUp session and channel in which the command gets executed
     * @param rgb ArrayList with converted RGB
     */
    @SuppressLint("StaticFieldLeak")
    public void setUpCommand(final ArrayList<Integer> rgb) {

        // to do this simultaneously apart from this main_activity thread, a new one is created
        new AsyncTask<Integer, Void, Void>(){
            @SuppressLint("StaticFieldLeak")
            @Override
            protected Void doInBackground(Integer... params) {
                try {
                    executeSSHcommand(rgb);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(1);
    }




    /**
     * Once the session has connected, a new channel gets opened. (exec is a command for the bash, that sets into the session an executable command)
     * it is the channel that gets the effective command
     * set a session to the Pi so every executeSSHcommand can be executed by a connected session with a new JSch instance
     * the login is used by the user default user pi
     */
    @SuppressLint({"ShowToast", "SetTextI18n"})
    public void executeSSHcommand(ArrayList<Integer> rgb) {

        // value for the pin 17 RED
        String color1 = String.valueOf(rgb.get(0));
        // value for the pin 22 GREEN
        String color2 = String.valueOf(rgb.get(1));
        // value for the pin 24 BLUE
        String color3 = String.valueOf(rgb.get(2));


        // setUp connection three times to execute the commandSkript
            try {
                // Connection Variables
                String host = piip;
                String user = "pi";
                String password = "raspberry";
                // Testumgebung
                //String host = "192.168.10.99";
                //String host = "192.168.178.51";
                int port = 22;

                JSch jsch = new JSch();
                session = jsch.getSession(user, host, port);
                session.setPassword(password);
                session.setConfig("StrictHostKeyChecking", "no");
                session.setTimeout(10000);
                session.connect();
                ChannelExec channel = (ChannelExec) session.openChannel("exec");
                commandView.setText("python /home/pi/commandSkript.py" + " " + color1 + " " + color2 + " " + color3);
                channel.setCommand("python /home/pi/commandSkript.py" + " " + color1 + " " + color2 + " " + color3);
                channel.connect();
                channel.disconnect();
                // to not having opened multiple session, close after each commad
                session.disconnect();
            } catch (JSchException e) {
                Toast.makeText(this, "unfortunately, the connection failed", Toast.LENGTH_LONG);
                e.printStackTrace();
            }
        }


    /**
     * This is a converter method
     * @param argb negative int value from the Holo Color Picker
     * @return an ArrayList<Integer> with 3 numbers from 0 to 255, the RGB colors
     */
    public ArrayList<Integer> aarrggbbConverter(int argb) {
        int r = (argb>>16)&0xFF;
        int g = (argb>>8)&0xFF;
        int b = (argb)&0xFF;

        // ArrayList with all the values in RGB
        ArrayList<Integer> rgb = new ArrayList<>();

        // adding to ArrayList
        rgb.add(r);
        rgb.add(g);
        rgb.add(b);

        return rgb;
    }



    public void onColorChangedAction() {
        Timestamp currentTimeStamp = new Timestamp(System.currentTimeMillis());
        if (currentTimeStamp.getTime() > timestamp.getTime() + 1000) {

            // the class varable gets always the current value of the wheel
            currentColor = picker.getColor();

            // Here is the colorView set with the current color
            colorView.setText(String.valueOf(currentColor));

            // check if the light is on & send the command
            if (!off) {
                int argb = picker.getColor();
                ArrayList<Integer> rgb = aarrggbbConverter(argb);
                setUpCommand(rgb);
            }
            timestamp = currentTimeStamp;
        }
    }

    public void setPIIP() {
        piip = plain_text_input.getText().toString();
    }

    /**
     * getter
     * @return String
     */
    public String getPIIP() {
        return piip;
    }
}
