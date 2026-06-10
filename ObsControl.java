/*
 * **********************************************************************
 * Observatory control java software
 * 
 * This module talks to the pi hardware. It's written to use the 40
 * pin header connection.
 * 
 * Disable SPI and I2C to use pins for I/O - blacklist 2 modules
     /etc/modprobe.d/raspi-blacklist.conf
	i2c_bcm2708
	spi_bcm2708

  pi4j I/O pin assignments and usage
	NOTE: pin set low activates a relay, high deactivates it

    PIN	USE
        INPUTS      
     1	roof open
     2	roof closed
     3	scope 1 parked (Abe)
     4	scope 2 parked (Phil)
     5	ac power available
     6	door, outside, open
     7	door, control room, open
     8	scope 1 power on	*** has 1.8k pullup ***
     9	scope 2 power on	*** has 1.8k pullup ***
    10  Computer 1 powered up (isHigh)
    11  Computer 2 powered up (isHigh)
    12

        OUTPUTS
    17	open roof
    18	close roof
    19	inverter power button
    20	computer power 1
    21	computer power 2
    22	scope 1  power 1 (Abe)
    23  scope 1  power 2 
    24	scope 2  on	 (Phil - latching power relay)
    25  scope 2  off
    26  scope 3  power
    27
    28
    29


  2016/03/20 PLS initial authoring started 
  2016/04/08 PLS change scope2 power, add scope parked power code 
  2016/04/16 PLS add code to allow running without pi hardware
		 needs changes to ObsControl to manage scopes parked
		 scopes parked forced in toggleScopesParkedPower()
  2016/04/23 PLS #570 code to not turn off enet switches
		 *** Needs inputs from computer's USB power lines
		     to work properly
  2016/04/23 PLS redo all pin assignments for pi
		 drop computer2 from scope 2 - run 2 relays from 1 pin
  2016/06/10 PLS redo turning on computer1 (ethernet) code ~600+
		 changes also in wakeup()
  2016/09/20 PLS Force scope3 parked - refreshStatus()
		   os.setScope3Parked(true)  - 3 places
		 change ethernet switch  delay to 2.5 sec (2500)
  2016/09/27 PLS Change default pin state oPin02 to LOW - ethernet sw
  2016/11/23 PLS Add disable to scope 2 safe when bypass activated

  2017/01/01 PLS refreshStatus - no change if status matches hardware
       ( note all code for above can be removed, now in ObsStatus)
  		 changes to stop roof - look at parked???
  2020/07/21 PLS add to updateStatus() invocation of voltage update
**********************************************************************
 * 
 * 
 *    SEE rt.exec lines (3) for old/new java implementation
 * 
*/


//   following imports needed to use raw IO pins on header

//import com.pi4j.io.gpio.GpioController;
//import com.pi4j.io.gpio.GpioFactory;
//import com.pi4j.io.gpio.GpioPinDigitalInput;
//import com.pi4j.io.gpio.GpioPinDigitalOutput;
//import com.pi4j.io.gpio.PinPullResistance;
//import com.pi4j.io.gpio.PinState;
//import com.pi4j.io.gpio.RaspiPin;
//import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
//import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import java.io.*;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.*;

//   PinPull values OFF, PULL_UP, PULL_DOWN
//     use PULL_UP and eliminate need for divider resistor network

public class ObsControl
{
 static GpioController		gpio;
 static GpioPinDigitalOutput    
			 oPin00		//open roof
                        ,oPin01		//close roof
			,oPin02		//ethernet switches
			,oPin03		//backup drive
			,oPin04		//scope 1 power 1
                        ,oPin05		//scope 1 power 2
			,oPin06		//scope safe sensors power
			,oPin07		//scope 2 on (2 relays)
			,oPin13		//scope 3 power
			,oPin14		//lights power
			,oPin21		//inverter power button
			,oPin22
			;

