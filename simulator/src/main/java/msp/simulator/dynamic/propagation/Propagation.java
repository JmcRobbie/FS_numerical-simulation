/* Copyright 2017-2018 Melbourne Space Program */

package msp.simulator.dynamic.propagation;

import org.hipparchus.complex.Quaternion;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import msp.simulator.dynamic.forces.Forces;
import msp.simulator.dynamic.guidance.Guidance;
import msp.simulator.dynamic.propagation.integration.Integration;
import msp.simulator.dynamic.propagation.integration.SecondaryStates;
import msp.simulator.dynamic.torques.Torques;
import msp.simulator.environment.Environment;
import msp.simulator.satellite.Satellite;
import msp.simulator.satellite.assembly.SatelliteStates;
import msp.simulator.utils.logs.CustomLoggingTools;

/**
 * This class is the Top Class implementing all of the
 * propagation services for the simulation.
 * <p>
 * The propagation is the core of the dynamic as it allows
 * the simulation to calculate the next step of the satellite
 * defined states through the environment, the applied forces
 * and other user-defined equations, e.g. the torques processing. 
 * 
 * @see NumericalPropagator
 *
 * @author Florian CHAUBEYRE
 */
public class Propagation {

	/** Instance of the Logger of the class. */
	private static final Logger logger = LoggerFactory.getLogger(Propagation.class);

	/** Instance of the intagration manager. */
	private Integration integrationManager;

	/** Instance of Propagator in the simulation. */
	private NumericalPropagator propagator;

	/** Instance of the satellite in the simulation. */
	private SatelliteStates satelliteStates;

	/**
	 * Create and Configure the Instance of Propagator
	 * of the Simulation.
	 * @param environment Instance of the Simulation
	 * @param satellite Instance of the Simulation
	 * @param forces Instance of the Simulation
	 * @param torques Instance of the Simulation
	 * @param guidance Instance of the Simulation
	 */
	public Propagation(Environment environment, Satellite satellite, 
			Forces forces,
			Torques torques,
			Guidance guidance
			) {
		Propagation.logger.info(CustomLoggingTools.indentMsg(Propagation.logger,
				"Registering to the Propagation Services..."));

		/* Linking the main simulation objects. */
		this.satelliteStates = satellite.getStates();

		/* Building the integration manager. */
		this.integrationManager = new Integration(satellite, torques.getTorqueProvider());

		try {
			/* Creating the Instance of Propagator. 
			 * Be aware that this new instance will have its default parameters.
			 * The user needs to configure it afterwards.
			 */
			this.propagator = new NumericalPropagator(
					this.integrationManager.getIntegrator());

			/* Set the propagation mode. More information about step handling
			 * and master mode in this package-info.
			 */
			this.propagator.setSlaveMode();

			/* Set the orbit type. */
			/* NB: otherwise the propagation converts the initial orbit to
			 * the corresponding orbit of its default set type (equinoctial).
			 */
			this.propagator.setOrbitType(environment.getOrbit().getType());

			/* Registering the implemented force models. */
			Propagation.logger.info(CustomLoggingTools.indentMsg(Propagation.logger,
					"-> Registering the implemented Linear Force Models..."));
			for (ForceModel forceModel : forces.getListOfForces() ) {
				this.propagator.addForceModel(forceModel);
				Propagation.logger.info(CustomLoggingTools.indentMsg(Propagation.logger,
						"   + " + forceModel.toString()));
			}

			/* Configuring the initial state of the satellite. */
			Propagation.logger.info(CustomLoggingTools.indentMsg(Propagation.logger,
					"-> Configuring the initial state of the satellite..."));
			this.propagator.setInitialState(
					satelliteStates.getInitialState());

			/* Registering the different providers. */
			/*  + Attitude						*/
			this.propagator.setAttitudeProvider(
					guidance.getAttitudeProvider());

			/*  + Additional Provided State		*/
			this.propagator.addAdditionalStateProvider(
					this.integrationManager.getRotAccProvider());

			/*  + Additional Integrated States	*/
			this.propagator.addAdditionalEquations(
					this.integrationManager.getSecondaryStatesEquation());

		} catch (OrekitException e) {
			e.printStackTrace();
		}

		Propagation.logger.info(CustomLoggingTools.indentMsg(Propagation.logger,
				"Propagator Configured."));
	}

	/**
	 * Ensure the propagation to the target date and update the satellite
	 * state.
	 * <p>
	 * Note that the time resolution of the propagation is not given by the
	 * target date but by the integration time step.
	 * 
	 * @param targetDate The date where the propagation processing ends.
	 * @deprecated since v0.4: the propagation should be made at each integration step.
	 */
	public void propagate(AbsoluteDate targetDate) {
		try {
			SpacecraftState propagatedState = this.propagator.propagate(targetDate);
			this.satelliteStates.setCurrentState(propagatedState);

		} catch (OrekitException e) {
			e.printStackTrace();
		}
	}

