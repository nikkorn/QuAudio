package QuInterface;

import java.io.IOException;
import Server.Log;

/**
 * Interacts with the Python led_interface.py program
 * @author Nikolas Howard
 *
 */
public class QuInterface {
	
	/**
	 * Show a RGB Qu system colour.
	 * @param r
	 * @param g
	 * @param b
	 * @param behaviour
	 */
	public static void show(int r, int g, int b, LEDBehaviour behaviour) {
		try {
			Runtime.getRuntime().exec("python led_interface.py " + r + " "  + g + " "  + b + " "  + behaviour.ordinal());
		} catch(IOException e) {
			Log.log(Log.MessageType.ERROR, "LED_INTERFACE", "failed to execute led_interface.py");
		}
	}

	/**
	 * Show a default Qu system colour.
	 * @param colourDefault
	 * @param behaviour
	 */
	public static void show(LEDColourDefault colourDefault, LEDBehaviour behaviour) {
		switch(colourDefault) {
		case ORANGE:
			show(242, 183, 73, behaviour);
			break;
		case RED:
			show(242, 89, 56, behaviour);
			break;
		case WHITE:
			show(255, 255, 255, behaviour);
			break;
		case YELLOW:
			show(245, 237, 12, behaviour);
			break;
		}
	}
}
