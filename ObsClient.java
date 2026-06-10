/*
*  Observatory client program - communicates with server running on Pi
*  GUI version - developed from the terminal interface version
*
*  This is a temporary command driven terminal program to control the
*  observatory. It will be replaced with a GUI interface at some future
*  time.
*
*  2016-04-09 PLS complete initial authoring - terminal software
*  		  add roof movement error message
*  2016-12-30 PLS Start conversion to GUI
*  2017-01-01 PLS Gui conversion complete, detect missing command responses
*  2017-02-12 PLS Fix scopes parked bad code - wrong switches tested-scopes 2,3
*  2017-02-18 PLS Fix os.setAll() call to propagate this progs tracer value
*		  fix missing {} at start of clientProcess()
*  2017-09-23 PLS UDSAC rewrite - see literals starting at around line 105
*/

/*    Button usage - incomplete
  
  b01  - open roof
  b02  - stop roof
  b03  - close roof


 Toggle buttons
  parkOverride 		- bypass scopes parked - allow roof move when not parked
  scopePowerButton1	- power on/off for scope 1
  scopePowerButton1a	- aux (camera) power for scope 1, requires above active
//  scopePowerButton2	- power on/off for scope 2
//  scopePowerButton3	- power on/off for scope 3
  electronics1		- power on/off for circuit 1 (ethernet)
  electronics2		- power on/off for circuit 2 (backup drive)
  electronics3		- power on/off for circuit 3 (parked sensors)
  electronics4		- power on/off for circuit 4 (lights)
  electronics5		- 
  electronics6

*/


import java.net.*;
import javax.swing.*;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.lang.Integer;
import java.text.SimpleDateFormat;

public class ObsClient
{PrintWriter out;
 public static boolean tracer = false;
 static boolean gui = false, waitingForServer = false;
 static String hostName = "";
 static int portNumber = 0;
 public static int readWaitCountMax = 100000, readWaitCount = 0, readWaitShort = 5;
 static ObsStatus os = new ObsStatus();
 static      BufferedReader in;
 static Socket socket;
 String errorMessage = "";
 String parkedWord = "";
 private static final String
	 securityCode = "d43909dbd40f9e6861e2676945e74992"

	;
 private static MyButtonHandler bh1;
 private static JFrame applFrame;
 private static JPanel applPanel;
 private static JButton b01, b02, b03, b04, b05, b06, b07, b08, b09, b10

			;
 private static String lastUserInput = "";

 private static JLabel panelMessage = new JLabel(""); 

 private static JTextArea 
		 roofStatus
		,scopeParked1
		,scopeParked2
		,scopeParked3
		,acStatus
		;
 public static JToggleButton 
		 parkOverride
		,scopePowerButton1
		,scopePowerButton2
		,scopePowerButton3
		,scopePowerButton1a
		,electronics1
		,electronics2
		,electronics3
		,electronics4
		,electronics5
		,electronics6
		;
 private static final SimpleDateFormat sdf = new SimpleDateFormat("HH.mm.ss");
 private static final Font textFont = new Font("SANS_SERIF",Font.BOLD,14);
 private static final Font textFont2= new Font("SANS_SERIF",Font.PLAIN,20);
 private static final Font textFontBold = new Font("SANS_SERIF",Font.BOLD,20);
 private Container applFrameContent;

 static final String
	 scopeName1 = "Primary"
	,scopeName2 = ""
	,scopeName3 = ""
	,scopePowerButtonText = "Power on/off"
	,scopePowerButton1aText = "Camera on/off"
	,electronicsButtonText1 = "Relay5 48V"
	,electronicsButtonText2 = "Relay6 computer power"
	,electronicsButtonText4 = "Relay8 12V park sensors"
	,electronicsButtonText3 = "Relay7 7.4V Canon power"
	,b04Text = "Mount computer"
//	,b05Text = "Abe Laptop"
//	,b06Text = "Phil Laptop"

	,yes = "YES"
	,no  = "NO"
	,unk = "Unknown"
	,parked = "Parked"
	,parkedNo = "NOT PARKED"
	;


