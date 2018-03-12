/* Copyright 2017-2018 Melbourne Space Program */

package msp.simulator.dynamic.test;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.attitudes.Attitude;
import org.orekit.propagation.SpacecraftState;

import msp.simulator.NumericalSimulator;
import msp.simulator.user.Dashboard;


/**
 * JUnit Tests of the attitude propagation engine.
 * 
 * @author Florian CHAUBEYRE
 */
public class TestAttitudePropagation {

	/**
	 * Process a simple rotation of Pi at constant spin to
	 * check the attitude quaternion propagation.
	 * 
	 * <p><b>Justification:</b><p>
	 * 
	 * + At the initial state: <p>
	 * Q0 = (1, 0, 0, 0) i.e. the satellite frame is aligned
	 * with the inertial frame.
	 * <p>
	 * + At any time t: <p>
	 * Q	 = ( cos(a/2), sin(a/2).n )
	 *  	= ( cos(w.t/2), sin(w.t/2).n )
	 *  with w = Pi/dur
	 *  <p>
	 * + At the time t = dur, i.e. the end state:<p>
	 *  Q = ( cos(Pi/2), sin(Pi/2).n )
	 *    = ( 0, nx, ny, nz)
	 * @throws Exception 
	 * 
	 */
	@Test 
	public void testSimpleRotation() throws Exception {
		
		/* *** CONFIGURATION *** */
		double rotationTime = 100;
		Vector3D n = new Vector3D(1,2,3).normalize();
		/* ********************* */
		
		NumericalSimulator simu = new NumericalSimulator();
		Dashboard.setDefaultConfiguration();
		
		Dashboard.setIntegrationTimeStep(0.1);
		Dashboard.setEphemerisTimeStep(1.0);
		Dashboard.setSimulationDuration(rotationTime);
		Dashboard.setInitialAttitudeQuaternion(1, 0, 0, 0);
		Dashboard.setInitialSpin(new Vector3D(FastMath.PI / rotationTime, n));
		Dashboard.setInitialRotAcceleration(new Vector3D(0,0,0));
		
		simu.launch();
		
		/* Actual end state of the satellite. */
		Attitude endAttitude = simu.getSatellite().getStates().getCurrentState().getAttitude();
		double[] actualAttitudeArray = new double[] {
				endAttitude.getRotation().getQ0(),
				endAttitude.getRotation().getQ1(),
				endAttitude.getRotation().getQ2(),
				endAttitude.getRotation().getQ3(),
		};
		
		/* Expected state of the satellite after the processing. */
		double[] expectedAttitudeArray = new double[] {0, n.getX(), n.getY(), n.getZ()} ;
		
		/* Approximation error during the propagation. */
		double delta = 1e-6 ;
		
		/* Testing the attitude of the satellite after the processing. */
		Assert.assertArrayEquals(
				expectedAttitudeArray, 
				actualAttitudeArray,
				delta);
	}
	
	@Test
	public void testSimpleAcceleration() throws Exception {
		
		/* *** CONFIGURATION *** */
		double accelerationTime = 100;
		Vector3D initialSpin = new Vector3D(2.7, -1.5, 0.3);
		Vector3D fixedRateAcceleration = new Vector3D(0.01, 0.02, -0.03);
		/* ********************* */
		
		NumericalSimulator simu = new NumericalSimulator();
		Dashboard.setDefaultConfiguration();
		
		Dashboard.setIntegrationTimeStep(0.1);
		Dashboard.setEphemerisTimeStep(1.0);
		Dashboard.setSimulationDuration(accelerationTime);
		
		Dashboard.setInitialSpin(initialSpin);
		Dashboard.setInitialRotAcceleration(fixedRateAcceleration);
		
		/*
		ArrayList<AutomaticTorqueLaw.Step> autoTorqueScenario = 
				new ArrayList<AutomaticTorqueLaw.Step>();
		autoTorqueScenario.add(new AutomaticTorqueLaw.Step(1., 3., new Vector3D(1,0,0)));
		autoTorqueScenario.add(new AutomaticTorqueLaw.Step(5., 3., new Vector3D(-1,0,0)));
		autoTorqueScenario.add(new AutomaticTorqueLaw.Step(55., 10., new Vector3D(1,2,3)));
		autoTorqueScenario.add(new AutomaticTorqueLaw.Step(70., 10., new Vector3D(-1,-2,-3)));
		Dashboard.setTorqueScenario(autoTorqueScenario);
		 */
		
		Dashboard.checkConfiguration();
		
		/* Launching the simulation. */
		simu.launch();
		
		/* Extracting final state. */
		SpacecraftState finalState = simu.getSatellite().getStates().getCurrentState();
		
		/* Checking Rotational Acceleration. */
		Assert.assertArrayEquals(
				fixedRateAcceleration.toArray(), 
				finalState.getAdditionalState("RotAcc"), 
				0.0);
		
		/* Checking Spin */
		Assert.assertArrayEquals(
				initialSpin.add(fixedRateAcceleration.scalarMultiply(accelerationTime)).toArray(), 
				finalState.getAdditionalState("Spin"), 
				1e-9);
		
	}
	
}