package com.example.lightup;


import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

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
 * MainActivity is already the whole App: simple design with intuitive ColorPicker, which continously
 * updates the light and therefore sets a SSH session and connect tho the Pi
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Session instance to connect to the pi and
     */
    protected Session session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        final TextView colorView = findViewById(R.id.colorView);

        final ColorPicker picker = findViewById(R.id.picker);
        SaturationBar saturationBar = findViewById(R.id.saturationbar);

        // add a saturationBar to the picker
        picker.addSaturationBar(saturationBar);

        //to turn of showing the old color
        picker.setShowOldCenterColor(true);

        //To set the old selected color
        picker.setOldCenterColor(picker.getColor());



        // onColorChanged (which detects every slight deviation as a different number) calls the method to setUp a SSH connection and exec the handed command
        picker.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {

                // Here comes a method call to call the method to set the connection to the pi and the params
                Log.v("color", String.valueOf(picker.getColor()));
                colorView.setText(String.valueOf(picker.getColor()));

                int argb = picker.getColor();
                ArrayList<Integer> rgb = aarrggbbConverter(argb);
                setUpCommand(rgb);
            }
        });


        // onSaturationChanged (which detects every slight deviation as a different number) calls the method to setUp a SSH connection and exec the handed command
        saturationBar.setOnSaturationChangedListener(new SaturationBar.OnSaturationChangedListener() {
            @Override
            public void onSaturationChanged(int saturation) {

                // Here comes a method call to call the method to set the connection to the pi and hand params
                Log.v("color", String.valueOf(picker.getColor()));
                colorView.setText(String.valueOf(picker.getColor()));

                int argb = picker.getColor();
                ArrayList<Integer> rgb = aarrggbbConverter(argb);
                setUpCommand(rgb);
            }
        });
    }


    public ArrayList<Integer> aarrggbbConverter(int argb) {
        int r = (argb>>16)&0xFF;
        int g = (argb>>8)&0xFF;
        int b = (argb)&0xFF;

        ArrayList<Integer> rgb = new ArrayList<>();

        rgb.add(r);
        rgb.add(g);
        rgb.add(b);

        return rgb;
    }


    /**
     * setUpSSH tries to setUp session and channel in which the command gets executed
     * @param rgb ArrayList with converted RGB
     */
    @SuppressLint("StaticFieldLeak")
    public void setUpCommand(ArrayList<Integer> rgb) {

        for (int i = 0; i <3; i++) {
            Log.v("color", rgb.get(i).toString());
        }


        final String command = "sudo python lightUp.py";
        new AsyncTask<Integer, Void, Void>(){
            @SuppressLint("StaticFieldLeak")
            @Override
            protected Void doInBackground(Integer... params) {
                try {
                    executeSSHcommand(command);
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
    public void executeSSHcommand(String command) {
        String user = "pi";
        String password = "raspberry";
        String host = "192.168.3.42";
        int port = 22;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(100000);
            session.connect();
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            System.out.println(command);
            channel.connect();
            channel.disconnect();
        }
        catch (JSchException e) {
            e.printStackTrace();
        }
    }
}
