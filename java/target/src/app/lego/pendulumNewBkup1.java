package lego;

import joprt.RtThread;
import lego.lib.Buttons;
import lego.lib.DigitalInputs;
import lego.lib.FutureUse;
import lego.lib.Leds;
import lego.lib.Motor;
import lego.lib.Sensors;

//import util.Timer;
//import com.jopdesign.sys.Const;
//import com.jopdesign.sys.Native;

/**
 * 
 * @author Alexander Dejaco (alexander.dejaco@student.tuwien.ac.at)
 *
 */

public class pendulumNewBkup1 {
		
	static Motor[] motors;	
	static StringBuffer output;
	
	public static final int ALED0 = 1<<1; 	
	public static final int ALED1 = 1<<3;
	public static final int ALED2 = 1<<5;
	public static final int ALED3 = 1<<7;
	
	static int startval, soll, ist, stell, counter, counterSum, e, esum, last_sum;
	
	static boolean active = false;
	static boolean btn3, btn2, btn1, din0, din1, hold, refreshOutput, setstell;
	
	public static void init() {
		output = new StringBuffer();
		motors = new Motor[]
		{
				new Motor(InvertedPendulumSettings.MOTOR0),
				new Motor(InvertedPendulumSettings.MOTOR1)
		};
		
		startval = -1;
		hold = true;
		esum = 0;
		e = 0;
		soll = -1;
		counter = 0;
		last_sum = 0;

	}


	public static void main(String[] agrgs) {
		System.out.println("Initializing...");
	
	

		init();


		new RtThread(10, 10*1000)
		{			
			public void run()
			{
				while (active) {
					btn1 = Buttons.getButton(1);
					btn2 = Buttons.getButton(2);
					btn3 = Buttons.getButton(3);
					
					din0 = DigitalInputs.getDigitalInput(0);
					din1 = DigitalInputs.getDigitalInput(1);
					
					if ((!din0)||(!din1)) {
						hold = true;
					}
					
					++counter;
					refreshOutput = (counter % 0x1000 == 0);
				
					
					if (refreshOutput)
						output.setLength(0);
					
					if (btn1) { // RUN
						hold = false;
					}
					
						if (btn2) {		// STOP
						motors[0].setState(Motor.STATE_OFF);
						hold = true;
						motors[1].setState(Motor.STATE_OFF);
					 }
						
					if (btn3) {		// RESET START VALUE, START AGAIN
					  motors[0].setState(Motor.STATE_OFF);
					  motors[1].setState(Motor.STATE_OFF);
					  esum = 0;
					  soll = -1;
					}
					
			//	ist = InvertedPendulumSettings.getAngle();
					
				ist = Sensors.readSensor(InvertedPendulumSettings.TILT_SENSOR0);
				
				if (refreshOutput)
				output.append(" ist: ").append(ist);
				
				if (soll == -1)
					soll = ist;
				
			
				

				
				
				e = ist-soll;				
				esum = Math.max((Math.min((esum + e),10001)),-10001);
				
				
				if ((e < 2) && (e > -2)) {
					FutureUse.writePins(ALED2|ALED1);
				}else
				if ((e < 10) && (e > 2)) {
					FutureUse.writePins(ALED1);
				}else
				if (e > 10) {
					FutureUse.writePins(ALED0);
				}else
				if ((e > -10)&&(e < -2)) {
								FutureUse.writePins(ALED2);
				}else
				if (e < -10){
					FutureUse.writePins(ALED3);
				}
					
				
				
				if (esum > 0) {
					stell = 0;

					
					if (esum > 3500) {
						stell = 4000; 
						Leds.setLed(0, true);
						Leds.setLed(1, false);
						Leds.setLed(2, false);
						Leds.setLed(3, false);

					}
					if (esum > 4200) {
						stell = 6000; 
					}
					if (esum > 6000) {
						stell = 10000; 
						Leds.setLed(1, true);

					}
					if (esum > 10000) {
						stell = 15000;
						Leds.setLed(2, true);
					}
					if (esum > 15000) {
						stell = 50000;
						Leds.setLed(3, true);
					}
					
				} else
				if (esum < 0) {
					stell = 0;

					
					if (esum < -3500) {
						Leds.setLed(0, true);
						Leds.setLed(1, false);
						Leds.setLed(2, false);
						Leds.setLed(3, false);
						stell = -5000;

					}
					if (esum < -4200) {
						stell = -6000; 
					}
					if ( esum < -6000) {
						stell = -10000;
						Leds.setLed(1, true);

					}
					if (esum < -10000) {
						stell = -15000;
						Leds.setLed(2, true);
					}
					if (esum < -15000) {
						stell = -50000;
						Leds.setLed(2, true);
					}
		
				} else
				if (esum == 0) {

					stell = 0;

				}

					
				
			//	stell = esum;
				
				
				
				if (refreshOutput) {
				output.append(" e: ").append(e);
				output.append(" esum: ").append(esum);
				output.append(" stell: ").append(stell);
				
				}
				

				if (refreshOutput)
					System.out.println(output);

				
				if (!hold) {
					//setValue(stell*166116);
					//setValue(stell*10000);
					setValue(stell);
				//	setValue(stell >= 0 ? (stell < 3500 ? 3500 : stell) : (stell > -3000 ? -3000 : stell));
				} else
					setValue(0);
					
				}
			}
		};

		while (Buttons.getButtons() == 0);
		while (Buttons.getButtons() != 0);
		
		active = true;
		RtThread.startMission();
		
	}
	
	static void setValue(int value) {
		motors[0].setMotor(value >= 0 ? Motor.STATE_BACKWARD : Motor.STATE_FORWARD, 
				true, Math.max(0, Math.min(Motor.MAX_DUTYCYCLE, Math.abs(value))));
		motors[1].setMotor(value >= 0 ? Motor.STATE_BACKWARD : Motor.STATE_FORWARD, 
				true, Math.max(0, Math.min(Motor.MAX_DUTYCYCLE, Math.abs(value))));
	}

}