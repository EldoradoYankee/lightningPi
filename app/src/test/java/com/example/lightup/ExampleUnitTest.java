package com.example.lightup;

import android.widget.Button;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest extends MainActivity {


    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testARGB () {
        MainActivity mainActivity = new MainActivity();
        assertEquals("the test results positive if the argb color is converted correctly into rgb color scheme", Arrays.asList(255, 255, 0), mainActivity.aarrggbbConverter(-256));
    }

    @Test
    public void testButtonText () {
        MainActivity mainActivity = new MainActivity();
        assertEquals("if the lights are on the text should say \"on\"", "On", ((Button) mainActivity.findViewById(R.id.powerButton)).getText());

        // simulate a button click, which start an AsyncTask and update TextView when done.
        final Button button = (Button) mainActivity.findViewById(R.id.powerButton);
        mainActivity.runOnUiThread(new Runnable() {
            public void run() {
                button.performClick();
            }
        });

        // assume AsyncTask will be finished in 6 seconds.
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals("if the lights are off the text should say \"Off\"\"", "Off", ((Button) mainActivity.findViewById(R.id.powerButton)).getText());
    }


    @Test
    public void testGetPIIP() {
        assertEquals("192.168.10.99", getPIIP());
    }


}