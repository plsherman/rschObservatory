/* This is the data area that is used to communicate the status of 
   all monitored functions in the observatory.

  2016-03-22 PLS initial authoring
  2017-01-01 PLS add fetch for tracer
  2020-07-16 PLS add field for voltage - code to update and fetch
  2026-06-09 PLS recode Runtime .exec for new java version
  2026-06-13 PLS change computer2 to backupDrive, 


*/
import java.util.Observable;
import java.io.*;

public class ObsStatus extends Observable 
{boolean tracer = false				// 0
	,roofOpen = false			// 1
	,roofClosed = false			// 2
	,roofOpening = false			// 3
	,roofClosing = false			// 4
	,scope2Parked = false			// 5
	,scope1Parked = false			// 6
	,overrideScopesParked = false		// 7
	,acPowerAvailable = false		// 8
	,scope1aPoweredUp = false		// 9
	,scope1bPoweredUp = false		// 10
	,scope2PoweredUp = false		// 11
	,scope3PoweredUp = false		// 12
	,computer1PoweredUp = false		// 13
	,NASPoweredUp = false			// 14
	,lightsOn = false			// 15
	,scope3Parked = true			// 16
	,scopesParkedPowerOn			// 17
	;
 int flagCount  = 16;
 int j = 0;
 boolean newState = false;
 String oldStatus = "", newStatus = "", voltage = "UNK";

 public void setAll(String flags)
/*************************************************************************
*  This is not used for communication changes - only to initialize a copy 
*************************************************************************/
  {if (tracer) System.out.println("OS.setAll("+flags+")");
   String[] parts = flags.split(" ");

   voltage = parts[parts.length - 1];  // set new voltage value

   for (int i=0;i<(parts.length - 1);i++) // -1 for voltage 
    {j = Integer.parseInt(parts[i]);
     if (j==0)
       newState = false;
     else
       newState = true;
     switch (i)
      { case 0: tracer = newState; 		break;
	case 1: roofOpen = newState; 		break;
	case 2: roofClosed = newState; 		break;
	case 3: roofOpening = newState; 	break;
	case 4: roofClosing = newState; 	break;
	case 5: scope2Parked = newState; 	break;
	case 6: scope1Parked = newState; 	break;
	case 7: overrideScopesParked = newState; break;
	case 8: acPowerAvailable = newState; 	break;
	case 9: scope1aPoweredUp = newState;	break;
	case 10: scope1bPoweredUp = newState;	break;
	case 11: scope2PoweredUp = newState;	break;
	case 12: scope3PoweredUp = newState;	break;
	case 13: computer1PoweredUp = newState; break;
	case 14: NASPoweredUp = newState;	break;
	case 15: lightsOn = newState;		break;
	case 16: scope3Parked = newState;	break;
	case 17: scopesParkedPowerOn= newState; break;
  	default: System.out.println("OS.setAll("+flags+") too many flags");
      }
// *****************   readVoltage()  inserted here  ********************
    }
  }

 public String getAll()
  {if (tracer) System.out.println("OS.getAll()");
   String s = new String("");
   if (tracer)			s = "1 ";
   else				s = "0 ";

   if (roofOpen)		s = s+"1 ";
   else				s = s+"0 ";

   if (roofClosed)		s = s+"1 ";
   else				s = s+"0 ";

   if (roofOpening)		s = s+"1 ";
   else				s = s+"0 ";

   if (roofClosing)		s = s+"1 ";
   else				s = s+"0 ";

   if (scope2Parked)		s = s+"1 ";
   else				s = s+"0 ";

   if (scope1Parked)		s = s+"1 ";
   else				s = s+"0 ";

   if (overrideScopesParked)	s = s+"1 ";
   else				s = s+"0 ";

   if (acPowerAvailable)	s = s+"1 ";
   else				s = s+"0 ";

   if(scope1aPoweredUp)		s = s+"1 ";
   else				s = s+"0 ";

   if(scope1bPoweredUp)		s = s+"1 ";
   else				s = s+"0 ";

   if(scope2PoweredUp)		s = s+"1 ";
   else				s = s+"0 ";

   if(scope3PoweredUp)		s = s+"1 ";
   else				s = s+"0 ";

   if(computer1PoweredUp)	s = s+"1 ";
   else				s = s+"0 ";

   if(NASPoweredUp)		s = s+"1 ";
   else				s = s+"0 ";

   if(lightsOn)			s = s+"1 ";
   else				s = s+"0 ";

   if (scope3Parked)		s = s+"1 ";
   else				s = s+"0 ";

   if (scopesParkedPowerOn)	s = s+"1 ";
   else				s = s+"0 ";

   readVoltage();
   s = s+voltage+" ";
   return s;
  }

