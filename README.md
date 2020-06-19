# lightningPi
App to connect to Pi within same network 



The App implements the Holo ColorPicker from Lars Werkman 
With JSch, the connection to the Pi is set
The Colorcode gets converted from the negative 3 to 8 digit number to the RGB color code 
With the tree converted numbers, three commands get exectuted in on the Pi which trigger the 
PIGPIOD deamon which activates the GPIO Pins in a certain manner to set the transistor to the 
right amount so the LED strip gets its color
