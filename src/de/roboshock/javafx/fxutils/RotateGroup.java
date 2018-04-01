package de.roboshock.javafx.fxutils;

import javafx.scene.Group;
import javafx.scene.transform.Rotate;

public class RotateGroup extends Group {

    public Rotate rx = new Rotate();
    public Rotate ry = new Rotate();
    public Rotate rz = new Rotate();

    public RotateGroup() { 
        super();
        rx.setAxis(Rotate.X_AXIS);
        ry.setAxis(Rotate.Y_AXIS);
        rz.setAxis(Rotate.Z_AXIS);
        getTransforms().addAll(rz, ry, rx); 
    }

}
