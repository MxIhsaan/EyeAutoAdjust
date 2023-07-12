% TODO:
% Add intermediate screen after practice trials
% Add eyetracking script
% saving the struct to a csv file
% clear mex after each recording
% clear mex functions
%clear mex;
% remove unnecessary variables
% restructure to set var. information at the beginning
% add comments
% restructure to include a udp communications script
% save jitter/gaus times for each trial

close all;
clearvars;
commandwindow;

% record participant student ID & subject number
% Use subject number as a replicable, yet unique 
% sequence generator.
prompt = {'Participant ID:', 'Participant number:'};
title = 'Participant Information';
partInfo = inputdlg(prompt, title);
seed = partInfo{2};
seed = str2num(seed);

% Setup PTB with some default values
PsychDefaultSetup(2);

% Seed the random number generator for the Shuffle function
rng(seed);

% Skip sync tests for demo purposes only
%Screen('Preference', 'SkipSyncTests', 0);


%----------------------------------------------------------------------
%                       Screen setup
%----------------------------------------------------------------------

% Set the screen number to the external monitor 
% if it exists
screenNumber = max(Screen('Screens'));

% Define black, white and grey
white = WhiteIndex(screenNumber);
grey = white / 2;
black = BlackIndex(screenNumber);

% Open the screen
[window, windowRect] = PsychImaging('OpenWindow', screenNumber, grey, [30 30 600, 400], 32, 2);

% Flip to clear
%Screen('Flip', window);

% Query the frame duration 
ifi = Screen('GetFlipInterval', window);

% Set the text size
Screen('TextSize', window, 60);

% Query the maximum priority level
topPriorityLevel = MaxPriority(window);

% Get the centre coordinate of the window
[xCenter, yCenter] = RectCenter();

% Set the blend funciton for the screen
Screen('BlendFunction', window, 'GL_SRC_ALPHA', 'GL_ONE_MINUS_SRC_ALPHA');



%----------------------------------------------------------------------
%                       Timing Information
%----------------------------------------------------------------------

% Interstimulus interval time in seconds and frames
isiTimeSecs = 1;
isiTimeFrames = round(isiTimeSecs / ifi);

% Numer of frames to wait before re-drawing
waitframes = 1;

% How long should the image stay up during flicker in time and frames
imageSecs = 1;
imageFrames = round(imageSecs / ifi);

% Duration (in seconds) of the blanks between the images during flicker
blankSecs = 0.25;
blankFrames = round(blankSecs / ifi);

% Make a vector which shows what we do on each frame
presVector = [ones(1, imageFrames) zeros(1, blankFrames)...
    ones(1, imageFrames) .* 2 zeros(1, blankFrames)];
numPresLoopFrames = length(presVector);



%----------------------------------------------------------------------
%                       Keyboard information
%----------------------------------------------------------------------

% Keybpard setup
spaceKey = KbName('space');
escapeKey = KbName('ESCAPE');
stableKey = KbName('s');
unstableKey = KbName('l');
RestrictKeysForKbCheck([spaceKey, stableKey, unstableKey, escapeKey]);

%----------------------------------------------------------------------
%                      Experimental Image List
%----------------------------------------------------------------------

% Get the image files for the experiment
imageFolder = '/PycharmProjects/Main Loop/';
imgList = dir(fullfile(imageFolder, '*.png'));
imgList = {imgList(:).name};
randImgList = Shuffle(imgList); % make sure MATLAB doesn't radomize the same way everytime it's started
numImages = length(imgList);

% Practice trials
imagePracticeFolder = '/PycharmProjects/Practice/';
imgPracticeList = dir(fullfile(imagePracticeFolder, '*.png'));
imgPracticeList = {imgPracticeList(:).name};
randPracticeList = Shuffle(imgPracticeList);
numPracticeTrials = length(imgPracticeList);

numTotals = numImages + numPracticeTrials;
imgTotalList = [randPracticeList, randImgList];


%addpath(genpath('U:\Experiment'));

%----------------------------------------------------------------------
%                        Results Matrix and Structs
%----------------------------------------------------------------------

% For this demo we have a (1) "stable" condition and (2) "unstable"
% We will call this our "trialType"
trialType = [1 2];

% Make a  matrix which which will hold all of our results
resultsMatrix = nan(numTotals, 3);

% Make a directory for the results
resultsDir = [cd '/Results/'];
if exist(resultsDir, 'dir') < 1
    mkdir(resultsDir);
end

data = struct; 
data_tracker = struct;

%----------------------------------------------------------------------
%                        Fixation Cross
%----------------------------------------------------------------------

% Screen Y fraction for fixation cross
crossFrac = 0.0167;

% Here we set the size of the arms of our fixation cross
fixCrossDimPix = windowRect(4) * crossFrac;

% Now we set the coordinates (these are all relative to zero we will let
% the drawing routine center the cross in the center of our monitor for us)
xCoords = [-fixCrossDimPix fixCrossDimPix 0 0];
yCoords = [0 0 -fixCrossDimPix fixCrossDimPix];
allCoords = [xCoords; yCoords];

% Set the line width for our fixation cross
lineWidthPix = 4;


%----------------------------------------------------------------------
%                      Experimental Loop
%----------------------------------------------------------------------

% Start screen
Screen('TextFont', window, 'Arial')
DrawFormattedText(window, 'Press Space To Begin', 'center', 'center', black);
Screen('Flip', window);
KbWait;

%myex('connect');