 public static void main(String[] args) throws IOException
  {if (args.length == 0)
    {hostName = "127.0.0.1";
     portNumber = 8080;
     System.out.println("No parms; using:   127.0.0.1 8080");
     System.out.println("  parms: [<host_name> <port>]  [gui] [trace]");
    }
   else if (args.length >1)
    {hostName = args[0];
     portNumber = Integer.parseInt(args[1]);
     if (args.length > 2)
      {for (int i = 1; i<args.length; i++)
        {if ("GUI".equals(args[i].toUpperCase()))
           gui = true;
         else if ("TRACE".equals(args[i].toUpperCase()))
	   tracer = true;
	}
      }
    }
   else if ((args.length == 1) && ("GUI".equals(args[0])))
    {gui = true;
     hostName = "127.0.0.1";
     portNumber = 8080;
    }
   else
    {System.err.println("Usage: java ObsClient <host name> <port number> [trace]");
     System.exit(1);
    }
   ObsClient oc = new ObsClient();
   if (gui)
    {
 
     applFrame = new JFrame("ObservatoryControl");
     applFrame.setSize(800,700);
     applFrame.setResizable(true);
     applFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

     oc.init() ;
     oc.start();
//    applFrame.pack();
     applFrame.setVisible(true);
    }

   oc.clientProcess();
  }

  public void start() {
    return;
    }



  public void init()
   {if (tracer) System.out.println("OCG.init()");
    applFrameContent = applFrame.getContentPane();
    applPanel = new JPanel(new GridLayout(7,1));
    applPanel.setAlignmentY(Component.LEFT_ALIGNMENT);
    applFrameContent.add(applPanel,BorderLayout.CENTER);
    bh1 = new MyButtonHandler(this);
    buildButtonPanels();
   }

 private void updatePanels()
  {if (tracer) System.out.println("OCG.updatePanels");
   waitingForServer = false;
   errorMessage = "";
   updatePanelMessage();

   if (os.getRelay7Active())
     electronics3.setSelected(true);
   else
     electronics3.setSelected(false);

   if (os.getMountParkedPowerOn())
     electronics4.setSelected(true);
   else
     electronics4.setSelected(false);

   if (os.getRelay6Active())
     electronics2.setSelected(true);
   else
     electronics2.setSelected(false);

   if (os.getRelay5Active())
     electronics1.setSelected(true);
   else
     electronics1.setSelected(false);

   if (os.getOverrideMountParked())
     parkOverride.setSelected(true);
   else
     parkOverride.setSelected(false);

   if (os.getMountPoweredUp())
     scopePowerButton1.setSelected(true);
   else
     scopePowerButton1.setSelected(false);

   if (os.getMountCameraPoweredUp())
     scopePowerButton1a.setSelected(true);
   else
     scopePowerButton1a.setSelected(false);

   if (os.getRoofClosed())
     roofStatus.setText("CLOSED");
   else if (os.getRoofOpen())
     roofStatus.setText("OPEN");
   else if (os.getRoofOpening())
     roofStatus.setText("Opening");
   else if (os.getRoofClosing())
     roofStatus.setText("Closing");
   else
     roofStatus.setText("Unknown");

   if (os.getRelay8Active())				// power to scope safe sensor
    {if (os.getMountParked())
       scopeParked1.setText(parked);
     else
       scopeParked1.setText(parkedNo);
    }
   else
    {
     scopeParked1.setText(unk);
    }

  }


 public void updatePanelMessage()
  {if (tracer) System.out.println("OCG.updatePanelMessage()");
   if (waitingForServer)
     panelMessage.setText(errorMessage);
   else
     panelMessage.setText(""); 
  }


 private void buildRoofPanel()
  {if (tracer) System.out.println("OCG.buildRoofPanel()");
   JPanel roofButtonPanel = new JPanel();
   JLabel label1 = new JLabel("Roof   ");
   JLabel label2 = new JLabel("   Status:");
   roofStatus = new JTextArea("Unknown");
   parkOverride = new JToggleButton("Scopes parked override");
   parkOverride.setActionCommand("spo");
   label1.setFont(textFontBold);
   label2.setFont(textFont);
   roofStatus.setFont(textFont);
   b01 = new JButton("Open");
   b02 = new JButton("Stop");
   b03 = new JButton("Close");
   b01.setActionCommand("b01");
   b02.setActionCommand("b02");
   b03.setActionCommand("b03");
   b01.addActionListener(bh1);
   b02.addActionListener(bh1);
   b03.addActionListener(bh1);
   parkOverride.addActionListener(bh1);
   roofButtonPanel.add(label1);
   roofButtonPanel.add(b01);
   roofButtonPanel.add(b02);
   roofButtonPanel.add(b03);
   roofButtonPanel.add(label2);
   roofButtonPanel.add(roofStatus);
   roofButtonPanel.add(parkOverride);
   applPanel.add(roofButtonPanel);
  }