  static GpioPinDigitalInput
                         iPin08		//roof open
			,iPin09		//roof closed
                        ,iPin10		
                        ,iPin27		//ac power available
                        ,iPin12		
                        ,iPin22		
			,iPin24		//computer 1 powered on
                        ,iPin23		//scope 3 parked
                        ,iPin25		
                        ,iPin26		//scope 2 parked
                        ,iPin11		//scope 1 parked
			;
/* Available pins
			, pin10
			, pin15
			, pin16
			, pin25
			, pin28
			, pin29
*/
  static GpioListener pinListener;

  private ObsStatus os;
  
  static int	 debounceDelay = 5	// miliseconds
		,debounceDelayLong = 800	// AC on debounce
		,roofStopDelay = 1500
		,inverterPulseTime = 400
		,scopeParkedWarmup = 750
		,ethernetWarmup=2500
		;
  static boolean tracer = false 
		,scopesSafe = false
		,runOnPi = true
		;
  private ObsControlConsoleReader occr = new ObsControlConsoleReader();
  private NasShutdown nas = new NasShutdown();

public ObsControl(ObsStatus os)
{this(os,false);
 System.out.println("ObsControl started with no trace parameter");
}

public ObsControl(ObsStatus os1, boolean b)
{os = os1;
 System.out.println("ObsControl started with trace: "+b);
 tracer = b;
 init();
}

private void init()
{if (tracer) System.out.println("OC.init()");
 if (System.getProperty("os.arch").equals("arm"))
  {runOnPi = true;
   gpio = GpioFactory.getInstance(); // create controller
  }
 else
  {System.out.println("Not running on Pi");
   runOnPi = false;
   tracer = true;
   os.setRoofClosed(true);
   BufferedReader stdIn =
   	new BufferedReader(new InputStreamReader(System.in));
   occr.setReader(stdIn);
   occr.start();
  }
 if (runOnPi) setupIOPins();
 refreshStatus();
}

private void setupIOPins()
 {if (tracer) System.out.println("PiC.setupIOPins()");
  pinListener = new GpioListener(this,os);
// setup output pins  PinStates: LOW HIGH
  oPin00  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_00,"pin00",PinState.HIGH);
  oPin00.setShutdownOptions(true, PinState.HIGH);

  oPin01  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_01,"pin01",PinState.HIGH);
  oPin01.setShutdownOptions(true, PinState.HIGH);

  oPin02  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_02,"pin02",PinState.LOW);
  oPin02.setShutdownOptions(true, PinState.LOW);

  oPin03  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_03,"pin03",PinState.HIGH);
  oPin03.setShutdownOptions(true, PinState.HIGH);

  oPin04  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_04,"pin04",PinState.HIGH);
  oPin04.setShutdownOptions(true, PinState.HIGH);

  oPin05  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_05,"pin05",PinState.HIGH);
  oPin05.setShutdownOptions(true, PinState.HIGH);

  oPin06  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_06,"pin06",PinState.HIGH);
  oPin06.setShutdownOptions(true, PinState.HIGH);

  oPin07  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_07,"pin07",PinState.HIGH);
  oPin07.setShutdownOptions(true, PinState.HIGH);

  oPin13  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_13,"pin13",PinState.HIGH);
  oPin13.setShutdownOptions(true, PinState.HIGH);

  oPin14  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_14,"pin14",PinState.HIGH);
  oPin14.setShutdownOptions(true, PinState.HIGH);

  oPin21  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_21,"pin21",PinState.HIGH);
  oPin21.setShutdownOptions(true, PinState.HIGH);
/*
  oPin22  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_22,"pin22",PinState.HIGH);
  oPin22.setShutdownOptions(true, PinState.HIGH);
*/

 // PinPull constants: OFF PULL_DOWN PULL_UP
  iPin08 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_08,"iPin08",PinPullResistance.OFF);
  iPin08.addListener(pinListener);
  iPin08.setDebounce(debounceDelay);

  iPin09 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_09,"iPin09",PinPullResistance.OFF);
  iPin09.addListener(pinListener);
  iPin09.setDebounce(debounceDelay);
