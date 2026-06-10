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
   ObsStatus os = new ObsStatus();
   os.setTracer(tracer);

   initializeVoltmeter();

   ObsControl oc = new ObsControl(os,tracer);
   int defaultPortNum = 8080
	,portNum = 0
	;
   if (args.length < 1)
    {portNum = defaultPortNum;
     System.out.println("Listening on port "+portNum);
    }
   else
    {if (args.length != 1)
      {System.err.println("Usage: java ObsServer <port number>");
       System.exit(1);
      }
     else
       portNum = Integer.parseInt(args[0]);
    }
        
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