 private void buildScopesPanel()
  {if (tracer) System.out.println("OCG.buildScopesPanel()");
   JPanel scopesPanel = new JPanel();
   JPanel scopesPanel2 = new JPanel(new GridLayout(2,2));
   JLabel label0 = new JLabel("Mount");
   JLabel label1 = new JLabel(scopeName1);
//   JLabel label2 = new JLabel(scopeName2);
//   JLabel label3 = new JLabel(scopeName3);
   JLabel label4 = new JLabel("   Parked:");
//   JLabel label5 = new JLabel("   Parked:");
//   JLabel label6 = new JLabel("   Parked:");
   scopeParked1 = new JTextArea(unk);
//   scopeParked2 = new JTextArea(unk);
//   scopeParked3 = new JTextArea(unk);
   scopePowerButton1  = new JToggleButton(scopePowerButtonText);
//   scopePowerButton2  = new JToggleButton(scopePowerButtonText);
//   scopePowerButton3  = new JToggleButton(scopePowerButtonText);
   scopePowerButton1a = new JToggleButton(scopePowerButton1aText); 

   scopePowerButton1.setActionCommand("s1p1");
   scopePowerButton1a.setActionCommand("s1p2");
//   scopePowerButton2.setActionCommand("s2p");
//   scopePowerButton3.setActionCommand("s3p");

   scopePowerButton1.addActionListener(bh1);
//   scopePowerButton2.addActionListener(bh1);
//   scopePowerButton3.addActionListener(bh1);
   scopePowerButton1a.addActionListener(bh1);

   label0.setFont(textFontBold);
   label1.setFont(textFont);
//   label2.setFont(textFont);
//   label3.setFont(textFont);
   label4.setFont(textFont);
//   label5.setFont(textFont);
//   label6.setFont(textFont);
   scopeParked1.setFont(textFont);
//   scopeParked2.setFont(textFont);
//   scopeParked3.setFont(textFont);

   scopesPanel2.add(scopePowerButton1);
   scopesPanel2.add(scopePowerButton1a);
//   scopesPanel2.add(label1);
   scopesPanel2.add(label4);
   scopesPanel2.add(scopeParked1);


   
//   scopesPanel2.add(label2);
//   scopesPanel2.add(label5);
//   scopesPanel2.add(scopeParked2);
//   scopesPanel2.add(scopePowerButton2);
//   scopesPanel2.add(new JLabel(""));

//   scopesPanel2.add(label3);
//   scopesPanel2.add(label6);
//   scopesPanel2.add(scopeParked3);
//   scopesPanel2.add(scopePowerButton3);
//   scopesPanel2.add(new JLabel(""));

   scopesPanel.add(label0,BorderLayout.EAST);
   scopesPanel.add(scopesPanel2,BorderLayout.CENTER);
   applPanel.add(scopesPanel);
  }

 private void buildPowerPanel()
  {if (tracer) System.out.println("OCG.buildPowerPanel()");
   JPanel powerPanel = new JPanel();
   JPanel powerPanelSub = new JPanel(new GridLayout(3,2));
   JLabel label1 = new JLabel("Power");
   electronics1 = new JToggleButton(electronicsButtonText1);
   electronics2 = new JToggleButton(electronicsButtonText2);
   electronics3 = new JToggleButton(electronicsButtonText3);
   electronics4 = new JToggleButton(electronicsButtonText4);
   JLabel electronics5 = new JLabel("");
   JLabel electronics6 = new JLabel("");
   JLabel electronics7 = new JLabel("");
   JLabel electronics8 = new JLabel("");

   electronics1.addActionListener(bh1);
   electronics2.addActionListener(bh1);
   electronics3.addActionListener(bh1);
   electronics4.addActionListener(bh1);


   electronics1.setActionCommand("r5");
   electronics2.setActionCommand("r6");
   electronics3.setActionCommand("r7");
   electronics4.setActionCommand("r8");


   label1.setFont(textFontBold);

   powerPanelSub.add(electronics1);
   powerPanelSub.add(electronics4);
   powerPanelSub.add(electronics3);
   powerPanelSub.add(electronics5);
   powerPanelSub.add(electronics6);
//   powerPanelSub.add(electronics7);
//   powerPanelSub.add(electronics8);
   powerPanelSub.add(electronics2);

   powerPanel.add(label1,BorderLayout.EAST);
   powerPanel.add(powerPanelSub,BorderLayout.CENTER);
   applPanel.add(powerPanel);   
  }