/*
  iPin10 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_10,"iPin10",PinPullResistance.PULL_UP);
  iPin10.addListener(pinListener);
  iPin10.setDebounce(debounceDelay);
*/
  iPin27 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_27,"iPin27",PinPullResistance.PULL_DOWN);
  iPin27.addListener(pinListener);
  iPin27.setDebounce(debounceDelayLong);

  iPin12 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_12,"iPin12",PinPullResistance.PULL_UP);
  iPin12.addListener(pinListener);
  iPin12.setDebounce(debounceDelay);

  iPin11 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_11,"iPin11",PinPullResistance.PULL_DOWN);
  iPin11.addListener(pinListener);
  iPin11.setDebounce(debounceDelay);

  iPin26 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_26,"iPin26",PinPullResistance.OFF);
  iPin26.addListener(pinListener);
  iPin26.setDebounce(debounceDelay);

  iPin23 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_23,"iPin23",PinPullResistance.PULL_DOWN);
  iPin23.addListener(pinListener);
  iPin23.setDebounce(debounceDelay);

  iPin24 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_24,"iPin24",PinPullResistance.OFF);
  iPin24.addListener(pinListener);
  iPin24.setDebounce(debounceDelay);

//  iPin22 = gpio.provisionDigitalInputPin
//		(RaspiPin.GPIO_22,"iPin22",PinPullResistance.PULL_UP);
//  iPin22.addListener(pinListener);
//  iPin22.setDebounce(debounceDelay);

//  iPin25 = gpio.provisionDigitalInputPin
//		(RaspiPin.GPIO_25,"iPin25",PinPullResistance.PULL_UP);
//  iPin25.addListener(pinListener);
//  iPin25.setDebounce(debounceDelay);

//  iPin28 = gpio.provisionDigitalInputPin
//		(RaspiPin.GPIO_28,"iPin28",PinPullResistance.PULL_UP);
//  iPin28.addListener(pinListener);
//  iPin28.setDebounce(debounceDelay);

  refreshStatus();

 }

public void refreshStatus()
 {if (tracer) System.out.println("OC.refreshStatus()");
  if (runOnPi) {
    os.readVoltage();				// update voltage readout
    if (iPin08.isLow() & !os.getRoofOpen())
      os.setRoofOpen(true);
    else if (iPin09.isHigh() & os.getRoofOpen())
      os.setRoofOpen(false);

    if (iPin09.isLow() & !os.getRoofClosed())
      os.setRoofClosed(true);
    else if (iPin09.isHigh() & os.getRoofClosed())
      os.setRoofClosed(false);

    boolean b1 = oPin06.isHigh();
    if (b1) toggleScopesParkedPower();   // turn on to get scopes parked info

    if (iPin11.isLow() & !os.getScope1Parked())		// scope 1 parked
      os.setScope1Parked(true);
    else if (iPin11.isHigh() & os.getScope1Parked())
      os.setScope1Parked(false);

    if (iPin26.isLow() & !os.getScope2Parked())		// scope 2 parked
      os.setScope2Parked(true);
    else if (iPin26.isHigh() & os.getScope2Parked())
      os.setScope2Parked(false);

    if (iPin23.isLow() & !os.getScope3Parked())		// scope 3 parked
      os.setScope3Parked(true);
    else if (iPin23.isHigh() & os.getScope3Parked())
      os.setScope3Parked(false);
    os.setScope3Parked(true);				// force parked

    if (b1) toggleScopesParkedPower();   // turn off power if turned it on


    if (oPin04.isLow() & !os.getScope1aPoweredUp())		// scope 1 power
      os.setScope1aPoweredUp(true);
    else if (oPin04.isHigh() & os.getScope1aPoweredUp())
      os.setScope1aPoweredUp(false);

    if (oPin05.isLow() & !os.getScope1bPoweredUp())		// scope 1 power2
      os.setScope1bPoweredUp(true);
    else if (oPin05.isHigh() & os.getScope1bPoweredUp())
      os.setScope1bPoweredUp(false);

    if (oPin07.isLow() & !os.getScope2PoweredUp())		// scope 2 power
      os.setScope2PoweredUp(true);
    else if (oPin07.isHigh() & os.getScope2PoweredUp())
      os.setScope2PoweredUp(false);

    if (oPin13.isLow() & !os.getScope3PoweredUp())		// scope 3 power
      os.setScope3PoweredUp(true);
    else if (oPin13.isHigh() & os.getScope3PoweredUp())
      os.setScope3PoweredUp(false);

    if (oPin14.isLow() & !os.getLightsOn())		// scope 1 parked
      os.setLightsOn(true);
    else if (oPin14.isHigh() & os.getLightsOn())
      os.setLightsOn(false);

    if (oPin02.isLow() & !os.getComputer1PoweredUp())		// computer 1 power
      os.setComputer1PoweredUp(true);
    else if (oPin02.isHigh() & os.getComputer1PoweredUp())
      os.setComputer1PoweredUp(false);

    if (oPin03.isLow() & !os.getComputer2PoweredUp())		// computer 2 power
      os.setComputer2PoweredUp(true);
    else if (oPin03.isHigh() & os.getScope2PoweredUp())
      os.setComputer2PoweredUp(false);

    if (iPin27.isLow()) os.setAcPowerAvailable(false);
    else		os.setAcPowerAvailable(true);

   }
  else
   {

   }
 }

