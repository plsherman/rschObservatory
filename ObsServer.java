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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class ObsServer
{
 private static class WorkerThreadFactory implements ThreadFactory
  {private final AtomicInteger numThreads = new AtomicInteger();

   @Override
   public Thread newThread(Runnable runnable)
    {String name = String.format("ObsWorkerThread-%03d", numThreads.getAndAdd(1));
     Thread thread = new Thread(runnable, name);
     thread.setDaemon(true);
     return thread;
   }
 }

 public static void main(String[] args)
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

   try (ExecutorService executor = Executors.newCachedThreadPool(new WorkerThreadFactory());
        ServerSocket serverSocket = new ServerSocket(portNum))
    {serverSocket.setSoTimeout(0);    // never close socket
     while (true)
      {try
       {ObsWorker worker = new ObsWorker(serverSocket.accept(), oc, os);
         executor.execute(worker);
       } catch (SocketException e)
        {System.out.println("OS.main() closed socket detected - shutdown");
         if (os.getNASPoweredUp())
           oc.togglePowerNAS(); // controlled shutdown may take 60 seconds
         System.exit(1);
       }  // catch
     }    // while
   }      // try-with-resources
   catch (IOException e)
    {System.out.println("  Could not listen on port " + portNum);
     System.exit(-1);
   }
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
}