 private void buildComputerPanel()
  {if (tracer) System.out.println("OCG.builComputerPanel()");
   JPanel computerPanel = new JPanel();
   JPanel computerPanelSub = new JPanel(new GridLayout(1,3));
   JLabel label1 = new JLabel("Wake on Lan");
   b04 = new JButton(b04Text);
//   b05 = new JButton(b05Text);
//   b06 = new JButton(b06Text);

   b04.setActionCommand("b04");
//   b05.setActionCommand("b05");
//   b06.setActionCommand("b06");

   b04.addActionListener(bh1);
//   b05.addActionListener(bh1);
//   b06.addActionListener(bh1);

   label1.setFont(textFontBold);
   computerPanelSub.add(b04);
//   computerPanelSub.add(b05);
//   computerPanelSub.add(b06);
   computerPanel.add(computerPanelSub,BorderLayout.CENTER);
   computerPanel.add(label1,BorderLayout.EAST);
   computerPanel.add(computerPanelSub,BorderLayout.CENTER);
   applPanel.add(computerPanel);
  }

 private void buildButtonPanels()
  {if (tracer) System.out.println("OCG.buildButtonPanels()");
   buildRoofPanel();
   buildScopesPanel();
   buildPowerPanel();
   buildComputerPanel();
   JPanel refreshPanel = new JPanel();
   b07 = new JButton("Refresh");
   b07.addActionListener(bh1);
   b07.setActionCommand("b07");
   refreshPanel.add(new JLabel(" "),BorderLayout.EAST);
   refreshPanel.add(b07);
   refreshPanel.add(new JLabel(" "),BorderLayout.CENTER);
   refreshPanel.add(new JLabel(" "),BorderLayout.WEST);
   applPanel.add(refreshPanel);
   panelMessage.setFont(textFontBold);
   applPanel.add(panelMessage);
  } 

 private String resetSocket()
  {if (tracer) System.out.println("OCG.resetSocket()");
   String fromServer = null;

   if (socket != null)
    {try {socket.close();}
     catch (IOException e) {}
    }

   try 
    {socket = new Socket(hostName, portNumber);
     out = new PrintWriter(socket.getOutputStream(), true);
     out.println(securityCode);
     in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
    }
   catch (SocketException e)
    {System.out.println("OCG - Unable to [re]build socket connection");
     System.out.println(e);
     System.exit(8);
    }
   catch (Exception e) {}
   fromServer = "";

   try 
    {fromServer = in.readLine();
     readWaitCount = readWaitCountMax;
     if (tracer) System.out.println(" resetSocket()  Server response: " + fromServer);
    }
   catch (Exception e)
    {// if (tracer)
      {System.out.println("OCG.clientProcess(socket fail)");
       System.out.println('\n'); System.out.println(e);
       if (gui)
         errorMessage = "Connection with server not established";
	 updatePanelMessage(); 
	 try {Thread.sleep(2000);}
         catch (Exception e1) {}	
      }
    }

   try {socket.setSoTimeout(1000);}
   catch (SocketException e)
    {System.out.println("Unable to set socket timeout");
     System.exit(8);
    }
   return fromServer;
  }

 public void clientProcess()
  {if (tracer) System.out.println("OCG.clientProcess()");
   ObsClientConsoleReader occr = new ObsClientConsoleReader();
     String fromServer = "";
     String fromUser = "";
     String sb = "0";

   fromServer = resetSocket();
   BufferedReader stdIn =
	new BufferedReader(new InputStreamReader(System.in));
   occr.setReader(stdIn);
   occr.start();


/* This routine reads data from the socket. a 1 second timeout within a loop is
   used to detect missing responses from the server. The loop will normally sit
   forever because the initial counter is larger than 24 hours to support receipt
   of server messages that are generated from other sources. When a command that
   should have a response is sent to the server, the count is set to a few seconds.
   When no response has been received to the command, the socket is closed and 
   reopened. The command is NOT resent. The user needs to recognize that the 
   command was not received by the following happening:
    1. "Waiting for server response" message appears for a few seconds
    2. Toggle does not change state after the waiting message disappears
   If the command was a button, there's no way to detect if the command was
   received at the server and processed.
*/
   while (true)
    {if (fromServer.equals(""))
      {while (readWaitCount-- > 0)
        {if (tracer)
           if ((readWaitCount > (readWaitCountMax-5))|(readWaitCount < 10)
		| (((readWaitCount/30)*30) == readWaitCount)
	      )
            {System.out.println("   loopCt "+readWaitCount+" "+
				sdf.format(new Date())
				);
	    }
         try {fromServer = in.readLine();}
         catch (SocketTimeoutException e)
	  {if (readWaitCount <= 0)
	    {fromServer = resetSocket();
	     break;
	    }
	   else
	     continue;
	  }
         catch (IOException e)
	   {System.out.println("IO error reading data from host");
	    System.exit(8);
	   }
         try 
          {if (fromServer.equals(""))
             continue;
           else
            {readWaitCount = readWaitCountMax;
	     break;
            }
          }
         catch (NullPointerException e)
          {System.out.println("Server closed connection");
	   System.exit(0);
	  }
        }
      }
         if (tracer) 
	   {System.out.println("   Server response: " + fromServer);
	    System.out.println(" test output");
	   }	

         if (fromServer == null)
           System.exit(0);
         if (fromServer.equals("quit"))
           break;
	 if (tracer)
	   sb = "1";
	 else
	   sb = "0";
	 fromServer = sb.concat(fromServer.substring(1));
         os.setAll(fromServer);
         fromServer = "";
         if (gui)
	   updatePanels();
         else
           displayUpdatedStatus();
    }

  }

