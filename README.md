# Auto Adjusting Brightness and Contrast with an Eye Tracker

## Setup guide:
- Download the [Tobii software](https://gaming.tobii.com/getstarted/)
    - Select the hardware: Tobii Eyetracking (left-most option)
    - Select Tobii device: Tobii EyeX (bottom option)
    - Download and run to set up the eye tracker
- Download [Fiji](https://fiji.sc/)
- Download one of these plugins
    - [Tobii with hard-coded callibration version](https://git.cs.bham.ac.uk/projects-2022-23/mxi986/-/blob/main/Eye_Adjust_Tobii.java)
    - [Other eye tracker version](https://git.cs.bham.ac.uk/projects-2022-23/mxi986/-/blob/main/Eye_Adjust_Other.java)
    - [Mouse version](https://git.cs.bham.ac.uk/projects-2022-23/mxi986/-/blob/main/Eye_Adjust_Mouse.java)
- Open Fiji
    -  Go to Plugins > Install (Ctrl+Shift+M)
    -  Select the plugin you downloaded (e.g Eye_Adjust_Tobii.java)
    -  Install plugin
    -  Restart Fiji
- Download the [Tobii folder](https://git.cs.bham.ac.uk/projects-2022-23/mxi986/-/tree/main/Tobii)
- Download MATLAB v2016b or earlier (does not work with later versions)
- Open MATLAB
    - Open the EyeTrackerUDP.m file
    - Run the code (F5) to see if it works - it should output the eye coordinates.

## Running the plugin:
- Run the MATLAB EyeTrackerUDP.m code
- Open Fiji
- Open an Image
- Run the Eye Adjust plugin you downloaded
    - It should be at the bottom of the Plugins tab, you may have to scroll a bit
- Image should be enlarged and moved to the left then the auto adjusting begins 

## Using Another Eye Tracker
The MATLAB code is specifically used to collect the data from the Tobii eye tracker and send it via UDP to the plugin. If you are using another eye tracker then this step in the setup can be ignored. Instead, you must first create a UDP server that collects the eye coordinates from your eye tracker and then sends the x and y coordinates in the format `x,y` to the localhost (127.0.0.1) at port 5665. Note that you will need to download the [other](https://git.cs.bham.ac.uk/projects-2022-23/mxi986/-/blob/main/Eye_Adjust_Other.java) eye tracker plugin. 
To run the plugin, start up the server you created to send the data instead of running the MATLAB code. The rest of the steps in 'Running the plugin' should be similar. 

## Using the Mouse Version
For this, you don't need any server code running since there is no eye tracker. Simply download and run the plugin with an opened image and move the mouse around the image to see the changes. 
