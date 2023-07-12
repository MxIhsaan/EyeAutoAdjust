# myex: a MATLAB interface for the Tobii EyeX eye-tracker

myex is a MATLAB interface for the Tobii EyeX eye-tracker. It allows MATLAB users to receive incoming data from the eye-tracker, by providing a data buffer that can receive data from the EyeX, and be queried by the user on demand. Myex enables MATLAB users to take advantage of low-cost, portable eye-tracking technology, ideal for use in gaze-contingent psychophysical paradigms, or for users looking to develop assistive devices for individuals with impaired mobility.		

### Quick Start: Setting up
1. Download the Zip archive and unzip it into an appropriate directory
2. Run EyeTrackerUDP.m in MATLAB

### System Requirements
**Operating system:**
myex is compatible with Windows 7 and Windows 10 (the only platforms supported by the EyeX eye-tracker).

**Programming language:**
myex is compatible with all known versions of MATLAB.

**Additional system requirements:**
myex is designed to interface with the Tobii EyeX eye-tracker (Tobii Technology, Stockholm, Sweden), which requires a USB 3.0 connection.

**Dependencies:**
myex requires is compatible with all versions of the Tobii EyeX Interaction Engine from v1.2.0 onwards (at the time of writing the latest version is v1.9.4). There are no MATLAB dependencies. However, users wishing to compile Myex from source may need to install an appropriate C/C++ compiler (run “mex -setup” from within MATLAB for more info.

### License
GNU GPL v3.0

### In case of difficulties
Please post an issue on the GitHub repository, or contact the author directly at: petejonze@gmail.com


### Enjoy!
@petejonze  
12/03/2018


### Added note by Chris Miall 23/August/2018
This works straightforwardly and allows easy collection of gaze position in screen coordinates at about 50Hz.

The Tobii software must be installed first, and the icon in the system tray can be used to activate/disable the Tobii.
Once active, it is (I think) continuously streaming gaze positions (and left right eye positions in 3-d space). You can 
therefore get data on viewing distance if needed. There is no information on pupil size, nor on blinks, but there is a 
binary code for each eye that might mean good/bad data (i.e. blinks?)
 
Two additional issues - it must be in a USB 3 port, not USB 2, and at least on some of the new machines, it will only work in the 
USB ports on the back of the PC, not the ones on the front panel.


### Added note by Mohammad Ihsaan 6/April/2023
I added changes to the original code which can be found [here](https://github.com/petejonze/myex). 

Created a UDP server to send the x and y coordinates of where the user is looking at on the screen to the localhost on port 5665.
Reduced the frequency to 25 Hz as that is enough for my plugin. 
