package com.example.lightup;


import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
    private final ArrayList<Integer> dark = new ArrayList<>();
    private int currentColor;
    private boolean off = false;


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
        final TextView colorView = findViewById(R.id.colorView);

        picker = findViewById(R.id.picker);
        SaturationBar saturationBar = findViewById(R.id.saturationbar);

        dark.add(0);
        dark.add(0);
        dark.add(0);


        // add a saturationBar to the picker
        picker.addSaturationBar(saturationBar);

        //to turn of showing the old color
        picker.setShowOldCenterColor(true);

        //To set the old selected color
        picker.setOldCenterColor(picker.getColor());

        btn = findViewById(R.id.powerButton);



         // onColorChanged (which detects every slight deviation as a different number) calls the method to setUp a SSH connection and exec the handed command
         picker.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                // the class varable gets always the current value of the wheel
                currentColor = picker.getColor();

                // Here comes a method call to call the method to set the connection to the pi and the params
                Log.v("color", String.valueOf(picker.getColor()));
                Log.v("color", "currentColor is: " + currentColor);
                colorView.setText(String.valueOf(picker.getColor()));


                // check if the light is on
                if (!off) {
                    int argb = picker.getColor();
                    ArrayList<Integer> rgb = aarrggbbConverter(argb);
                    setUpCommand(rgb);
                }
            }
        });



        // onSaturationChanged (which detects every slight deviation as a different number) calls the method to setUp a SSH connection and exec the handed command
        saturationBar.setOnSaturationChangedListener(new SaturationBar.OnSaturationChangedListener() {
            @Override
            public void onSaturationChanged(int saturation) {
                    // the class varable gets always the current value of the wheel
                    currentColor = picker.getColor();

                    // Here comes a method call to call the method to set the connection to the pi and hand params
                    Log.v("color", String.valueOf(picker.getColor()));
                    Log.v("color", "currentColor is: " + currentColor);
                    colorView.setText(String.valueOf(picker.getColor()));


                    // check if the light is on
                    if (!off) {
                        int argb = picker.getColor();
                        ArrayList<Integer> rgb = aarrggbbConverter(argb);
                        setUpCommand(rgb);
                }
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
    }

    /**
     * sets the current color from the class variable everytime the activity gets displayed again
     */
    @Override
    public void onResume(){
        super.onResume();
        picker.setColor(currentColor);
    }

    /**
     * setUpSSH tries to setUp session and channel in which the command gets executed
     * @param rgb ArrayList with converted RGB
     */
    @SuppressLint("StaticFieldLeak")
    public void setUpCommand(final ArrayList<Integer> rgb) {

        /*
        for (int i = 0; i <3; i++) {
            Log.v("color", rgb.get(i).toString());
        }

        // testscript command to test circuit
        //final String command = "sudo python lightUp.py";


        // command for the pin 17 RED
        String command1 = "pigs p 17 " + rgb.get(0).toString();
        // command for the pin 22 GREEN
        String command2 = "pigs p 22 " + rgb.get(1).toString();
        // command for the pin 24 BLUE
        String command3 = "pigs p 24 " + rgb.get(2).toString();

        ArrayList<String> commands = new ArrayList<>();

        commands.add(command1);
        commands.add(command2);
        commands.add(command3);
         */



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
     * Once the session has connected, a new channel gets opened. (exec is a command for the bash, that sets the session to an executable command)
     * it is the channel that gets the effective command
     * set a session to the Pi so every executeSSHcommand can be executed by a connected session with a new JSch instance
     * the login is used by the user default user pi
     */
    @SuppressLint("ShowToast")
    public void executeSSHcommand(ArrayList<Integer> rgb) {

        // command for the pin 17 RED
        String color1 = "pigs p 17 " + rgb.get(0);
        // command for the pin 22 GREEN
        String color2 = "pigs p 22 " + rgb.get(1);
        // command for the pin 24 BLUE
        String color3 = "pigs p 24 " + rgb.get(2);

        ArrayList<String> commands = new ArrayList<>();
        commands.add(color1);
        commands.add(color2);
        commands.add(color3);

        // setUp connection three times to execute three different commands
        for (int i = 0; i < 3; i++) {
            try {

                // Connection Variables
                String user = "pi";
                String host = "192.168.10.99";
                String password = "raspberry";
                int port = 22;

                JSch jsch = new JSch();
                session = jsch.getSession(user, host, port);
                session.setPassword(password);
                session.setConfig("StrictHostKeyChecking", "no");
                session.setTimeout(10000);
                session.connect();
                ChannelExec channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(commands.get(i));
                channel.connect();
                channel.disconnect();
                // to not having opened multiple session, close after each command
                session.disconnect();
            } catch (JSchException e) {
                Toast.makeText(this, "unfortunately, the connection failed", Toast.LENGTH_LONG);
                e.printStackTrace();
            }
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
}
