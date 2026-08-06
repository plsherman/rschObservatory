/*
* Server code for the observatory control program. This is based on the 
* Knock Knock server example from Oracle
*
* This program monitors the incoming traffic port and passes requests off to a
* worker program that monitors the observtory controls status and communicates them
* to the requesting client. The worker program also listens for traffic directed to
* it to make changes to the status of the observatory
*
* ****************************************************************
*   WARNING WARNING WARNING WARNING WARNING
*
* This server program requires 1 of 2 non application things
* 1. Run as service with automatic restart if program ends
* 2. Run another script/application using "ps" to verify that
*    it's running and restart it if it isn't.
*
* Socket shutdowns appear to happen if no connections for a long
* period of time (?days?). No connections means everything should
* be off and the roof closed. The NAS might be powered on.
*
* ****************************************************************
* Recent Maintenance History
* 2026/07/08 PLS add test for socket timeout - shutdown
*
*
*
*
*
*
*
*/

import java.net.*;
import java.io.*;
import java.time.*;
import java.util.concurrent.TimeUnit;
import java.net.InetAddress;

public class ObsServer
{private static ServerSocket serverSocket = null;
	
 public static void main(String[] args) throws IOException
  {System.out.println("OS.main()");
   boolean tracer = true;
   int defaultPortNum = 8080
	,portNum = 0
	;

   for (String s1 : args)
    {s1 = s1.toUpperCase();
     switch (s1)
      {case "TRACE" -> tracer=true;
       case "TRUE"  -> tracer = true;
       case "FALSE" -> tracer = false;
       default      ->
        {try {portNum = Integer.parseInt(s1);}
	 catch (NumberFormatException e)
          {System.out.println("  Unrecognized parameter (uppercased) ["+s1+"]");
	  }
        } // end default 
      } // end switch
     if (portNum == 0)
      {portNum = defaultPortNum;
       if (tracer) System.out.println("  No port supplied - using default "+portNum);
      }
    } // end for

   initializeVoltmeter();
   if(tracer) System.out.println("  voltmeter initialized");

   ObsStatus os = new ObsStatus();
   os.setTracer(tracer);
   ObsControl oc = new ObsControl(os,tracer);
   ObsServerMonitor obsServerMonitor = new ObsServerMonitor(os,tracer);
   obsServerMonitor.start();

   try
    {serverSocket = new ServerSocket(portNum);
     serverSocket.setSoTimeout(0);	// never close socket
     while (true)
      {try {new ObsWorkerThread(serverSocket.accept(),oc,os).start();}
       catch (SocketException e)
        {System.out.println("OS.main() closed socket detected - shutdown");
         if (os.getNASPoweredUp())
           oc.togglePowerNAS();	// controlled shutdown may take 60 seconds
         System.exit(1); 
        }
      }   // while
    }     // try
   catch (IOException e)
    {System.out.println("  Could not listen on port " + portNum);
     System.exit(-1);
    }

  }

/*
 *   this method is only called by the monitor routing as of 2026/08/05 
*/ 
 public static void shutdownServer(boolean tracer)
  {if (tracer) System.out.println("oServer.shutdownServer");
   try {serverSocket.close();}
   catch (Exception e) {}			// ignore errors 
   System.exit(1);
  }


 private static void initializeVoltmeter()
  {try
    {Process p = Runtime.getRuntime().exec(new String[]{"sudo readit.py","1"});
     BufferedReader stdInput = new BufferedReader(new 
                 InputStreamReader(p.getInputStream()));
     String voltage = stdInput.readLine();
    }
   catch (IOException e)
    {System.out.println("OS.initializeVoltmeter() Error reading from Python routine\n");
    	e.printStackTrace();
//    	System.exit(-1);

   }

  }

		 
  
 public static class ObsServerMonitor extends Thread
  {private static LocalTime
	              timeCurrent
				 ,timeLast
				 ;
   private static ObsServer server;
   private static ObsStatus os;
   private static boolean tracer = false
						 ,pingGood = false
						 ;
   private static int testInterval = 15
					 ;
	 private static String routerIPAddress ="192.168.65.1";
	 private static String[] IPAddresses = {"192.168.0.1","192.168.65.1"}; // observatory & Phil's house
	 private static InetAddress routerInetAddress = null;

   public ObsServerMonitor(ObsStatus oStatus, boolean b1)
    {// server = oServer;
	 os = oStatus;
	 tracer = b1;
	} 

   public void run()
    {if (tracer) System.out.println("OSM.run()");
	 timeLast = LocalTime.now();
	 int reportInterval = 4*60;  // (4hrs * 60min) report every 4 hours
	 
// *******************************  start test only code  ***********************
//	 reportInterval = 2;
//	 testInterval   = 1;
// *******************************  end   test only code  ***********************
int killCount = 5;
	 while (true)
	  {
	   try {TimeUnit.MINUTES.sleep(testInterval);}
	   catch(InterruptedException e) {}
	   timeCurrent = LocalTime.now();
	   if (timeCurrent.minusMinutes(reportInterval).isAfter(timeLast))
	    {if (tracer) System.out.println("OSM.run() all is well at "+timeCurrent);
		 timeLast = timeCurrent;
	    }
	   if (pingTest())
	     continue;
/*
 *
 * Ping failed - wait 3 minutes then try again. 2nd failure force restart by exiting 
*/
       try {TimeUnit.MINUTES.sleep((long)3);}
	   catch(InterruptedException e) {} 
	   if (!pingTest())
	    {System.out.println("two 3 min apart pings failed - terminating ObsServer");
		 shutdownServer(true);
	    }    
      } 
    } 
  private static boolean pingTest()
   {if (tracer) System.out.println("OSM.pingTest()");
	pingGood = false;
	try
	 {for (String routerIPAddress : IPAddresses)
       {routerInetAddress = InetAddress.getByName(routerIPAddress);
		if (tracer) System.out.println("  testing ["+routerIPAddress+"]");
		if (routerInetAddress.isReachable(125))		// response s/b < 1/8 second
	     {pingGood = true;
		  break;
	     }
	   }  
     }
    catch (IOException e)
     {System.out.println("   ping IOException\n   "+e);
	  System.exit(1);
	 }
	return pingGood;
   }
  }
  
			
}
