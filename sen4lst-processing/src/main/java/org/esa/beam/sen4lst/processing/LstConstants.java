package org.esa.beam.sen4lst.processing;

/**
 * Constants for LST retrieval.
 *
 * @author olafd
 */
public class LstConstants {

    public static final String LST_PROCESSING_VERSION = "v1.1-SNAPSHOT";

    public static final String[] NADIR_RADIANCE_BAND_NAMES =
            new String[]{"L1n", "L2n", "L3n", "L4n", "L5n", "L6n", "L7n", "L8n", "L9n"};

    public static final String NADIR_BT_8_BAND_NAME = "T8n";
    public static final String NADIR_BT_9_BAND_NAME = "T9n";
    public static final String OBLIQUE_RADIANCE_8_BAND_NAME = "L8o";
    public static final String OBLIQUE_RADIANCE_9_BAND_NAME = "L9o";
    public static final String OBLIQUE_BT_8_BAND_NAME = "T8o";
    public static final String OBLIQUE_BT_9_BAND_NAME = "T9o";
    public static final String LST_INSITU_BAND_NAME = "LST";
    public static final String NADIR_EMISSIVITY_8_BAND_NAME = "e8n";
    public static final String OBLIQUE_EMISSIVITY_8_BAND_NAME = "e8o";
    public static final String NADIR_EMISSIVITY_9_BAND_NAME = "e9n";
    public static final String OBLIQUE_EMISSIVITY_9_BAND_NAME = "e9o";

    public static final String[] LST_BAND_NAMES = new String[]{"LST_SW", "LST_DA", "LST_SWDA", "LST_INSITU"};
    public static final String EMISSIVITY_BAND_NAME = "EMISSIVITY_mean";
    public static final String EMISSIVITY_BAND1_BAND_NAME = "EMISSIVITY_Band1";
    public static final String EMISSIVITY_BAND2_BAND_NAME = "EMISSIVITY_Band2";

    //    public static final double[] LST_SW_COEFFS = new double[] {-0.268,1.084,0.277,45.11,-0.73,-125.00,16.70};
    // NEW SW coefficients Juan-Carlos, 2013/09/10:
    //    c=[-0.218, 1.356, 0.159, 54.72, -2.58, -125.17, 16.19]
    public static final double[] LST_SW_COEFFS = new double[] {-0.218,1.356,0.159,54.72,-2.58,-125.17,16.19};

    public static final double[] LST_DA_COEFFS = new double[] {-0.441,1.790,0.221,64.26,-7.60,-30.18,3.14};
    public static final double[] LST_SWDA_COEFFS = new double[] {-0.510,-0.053,-0.180,2.13,0.377,71.4,-10.04,-5.9,1.01};

    public static final String MODTRAN_FILENAME_PREFIX = "SEN4LST_TOA_";

    public static double NDVI_MIN = -10.0;  // minimum NDVI considered for extrema search

}