 public void processUserInput(String userIn)
  {if (tracer) System.out.println("OC.processUserInput '"+userIn+"'");
   lastUserInput = userIn;

   if (userIn.equals("1") | userIn.equals("2"))
    {if (os.getMountParked()
         | os.getOverrideMountParked()
         | (!os.getMountParkedPowerOn())
        )
      {try {out.println(userIn);}			// send to server
       catch (Exception e) {System.out.println(e);}
       readWaitCount = readWaitShort;
       waitingForServer = true;
      }
     else
      {errorMessage = "Scopes not parked - cannot move roof";

       if (gui)
         updatePanelMessage();
       else 
         System.out.println(errorMessage);
      }
    }
   else
    {out.println(userIn);				// send to server
     if (userIn.equals("99"))				// no wait needed 
       {}
     else
      {readWaitCount = readWaitShort;
       errorMessage = "Waiting for server response";
       waitingForServer = true;
       if (gui)
         updatePanelMessage();
       else
         System.out.println(errorMessage);
      }
    }
   if (userIn.equals("99"))
     System.exit(0);
  }

 private void displayUpdatedStatus()
  {if (tracer) System.out.println("OC.displayUpdatesStatus()");
   for (int i=0;i<12;i++)
     System.out.println("");
   System.out.println(errorMessage);
   waitingForServer = false;
   errorMessage = "";
/*
   try {Runtime.getRuntime().exec("/usr/bin/clear");}	// clear the screen
   catch (IOException e) {System.out.println("clear console error:");
			  System.out.println(e);
			 }
*/
   String s1 ="", s2 = "";

   if (os.getRoofOpen()) s1 = "Roof is     open";
   else 		 s1 = "Roof is not open";
   if (os.getRoofOpening()) s2 = "Roof is     opening";
   else			    s2 = "Roof is not opening";
   System.out.println(s1+"\t"+"\t"+"\t"+s2);

   if (os.getRoofClosed()) s1 = "Roof is     closed";
   else			   s1 = "Roof is not closed";
   if (os.getRoofClosing()) s2 = "Roof is     closing";
   else			    s2 = "Roof is not closing";
   System.out.println(s1+"\t"+"\t"+"\t"+s2);
// System.out.println("");

   if (os.getMountParked())
     parkedWord = "parked";
   else
     parkedWord = "\t";
   if (os.getMountParked() |(!os.getMountParkedPowerOn()))
     s1 = "Mount   is     ";
   else		 	     s1 = "Mount   is not ";
   s1 = s1+parkedWord;
   s2 = "Override scopes parked is: ";
   if (os.getOverrideMountParked())	s2 = s2+"TRUE";
   else					s2 = s2+"FALSE";
   System.out.println(s1+"\t"+"\t"+"\t"+s2);

// System.out.println("");
   
   if (os.getMountPoweredUp()) s1 = "Mount       is     powered up";
   else			       s1 = "Mount       is not powered up";
   if (os.getMountCameraPoweredUp()) s2 = "Mount camera is     powered up";
   else				  s2 = "Mount camera is not powered up";
   System.out.println(s1+"\t\t"+s2);


   s2 = "Scopes parked sensors have power: ";
   if (os.getMountParkedPowerOn())
        s2 = s2 + "YES";
   else s2 = s2 + "NO";
   System.out.println(s1+"\t\t"+s2);


   if (os.getRelay5Active())  s1 = "Relay5      is     powered   ";
   else			      s1 = "Relay5      is not powered   ";
   if (os.getRelay6Active())  s2 = "Relay6     is     powered";
   else			      s2 = "Relay6     is not powered";
   System.out.println(s1+"\t\t"+s2);

   if (os.getRelay7Active())  s1 = "Relay7      is     powered   ";
   else			      s1 = "Relay7      is not powered   ";
   if (os.getRelay8Active())  s2 = "Relay8     is     powered";
   else			      s2 = "Relay8     is not powered";
   System.out.println(s1+"\t\t"+s2);

   System.out.println(""); 
   System.out.println("     Enter a command");
   System.out.println(" 1 Open  roof			 4 Toggle scope safe bypass");
   System.out.println(" 2 Close roof			 5 Toggle relay7 {Canon pwr) ");
   System.out.println(" 3 Stop  roof");
   System.out.println("");
   System.out.println(" 6 Toggle mount power             8 Toggle scopes parked power");
   System.out.println(" 7 Toggle mount camera power      9 Toggle relay5 (48V)    ");

   System.out.println("10 Toggle computer power");
   System.out.println("12 Wake up  computer (Abe)");
   System.out.println("98 Refresh display               99 Exit program");
   System.out.println(""); 
  }
     	

public class ObsClientConsoleReader extends Thread
  {private BufferedReader br;
   private String inData="";
   public ObsClientConsoleReader()
    {}

