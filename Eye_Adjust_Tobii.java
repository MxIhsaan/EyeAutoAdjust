package com.bham.project;

import ij.*;
import ij.IJ;
import ij.plugin.frame.PlugInFrame;
import ij.process.*;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;

import java.awt.*;
import java.net.*;
import java.io.IOException;

public class Eye_Adjust_Tobii extends PlugInFrame implements ImageListener, Runnable 
    {
    // ImageListener: listens to closing of the image (and changes of image data)
    // Runnable: for background thread

    /* preferences */
    int rWidth = 50;        // rectanlge roi size in pixels: 25x25
    int rHeight = 50;

    /* current status */
    private boolean isFixed = false;    // whether the position is fixed and does not move with the cursor
    private int mX, mY;                 // mouse coords
    private int eX, eY;                 // eye tracker coords
    private int cX, cY;                 // image canvas coords
    private int iX, iY;                 // image relative coords
    private int jX, jY;
    private int rX, rY;                 // roi coords
    Point tl = new Point(),             // image canvas corner coords
          tr = new Point(), 
          bl = new Point(), 
          br = new Point();
    double iWidth, iHeight;             // image width x height
    double wWidth, wHeight;             // image window width x height
    double cWidth, cHeight;             // image canvas width x height
    double wScale, hScale;              // ratio between image pixels and canvas pixels  
    double avgScale;                    
    double min = 0;
    double max = 65535;
    double alpha = 0.88;                // rate in which the brightness changes
    int nextUpdate;                     // type of next update
    final static int POSITION_UPDATE = 1, FULL_UPDATE = 2;
 
    /* udp server details for matlab data */
    String hostname = "127.0.0.1";
    int port = 5665;
    DatagramSocket socket;
    byte[] buffer;
    DatagramPacket eyeData;
    String msg = new String();
    
    /* ij variables */
    ImageJ ij;
    ImagePlus imp;                  // the ImagePlus that we listen to
    int impType;                    // the type of the ImagePlus
    ImageCanvas canvas;             // the canvas of imp
    Window imgWin;                  // image window
    Thread bgThread;                // thread for output 

    /* Initialization */
    public Eye_Adjust_Tobii() 
    {
        super("Eye_Adjust_Tobii");
        
        ij = IJ.getInstance();
        if (ij == null) 
            return;
        
        imp = WindowManager.getCurrentImage();
        if (imp==null) 
        {
            IJ.noImage(); 
            return;
        }
        
        impType = imp.getType();

        addImageListeners();

        bgThread = new Thread(this, "Eye_Adjust_Tobii");         // creates the thread
        bgThread.start();
        bgThread.setPriority(Math.max(bgThread.getPriority()-3, Thread.MIN_PRIORITY));
        
        update(FULL_UPDATE);                // the first data display
    }

    public void close()
    {
        removeImageListeners();
        isFixed = false;
        
        System.out.println("Closing...");

        synchronized(this)
        {
            bgThread.interrupt();       // terminates thread
        }
        
        super.close();
        socket.close();
    }

    private void addImageListeners() 
    {
        imp.addImageListener(this);

        ImageWindow win = imp.getWindow();
        if (win == null) 
            close();
        
        canvas = win.getCanvas();
    }

    private void removeImageListeners()
    {
        imp.removeImageListener(this);
    }
    
    public void imageUpdated(ImagePlus imp) 
    { 
        update(FULL_UPDATE); 
    }
    public void imageOpened(ImagePlus imp) 
    {

    }
    public void imageClosed(ImagePlus imp) 
    {
        if (imp == this.imp) close();
    }

    synchronized void update(int whichUpdate) 
    {
        if (isFixed && whichUpdate == POSITION_UPDATE) 
            return;

        if (nextUpdate < whichUpdate) 
            nextUpdate = whichUpdate;

        notify();           // wake up the thread
    }

    public void mouseCoords()      // get mouse coords on screen
    {
        mX = MouseInfo.getPointerInfo().getLocation().x;
        mY = MouseInfo.getPointerInfo().getLocation().y;
    }

    public void mouseCoords(Canvas canvas)      // get mouse coords on canvas
    {
        Point cursorLoc = ((ImageCanvas) canvas).getCursorLoc();
        mX = cursorLoc.x;
        mY = cursorLoc.y;
    }

    public void rcvData() throws IOException    // process data from eye tracker
    {
        buffer = new byte[512];
        eyeData = new DatagramPacket(buffer, buffer.length);
        socket.receive(eyeData);
        msg = new String(buffer, 0, eyeData.getLength());

        eX = Integer.parseInt(msg.split(",")[0]);   // eye tracker recieved coords
        eY = Integer.parseInt(msg.split(",")[1]);
    }
    
    public void canInf(Canvas canvas)     // get canvas info
    {
        Point cLoc = canvas.getLocationOnScreen();   // get top left of canvas coords
        cX = cLoc.x;
        cY = cLoc.y;
        
        cWidth = canvas.getBounds().width;          // image window may be shrunk by user
        cHeight = canvas.getBounds().height;
        
        wScale = cWidth / iWidth;                   // get ratio between canvas size and image size 
        hScale = cHeight / iHeight;
        avgScale = (wScale+hScale)/2;
        
        tl = new Point(cX, cY);                     // calculate each corner location
        tr = new Point(cX+(int)cWidth, cY);
        bl = new Point(cX, cY+(int)cHeight);
        br = new Point(cX+(int)cWidth, cY+(int)cHeight);
    }

    public void conCoM(int x, int y)         // convert mouse screen coords to image coords
    {
        jX = (int) ((x - cX)/avgScale);  
        jY = (int) ((y - cY)/avgScale);
        
        if (jX > iWidth)
            jX = (int)iWidth;
        else if (jX < 0)
            jX = 0;
        if (jY > iHeight)
            jY = (int)iHeight;
        else if (jY < 0)
            jY = 0;
    }

    public void conCo(int x, int y)         // convert eye tracker screen coords to image coords
    {
        iX = (int) (((x*0.808168317 - cX)/avgScale));       // 0.808... = scale factor to get correct eye tracker coords
        iY = (int) (((y*0.808168317 - cY)/avgScale));       // matlab code doesnt give correct value. it is offset to the side
        
        if (iX > iWidth)
            iX = (int)iWidth;
        else if (iX < 0)
            iX = 0;
        if (iY > iHeight)
            iY = (int)iHeight;
        else if (iY < 0)
            iY = 0;
    }
    
    public void setupRoi()
    {
        rX = iX - rWidth/2;         // coordinates where the roi starts from (center of roi is eye coords)
        rY = iY - rHeight/2;

        imp.setRoi(rX, rY, rWidth, rHeight);        // create roi using rectangle at cursor location
    }

    public void findRoiMinMax(ImagePlus imp)
    {
        ImageStatistics stats = imp.getStatistics();    //get min and max in roi
        max = max * alpha + stats.max * (1-alpha);      // alpha makes it so the plugin adjusts to new min max slower
        min = min * alpha + stats.min * (1-alpha);      // like adjusting eyes to brightness

        imp.resetRoi();                 // remove roi  
    }

    public void setMinMax(ImagePlus imp)
    {
        imp.setDisplayRange(min, max);  // change images brightness/contrast

        imp.updateAndDraw();            // dislay new image 
    }

    public void reset(ImageProcessor ip)
    {
        ip.reset();
    }

    // the background thread for updating the table
    public void run()
    {
        System.out.println("Starting up...");
        
        imp = WindowManager.getCurrentImage();      // gets image from screen
        new ImageConverter(imp).convertToGray16();  // convert to greyscale
        ImageProcessor ip = imp.getProcessor();
        
        iWidth = ip.getWidth();      // image dimensions
        iHeight = ip.getHeight();
        
        if (iWidth < iHeight)
        {
            System.out.println("Rotating...");
            ip = ip.rotateLeft();
            imp.setProcessor(ip);
            imp.show();
            iWidth = ip.getWidth();
            iHeight = ip.getHeight();
        }

        imgWin = WindowManager.getWindow(imp.getTitle());           // set window to top left and standardise size
        imgWin.toFront();
        imgWin.setLocation(0, 0);
        imgWin.setSize((int)(iWidth*(831/iHeight)), 831);    // for 1920x1080 res, vertical window from top to taskbar

        canvas.fitToWindow();

        int i = 0;      // for debugging
        
        try 
        {
            InetAddress address = InetAddress.getByName(hostname);
            socket = new DatagramSocket(port, address);

            System.out.println("Waiting for eye tracker...");

            while (true)
            {
                i += 1;
                
                mouseCoords();                  // get mouse coords on screen
                
                // mouseCoords(canvas);            // get mouse coords on image
                
                rcvData();                      // recieve data from eye tracker udp server
                
                canInf(canvas);                 // get canvas coords and sizes

                conCoM(mX, mY);                  // convert mouse coords to relative image coords
                
                conCo(eX, eY);                  // convert eye coords to relative image coords
                
                // ip.putPixel(iX, iY, 65535);
                // imp.updateAndDraw();

                setupRoi();                     // create roi around eye coords to find min max
                
                findRoiMinMax(imp);             // get min and max
                
                setMinMax(imp);                 // change image min max

                reset(ip);                      // revert to original image
                
                IJ.wait(40);             // loops 25 times per second

                synchronized(this) 
                {
                    if (nextUpdate == 0) 
                    {
                        try 
                        {
                            wait();         //notify wakes up the thread
                        }               
                        catch(InterruptedException e) 
                        {               
                            return;         // interrupted tells the thread to exit
                        }
                    }
                }

                if (i % 25 == 0)            // print stats every second
                {
                    // System.out.println("Win" +WindowManager.getActiveWindow().getBounds().toString());
                    // System.out.println("Loc: (" +iX +"," +iY +") - Value: " +ip.getPixelValue(iX, iY));
                    // System.out.println("tpl: " +canvas.getLocationOnScreen());   // canvas coords relative to screen
                    // System.out.println("rel: " +canvas.getBounds() +"\n");      // canvas coords relative to window
                    
                    System.out.println("(" +tl.x +"," +tl.y +")          (" +tr.x +"," +tr.y +")");
                    System.out.println("\t|------|");
                    System.out.println("\t| x  x |");
                    System.out.println("\t|  xx  |");
                    System.out.println("\t| x  x |");
                    System.out.println("\t|------|");
                    System.out.println("(" +bl.x +"," +bl.y +")          (" +br.x +"," +br.y +")\n");

                    System.out.println("iDims: " +iWidth +"x" +iHeight);
                    System.out.println("cDims: " +cWidth +"x" +cHeight);
                    System.out.println("Scale: " +avgScale +"\n");
                    
                    System.out.println(mX +"," +mY +" converted to " +jX +"," +jY);
                    System.out.println(eX +"," +eY +" converted to " +iX +"," +iY);
                    System.out.println("val: " +ip.getPixelValue(iX, iY) +", range: " +(int)min +" - " +(int)max +"\n\n");
                }
            }
        }
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
}