public void setOverrideScopesParked(boolean b)
 {if (tracer) System.out.println("OC.setOverrideScopesParked("+b+")");
  os.setOverrideScopesParked(b);
 }

// *************************************************************************

public void openRoof()	// 0
 {if (tracer) System.out.println("OC.openRoof()");
  if (runOnPi)
   {if (oPin06.isHigh())
      toggleScopesParkedPower();		// turn on power for parked sensors
    if (os.getScopesSafe() | os.getOverrideScopesParked())
     {if (oPin01.isLow())		// roof is closing - stop first
       {stopRoof();
        try {Thread.sleep(roofStopDelay);}	// wait for roof to stop
        catch (InterruptedException e) {}
       }
      oPin00.low();
      os.setRoofOpening(true);
     }
   }
  else
   {if (!os.getScopesParkedPowerOn())
      toggleScopesParkedPower();
    if (os.getScopesSafe() | os.getOverrideScopesParked())
      {os.setRoofOpening(true);
       os.setRoofClosed(false);
//       try {Thread.sleep(10000);}
//       catch (InterruptedException e) {}
//       os.setRoofOpen(true);
//       stopRoof();
      }
   }
 }

public void closeRoof()  // 1
 {if (tracer) System.out.println("OC.closeRoof()");
  if (runOnPi)  
   {if (oPin06.isHigh())
      toggleScopesParkedPower();	// turn on scopes parked power
    if (os.getScopesSafe() | os.getOverrideScopesParked())
     {if (oPin00.isLow())		// roof is opening - stop first
       {stopRoof();
        try {Thread.sleep(roofStopDelay);}  // wait for roof to stop
        catch (InterruptedException e) {}
       }
      oPin01.low();
      os.setRoofClosing(true);
     }
   }
  else
   {if (!os.getScopesParkedPowerOn())
      toggleScopesParkedPower();
    if (os.getScopesSafe() | os.getOverrideScopesParked())
     {os.setRoofClosing(true);
      os.setRoofOpen(false);
//      try {Thread.sleep(10000);}
//      catch (InterruptedException e) {}
//      os.setRoofClosed(true);
//      stopRoof();
     }
   }
 }

public void stopRoof()
 {if (tracer) System.out.println("OC.stopRoof()");
  if (runOnPi)
   {oPin00.high();
    oPin01.high();
   }
  if (os.getRoofOpening())
    os.setRoofOpening(false);
  if (os.getRoofClosing())
    os.setRoofClosing(false);
  if (okToTurnOffSensorPower())
    toggleScopesParkedPower();



//   Refresh all status flags if necessary - insert code here
  refreshStatus();

 }