for trial = 1:numTotals
    
    if trial <= numPracticeTrials
        Practice = true;
    elseif trial == 21
        % Intermediate Screen presentation
        Screen('FillRect', window, [.8 .8 .8]);
        Screen('TextFont', window, 'Arial');
        DrawFormattedText(window, 'Press space to begin the experiment', 'center', 'center', black);
        Screen('Flip', window);
        KbWait();
        % this is not a practice trial
        Practice = false;
    else
        Practice = false;
    end

    % load the image
    if Practice == true
        theImage = imread([imagePracticeFolder randPracticeList{trial}]);
        imgName = randPracticeList{trial};
    else
        theImage = imread([imageFolder randImgList{trial-20}]);
        imgName = randImgList{trial-20};
    end
        
    % determine the correct answer
    Ans = strfind(imgName, 's');
    if ~isempty(Ans)
        % the image is stable
        corrans = 0; 
        corrkey = 's'; % stable answer key
    elseif isempty(Ans)
        % the image is unstable
        corrans = 1;
        corrkey = 'l'; % unstable answer key
    end
    

    % Make the images into textures
    tex = Screen('MakeTexture', window, theImage);

    % Draw a fixation cross for the start of the trial
    Screen('FillRect', window, grey);

    % Draw the fixation cross in white, set it to the center of our screen and
    % set good quality antialiasing
    Screen('DrawLines', window, allCoords,...
        lineWidthPix, white, [xCenter yCenter], 2);
    
    % Draw a length of time the cross will be presented from a normal
    % distribution
    cross = normrnd(1.2, 0.1);

    Screen('Flip', window);
    WaitSecs(cross);

    % This is our image showing and eye-tracking loop
    Priority(topPriorityLevel);
    tEnd = GetSecs + 1;
    
    % may need to connect here
    %myex('connect'); 
    % clear data buffer
    %myex('getdata');

    % allow to track until window closed
    %x_all = [];
    
    while GetSecs < tEnd
        % Draw the image to the screen, unless otherwise specified PTB will draw
        % the texture full size in the center of the screen. We first draw the
        % image in its correct orientation.
        Screen('DrawTexture', window, tex, [], [], 0);

        % Flip to the screen
        Screen('Flip', window);
        
        % integrate eye recording here? 
        %data_row = myex('getdata');
        %if ~isempty(data_row)
        %    x_all = [x_all; data_row];
        %end
        
        WaitSecs(0.001);
    end
    
    % answer recording loop
    tAns = GetSecs + 2; 
    respMade = 0;
    while (respMade == 0) 
        % clear data buffer
        %myex('getdata');        
        % Present answer prompt
        Screen('FillRect', window, [.8 .8 .8]);
        Screen('TextFont', window, 'Arial');
        DrawFormattedText(window, 'Answer', 'center', 'center', black);
        Screen('Flip', window);

        % Poll the keyboard for the answer keys
        [keyIsDown, respSecs, keyCode] = KbCheck;
        if (keyCode(KbName('l')) == 1) || (keyCode(KbName('s')) == 1)
            respMade = 1;
            disp('prints');
            if (keyCode(KbName('l')) == 1)
                % they pressed the UNSTABLE key
                givenAns = 1;
            elseif (keyCode(KbName('s')) == 1)
                % they pressed the STABLE key
                givenAns = 0;
            end
        elseif keyCode(KbName('ESCAPE')) == 1
            sca;
            disp('*** Experiment terminated ***');
            return
            % end of answer loop
        end
       
    if ((GetSecs - tAns) >= 2)
        givenAns = 999;
        break
    end
    end
    
    % Switch to low priority for after trial tasks
    Priority(0);
    % Bin the textures we used
    Screen('Close', tex);
    
    %%%% FEEDBACK LOOP %%%%
    if Practice == true
        % determine whether the response was congruent with the trial
        if givenAns == 999
            col = [0 .3 .7];
            message = 'too late';
        elseif givenAns == corrans
            disp(givenAns)
            disp(corrans)
            % then answer is correct; give positive feedback
            col = [.2 .8 .1];
            message = 'correct';
        elseif givenAns ~= corrans
            disp(givenAns)
            disp(corrans)
            % the answer was incorrect; gove negative feedback
            col = [.8 .2 .1];
            message = 'Incorrect';
        end
        % Present feedback
        Screen('FillRect', window, col);
        Screen('TextFont', window, 'Arial');
        DrawFormattedText(window, message, 'center', 'center', black);
        Screen('Flip', window);
        WaitSecs(2);
    end
    
    % clear buffer
    %myex('getdata');
    % save the results
    data.trialNo(trial,1) = trial;
    data.image{trial,1} = imgName;
    data.sceneType(trial,1) = corrans;
    data.responseKey(trial,1) = givenAns;
    data.RT(trial,1) = respSecs;
    % save eyetracking data
    %data_tracker(trial).matrix = x_all;
    
    % save the mat file
    % save data as mats, just in case any further manipulation is needed
    fn = strcat('U:\Experiment\Data','data', partInfo{2},'.mat');
    save(fn, 'data', 'data_tracker');
  
end

% disconnect from the eyex
%myex('disconnect');

% goodbye screen
Screen('TextFont', window, 'Arial')
DrawFormattedText(window, 'Thank you! You can now get the experimenter', 'center', 'center', black);
Screen('Flip', window);
KbWait;

% Close the onscreen window
sca

% save data to struct 
filename1 = strcat('U:\Experiment\Data\CSV\','data.csv', partInfo{2});
filename2 = strcat('U:\Experiment\Data\CSV\','tracker_data.csv', partInfo{2});
struct2csv(data, filename1);
%struct2csv(data_tracker, filename2);

% save data as mats, just in case any further manipulation is needed
fn = strcat('U:\Experiment\Data','data', partInfo{2},'.mat');
%save(fn, 'data', 'data_tracker');

return
