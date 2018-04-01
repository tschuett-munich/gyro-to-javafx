/**
 * Author: Thomas Schuett, roboshock.de
 * 
 * License: Free to use in any way. No warrenty. Please leave 
 *          a note about the author name, thank you.
 */
package de.roboshock.javafx.gyropuppet;

import java.util.ArrayList;
import java.util.List;


public class TiltCollector {
	
    private static class ValueHolder {
    	public double x;
    	public double y;
    	public double z;
    }
    
    public static class Tilt {
    	public double x;
    	public double z;
    	public boolean valid;
    }

	final int validAccelsMinNeeded = 5;
	List<ValueHolder> validAccelsRows = new ArrayList<>();
	double scaleTilt = 60.0;           // to get degrees 0 90 180 ...

	public void addNewAccelValues(double ax, double ay, double az) {
		boolean debug = false;
		if (debug) {
			double scaleAccel = 6.0 / 1000d;  // -->  1g == 100.0
			System.out.println((int) (ax * scaleAccel) 
					+ "    " + (int) (ay * scaleAccel)
					+ "    " + (int) (az * scaleAccel));
		}

		double sumAccel = ax*ax + ay*ay + az*az;
		if (debug) System.out.println("acceleration sum: " + sumAccel);
		if (sumAccel < 89.0 || sumAccel > 107.0) { // if not around 9.81^2
			// tilt is invalid
			validAccelsRows.clear();
		}
		else {
			ValueHolder accel = new ValueHolder();
			accel.y = ax; accel.z = ay; accel.x = az;
			addValidAccel(accel);
		}
	}
	
	private void addValidAccel(ValueHolder accel) {
		validAccelsRows.add(0, accel);
		if (validAccelsRows.size() > validAccelsMinNeeded) {
			for (int i=validAccelsRows.size() - 1; i > validAccelsMinNeeded; i--) {
				validAccelsRows.remove(i);
			}
		}
	}
	
	/**
	 * @return The smoothed resulting tilt. 
	 *         If there are not enough valid readings in a row, all values will be zero
	 */
	public Tilt getSummarizedTilt() {
		Tilt res = new Tilt();
		res.valid = false; // invalid measure

		if (validAccelsRows.size() < validAccelsMinNeeded) {
			return res;
		}
		
		// ewma low pass filter
		// init with oldest value
		double d = 0.3;
		double sumX = validAccelsRows.get(validAccelsMinNeeded-1).x;
		double sumY = validAccelsRows.get(validAccelsMinNeeded-1).y;
		double sumZ = validAccelsRows.get(validAccelsMinNeeded-1).z;
		
		for (int i=validAccelsMinNeeded-2; i >= 0; i--) {
			sumX = (1.0 - d) * sumX + d * validAccelsRows.get(i).x;
			sumY = (1.0 - d) * sumY + d * validAccelsRows.get(i).y;
			sumZ = (1.0 - d) * sumZ + d * validAccelsRows.get(i).z;
		}
		
		res.x = Math.atan(sumX / Math.sqrt(sumZ*sumZ + sumY*sumY)) * scaleTilt;
		res.z = Math.atan(sumZ / Math.sqrt(sumX*sumX + sumY*sumY)) * scaleTilt;
		res.valid = true; // valid measure
		
		return res;
	}

	
	public boolean isAccelInMotion() {
		if (validAccelsRows.size() < 2) return true; // euclid sum of accels not =g
		double treshold = 0.5; // each accel will be between 0 and ~10
		if (Math.abs(validAccelsRows.get(0).y - validAccelsRows.get(1).y) > treshold) return true;
		if (Math.abs(validAccelsRows.get(0).z - validAccelsRows.get(1).z) > treshold) return true;
		if (Math.abs(validAccelsRows.get(0).x - validAccelsRows.get(1).x) > treshold) return true;
		return false;
	}
}