public void pushInverterPowerButton()
 {if (tracer) System.out.println("OC.pushInverterPowerButton()");
  if (runOnPi)
    oPin21.low();
    try {Thread.sleep(inverterPulseTime);}
    catch (InterruptedException e) {}    
    oPin21.high();
 }

public void togglePowerS1R1()			//  scope power 
 {if (tracer) System.out.println("OC.togglePowerS1R1()");
//     if powered up and camera powered up, power off camera
  if (runOnPi)
   {if (oPin05.isLow() & oPin04.isLow())
       togglePowerS1R2();
    oPin04.toggle();
    if (oPin04.isLow())
     {os.setScope1aPoweredUp(true);
      if (oPin06.isHigh())		// no power to parked sensors
        toggleScopesParkedPower();
     }
    else
     {os.setScope1aPoweredUp(false);
      if (oPin06.isLow() & okToTurnOffSensorPower()) // parked sensors on
        toggleScopesParkedPower();
     }
   }
  else
   {if (os.getScope1bPoweredUp())
      os.setScope1bPoweredUp(false);
    os.setScope1aPoweredUp(!os.getScope1aPoweredUp());
    if (os.getScope1aPoweredUp())
     {if (!os.getScopesParkedPowerOn())
        toggleScopesParkedPower();
     }
    else
      if (okToTurnOffSensorPower() & os.getScopesParkedPowerOn())
        toggleScopesParkedPower();
   }

 }

public void togglePowerS1R2()   		// camera power
 {if (tracer) System.out.println("OC.togglePowerS1R2()");
//  only toggle if power applied to S1R1
  if (runOnPi)
   {if (oPin04.isLow())  
     oPin05.toggle();
    if (oPin05.isLow())
      os.setScope1bPoweredUp(true);
    else
      os.setScope1bPoweredUp(false);
   }
  else
   {if (os.getScope1aPoweredUp())
      os.setScope1bPoweredUp(!os.getScope1bPoweredUp());
   }
 }

public void togglePowerS2()
 {if (tracer) System.out.println("OC.togglePowerS2()");
  if (runOnPi)
   {oPin07.toggle();
    if (oPin07.isLow())			//power on now      
     {os.setScope2PoweredUp(true);
      if (oPin06.isHigh())		//no power to parked sensors
        toggleScopesParkedPower();
//    if (oPin03.isHigh())		// turn on Phil's icron 
//      togglePowerComputer2();
     }
    else
     {os.setScope2PoweredUp(false);	//power off now
      if (oPin06.isLow() & okToTurnOffSensorPower()) // parked sensors on
        toggleScopesParkedPower();
//    if (oPin03.isLow())
//      togglePowerComputer2();
     }
   }
  else
   {os.setScope2PoweredUp(!os.getScope2PoweredUp());
     if (os.getScope2PoweredUp())
      {if (!os.getScopesParkedPowerOn())
         toggleScopesParkedPower();
//     if (!os.getComputer2PoweredUp())
//       togglePowerComputer2();
      }
     else
      {if (okToTurnOffSensorPower() & os.getScopesParkedPowerOn())
         toggleScopesParkedPower();
//     if (os.getComputer2PoweredUp())
//       togglePowerComputer2();
      }
   }
 }

private boolean okToTurnOffSensorPower()
 {if (tracer) System.out.println("OC.okToTurnOffSensorPower()");
  if (runOnPi)
   {if (oPin06.isHigh() | os.getOverrideScopesParked()) // power off / bypass on
      return false;
    if (iPin08.isLow() | iPin09.isLow())		// roof open | closed
      return true;
    if (iPin11.isLow() & iPin23.isLow() & iPin26.isLow() //scopes parked
	& oPin05.isHigh() & oPin07.isHigh() & oPin13.isHigh() // scopes off
       )
      return true;
    return false;
   }
  else
//		simulation assumes scopes always parked
   {if (os.getOverrideScopesParked() | os.getScopesParkedPowerOn())
      return false;
    if (os.getRoofOpen() | os.getRoofClosed())
      return true;
    else
     {if (os.getScope1Parked() & os.getScope2Parked() & os.getScope3Parked()
	   & !os.getScope1aPoweredUp()
	   & !os.getScope2PoweredUp()
	   & !os.getScope3PoweredUp()
         )
        return true;
      else
        return false;
     }
   }
 }