	public void propagateStep() {

		/* Step before the integration s(t). */
		SpacecraftState s_t = null;

		/* Step after the integration s(t + dt). */
		SpacecraftState s_t_dt = null;

		/* Step at the end of all of the propagations.
		 * (Integration, orbit, additional states and attitude)
		 */
		SpacecraftState propagatedState = null;

		try {
			/* Get s(t) */
			s_t = this.satelliteStates.getCurrentState();

			/* Get s(t+dt) */
			s_t_dt = this.propagator.propagate(
					this.satelliteStates.getCurrentState().getDate()
					.shiftedBy(
							this.getIntegrationManager().getStepSize())
					);

			/* Debug log. */
			logger.debug("#### PROPAGATION STEP: " 
					+ s_t.getDate().toString()
					+ " ---> "
					+ s_t_dt.getDate().toString()
					);


			/* Propagate the attitude. */
			propagatedState = this.propagateAttitude(s_t, s_t_dt);

			/* Set the updated satellite state. */
			this.satelliteStates.setCurrentState(
					propagatedState);

		} catch (OrekitException e) {
			logger.error("Progation failed - Step "
					+ this.satelliteStates.getCurrentState().getDate().toString()
					+ " ---> "
					+ this.satelliteStates.getCurrentState().getDate()
					.shiftedBy(Integration.integrationTimeStep)
					);
			e.printStackTrace();
		}
	}

	/**
	 * Compute and Update the satellite state at the next time step.
	 * <p>
	 * The dynamic computation is as follow: 
	 * Torque -- Spin -- Attitude.
	 * <p>
	 * The main OreKit integration takes care of the orbital 
	 * parameters and integrates the additional data - e.g. the 
	 * spin - where the secondary integration resolves the new 
	 * attitude through the Wilcox algorithm.
	 * <p>
	 * This process is called "Propagation".
	 * 
	 * @param currentState The state s(t), i.e. the one before integration.
	 * @param integratedState The state s(t + dt), i.e. resulting 
	 * from the integration of the step s(t).
	 * @return 
	 * 
	 * @see NumericalPropagator#propagate(AbsoluteDate)
	 * 
	 */
	public SpacecraftState propagateAttitude(
			SpacecraftState currentState,
			SpacecraftState integratedState
			) {

		/* Final state fully propagated at t + dt. */
		SpacecraftState secondaryPropagatedState = null;

		try {

			/** TODO: Update the comments. */

			/* At that point the main propagation of the orbital parameters
			 * and the additional states (spin and rotational acceleration)
			 * is done and we have access to a SpacecraftState containing 
			 * the current Attitude and the new integrated spin.
			 * Thus we can compute the attitude at the next step.
			 */
			/* Nonetheless as said in the main introduction of the class
			 * the date contained in the main propagated state is not updated,
			 * it means that we have access to the right orbit but with the
			 * previous date. Thus we have to update this date ourselves to
			 * store it in our own satellite states.
			 * As the date is accessed through the orbit, we have to build
			 * a clone of the orbit but with the right date.
			 */

			/* Determine the data of the rotation at the next step. 
			 * The additional states are already updated.
			 */

			/* Rotational Acceleration */
			Vector3D rotAcc = new Vector3D(integratedState.getAdditionalState("RotAcc"));

			/* Spin */
			Vector3D spin = new Vector3D(
					SecondaryStates.extractState(
							integratedState.getAdditionalState(SecondaryStates.key),
							SecondaryStates.SPIN)
					);

			/* dTheta: small angle of rotation during the step. */
			/* theta(t + dt) */
			Vector3D theta_T_DT = new Vector3D(
					SecondaryStates.extractState(
							integratedState.getAdditionalState(SecondaryStates.key),
							SecondaryStates.THETA)
					);

			/* theta(t) */
			Vector3D theta_T = new Vector3D(
					SecondaryStates.extractState(
							currentState.getAdditionalState(SecondaryStates.key),
							SecondaryStates.THETA)
					);

			/* dTheta = theta(t + dt) - theta(t) */
			Vector3D dTheta = theta_T_DT.subtract(theta_T) ;

			/* Attitude determination: it needs to be propagated. */
			Attitude currentAttitude =
					this.satelliteStates.getCurrentState().getAttitude();

			Quaternion currentQuaternion = new Quaternion (
					currentAttitude.getRotation().getQ0(),
					currentAttitude.getRotation().getQ1(),
					currentAttitude.getRotation().getQ2(),
					currentAttitude.getRotation().getQ3()
					);

			/* 		-> Propagate the attitude quaternion. */
			Quaternion propagatedQuaternion =
					wilcox(		
							currentQuaternion, 
							dTheta, 
							this.integrationManager.getStepSize()
							);




			/* 		-> Build the final attitude. */
			Attitude propagatedAttitude = new Attitude (
					integratedState.getDate(),
					integratedState.getFrame(),
					new AngularCoordinates(
							new Rotation(
									propagatedQuaternion.getQ0(),
									propagatedQuaternion.getQ1(),
									propagatedQuaternion.getQ2(),
									propagatedQuaternion.getQ3(),
									true /* Normalize the quaternion. */
									),
							spin,
							rotAcc
							)
					);

			/* Finally mount the new propagated state: only the attitude is modified. */
			secondaryPropagatedState = new SpacecraftState(
					integratedState.getOrbit(),
					propagatedAttitude,
					integratedState.getMass(),
					integratedState.getAdditionalStates()
					);

		} catch (OrekitException e) {
			e.printStackTrace();
		}

		return secondaryPropagatedState;
	}

