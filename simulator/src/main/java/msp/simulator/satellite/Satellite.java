/* Copyright 2017-2018 Melbourne Space Program */

package msp.simulator.satellite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import msp.simulator.environment.Environment;
import msp.simulator.satellite.assembly.Assembly;
import msp.simulator.satellite.assembly.SatelliteStates;
import msp.simulator.satellite.sensors.Sensors;
import msp.simulator.utils.logs.CustomLoggingTools;

/**
 *
 * @author Florian CHAUBEYRE
 */
public class Satellite {

	/** Logger of the class */
	private static final Logger logger = LoggerFactory.getLogger(Satellite.class);

	/** Instance of environment of the simulation. */
	private Environment environment;
	
	/** Instance of Assembly of the Satellite. */
	private Assembly assembly;
	
	/** Instance of the Sensors of the satellite. */
	private Sensors sensors;

	/**
	 * Build the intance of the Satellite in the simulation.
	 * @param environment Instance of the Simulation
	 */
	public Satellite(Environment environment) {
		Satellite.logger.info(CustomLoggingTools.indentMsg(Satellite.logger,
				"Building the Satellite..."));
		
		/* Linking the satellite module to the simulation. */
		this.environment = environment;

		/* Building the Assembly of the Satellite. */
		this.assembly = new Assembly(this.environment);
		
		/* Building the sensors. */
		this.sensors = new Sensors(this.environment, this.assembly);
		
		
	}

	/**
	 * Return the assembly of the satellite.
	 * @return Assembly
	 * @see msp.simulator.satellite.assembly.Assembly
	 */
	public Assembly getAssembly() {
		return this.assembly;
	}

	/**
	 * Return the States object of the satellite.
	 * @return SpacecraftState
	 * @see msp.simulator.satellite.assembly.SatelliteStates
	 */
	public SatelliteStates getStates() {
		return this.getAssembly().getStates();
	}

	/**
	 * Return the satellite sensors.
	 * @return Sensors
	 * @see msp.simulator.satellite.sensors.Sensors
	 */
	public Sensors getSensors() {
		return sensors;
	}

}
