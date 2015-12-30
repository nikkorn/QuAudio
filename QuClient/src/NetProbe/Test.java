package NetProbe;

import java.util.ArrayList;

public class Test {
	
	public static void main(String[] args) {
		NetProbe probe = new NetProbe();
		
		System.out.println("ready to initalise!");
	
		if(probe.initialise()) {
			ArrayList<ReachableQuDevice> devices = probe.getReachableQuDevices();
			
			for(ReachableQuDevice device : devices) {
				System.out.println("DEVICE:");
				System.out.println("   ID        : " + device.getDeviceId());
				System.out.println("   NAME      : " + device.getDeviceName());
				System.out.println("   ADDRESS   : " + device.getAddress());
				System.out.println("   PROTECTED : " + device.isProtected());
			}
			
		} else {
			System.out.println("We Failed to initalise!!!");
		}
	}

}
