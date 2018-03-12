/* Copyright 2017-2018 Melbourne Space Program */

package msp.simulator.satellite.sensors;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.models.earth.GeoMagneticElements;
import org.orekit.propagation.SpacecraftState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import msp.simulator.environment.Environment;
import msp.simulator.environment.geomagneticField.EarthMagneticField;
import msp.simulator.environment.solarSystem.Earth;
import msp.simulator.satellite.assembly.Assembly;
import msp.simulator.utils.logs.CustomLoggingTools;

/**
 * This class represents the magnetometer sensor of the
 * satellite.
 *
 * @author Florian CHAUBEYRE
 */
public class Magnetometer {
	
	/* ******* Public Static Attributes ******* */

	/** This intensity is used to generate a random number to be
	 * added to each components of the true magnetic field. 
	 * (nanoTesla)
	 */
	public static double defaultNoiseIntensity = 1e2 ;

	/* **************************************** */

	/** Logger of the class. */
	private static final Logger logger = LoggerFactory.getLogger(
			Magnetometer.class);

	/** Instance of the OreKit Magnetic Field. */
	private EarthMagneticField geomagField;

	/** Instance of the Earth. */
	private Earth earth;

	/** Assembly of the satellite. */
	private Assembly assembly;
	
	/** Private attribute for the noise intensity. */
	private double noiseIntensity;

	public Magnetometer(Environment environment, Assembly assembly) {
		logger.info(CustomLoggingTools.indentMsg(logger,
				"Building the Magnetometer..."));

		/* Linking the class to the rest of the simulation. */
		this.geomagField = environment.getGeoMagneticField();
		this.earth = environment.getSolarSystem().getEarth();
		this.assembly = assembly;

		/* Initializing the class. */
		this.noiseIntensity = Magnetometer.defaultNoiseIntensity;
	}

	/**
	 * Return a measurement disturbed by a random noise.
	 * The intensity of the noise factor can be modified at
	 * initialization.
	 * @return GeoMagneticElements (where field vector is expressed in nT)
	 * @see #retrievePerfectMeasurement()
	 */
	public GeoMagneticElements retrieveNoisyMeasurement() {
		/* Perfect Measure. */
		GeoMagneticElements perfectMeasure = this.retrievePerfectMeasurement();

		/* Normally distributed random noise contribution. */
		Vector3D noise = new Vector3D ( new double[] { 
				2 * (FastMath.random() - 0.5) * this.noiseIntensity,
				2 * (FastMath.random() - 0.5) * this.noiseIntensity,
				2 * (FastMath.random() - 0.5) * this.noiseIntensity
		});

		/* Disturbing the perfect measurement. */
		Vector3D noisyFieldVector = 
				perfectMeasure.getFieldVector().add(noise);

		/* Creating the noisy measure. */
		GeoMagneticElements noisyMeasure = new GeoMagneticElements(noisyFieldVector);	
		
		logger.debug("Noisy Geo" + noisyMeasure.toString());
		
		
		return noisyMeasure;
	}



	/**
	 * Retrieve a perfect measured data from the sensors, i.e. an
	 * ideal measurement without any noise or interference.
	 * 
	 * @return GeoMagneticElements at the location of the satellite.
	 * (where field vector is expressed in nT)
	 * @see GeoMagneticElements 
	 * @see org.orekit.bodies.OneAxisEllipsoid#transform(
	 * org.orekit.utils.PVCoordinates, 
	 * org.orekit.frames.Frame, 
	 * org.orekit.time.FieldAbsoluteDate
	 * )
	 * 
	 */
	public GeoMagneticElements retrievePerfectMeasurement() {

		SpacecraftState satState = this.assembly.getStates().getCurrentState() ;

		Vector3D positionOnEarth = 
				satState.getOrbit().getPVCoordinates().getPosition();

		GeodeticPoint geodeticPosition = null;

		try {
			/* The transformation from cartesian to geodetic coordinates is actually
			 * not straight as it needs to solve some 2-unknowns non-linear equations.
			 * So it needs a processing algorithm like the one presented by OreKit
			 * in the following method.
			 */
			geodeticPosition = earth.getEllipsoid().transform(
					positionOnEarth, 
					satState.getOrbit().getFrame(), 
					satState.getDate()
					);
		} catch (OrekitException e) {
			e.printStackTrace();
		}

		/* Calculate the magnetic field at the projected geodetic point.
		 * Note that the algorithm brings some approximation, for instance
		 * the altitude of the satellite is slightly shifted from the true 
		 * one.
		 */
		GeoMagneticElements trueMagField = this.geomagField.getField().calculateField(
				FastMath.toDegrees(geodeticPosition.getLatitude()),	/* decimal deg */
				FastMath.toDegrees(geodeticPosition.getLongitude()),	/* decimal deg */
				(satState.getA() - this.earth.getRadius()) / 1e3		/* km */
				);
		
		logger.debug("Magnetometer Measurement: \n" +
				"Latitude: " + FastMath.toDegrees(geodeticPosition.getLatitude()) + " °\n" +
				"Longitud: " + FastMath.toDegrees(geodeticPosition.getLongitude()) + " °\n" +
				"Altitude: " + (satState.getA() - this.earth.getRadius()) / 1e3 + " km\n" +
				"True Geo" + trueMagField.toString()
				);

		return trueMagField;
	}

	/**
	 * @return The noise intensity.
	 */
	public double getNoiseIntensity() {
		return noiseIntensity;
	}

}