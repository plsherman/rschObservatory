/*
* This code was built from Oracle's Knock Knock multi server thread code
* It is started by receipt of a connection by the server
* It listens for communication from the socket and is notified of all changes to
*   the observatory status flags which it sends to the requesting client
*
* Input commands from the user will be numbers, indicating what function is to be
* performed
* 
* 01 - open the roof
* 02 - close the roof
* 03 - stop the roof
* 04 - toggle scope safe bypass
* 05 - push inverter power button
* 06 - toggle scope 1 power 1
* 07 - toggle scope 1 power 2
* 08 - toggle scopes parked sensor power
* 09 - toggle scope2 power
* 10 - toggle scope 3 power
* 11 - toggle computer1 power
* 12 - wakeup Abe computer
* 13 - toggle NAS power
* 14 - wakeup Phil computer
* 15 - toggle lights
* 98 - refresh the client display
* 99 - quit
*
* all other numeric entries will be ignored - error message to console
*
* Maintenance history
* 2026/07/03 PLS add additional print statements for diagnostic work
* 2026/07/06 PLS add user error msg if bad security code passed.
*                add timestamp to start/stop messages
*
 */ 
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.net.*;
import java.io.*;
import java.util.Observer;
import java.util.Observable;

public class ObsWorkerThread extends Thread implements Observer
 {private Socket socket = null;
  private ObsControl oc;
  private ObsStatus  os;
  private int socketTimeout = 1000;	// time in milliseconds
  private int useCount = 0;
  private PrintWriter out;
  private static final
	String securityCode = "d43909dbd40f9e6861e2676945e74992";
  private static boolean tracer = false;
  private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
  
  public ObsWorkerThread(Socket socket,ObsControl oc, ObsStatus os)
   {super("ObsWorkerThread");
    this.socket = socket;
    this.oc = oc;
    this.os = os;
    tracer = os.getTracer();
   }

  public void run()
   {if (tracer) System.out.println("OWT connected to: "+socket.getInetAddress()
                                   +" at "+LocalTime.now().format(dtf)
                                  );
    try
     {out = new PrintWriter(socket.getOutputStream(), true);
      BufferedReader in = new BufferedReader
	      (new InputStreamReader(socket.getInputStream()));
      os.addObserver(this); 
      String inputLine = "", outputLine = "";
      update (os,inputLine);
//      outputLine = "Output line from worker bee ";
//      out.println(outputLine+useCount);
//      outputLine = "Additional output line from worker bee ";
      boolean continueProcessing = true;
      socket.setSoTimeout(socketTimeout);	// short delay for security code 
      try {inputLine = in.readLine();}
      catch (SocketTimeoutException e)
       {System.out.println("OWT security code not received in time");
       }
      if (!inputLine.equals(securityCode))	// check for valid client
       {continueProcessing = false;
        System.out.println("OWT bad scty code - disconnected ["+inputLine+"]");
        inputLine = "Bad security code - disconnected";
       }
      socket.setSoTimeout(0);			// allow infinite wait
      while (continueProcessing)
       {try {inputLine = in.readLine();}
        catch (SocketTimeoutException e)
         {}
        if (inputLine == null)			// remote pgm disconnected
          break; 
        if (inputLine.equals("quit"))
          break;

        ObsRequest request = ObsRequest.unmarshall(inputLine);

	    if (tracer)
	      System.out.println("OWT worker thread has request: " + request);

        switch (request)
         {case NO_OP:
            break;
          case OPEN_ROOF:
            oc.openRoof();
            break;
          case CLOSE_ROOF:
            oc.closeRoof();
            break;
          case STOP_ROOF:
            oc.stopRoof();
            break;
          case TOGGLE_OVERRIDE_SCOPES_PARKED:
            oc.setOverrideScopesParked(!os.getOverrideScopesParked());
            break;
          case PUSH_INVERTER_POWER_BUTTON:
            oc.pushInverterPowerButton();
            break;
          case TOGGLE_POWER_SCOPE1_POWER1:
            oc.togglePowerS1R1();
            break;
          case TOGGLE_POWER_SCOPE1_POWER2:
            oc.togglePowerS1R2();
            break;
          case TOGGLE_SCOPES_PARKED_POWER:
            oc.toggleScopesParkedPower();
            break;
          case TOGGLE_POWER_SCOPE2:
            oc.togglePowerS2();
            break;
          case TOGGLE_POWER_SCOPE3:
            oc.togglePowerS3();
            break;
          case TOGGLE_POWER_COMPUTER1:
            oc.togglePowerComputer1();
            break;
          case WAKE_UP_ABE_LAPTOP:
            oc.wakeUp("Abe");
            break;
          case WAKE_UP_ABE_DESKTOP:
            System.out.println("Unsupported request "+request+" ignored");
            break;
          case TOGGLE_POWER_NAS:
            oc.togglePowerNAS();
            break;
          case WAKE_UP_PHIL:
            oc.wakeUp("Phil");
            break;
          case TOGGLE_LIGHTS:
            oc.toggleLights();
            break;
          case REFRESH_DISPLAY:
            refresh();
            break;
          case STOP_PROCESSING:
            continueProcessing = false;
            break;
          default:
            System.out.println("Unsupported request "+request+" ignored");
            break;
         }
        if (continueProcessing)
	      inputLine = "";
        else
          inputLine = "quit";
       }				// end of while loop
      out.println(inputLine);
      if (tracer) System.out.println("OWT disconnect from: "+socket.getInetAddress()
                                   +" at "+LocalTime.now().format(dtf)
                                  );
      socket.close();
     }
    catch (IOException e)
     {e.printStackTrace();
     }
   }



  public void update(Observable ob,Object obj)
/***********************************************************************
* invoked whenever changes occur to obs status. convert flags to a 
* string and send them to the client
***********************************************************************/
   {String s = os.getAll();
    if (tracer) System.out.println("OWT.update("+s+") "+socket.toString());
    out.println(s);
   }  



  public void refresh()
/***********************************************************************
* invoked whenever changes occur to obs status. convert flags to a 
* string and send them to the client
***********************************************************************/
   {String s = os.getAll();
    if (tracer) System.out.println("OWT.refresh("+s+") getAll() precedes");
    out.println(s);
   }
}
