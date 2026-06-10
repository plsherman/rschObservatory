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
    10  scope 3 power on
    11
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
**********************************************************************
 * 
 * 
 * 
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
			,oPin02		//computer power 1
			,oPin03		//computer power 2
			,oPin04		//scope 1 power 1
                        ,oPin05		//scope 1 power 2
			,oPin06		//scope safe sensors power
			,oPin07		//scope 2 on
			,oPin08		//scope 3 power
			,oPin09		//lights power
			,oPin10		//inverter power button
			,oPin11
			;

  static GpioPinDigitalInput
                         iPin00		//roof open
			,iPin01		//roof closed
                        ,iPin02		//scope 1 parked
                        ,iPin03		//scope 2 parked
                        ,iPin04		//scope 3 parked
			,iPin05		//ac power available
                        ,iPin06		//door, outside, open
                        ,iPin07		//door, control room, open
                        ,iPin08		
                        ,iPin09		
                        ,iPin10		//scope 2 power on
                        ,iPin11
			;
  static GpioListener pinListener;

  private ObsStatus os;
  
  static int	 debounceDelay = 5	// miliseconds
		,roofStopDelay = 1500
		,inverterPulseTime = 200
		,scopeParkedWarmup = 250
		;
  static boolean tracer = false 
		,scopesSafe = false
		;

public ObsControl(ObsStatus os)
{this(os,false);
}

public ObsControl(ObsStatus os1, boolean b)
{os = os1;
 tracer = b;
 init();
}

private void init()
{if (tracer) System.out.println("OC.init()");
 gpio = GpioFactory.getInstance(); // create controller
 setupIOPins();
}
//  pins set up as input, output, ALT0....ALT5
//  pins avail 0 1           7 8 9 10 11 12 13 14
//             21 22 23 24    26 27 28 29
//  SD card  2 3 4 5 6 25
//  Serial   15 16
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
		(RaspiPin.GPIO_07,"pin02",PinState.HIGH);
  oPin02.setShutdownOptions(true, PinState.HIGH);

  oPin03  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_08,"pin03",PinState.HIGH);
  oPin03.setShutdownOptions(true, PinState.HIGH);

  oPin04  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_09,"pin04",PinState.HIGH);
  oPin04.setShutdownOptions(true, PinState.HIGH);

  oPin05  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_10,"pin05",PinState.HIGH);
  oPin05.setShutdownOptions(true, PinState.HIGH);

  oPin06  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_11,"pin06",PinState.HIGH);
  oPin06.setShutdownOptions(true, PinState.HIGH);

  oPin07  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_12,"pin07",PinState.HIGH);
  oPin07.setShutdownOptions(true, PinState.HIGH);

  oPin08  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_13,"pin08",PinState.HIGH);
  oPin08.setShutdownOptions(true, PinState.HIGH);

  oPin09  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_14,"pin09",PinState.HIGH);
  oPin09.setShutdownOptions(true, PinState.HIGH);

  oPin10  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_21,"pin10",PinState.HIGH);
  oPin10.setShutdownOptions(true, PinState.HIGH);

  oPin11  = gpio.provisionDigitalOutputPin
		(RaspiPin.GPIO_22,"pin11",PinState.HIGH);
  oPin11.setShutdownOptions(true, PinState.HIGH);


 // PinPull constants: OFF PULL_DOWN PULL_UP
  iPin00 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_29,"iPin00",PinPullResistance.PULL_UP);
  iPin00.addListener(pinListener);
  iPin00.setDebounce(debounceDelay);

  iPin01 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_28,"iPin01",PinPullResistance.PULL_UP);
  iPin01.addListener(pinListener);
  iPin01.setDebounce(debounceDelay);

  iPin02 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_27,"iPin02",PinPullResistance.PULL_UP);
  iPin02.addListener(pinListener);
  iPin02.setDebounce(debounceDelay);

  iPin03 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_26,"iPin03",PinPullResistance.PULL_UP);
  iPin03.addListener(pinListener);
  iPin03.setDebounce(debounceDelay);

  iPin04 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_24,"iPin04",PinPullResistance.PULL_UP);
  iPin04.addListener(pinListener);
  iPin04.setDebounce(debounceDelay);

  iPin05 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_23,"iPin05",PinPullResistance.PULL_UP);
  iPin05.addListener(pinListener);
  iPin05.setDebounce(debounceDelay);
