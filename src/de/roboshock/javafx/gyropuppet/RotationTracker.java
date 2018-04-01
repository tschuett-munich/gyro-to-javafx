/**
 * Author: Thomas Schuett, roboshock.de
 * 
 * License: Free to use in any way. No warrenty. Please leave 
 *          a note about the author name, thank you.
 */
package de.roboshock.javafx.gyropuppet;

import javafx.geometry.Point3D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;

public class RotationTracker {

	boolean debug = false;
	private Affine rot;
	
	
    public RotationTracker(Affine rot) {
		super();
		this.rot = rot;
	}

	public void addRotate(Point3D axis, double angle) {

    	// thanks to Jose Pereda for this axis conversion
        Point3D localAxisX = new Point3D(rot.getMxx(), rot.getMyx(), rot.getMzx());
        Point3D localAxisY = new Point3D(rot.getMxy(), rot.getMyy(), rot.getMzy());
        Point3D localAxisZ = new Point3D(rot.getMxz(), rot.getMyz(), rot.getMzz());
        
        // apply rotation
        rot.prepend(axis == Rotate.X_AXIS ? new Rotate(angle, localAxisX)
        		: axis == Rotate.Y_AXIS ? new Rotate(angle, localAxisY) 
        		: new Rotate(angle, localAxisZ));
    }
    
    public void addRotate(double angleX, double angleY, double angleZ) {

    	// thanks to Jose Pereda for this axis conversion
        Point3D localAxisX = new Point3D(rot.getMxx(), rot.getMyx(), rot.getMzx());
        Point3D localAxisY = new Point3D(rot.getMxy(), rot.getMyy(), rot.getMzy());
        Point3D localAxisZ = new Point3D(rot.getMxz(), rot.getMyz(), rot.getMzz());

        // apply rotation
        rot.prepend(new Rotate(angleX, localAxisX));
        rot.prepend(new Rotate(angleY, localAxisY));
        rot.prepend(new Rotate(angleZ, localAxisZ));

        boolean showTorsoRotations = false;
        if (showTorsoRotations) {
	        printConverted(rot.getMxx());
	        printConverted(rot.getMyx());
	        printConverted(rot.getMzx());
	        System.out.print("   ");
	        printConverted(rot.getMxy());
	        printConverted(rot.getMyy());
	        printConverted(rot.getMzy());
	        System.out.print("   ");
	        printConverted(rot.getMxz());
	        printConverted(rot.getMyz());
	        printConverted(rot.getMzz());
	        //System.out.printf("   ---   %3.0f    %3.0f ", sensorTiltX, sensorTiltY);
	        System.out.print("\n");
        }
    }
    
    public double getOrientationX() {
		return Math.asin(rot.getMyz()) * 180.0 / 3.14;
    }
    
    public double getOrientationZ() {
		return Math.asin(rot.getMyx()) * 180.0 / 3.14;
    }
    
    public double getOrientationY() {
    	// not tested
		return Math.asin(rot.getMzx()) * 180.0 / 3.14;
    }
    
    public void applyCorrection(double sensorTiltX, double sensorTiltZ) {
		double accelAngelX = sensorTiltX;
		double accelAngelZ = sensorTiltZ;

		double virtAngelX = Math.asin(rot.getMyz()) * 180.0 / 3.14;
        double virtAngelZ = Math.asin(rot.getMyx()) * 180.0 / 3.14;

        if (debug) System.out.printf("%3.2f  (%3.2f)      %3.2f  (%3.2f)    ", 
        				virtAngelX, accelAngelX,
        				virtAngelZ, accelAngelZ ); 
        
        double xCorr = accelAngelX + virtAngelX; // all values are in degree (0 .. 45 .. 180)
        double zCorr = accelAngelZ + virtAngelZ; // all values are in degree (0 .. 45 .. 180)
        
        if (Math.abs(xCorr) > 10 || Math.abs(zCorr) > 10 ) {
        	// fast correction
        	xCorr = limit(2.0, xCorr);
        	zCorr = limit(2.0, -zCorr);
        }
        else {
        	// smooth correction
        	xCorr = limit(0.3, ignore(0.3,  xCorr));
        	zCorr = limit(0.3, ignore(0.3, -zCorr));
        }
        
        if (debug) System.out.printf("%2.2f   %2.2f\n", xCorr,zCorr);
        
        addRotate(Rotate.X_AXIS, xCorr);
        addRotate(Rotate.Z_AXIS, zCorr);
    }
    
    private void printConverted(double d) {
    	double h = Math.asin(d) * 180.0 / 3.14; 
    	System.out.printf("%3.0f  ", h);
    }
    
    private double limit(double max, double in) {
    	if (in < 0.0) {
    		if (in < -max) return -max;
    		else return in;
    	}
    	else {
    		if (in > max) return max;
    		else return in;
    	}
    }
    
    private double ignore(double limit, double in) {
    	if (Math.abs(in) < limit) return 0;
    	return in;
    }

}
