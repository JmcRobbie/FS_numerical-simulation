/* Copyright 20017-2018 Melbourne Space Program
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package msp.simulator.environment;

import org.orekit.errors.OrekitException;
import org.orekit.orbits.CircularOrbit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import msp.simulator.utils.logs.CustomLoggingTools;

/**
 * Major Class related to the Space Environment in the
 * simulation.
 * 
 * This class handles a set of sub-classes representing
 * the modules actually used for the accuracy of the environment.
 * E.g. OrbitWrapper, Atmosphere, GravitationalPotential, GeoMagneticField etc.
 * <p>
 * This class is responsible for creating and loading any object
 * or instance of such sub-classes and provides methods to access
 * and maintain these ones.
 * <p>
 * The Environment is considered as a set of non-interacting modules
 * only providing its instances and tools to the user.
 *
 * @author Florian CHAUBEYRE <chaubeyre.f@gmail.com>
 */
public class Environment {

	/** Logger of the class. */
	private static final Logger logger = LoggerFactory.getLogger(
			Environment.class);

	/** Instance of the Solar System in the simulation. */
	private msp.simulator.environment.solarSystem.SolarSystem solarSystem;

	/** Instance of the Earth Atmosphere in the simulation. */
	private msp.simulator.environment.atmosphere.Atmosphere atmosphere;

	/** Instance of the OrbitWrapper in the simulation. */
	private msp.simulator.environment.orbit.OrbitWrapper orbit;

	/** Instance of the Gravitational Potential. */
	private msp.simulator.environment.gravitationalPotential.GravitationalPotential
	gravitationalPotential;
	
	private msp.simulator.environment.geomagneticField.EarthMagneticField geoMagneticField;

	/**
	 * Constructor of the Space Environment of the Simulation.
	 * @throws OrekitException if OreKit initialization failed
	 */
	public Environment() throws OrekitException {
		logger.info(CustomLoggingTools.indentMsg(logger,
				"Building the Environment..."));

		/* Building the Solar System. */
		this.solarSystem = new msp.simulator.environment.solarSystem.SolarSystem();

		/* Building the Earth Atmosphere. */
		this.atmosphere = new msp.simulator.environment.atmosphere.Atmosphere(
				this.solarSystem.getEarth(),
				this.solarSystem.getSun());

		/* Building the orbit. */
		this.orbit = new msp.simulator.environment.orbit.OrbitWrapper(this.solarSystem);
	
		/* Building the Earth Gravity Field (Potential) */
		this.gravitationalPotential = new msp.simulator.environment.gravitationalPotential.
				GravitationalPotential();
		
		/* Building the Earth Magnetic Field. */
		this.geoMagneticField = new msp.simulator.environment.geomagneticField.EarthMagneticField();
	}


	/**
	 * @return the solarSystem
	 */
	public msp.simulator.environment.solarSystem.SolarSystem getSolarSystem() {
		return solarSystem;
	}

	/**
	 * @return the atmosphere
	 */
	public msp.simulator.environment.atmosphere.Atmosphere getAtmosphere() {
		return atmosphere;
	}

	/**
	 * @return the orbit
	 */
	public CircularOrbit getOrbit() {
		return this.orbit.getOrbit();
	}

	/**
	 * @return the gravitationalPotential
	 */
	public msp.simulator.environment.gravitationalPotential.GravitationalPotential getGravitationalPotential() {
		return gravitationalPotential;
	}
	
	/**
	 * @return the geoMagneticField
	 */
	public msp.simulator.environment.geomagneticField.EarthMagneticField getGeoMagneticField() {
		return geoMagneticField ;
	}

}