/*
  iPin06 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_22,"iPin06",PinPullResistance.PULL_UP);
  iPin06.addListener(pinListener);
  iPin06.setDebounce(debounceDelay);

//  iPin07 = gpio.provisionDigitalInputPin
//		(RaspiPin.GPIO_08,"iPin07",PinPullResistance.PULL_UP);
//  iPin07.addListener(pinListener);
//  iPin07.setDebounce(debounceDelay);

//  iPin08 = gpio.provisionDigitalInputPin
//		(RaspiPin.GPIO_09,"iPin08",PinPullResistance.PULL_UP);
//  iPin08.addListener(pinListener);
//  iPin08.setDebounce(debounceDelay);

  iPin09 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_10,"iPin09",PinPullResistance.PULL_UP);
  iPin09.addListener(pinListener);
  iPin09.setDebounce(debounceDelay);

  iPin10 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_11,"iPin10",PinPullResistance.PULL_UP);
  iPin10.addListener(pinListener);
  iPin10.setDebounce(debounceDelay);

  iPin11 = gpio.provisionDigitalInputPin
		(RaspiPin.GPIO_12,"iPin11",PinPullResistance.PULL_UP);
  iPin11.addListener(pinListener);
  iPin11.setDebounce(debounceDelay);
*/

  refreshStatus();

 }

public void refreshStatus()
 {if (tracer) System.out.println("OC.refreshStatus()");
  if (iPin00.isLow())	os.setRoofOpen(true);
  else			os.setRoofOpen(false);
  if (iPin01.isLow())	os.setRoofClosed(true);
  else			os.setRoofClosed(false);

  boolean b1 = oPin06.isHigh();
  if (b1) toggleScopesParkedPower();
  if (iPin02.isLow()) os.setScope1Parked(true);	// scope 1 parked
  else                os.setScope1Parked(false);
  if (iPin03.isLow()) os.setScope2Parked(true);	// scope 2 parked
  else                os.setScope2Parked(false);
  if (iPin04.isLow()) os.setScope3Parked(true);	// scope 3 parked
  else                os.setScope3Parked(false);
  if (b1) toggleScopesParkedPower();

  if (oPin05.isLow()) os.setAcPowerAvailable(true);
  else		      os.setAcPowerAvailable(false);

  if (oPin04.isLow()) os.setScope1aPoweredUp(true);
  else		      os.setScope1aPoweredUp(false);

  if (oPin07.isLow()) os.setScope2PoweredUp(true);
  else		      os.setScope2PoweredUp(false);

  if (oPin08.isLow()) os.setScope3PoweredUp(true);
  else		      os.setScope3PoweredUp(false);

  if (oPin09.isLow()) os.setLightsOn(true);
  else		      os.setLightsOn(false);
 }

public void setOverrideScopesParked(boolean b)
 {if (tracer) System.out.println("OC.setOverrideScopesParked("+b+")");
  os.setOverrideScopesParked(b);
 }

// *************************************************************************

