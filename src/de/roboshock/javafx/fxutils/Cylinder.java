package de.roboshock.javafx.fxutils;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.transform.Rotate;

public class Cylinder extends javafx.scene.shape.Cylinder {

    final Rotate rx = new Rotate(0, Rotate.X_AXIS);
    final Rotate ry = new Rotate(0, Rotate.Y_AXIS);
    final Rotate rz = new Rotate(0, Rotate.Z_AXIS);

    public Cylinder(double sizeX, double sizeZ, Color color) {
        super(sizeX, sizeZ);
        setMaterial(new PhongMaterial(color));
        getTransforms().addAll(rz, ry, rx);
    }
}
