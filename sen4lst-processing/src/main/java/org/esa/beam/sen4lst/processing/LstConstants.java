package org.esa.beam.sen4lst.processing;

/**
 * Constants for LST retrieval.
 *
 * @author olafd
 */
public class LstConstants {

    public static final String[] NADIR_RADIANCE_BAND_NAMES =
            new String[]{"L1n", "L2n", "L3n", "L4n", "L5n", "L6n", "L7n", "L8n", "L9n"};

    public static final String NADIR_BT_8_BAND_NAME = "T8n";
    public static final String NADIR_BT_9_BAND_NAME = "T9n";
    public static final String OBLIQUE_RADIANCE_8_BAND_NAME = "L8o";
    public static final String OBLIQUE_RADIANCE_9_BAND_NAME = "L9o";
    public static final String OBLIQUE_BT_8_BAND_NAME = "T8o";
    public static final String OBLIQUE_BT_9_BAND_NAME = "T9o";
    public static final String LST_INPUT_BAND_NAME = "LST";
    public static final String NADIR_EMISSIVITY_8_BAND_NAME = "e8n";
    public static final String OBLIQUE_EMISSIVITY_8_BAND_NAME = "e8o";
    public static final String NADIR_EMISSIVITY_9_BAND_NAME = "e9n";
    public static final String OBLIQUE_EMISSIVITY_9_BAND_NAME = "e9o";

    public static final String[] LST_BAND_NAMES = new String[]{"LST_SW", "LST_DA", "LST_SWDA", "LST_INSITU"};


}