public void openRoof()	// 0
 {if (tracer) System.out.println("OC.openRoof()");
  if (oPin06.isHigh())
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

public void closeRoof()  // 1
 {if (tracer) System.out.println("OC.closeRoof()");
  if (oPin06.isHigh())
    toggleScopesParkedPower();		// turn on scopes parked power
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

public void stopRoof()
 {if (tracer) System.out.println("OC.stopRoof()");
  oPin00.high();
  oPin01.high();
  if (os.getRoofOpening())
    os.setRoofOpening(false);
  if (os.getRoofClosing())
    os.setRoofClosing(false);
  if (okToTurnOffSensorPower())
    toggleScopesParkedPower();

//   Refresh all status flags if necessary - insert code here

 }

public void pushInverterPowerButton()
 {if (tracer) System.out.println("OC.pushInverterPowerButton()");
  oPin10.pulse(inverterPulseTime,false);
 }

public void togglePowerS1R1()			//  scope power 
 {if (tracer) System.out.println("OC.togglePowerS1R1()");
//     if powered up and camera powered up, power off camera
  if (oPin05.isLow() & oPin04.isLow())
     togglePowerS1R2();
  oPin04.toggle();
  if (oPin04.isLow())
    os.setScope1aPoweredUp(true);
  else
   {os.setScope1aPoweredUp(false);
    if (okToTurnOffSensorPower())
      toggleScopesParkedPower();
   }
 }

public void togglePowerS1R2()   		// camera power
 {if (tracer) System.out.println("OC.togglePowerS1R2()");
//  only toggle if power applied to S1R1
  if (oPin04.isLow())  
   oPin05.toggle();
  if (oPin05.isLow())
    os.setScope1bPoweredUp(true);
  else
    os.setScope1bPoweredUp(false);
 }

public void togglePowerS2()
 {if (tracer) System.out.println("OC.togglePowerS2()");
  oPin07.toggle();
  if (oPin07.isLow())
    os.setScope2PoweredUp(true);
  else
   {os.setScope2PoweredUp(false);
    if (okToTurnOffSensorPower())
      toggleScopesParkedPower();
   }
 }

private boolean okToTurnOffSensorPower()
 {if (tracer) System.out.println("OC.okToTurnOffSensorPower()");
  if (oPin07.isHigh() & oPin08.isHigh() & oPin04.isHigh() //scopes unpowered
	& oPin00.isHigh() & oPin01.isHigh()	// roof not moving
        & (iPin00.isLow() | iPin01.isLow())	// roof open or closed
     )
    return true;
  return false;
 }

public void toggleScopesParkedPower()
 {if (tracer) System.out.println("OC.toggleScopesParkedPower()");
  oPin06.toggle();
  if (oPin06.isLow())
   {os.setScopesParkedPowerOn(true);
    try {Thread.sleep(scopeParkedWarmup);}
    catch (InterruptedException e) {}
   }
  else
   {os.setScopesParkedPowerOn(false);
   }
 }

public void togglePowerS3()
 {if (tracer) System.out.println("OC.togglePowerS3()");
  oPin08.toggle();
  if (oPin08.isLow())
    os.setScope3PoweredUp(true);
  else
   {os.setScope3PoweredUp(false);
    if (okToTurnOffSensorPower())
      toggleScopesParkedPower();
   }
 }

public void togglePowerComputer1()
 {if (tracer) System.out.println("OC.toggleComputerPower1()");
  oPin02.toggle();
  if (oPin02.isLow())
    os.setComputer1PoweredUp(true);
  else
    os.setComputer1PoweredUp(false);
 }

public void togglePowerComputer2()
 {if (tracer) System.out.println("OC.toggleComputerPower2()");
  oPin03.toggle();
  if (oPin03.isLow())
    os.setComputer2PoweredUp(true);
  else
    os.setComputer2PoweredUp(false);
 }


public void wakeUp(String computerOwner)
 {if (tracer) System.out.println("OC.wakeUp("+computerOwner+")");
  String command = "";
  if (computerOwner.equals("Abe"))
    command = "wakeupPhil";
  else if (computerOwner.equals("Phil"))
    command = "wakeupAbe";
  else
   System.out.println("Cannot wakeup computer for unknown user: "
	+computerOwner);
  if (!command.equals(""))
   {try
     {Runtime rt = Runtime.getRuntime();
      Process pr = rt.exec(command);
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
  oPin09.toggle();
  if (oPin09.isLow())
    os.setLightsOn(true);
  else
    os.setLightsOn(false);
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
   {if (event.getState().isLow())
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
                         iPin00		//roof open
			,iPin01		//roof closed
                        ,iPin02		//scope 1 parked
                        ,iPin03		//scope 2 parked
                        ,iPin04		//scope 3 parked
			,iPin05		//ac power available
                        ,iPin06		//door, outside, open
                        ,iPin07		//door, control room, open
                        ,iPin08		//lights on (maybe)
                        ,iPin09		
                        ,iPin10		
                        ,iPin11
			;
*/
    
    if (pinName.equals("iPin00"))	// roof open
     {if (switchClosed)
       {oc.stopRoof();
        os.setRoofOpen(true);
       }
      else
       {os.setRoofOpen(false);
       }
     }
    else if (pinName.equals("iPin01"))	// roof closed
     {if (switchClosed)
       {oc.stopRoof();
        os.setRoofClosed(true);
       }
      else
       {os.setRoofClosed(false);
       }
     }
    else if (pinName.equals("iPin02"))	// scope 1 parked
     {if (switchClosed)
       {os.setScope1Parked(true);
       }
      else
       {os.setScope1Parked(false);
       }
     }
    else if (pinName.equals("iPin03"))	// scope2 parked
     {if (switchClosed)
       {os.setScope2Parked(true);
       }
      else
       {os.setScope2Parked(false);
       }
     }
    else if (pinName.equals("iPin04"))	// scope 3 parked
     {if (switchClosed)
       {os.setScope3Parked(true);
       }
      else
       {os.setScope3Parked(false);
       }
     }
    else if (pinName.equals("iPin05"))	// AC power available
     {if (switchClosed)
       {os.setAcPowerAvailable(true);
       }
      else
       {os.setAcPowerAvailable(false);
       }
     }
    else if (pinName.equals("iPin06"))	// outside door open
     {if (switchClosed)
       {
       }
      else
       {
       }
     }
    else if (pinName.equals("iPin07"))	// control room door open
     {if (switchClosed)
       {
       }
      else
       {
       }
     }
    else if (pinName.equals("iPin08"))	// lights on - maybe
     {if (switchClosed)
       {
       }
      else
       {
       }
     }
    else if (pinName.equals("iPin09"))
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
    else if (pinName.equals("iPin11"))
     {if (switchClosed)
       {
       }
      else
       {
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


}
