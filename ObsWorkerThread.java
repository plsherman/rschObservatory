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
* 13 - toggle computer2 power
* 14 - wakeup Phil computer
* 15 - toggle lights
* 98 - refresh the client display
* 99 - quit
*
* all other numeric entries will be ignored - error message to console
*
*
*
*
*
*
 */ 

import java.net.*;
import java.io.*;
import java.util.Observer;
import java.util.Observable;

public class ObsWorkerThread extends Thread implements Observer
 {private Socket socket = null;
  private ObsControl oc;
  private ObsStatus  os;
  private int socketTimeout = 250;	// time in milliseconds
  private int useCount = 0;
  private PrintWriter out;
  private static final
	String securityCode = "d43909dbd40f9e6861e2676945e74992";
  private static boolean tracer = false;
  
  public ObsWorkerThread(Socket socket,ObsControl oc, ObsStatus os)
   {super("ObsWorkerThread");
    this.socket = socket;
    this.oc = oc;
    this.os = os;
    tracer = os.getTracer();
   }

  public void run()
   {try
     {out = new PrintWriter(socket.getOutputStream(), true);
      BufferedReader in = new BufferedReader
	      (new InputStreamReader(socket.getInputStream()));
      os.addObserver(this); 
      String inputLine = "", outputLine = "";
      update (os,inputLine);
      int requestNum = 0;
//      outputLine = "Output line from worker bee ";
//      out.println(outputLine+useCount);
//      outputLine = "Additional output line from worker bee ";
      boolean continueProcessing = true;
      try {inputLine = in.readLine();}
      catch (SocketTimeoutException e) {}
      if (!inputLine.equals(securityCode))	// check for valid client
	continueProcessing = false;
//    socket.setSoTimeout(socketTimeout);
      while (continueProcessing)
       {try {inputLine = in.readLine();}
        catch (SocketTimeoutException e)
         {}
        if (inputLine == null)
          break; 
        if (inputLine.equals("quit"))
          break;

        try {requestNum = Integer.parseInt(inputLine);}
        catch (NumberFormatException e)
	 {requestNum = 0;
         }

	if ((tracer) & (requestNum != 0))
	  System.out.println("worker thread has request :"+requestNum);
 
       switch (requestNum)
	 {case 0: break;
          case 1:
	    oc.openRoof();
	    break;
	  case 2:
	    oc.closeRoof();
	    break;
	  case 3:
	    oc.stopRoof();
	    break;
	  case 4:
	    if (os.getOverrideScopesParked())
	      oc.setOverrideScopesParked(false);
	    else
	      oc.setOverrideScopesParked(true);
	    break;
	  case 5:
	    oc.pushInverterPowerButton();
	    break;
	  case 6:
	    oc.togglePowerS1R1();
	    break;
	  case 7:
	    oc.togglePowerS1R2();
	    break;
	  case 8:
	    oc.toggleScopesParkedPower();
	    break;
	  case 9:
	    oc.togglePowerS2();
	    break;
	  case 10:
	    oc.togglePowerS3();
	    break;
	  case 11:
	    oc.togglePowerComputer1();
	    break;
	  case 12:
	    oc.wakeUp("Abe");
	    break;
	  case 13:
	    oc.togglePowerNAS();
	    break;
	  case 14:
	    oc.wakeUp("Phil");
	    break;
	  case 15:
	    oc.toggleLights();
	    break;
	  case 98:
	    refresh();
	    break;
	  case 99:
	    continueProcessing = false;
	    break;
	  default:
	    System.out.println("OWT.run() unknown user request: "+requestNum);
	    break;
	 }
        requestNum = 0; 
        if (continueProcessing)
	  inputLine = "";
	else
	  inputLine = "quit";      
       }				// end of while loop
      out.println(inputLine);
      if (tracer) System.out.println("   Closing worker thread socket and terminating");
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