   public void setReader(BufferedReader br)
    {this.br = br;
    }

   public void run()
    {
     while (true)
      {try {inData = br.readLine();
            processUserInput(inData);
	    if (inData == null)
	     {System.out.println("Null input from console");
              break;
             }
            if (inData.equals("quit"))
	      break;
           }
       catch (IOException e)
        {System.out.println("I/O error reading from console") ;
        }
      }
    }
  }


class MyButtonHandler implements ActionListener
 {private ObsClient oc;
  public MyButtonHandler(ObsClient a)
   {oc = a;
   }
  public void actionPerformed(ActionEvent e)
   {String ac = e.getActionCommand();
    if (oc.tracer) System.out.println("OCG_BH.actionPerformed("+ac+")");
//    System.out.println(oc.parkOverride.isSelected());   
    if (ac.equals("b01"))
     {if (oc.tracer) System.out.println("  Open roof");
      oc.processUserInput("1");
     }
    else if (ac.equals("b02"))
     {if (oc.tracer) System.out.println("  Stop roof");
      oc.processUserInput("3");
     }
    else if (ac.equals("b03"))
     {if (oc.tracer) System.out.println("  Close roof");
      oc.processUserInput("2");
     }
    else if (ac.equals("b07"))
     {if (oc.tracer) System.out.println("  Refresh display");
      oc.processUserInput("98");
     }
    else if (ac.equals("spo"))
     {if (oc.tracer) System.out.println("  Scopes parked override");
      oc.processUserInput("4");
     }
    else if (ac.equals("s1p1"))
     {if (oc.tracer) System.out.println("  Scope 1 Power 1");  // Abe scope power
      oc.processUserInput("6");
     }
    else if (ac.equals("s1p2"))
     {if (oc.tracer) System.out.println("  Scope 1 Power 2");  //Scope cam - scope power reqd
      oc.processUserInput("7");
     }
    else if (ac.equals("r5"))
     {if (oc.tracer) System.out.println("  Relay5"); 		// 48V power
      oc.processUserInput("9");
     }
    else if (ac.equals("r6"))
     {if (oc.tracer) System.out.println("  Relay6");		// 12V computer NC disconnect
      oc.processUserInput("10");
     }
    else if (ac.equals("r7"))
     {if (oc.tracer) System.out.println("  Relay7");		// Canon / 5V power
      oc.processUserInput("5");
     }
    else if (ac.equals("r8"))
     {if (oc.tracer) System.out.println("  parked sensors power");
      oc.processUserInput("8");
     }
    else if (ac.equals("b05"))
     {if (oc.tracer) System.out.println("  Mount computer");	// wakeup command
      oc.processUserInput("12");
     }
    else
     {System.out.println("    Unknown action command: "+ac);
      if (oc.errorMessage.equals(""))
       {oc.errorMessage = "    Unknown action command: "+ac;
        oc.updatePanelMessage();
       }
     }
   }

 }

}