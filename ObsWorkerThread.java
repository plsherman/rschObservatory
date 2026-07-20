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
* 2026/07/12 PLS add more exception handling and file/socket close calls
*



**********************************************************************
 REMOVE s2 CODE IF NO TESTING ERRORS  source code lines @215+
**********************************************************************
*/
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.net.*;
import java.io.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class ObsWorkerThread extends Thread implements PropertyChangeListener
 {private Socket socket = null;
  private ObsControl oc;
  private ObsStatus  os;
  private int socketTimeout = 1000      // time in milliseconds
             ,useCount      = 0
             ,requestNum    = 0
             ,socketPort   = 0
             ;
  private PrintWriter out;
  private BufferedReader in;
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
           +":"+socket.getPort()+" at "+LocalTime.now().format(dtf)
                                  );
      if (tracer) System.out.println("  this id ["+this+"]");
      os.addListener(this);
      socketPort = socket.getPort();

    try
     {out = new PrintWriter(socket.getOutputStream(), true);
      out.println("");                  // prime output queue
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      String inputLine = "", outputLine = "";

      int requestNum = 0;               // input from client is number <= 99
      boolean continueProcessing = true;
      socket.setSoTimeout(socketTimeout);       // short delay for security code
      try {inputLine = in.readLine();}
      catch (SocketTimeoutException e)
       {System.out.println("OWT security code not received in time");
       }
      if (!inputLine.equals(securityCode))      // check for valid client
       {continueProcessing = false;
        System.out.println("OWT bad scty code - disconnected ["+inputLine+"]");
        inputLine = "Bad security code - disconnected";
       }
      socket.setSoTimeout(0);                   // allow infinite wait
      while (continueProcessing)
       {try {inputLine = in.readLine();}
        catch (SocketTimeoutException e)
         {}
        if (inputLine == null)                  // remote pgm disconnected
          break;
        if (inputLine.equals("quit"))
          break;

        try {requestNum = Integer.parseInt(inputLine);}
        catch (NumberFormatException e)
         {requestNum = 0;
         }

        if ((tracer) & (requestNum != 0))
          System.out.println("OWT worker thread has request :"+requestNum);

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
       }                                // end of while loop
// *****************************  WHILE LOOP END  *******************
      out.println(inputLine);
      if (tracer) System.out.println("OWT disconnect from: "+socketPort
                                   +" at "+LocalTime.now().format(dtf)
                                  );
     }
    catch (IOException e)
     {e.printStackTrace();
     }

    try {out.close();}
    catch (Exception e)         // javadoc says IOException causes compile fail
      {System.out.println("OWT  socket writer close failed for port "
                         +socketPort
                         );
      }
    try {in.close();}
    catch (IOException e)
      {System.out.println("OWT  socket reader close failed for port "
                         +socketPort
                         );
      }
    try {socket.close();}
    catch (IOException e)
      {System.out.println("OWT  socket close failed for port "
                         +socketPort
                         );
      }
    os.removeListener(this);
    return;
   }


  @Override
  public void propertyChange(PropertyChangeEvent e)
/***********************************************************************
* invoked whenever changes occur to obs status. convert flags to a
* string and send them to the client
***********************************************************************/
   {String s1 = (String)e.getNewValue();
    if (tracer) System.out.println("OWT.propertyChange"+s1+") "+socket.toString());

//  *****************  start remove code after testing  *********
    String s2 = os.getAll();
    if (!s1.equals(s2))
     {if (tracer)
       {System.out.println(socketPort
            +"OWT.update() observed data does not match retrieved data"
        );
        System.out.println("  observed  ["+s1+"]");
        System.out.println("  retrieved ["+s2+"]");
       }
     }
//  *****************  end   remove code after testing  *********

    out.println(s1);
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