public void toggleScopesParkedPower()
 {if (tracer) System.out.println("OC.toggleScopesParkedPower()");
  if (runOnPi)
   {oPin06.toggle();
    if (oPin06.isLow())
     {os.setScopesParkedPowerOn(true);
      try {Thread.sleep(scopeParkedWarmup);}
      catch (InterruptedException e) {}
     }
    else
     {os.setScopesParkedPowerOn(false);
     }
   }
  else
   {os.setScopesParkedPowerOn(!os.getScopesParkedPowerOn());
    if (os.getScopesParkedPowerOn())
     {if (!os.getScope1Parked())
        os.setScope1Parked(true);
      if (!os.getScope2Parked())
        os.setScope2Parked(true);
      if (!os.getScope3Parked())
        os.setScope3Parked(true);
     }
   }
 }

public void togglePowerS3()
 {if (tracer) System.out.println("OC.togglePowerS3()");
  if (runOnPi)
   {oPin13.toggle();
    if (oPin13.isLow())			// power now on
     {os.setScope3PoweredUp(true);
      if (oPin06.isHigh())		// no power to parked sensors
	toggleScopesParkedPower();
     }
    else				// power now off
     {os.setScope3PoweredUp(false);
      if (oPin06.isLow() & okToTurnOffSensorPower()) // parked sensors on
        toggleScopesParkedPower();
     }
    }
   else
    {os.setScope3PoweredUp(!os.getScope3PoweredUp());
     if (os.getScope3PoweredUp())
      {if (!os.getScopesParkedPowerOn())
         toggleScopesParkedPower();
      }
     else
       if (okToTurnOffSensorPower() & os.getScopesParkedPowerOn())
	 toggleScopesParkedPower();
    }
 }

public void togglePowerComputer1()      // Ethernet switches
 {if (tracer) System.out.println("OC.toggleComputerPower1()");
  if (runOnPi)
   {
/* disabled because no way to see if computers turned on
   wakeup computer checks if on before calling
    if (oPin02.isLow() 			// ethernet is turned on
	 & (iPin27.isHigh() | iPin12.isHigh())	// computer on
       )
      {return;				// do not turn off
      }
*/
    oPin02.toggle();
    if (oPin02.isLow())
      os.setComputer1PoweredUp(true);
    else
      os.setComputer1PoweredUp(false);
   }
  else
   os.setComputer1PoweredUp(!os.getComputer1PoweredUp());
   //  can't determine if computers on so assume off (above)
 }

public void togglePowerComputer2()
 {if (tracer) System.out.println("OC.toggleComputerPower2()");
  String command = "ping -c1 192.168.0.90 1>/dev/null";
  if (runOnPi)
   {if (os.getComputer2PoweredUp())		// is powered up
      nas.shutNasDown();
/*
     {try
       {Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(command);		// ping tests if up
        int returnCode = pr.waitFor();
        if (returnCode == 0)			// is up - issue shutdown
         {command = "nasShutdown";
          pr = rt.exec(command);
         }
        else					// not up - can power off
         {oPin03.toggle();
          os.setComputer2PoweredUp(false);
         }       
       }
      catch (Throwable e)
       {System.out.println("NAS shutdown: "+command+" failed");
        System.out.println(e);
       }
     }
*/
    else					// is power off, power up
     {oPin03.toggle();
      os.setComputer2PoweredUp(true);
     }    
   }
  else						// not running on pi
    os.setComputer2PoweredUp(!os.getComputer2PoweredUp());
 }