 private void commonFunctions(boolean b)
  {if (tracer) System.out.println("OS.commonFunctions()");
   newStatus = getAll();
   if (oldStatus.equals(newStatus))
    {}
   else
    {oldStatus = newStatus;
     setChanged();
     notifyObservers();
    }
  }

 public void readVoltage()
  {if (tracer) System.out.println("OS.readVoltage()");
   try
    {Process p = Runtime.getRuntime().exec(new String[]{"readit.py"});
     BufferedReader stdInput = new BufferedReader(new 
                 InputStreamReader(p.getInputStream()));
     voltage = stdInput.readLine();
    }
  catch (IOException e)
   {System.out.println("Error reading from Python routine\n");
    	e.printStackTrace();
//    	System.exit(-1);
        voltage = "error";
   }
  }

 public String getVoltage()
  {if (tracer) System.out.println("OS.getVoltage()");
   return voltage;
  }

 public void setTracer(boolean b)
  {if (tracer) System.out.println("OS.setTracer()");
   tracer = b;
  }

 public void setComputer1PoweredUp(boolean b)
  {if (tracer) System.out.println("OS.setComputer1PoweredUp("+b+")");
   computer1PoweredUp = b;
   commonFunctions(b);
  }
 public void setComputer2PoweredUp(boolean b)
  {System.out.println("  Invalid invoke of setComputer2PoweredUp: processed");
   System.out.println("    change offending code to setNASPoweredUp()");
   setNASPoweredUp(b); 
  }
 public void setNASPoweredUp(boolean b)
  {if (tracer) System.out.println("OS.set NASPoweredUp("+b+")");
   NASPoweredUp = b;
   commonFunctions(b);
  }
 public void setRoofOpen(boolean b)
  {if (tracer) System.out.println("OS.setRoofOpen("+b+")");
   roofOpen = b;
   commonFunctions(b);
  }
  public void setRoofClosed(boolean b)
  {if (tracer) System.out.println("OS.setRoofClosed("+b+")");
   roofClosed = b;
   commonFunctions(b);
  }
 public void setRoofOpening(boolean b)
  {if (tracer) System.out.println("OS.setRoofOpening("+b+")");
   roofOpening = b;
   commonFunctions(b);
  }
  public void setRoofClosing(boolean b)
  {if (tracer) System.out.println("OS.setRoofClosing("+b+")");
   roofClosing = b;
   commonFunctions(b);
  }
  public void setScope3Parked(boolean b)
  {if (tracer) System.out.println("OS.setscope3Parked("+b+")");
   scope3Parked = b;
   commonFunctions(b);
  }
  public void setScope2Parked(boolean b)
  {if (tracer) System.out.println("OS.setscope2Parked("+b+")");
   scope2Parked = b;
   commonFunctions(b);
  }
  public void setScope1Parked(boolean b)
  {if (tracer) System.out.println("OS.setscope1Parked("+b+")");
   scope1Parked = b;
   commonFunctions(b);
  }
  public void setScope1aPoweredUp(boolean b)
  {if (tracer) System.out.println("OS.setscope1aPoweredUp("+b+")");
   scope1aPoweredUp = b;
   commonFunctions(b);
  }
  public void setScope1bPoweredUp(boolean b)
  {if (tracer) System.out.println("OS.setscope1bPoweredUp("+b+")");
   scope1bPoweredUp = b;
   commonFunctions(b);
  }
  public void setScope2PoweredUp(boolean b)
  {if (tracer) System.out.println("OS.setscope2PoweredUp("+b+")");
   scope2PoweredUp = b;
   commonFunctions(b);
  }
  public void setScope3PoweredUp(boolean b)
  {if (tracer) System.out.println("OS.setscope3PoweredUp("+b+")");
   scope3PoweredUp = b;
   commonFunctions(b);
  }
  public void setOverrideScopesParked(boolean b)
  {if (tracer) System.out.println("OS.setOverrideScopesParked("+b+")");
   overrideScopesParked = b;
   commonFunctions(b);
  }
  public void setAcPowerAvailable(boolean b)
  {if (tracer) System.out.println("OS.setAcPowerAvailable("+b+")");
   acPowerAvailable = b;
   commonFunctions(b);
  }

