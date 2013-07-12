package org.esa.beam.sen4lst.synergy;

/**
 * Constants for MERIS/AATSR synergy retrieval
 *
 * @author olafd
 */
public class MerisAatsrConstants {

    public static final int MERIS_BAND6_WVL = 620;
    public static final int MERIS_BAND10_WVL = 753;

    public static final int AATSR_BAND1_WVL = 555;
    public static final int AATSR_BAND2_WVL = 659;

    public static final String MERIS_SDR_620_BANDNAME = "SynergySDR_620_MERIS";
    public static final String MERIS_SDR_753_BANDNAME = "SynergySDR_753_MERIS";

    public static final String AATSR_NADIR_SDR_555_BANDNAME = "SynergySDR_nadir_555_AATSR";
    public static final String AATSR_NADIR_SDR_659_BANDNAME = "SynergySDR_nadir_659_AATSR";

    public static final String AATSR_FWARD_SDR_555_BANDNAME = "SynergySDR_fward_555_AATSR";
    public static final String AATSR_FWARD_SDR_659_BANDNAME = "SynergySDR_fward_659_AATSR";

    public static final String AATSR_NADIR_BT_1100_BANDNAME = "btemp_nadir_1100_AATSR";
    public static final String AATSR_NADIR_BT_1200_BANDNAME = "btemp_nadir_1200_AATSR";

    public static final String AATSR_FWARD_BT_1100_BANDNAME = "btemp_fward_1100_AATSR";
    public static final String AATSR_FWARD_BT_1200_BANDNAME = "btemp_fward_1200_AATSR";

    public static final String MERIS_L1_FLAGS_BANDNAME = "l1_flags_MERIS";
    public static final String AATSR_NADIR_CONFID_FLAGS_BANDNAME = "confid_flags_nadir_AATSR";
    public static final String AATSR_FWARD_CONFID_FLAGS_BANDNAME = "confid_flags_fward_AATSR";
    public static final String SYNERGY_CLOUD_FLAGS_BANDNAME = "cloud_flags_synergy";

    public static final String MERIS_L2_WATER_VAPOUR_BAND_NAME = "water_vapour_L2";
}