public void wakeUp(String computerOwner)
 {if (tracer) System.out.println("OC.wakeUp("+computerOwner+")");
  String command = "";
  if (computerOwner.equals("Abe"))
    command = "wakeupAbe";
  else if (computerOwner.equals("Phil"))
    command = "wakeupPhil";
  else
   System.out.println("Cannot wakeup computer for unknown user: "
	+computerOwner);
  if (!command.equals(""))
   {if (!os.getComputer1PoweredUp())	// ethernet off?
      togglePowerComputer1();		//  turn on
    try {Thread.sleep(ethernetWarmup);}
    catch (InterruptedException e) {}
    try
     {Runtime rt = Runtime.getRuntime();
//    Process pr = rt.exec(command);
      Process pr = rt.exec(new String[]{command});
//    int returnCode = pr.waitFor();
     }
    catch (Throwable e)
     {System.out.println("Wakeup command: "+command+" failed");
      System.out.println(e);
     }
   }
 }                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   

public void toggleLights()
 {if (tracer) System.out.println("OC.toggleLights()");
  if (runOnPi)
   {oPin14.toggle();
    if (oPin14.isLow())
      os.setLightsOn(true);
    else
      os.setLightsOn(false);
   }
  else
   os.setLightsOn(!os.getLightsOn());
 }

public void done()
 {if (tracer) System.out.println("OC.done()");
//  gpio.shutdown();
 }


public static void main(String[] args)
 {ObsStatus xx = new ObsStatus();
  ObsControl testControl = new ObsControl(xx,true);
  testControl.done();

 }


public static class GpioListener implements GpioPinListenerDigital
 {private ObsControl oc;
  private ObsStatus  os;
  public GpioListener(ObsControl xx, ObsStatus yy)
   {oc = xx;
    os = yy;
   }
  @Override
  public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event)
   {// if (event.getState().isLow())
      System.out.println("GPIO pin state change for  " 
		+ "pin: "+event.getPin()
              + " new state: "+event.getState()+"  "+event.getPin().getName()
		);
//    displayButton(event);
    String pinName = event.getPin().getName();
//    int pinNumber  = event.getPin();		// GPIO pin number (Pi4j)
    boolean switchClosed = event.getState().isLow();

/*
  static GpioPinDigitalInput
                         iPin08		//roof open
			,iPin09		//roof closed
                        ,iPin11		//scope 1 parked
                        ,iPin26		//scope 2 parked
                        ,iPin23		//scope 3 parked
			,iPin24		//ac power available
                        ,iPin22		//door, outside, open
                        ,iPin25		//door, control room, open
                        ,iPin28		//lights on (maybe)
                        ,iPin10		
                        ,iPin27		
                        ,iPin12
			;
*/
    
    if (pinName.equals("iPin08"))	// roof open
     {if (switchClosed)
       {oc.stopRoof();
        os.setRoofOpen(true);
       }
      else
       {os.setRoofOpen(false);
       }
     }
    else if (pinName.equals("iPin09"))	// roof closed
     {if (switchClosed)
       {oc.stopRoof();
        os.setRoofClosed(true);
       }
      else
       {os.setRoofClosed(false);
       }
     }
    else if (pinName.equals("iPin11"))	// scope 1 parked
     {if (switchClosed)
       {os.setScope1Parked(true);
       }
      else
       {os.setScope1Parked(false);
       }
     }
    else if (pinName.equals("iPin26"))	// scope2 parked
//        ignore state changes when roof is moving or no power to sensors
//	  or override is set
     {if (!os.getRoofOpening() && !os.getRoofClosing()
				&& !os.getOverrideScopesParked()
				&& os.getScopesParkedPowerOn())
      {if (switchClosed)
        {os.setScope2Parked(true);
        }
       else
        {os.setScope2Parked(false);
        }
      }
     }
    else if (pinName.equals("iPin23"))	// scope 3 parked
     {if (switchClosed)
       {os.setScope3Parked(true);
       }
      else
       {os.setScope3Parked(false);
       }
      os.setScope3Parked(true);
     }
    else if (pinName.equals("iPin27"))	// AC power available
     {if (switchClosed)
       {os.setAcPowerAvailable(false);
       }
      else
       {os.setAcPowerAvailable(true);
       }
     }
    else if (pinName.equals("iPin22"))	// outside door open
     {if (switchClosed)
       {
       }
      else
       {
       }
     }
    else if (pinName.equals("iPin25"))	// control room door open
     {if (switchClosed)
       {
       }
      else
       {
       }
     }
    else if (pinName.equals("iPin28"))	// lights on - maybe
     {if (switchClosed)
       {
       }
      else
       {
       }
     }
    else if (pinName.equals("iPin10"))
     {if (switchClosed)
       {
       }
      else
       {
       }
     }
    else if (pinName.equals("iPin24"))
     {if (switchClosed)
       {os.setComputer1PoweredUp(false);
       }
      else
       {os.setComputer1PoweredUp(true);
       }
     }
    else if (pinName.equals("iPin12"))
     {if (switchClosed)
       {os.setComputer2PoweredUp(false);
       }
      else
       {os.setComputer2PoweredUp(true);
       }
     }
/*
    else if (pinName.equals("iPin12"))
     {if (switchClosed)
       {
       }
      else
       {
       }
     }
    else if (pinName.equals("iPin13"))
     {if (switchClosed)
       {
       }
      else
       {
       }
     }
    else if (pinName.equals("iPin14"))
     {if (switchClosed)
       {
       }
      else
       {
       }
     }
*/
    else
      System.out.println("Input deteced on "+pinName+" - not programmed");

   }
 }