  public void setLightsOn(boolean b)
  {if (tracer) System.out.println("OS.setLightsOn("+b+")");
   lightsOn = b;
   commonFunctions(b);
  }

  public void setScopesParkedPowerOn(boolean b)
  {if (tracer) System.out.println("OS.setScopesParkedPowerOn("+b+")");
   scopesParkedPowerOn = b;
   commonFunctions(b);
  }

 public boolean getTracer()
  {if (tracer) System.out.println("OS.getTracer("+tracer+")");
   return tracer;
  }

 public boolean getRoofOpen()
  {if (tracer) System.out.println("OS.getRoofOpen("+roofOpen+")");
   return roofOpen;
  }
  public boolean getRoofClosed()
  {if (tracer) System.out.println("OS.getRoofClosed("+roofClosed+")");
   return roofClosed;
  }
 public boolean getRoofOpening()
  {if (tracer) System.out.println("OS.getRoofOpening("+roofOpening+")");
   return roofOpening;
  }
  public boolean getRoofClosing()
  {if (tracer) System.out.println("OS.getRoofClosing("+roofClosing+")");
   return roofClosing;
  }
  public boolean getScope3Parked()
  {if (tracer) System.out.println("OS.getscope3Parked("+scope2Parked+")");
   return scope3Parked;
  }
  public boolean getScope2Parked()
  {if (tracer) System.out.println("OS.getscope2Parked("+scope2Parked+")");
   return scope2Parked;
  }
  public boolean getScope1Parked()
  {if (tracer) System.out.println("OS.getscope1Parked("+scope1Parked+")");
   return scope1Parked;
   }
  public boolean getOverrideScopesParked()
  {if (tracer) System.out.println("OS.getOverrideScopesParked("+overrideScopesParked+")");
   return overrideScopesParked;
  }
  public boolean getAcPowerAvailable()
  {if (tracer) System.out.println("OS.setAcPowerAvailable("+acPowerAvailable+")");
   return acPowerAvailable;
   }
  public int getFlagCount()
   {if (tracer) System.out.println("OS.getFlagCount()");
    return flagCount;
   }
  public boolean getScope1aPoweredUp()
   {if (tracer) System.out.println("OS.getScope1aPoweredUp()");
    return scope1aPoweredUp;
   }
  public boolean getScope1bPoweredUp()
   {if (tracer) System.out.println("OS.getScope1bPoweredUp()");
    return scope1bPoweredUp;
   }
  public boolean getScope2PoweredUp()
   {if (tracer) System.out.println("OS.getScope2PoweredUp()");
    return scope2PoweredUp;
   }
  public boolean getScope3PoweredUp()
   {if (tracer) System.out.println("OS.getScope3PoweredUp()");
    return scope3PoweredUp;
   }
  public boolean getComputer1PoweredUp()
   {if (tracer) System.out.println("OS.getComputer1PoweredUp()");
    return computer1PoweredUp;
   }
  public boolean getComputer2PoweredUp()  // catches old invokes
   {System.out.println("/nBad invoke of getComputer2PoweredUp(): processed");
    System.out.println("  change offending code to getNASPoweredUp()");
    return getNASPoweredUp();
   }
  public boolean getNASPoweredUp()
   {if (tracer) System.out.println("OS.getNASPoweredUp()");
    return NASPoweredUp;
   }
  public boolean getLightsOn()
   {if (tracer) System.out.println("OS.getLightsOn()");
    return lightsOn;
   }
  public boolean getScopesSafe()
   {if (tracer) System.out.println("OS.gtScopesSafe()");
    if (scope1Parked && scope2Parked && scope3Parked)
      return true;
    return false;
   }

  public boolean getScopesParkedPowerOn()
  {if (tracer) System.out.println("OS.getScopesParkedPowerOn()");
   return scopesParkedPowerOn;
  }
 public void update (Observable obsStatus)
  { System.out.println("status change recorded");
  }

 public static void main(String[] args)
  {ObsStatus obs1 = new ObsStatus();
   obs1.setAll("1 0 0 0 0 0 0 0 0 ");
   System.out.println("Current status is: "+obs1.getAll());
   System.out.println("Setting AC status");
   obs1.setAcPowerAvailable(true);
   System.out.println("New     status is: "+obs1.getAll());
  }
}