/**
 * - Shows a simple puppet with one arm in a javafx 3D space
 * - Receives gyro and tilt data via interface ValueSinkDouble6 and applies it to the puppet
 * - Offers sliders to move the arm
 * 
 * Author: Thomas Schuett, roboshock.de
 * 
 * License: Free to use in any way. No warrenty. Please leave 
 *          a note about the author name, thank you.
 */
package de.roboshock.javafx.gyropuppet;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import de.roboshock.javafx.fxutils.Cube;
import de.roboshock.javafx.fxutils.Cylinder;
import de.roboshock.javafx.fxutils.RotateGroup;

public class GyroPuppet extends Application implements ValueSinkDouble6 {

	private static class ValueHolder {
		public double x;
		public double y;
		public double z;
	}

	static boolean withGUI = true;
	int width = 900;
	int height = 700;

	boolean useDeviceInput = true;
	boolean rundeArme = true;

	RotateGroup torsoGroup;
	RotateGroup schulterGroup;
	RotateGroup oberarmGroup;
	RotateGroup unterarmGroup;

	double torsoX = 50;
	double torsoY = 80;
	double group1Length = 5;
	double group2Length = 40;
	double group3Length = 40;

	public Slider slider1;
	public Slider slider2;
	public Slider slider3;
	public Slider slider4;
	DeviceReaderThread readerThread;

	private Affine torsoRotate = new Affine();
	private TiltCollector tiltCollector = new TiltCollector();
	private RotationTracker rotationTracker = new RotationTracker(torsoRotate);

	final ValueHolder gyroDrift = new ValueHolder();

	public Parent createRobot() {
		Cube torso = new Cube(torsoX, torsoY, 20, Color.RED);
		Cube schulter = new Cube(group1Length, 10, 10, Color.YELLOW); // schulter
		Cube oberarm = new Cube(group2Length, 10, 10, Color.GREEN); // oberarm
		Cube unterarm = new Cube(group3Length, 10, 10, Color.BEIGE); // unterarm

		// runder oberarm
		Cylinder c2b = new Cylinder(5, group2Length, Color.GREEN);
		Rotate rz2b = new Rotate(90);
		rz2b.setAxis(Rotate.Z_AXIS);
		c2b.getTransforms().add(rz2b);

		// runder unterarm
		Cylinder c3b = new Cylinder(5, group2Length, Color.BISQUE);
		Rotate rz3b = new Rotate(90);
		rz3b.setAxis(Rotate.Z_AXIS);
		c3b.getTransforms().add(rz3b);

		torsoGroup = new RotateGroup();
		schulterGroup = new RotateGroup();
		oberarmGroup = new RotateGroup();
		unterarmGroup = new RotateGroup();

		torsoGroup.getTransforms().addAll(torsoRotate);
		torsoGroup.getChildren().addAll(torso, schulterGroup);
		schulterGroup.getChildren().addAll(schulter, oberarmGroup);
		if (rundeArme) {
			oberarmGroup.getChildren().addAll(c2b, unterarmGroup);
			unterarmGroup.getChildren().addAll(c3b);
		} else {
			oberarmGroup.getChildren().addAll(oberarm, unterarmGroup);
			unterarmGroup.getChildren().addAll(unterarm);
		}

		schulterGroup.setTranslateY((torsoY - 20) / -2);
		schulterGroup.setTranslateX((torsoX + group1Length) / 2);
		oberarmGroup.setTranslateX((group1Length + group2Length) / 2);
		unterarmGroup.setTranslateX((group2Length + group3Length) / 2);

		oberarmGroup.rz.pivotXProperty().set(-group2Length / 2);
		oberarmGroup.ry.pivotXProperty().set(-group2Length / 2);
		unterarmGroup.rz.pivotXProperty().set(-group3Length / 2);

		return torsoGroup;
	}