public void processUserInput(String s1)
 {if (tracer) System.out.println("OC.processUserInput("+s1+")");
  if (s1.equals("99"))
    System.exit(0);
  if (s1.equals("1"))
   {os.setRoofOpen(true);
    stopRoof();
   }
  else if (s1.equals("2"))
   {os.setRoofClosed(true);
    stopRoof();
   }
  else if (s1.equals("11"))
    os.setScope1Parked(!os.getScope1Parked());
  else if (s1.equals("12"))
    os.setScope2Parked(!os.getScope2Parked());
  else if (s1.equals("13"))
   {os.setScope3Parked(!os.getScope3Parked());
    os.setScope3Parked(true);
   }
 }


public class ObsControlConsoleReader extends Thread
  {private BufferedReader br;
   private String inData="";
   public ObsControlConsoleReader()
    {}

   public void setReader(BufferedReader br)
    {this.br = br;
    }

   public void run()
    {
     while (true)
      {try {inData = br.readLine();
	    if (inData == null)
	     {System.out.println("Null input from console");
//            break;
             }
            if (inData.equals("quit"))
	      inData = "99";
            processUserInput(inData);
           }
       catch (IOException e)
        {System.out.println("I/O error reading from console") ;
        }
      }
    }
  }



public class NasShutdown extends Thread
 {public NasShutdown()
   {}
  
  public void run()
   {}

  public void shutNasDown()
   {if (tracer) System.out.println("shutNasDown()");
    String command = "nasPing1"; 
    try
     {Runtime rt = Runtime.getRuntime();
//    Process pr = rt.exec(command);		// ping tests if up
      Process pr = rt.exec(new String[]{command});
      int returnCode = pr.waitFor();
      if (returnCode == 0)			// is up - shutdown with delay
       {command = "nasShutdown";
//        pr = rt.exec(command);
        pr = rt.exec(new String[]{command});
        returnCode = pr.waitFor();
        try {Thread.sleep(60000);}		// wait for shutdown to complete
        catch (InterruptedException e) {} 
       }
     }
    catch (Throwable e)
     {System.out.println("NAS shutdown: "+command+" failed");
      System.out.println(e);
     }
    oPin03.toggle();
    os.setComputer2PoweredUp(false);
   }
 }  



}