	/**
	 * The Wilcox Algorithm allows to compute the differential equation
	 * leading the quaternion kinematic. It means this can provide
	 * the quaternion at the next integration step (t+dt) regarding the orientation 
	 * and the rotational speed at the time t.
	 * <p>
	 * Be aware that this processing considers the orientation of the spin
	 * vector to be constant over a step. In the case the rotational speed
	 * vector is not constant all along the step, an error said an error
	 * of commutation appears and one should use the Edward's algorithm
	 * that propose a correction.
	 * 
	 * @param Qi Initial quaternion to propagate
	 * @param spin Instant rotational speed
	 * @param dt Integration time step
	 * @return Qj The final quaternion after the rotation due 
	 * to the spin during the step of time.
	 * 
	 * @see StepHandler#edwards
	 */
	public static Quaternion wilcox(Quaternion Qi, Vector3D theta, double dt) {

		/* Vector Angle of Rotation: Theta = dt*W */
		//Vector3D theta = new Vector3D(dt, spin);

		/* Compute the change-of-frame Quaternion dQ */
		double dQ0 = 1. - theta.getNormSq() / 8. ;
		double dQ1 = theta.getX() / 2. * (1. - theta.getNormSq() / 24.);
		double dQ2 = theta.getY() / 2. * (1. - theta.getNormSq() / 24.);
		double dQ3 = theta.getZ() / 2. * (1. - theta.getNormSq() / 24.);

		Quaternion dQ = new Quaternion(dQ0, dQ1, dQ2, dQ3);

		/* Compute the final state Quaternion. */
		Quaternion Qj = Qi.multiply(dQ).normalize();

		return Qj ;
	}

	/**
	 * Edward's algorithm enables to integrate the differential equation
	 * leading the Quaternion kinematic by trying to reduce the error said
	 * of commutation that appears when the spin vector does not have a
	 * constant direction during the integration step.
	 * <p>
	 * Nonetheless, the error of commutation is zero when the spin
	 * vector and the integrated spin vector over the step (or theta)
	 * are linear. Then the algorithm is equivalent to the typical Wilcox 
	 * algorithm.
	 * 
	 * @param Qi Initial Quaternion
	 * @param theta Integrated spin vector ( integral(spin, t, t+dt) )
	 * @param spin Rotational Speed vector
	 * @param dt Integration step
	 * @return The quaternion at t+dt
	 * 
	 * @see StepHandler#wilcox
	 */
	public static Quaternion edwards(Quaternion Qi, Vector3D theta, Vector3D spin, double dt) {

		/* Compute the error of commutation. */
		Vector3D commutation = 
				spin.scalarMultiply(1. / 2.)
				.crossProduct(theta.scalarMultiply(1. / 2.))
				.scalarMultiply(1. / 12.);

		/* Compute the transition quaternion. */
		double scalarPart = 1. - theta.getNormSq() / 8. ;
		Vector3D vectorPart = 
				theta
				.scalarMultiply( ( 1. - theta.getNormSq() / 24.) / 2. )
				.add(commutation);

		Quaternion dQ = new Quaternion(scalarPart, vectorPart.toArray());

		/* Finally compute the final quaternion. */
		Quaternion Qf = Qi.multiply(dQ).normalize();

		return Qf;
	}

	/**
	 * @return the integrationManager
	 */
	public Integration getIntegrationManager() {
		return integrationManager;
	}
}
