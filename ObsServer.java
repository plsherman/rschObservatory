/*
* Server code for the observatory control program. This is based on the 
* Knock Knock server example from Oracle
*
* This program monitors the incoming traffic port and passes requests off to a
* worker program that monitors the observtory controls status and communicates them
* to the requesting client. The worker program also listens for traffic directed to
* it to make changes to the status of the observatory
*
*/

import java.net.*;
import java.io.*;

public class ObsServer
{
 public static void main(String[] args) throws IOException
  {boolean tracer = true;
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
       System.out.println("  No port supplied - using default "+portNum);
      }
    } // end for


   ObsStatus os = new ObsStatus();
   os.setTracer(tracer);
   ObsControl oc = new ObsControl(os,tracer);
     initializeVoltmeter();
       
   try (ServerSocket serverSocket = new ServerSocket(portNum))
    {while (true)
      {new ObsWorkerThread(serverSocket.accept(),oc,os).start();
      }
    }
   catch (IOException e)
    {System.err.println("Could not listen on port " + portNum);
     System.exit(-1);
    }
  }
 
 private static void initializeVoltmeter()
  {try
    {Process p = Runtime.getRuntime().exec("sudo readit.py 1");
     BufferedReader stdInput = new BufferedReader(new 
                 InputStreamReader(p.getInputStream()));
     String voltage = stdInput.readLine();
    }
   catch (IOException e)
    {System.out.println("Error reading from Python routine\n");
    	e.printStackTrace();
//    	System.exit(-1);

   }

  } 
}