	public Parent createUI() {

		final VBox ui = new VBox(5);

		slider1 = createSlider(50, "group1 drehung", oberarmGroup.ry, ui);
		slider2 = createSlider(80, "group2 winkel", oberarmGroup.rz, ui);
		slider3 = createSlider(0, "group2 drehung", oberarmGroup.rx, ui);
		slider4 = createSlider(50, "group3 winkel", unterarmGroup.rz, ui);

		HBox buttonBox = new HBox();
		ui.getChildren().add(buttonBox);

		Button exit = new Button("Exit");
		buttonBox.getChildren().add(exit);
		exit.setOnAction((ActionEvent arg0) -> {
			try {
				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		Button reset = new Button("Reset");
		buttonBox.getChildren().add(reset);
		reset.setOnAction((ActionEvent arg0) -> {
			torsoGroup.getTransforms().remove(torsoRotate);
			torsoRotate = new Affine();
			torsoGroup.getTransforms().addAll(torsoRotate);
			rotationTracker = new RotationTracker(torsoRotate);
		});

		return ui;
	}

	private Slider createSlider(final double value, final String labelText,
			Rotate r, Pane ui) {
		final Slider slider = new Slider(-150, 151, value);
		slider.setMajorTickUnit(50);
		slider.setMinorTickCount(0);
		slider.setShowTickMarks(true);
		slider.setShowTickLabels(true);
		slider.setStyle("-fx-text-fill: white");

		// bind to the rotate object
		r.angleProperty().bind(slider.valueProperty());

		// add to the ui
		final HBox angleControl = new HBox(5);
		angleControl.getChildren().addAll(new Label(labelText), slider);
		HBox.setHgrow(slider, Priority.ALWAYS);
		ui.getChildren().addAll(angleControl);

		return slider;
	}

	public void setSliderPositions(double x, double y, double z) {
		slider2.setValue(x);
		slider3.setValue(y); // twist
		slider1.setValue(z);
	}

	@Override
	public void start(Stage stage) throws Exception {

		// a subScene for the 3D content
		SubScene subScene = new SubScene(createRobot(), width, height, true,
				SceneAntialiasing.BALANCED);
		PerspectiveCamera camera = new PerspectiveCamera(true);
		camera.setNearClip(0.01);
		camera.setFarClip(100000);
		camera.setTranslateZ(-500);
		subScene.setCamera(camera);

		StackPane combined = new StackPane(subScene, createUI());
		combined.setStyle("-fx-background-color: linear-gradient(to bottom, cornsilk, midnightblue);");

		Scene scene = new Scene(combined, width, height - 100);
		stage.setScene(scene);
		stage.show();

		if (useDeviceInput) {
			readerThread = new DeviceReaderThread((ValueSinkDouble6) this);
			readerThread.start();
		}
	}

	public void stop() throws Exception {
		System.out.println("stopping all threads");
		if (readerThread != null)
			readerThread.terminate();
		super.stop();
	}

	/**
	 * ax, ay, az: acceleration in m/s² (e.g. summarized 9.81)
	 * 
	 * gx, gy, gz: Changes in degree (0 .. 45 .. 180) since last reading,
	 * relative to the sensor axes (actually orientation change speed in degrees
	 * per 20 ms, to be precise)
	 */
	public void setNewValues(double ax, double ay, double az, double gx,
			double gy, double gz) {
		Platform.runLater(new Runnable() {
			public void run() {
				tiltCollector.addNewAccelValues(ax, ay, az);
				/*
				 * tilt.x, tilt.z : absolute tilt values in degree (0 .. 45 ..
				 * 180) tilt.valid == false -> invalid tilt due to sensor
				 * movement (when the euclid sum of ax,ay,az was not around g
				 * (9.81) in the last n readings)
				 */
				TiltCollector.Tilt tilt = tiltCollector.getSummarizedTilt();

				rotationTracker.addRotate(gy - gyroDrift.y, -gx + gyroDrift.x,
						gz - gyroDrift.z);

				double angleLimit = 60.0;
				if (tilt.valid && Math.abs(tilt.x) < angleLimit
						&& Math.abs(tilt.z) < angleLimit) {
					rotationTracker.applyCorrection(tilt.x, tilt.z);
				}

				System.out.printf("pitch: %3d   roll: %3d   turn: %3d\n",
						(int) rotationTracker.getOrientationX(),
						(int) rotationTracker.getOrientationZ(),
						(int) rotationTracker.getOrientationY());
			}
		});
	}

	public static void main(String[] args) {
		if (withGUI) {
			launch(args);
		} else {
			GyroPuppet app = new GyroPuppet();
			app.readerThread = new DeviceReaderThread((ValueSinkDouble6) app);
			app.readerThread.start();
		}
	}
}
