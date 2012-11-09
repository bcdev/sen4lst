package org.esa.beam.sen4lst.processing;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.sen4lst.synergy.MerisAatsrConstants;

/**
 * Operator for computation of LST from colocated MERIS/AATSR SDR data
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Sen4LST.MerisAatsr", version = "1.0",
                  authors = "Olaf Danne, Ralf Quast",
                  copyright = "(c) 2012 Brockmann Consult",
                  description = "Computes LST from colocated MERIS/AATSR SDR data.")
public class LstMerisAatsrOp extends PixelOperator {

    @SourceProduct(alias = "merisAatsrProduct", description = "MERIS/AATSR SDR product")
    private Product merisAatsrProduct;

    @Parameter(description = "MERIS minimum NDVI value of whole image")
    private double[] merisNdviMinMax;

    @Parameter(description = "AATSR Nadir minimum NDVI value of whole image")
    private double[] aatsrNadirNdviMinMax;

    private static final double WATER_VAPOUR_CONTENT = 2.0;

    //     - MERIS SDR b7, b10 (665nm, 753nm, as equivalent to OLCI b8, b17 (665nm, 764nm))
    //     - AATSR nadir SDR b1, b2 (550nm, 665nm, as equivalent to SLSTR nadir b1, b2)
    //     - AATSR oblique SDR b1, b2 (550nm, 665nm, as equivalent to SLSTR oblique b1, b2)
    //     - AATSR nadir BT2, BT3 (10850nm, 12000nm, as equivalent to SLSTR nadir BT3, BT4)

    private static final int SRC_MERIS_7 = 0;
    private static final int SRC_MERIS_10 = 1;

    private static final int SRC_AATSR_NADIR_SDR_1 = 2;
    private static final int SRC_AATSR_NADIR_SDR_2 = 3;

    private static final int SRC_AATSR_FWARD_SDR_1 = 4;
    private static final int SRC_AATSR_FWARD_SDR_2 = 5;

    private static final int SRC_AATSR_NADIR_BT_2 = 6;
    private static final int SRC_AATSR_NADIR_BT_3 = 7;

    private static final int SRC_AATSR_FWARD_BT_2 = 8;
    private static final int SRC_AATSR_FWARD_BT_3 = 9;

    private static final int SRC_SYNERGY_CLOUD_FLAGS = 10;
    private static final int SRC_MERIS_L1_FLAGS = 11;
    private static final int SRC_AATSR_NADIR_CONFID_FLAGS = 12;
    private static final int SRC_AATSR_FWARD_CONFID_FLAGS = 13;

    private int landOceanFlagBit;
    private int synergyCloudFlagBit;

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) throws OperatorException {
        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        for (String lstBandName : LstConstants.LST_BAND_NAMES) {
            final String insituBandname = LstConstants.LST_BAND_NAMES[3];
            if (!lstBandName.equalsIgnoreCase(insituBandname)) {
                Band band = targetProduct.addBand(lstBandName, ProductData.TYPE_FLOAT32);
                band.setNoDataValue(Float.NaN);
                band.setNoDataValueUsed(true);
            }
        }
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(SRC_MERIS_7, MerisAatsrConstants.MERIS_SDR_620_BANDNAME, merisAatsrProduct);
        sampleConfigurer.defineSample(SRC_MERIS_10, MerisAatsrConstants.MERIS_SDR_753_BANDNAME, merisAatsrProduct);

        sampleConfigurer.defineSample(SRC_AATSR_NADIR_SDR_1, MerisAatsrConstants.AATSR_NADIR_SDR_555_BANDNAME, merisAatsrProduct);
        sampleConfigurer.defineSample(SRC_AATSR_NADIR_SDR_2, MerisAatsrConstants.AATSR_NADIR_SDR_659_BANDNAME, merisAatsrProduct);

        sampleConfigurer.defineSample(SRC_AATSR_FWARD_SDR_1, MerisAatsrConstants.AATSR_FWARD_SDR_555_BANDNAME, merisAatsrProduct);
        sampleConfigurer.defineSample(SRC_AATSR_FWARD_SDR_2, MerisAatsrConstants.AATSR_FWARD_SDR_659_BANDNAME, merisAatsrProduct);

        sampleConfigurer.defineSample(SRC_AATSR_NADIR_BT_2, MerisAatsrConstants.AATSR_NADIR_BT_1100_BANDNAME, merisAatsrProduct);
        sampleConfigurer.defineSample(SRC_AATSR_NADIR_BT_3, MerisAatsrConstants.AATSR_NADIR_BT_1200_BANDNAME, merisAatsrProduct);

        sampleConfigurer.defineSample(SRC_AATSR_FWARD_BT_2, MerisAatsrConstants.AATSR_FWARD_BT_1100_BANDNAME, merisAatsrProduct);
        sampleConfigurer.defineSample(SRC_AATSR_FWARD_BT_3, MerisAatsrConstants.AATSR_FWARD_BT_1200_BANDNAME, merisAatsrProduct);

        sampleConfigurer.defineSample(SRC_SYNERGY_CLOUD_FLAGS, MerisAatsrConstants.SYNERGY_CLOUD_FLAGS_BANDNAME, merisAatsrProduct);
        sampleConfigurer.defineSample(SRC_MERIS_L1_FLAGS, MerisAatsrConstants.MERIS_L1_FLAGS_BANDNAME, merisAatsrProduct);
        sampleConfigurer.defineSample(SRC_AATSR_NADIR_CONFID_FLAGS, MerisAatsrConstants.AATSR_NADIR_CONFID_FLAGS_BANDNAME, merisAatsrProduct);
        sampleConfigurer.defineSample(SRC_AATSR_FWARD_CONFID_FLAGS, MerisAatsrConstants.AATSR_FWARD_CONFID_FLAGS_BANDNAME, merisAatsrProduct);

        final FlagCoding merisL1FlagCoding = merisAatsrProduct.getFlagCodingGroup().get(MerisAatsrConstants.MERIS_L1_FLAGS_BANDNAME);
        final int landOcealFlagVal = merisL1FlagCoding.getAttribute("LAND_OCEAN").getData().getElemInt();
        landOceanFlagBit = (int) (Math.log(landOcealFlagVal) / Math.log(2));

        final FlagCoding synergyCloudFlagCoding = merisAatsrProduct.getFlagCodingGroup().get(MerisAatsrConstants.SYNERGY_CLOUD_FLAGS_BANDNAME);
        final int synergyCloudFlagVal = synergyCloudFlagCoding.getAttribute("CLOUD").getData().getElemInt();
        synergyCloudFlagBit = (int) (Math.log(synergyCloudFlagVal) / Math.log(2));
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        for (int i = 0; i < LstConstants.LST_BAND_NAMES.length - 1; i++) {
            sampleConfigurer.defineSample(i, LstConstants.LST_BAND_NAMES[i]);
        }
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final int merisL1Flags = sourceSamples[SRC_MERIS_L1_FLAGS].getInt();   // 16
        final boolean isLand = (merisL1Flags & (1 << landOceanFlagBit)) != 0;

        final int synergyCloudFlags = sourceSamples[SRC_SYNERGY_CLOUD_FLAGS].getInt();   // 16
        final boolean isCloud = (synergyCloudFlags & (1 << synergyCloudFlagBit)) != 0;

        if (isAllInputsValid(sourceSamples) && !isCloud) {
            final double merisb7 = sourceSamples[SRC_MERIS_7].getDouble();
            final double merisb10 = sourceSamples[SRC_MERIS_10].getDouble();



            final double aatsrNadirBt2 = sourceSamples[SRC_AATSR_NADIR_BT_2].getDouble();
            final double aatsrNadirBt3 = sourceSamples[SRC_AATSR_NADIR_BT_3].getDouble();

            final double aatsrFwardBt2 = sourceSamples[SRC_AATSR_FWARD_BT_2].getDouble();
            final double aatsrFwardBt3 = sourceSamples[SRC_AATSR_FWARD_BT_3].getDouble();

            final double bt2N = aatsrNadirBt2;
            final double bt3N = aatsrNadirBt3;
            final double bt2O = aatsrFwardBt2;
            final double bt3O = aatsrFwardBt3;   // not needed? ok!

            // use MERIS NDVI:
//            final double ndviMeris = (merisb10 - merisb7) / (merisb10 + merisb7);
//            final double merisNdviMin = merisNdviMinMax[0];
//            final double merisNdviMax = merisNdviMinMax[1];
//            final double fvc = (ndviMeris - merisNdviMin) / (merisNdviMax - merisNdviMin);

            // use AATSR NDVI:
            final double aatsrNadirSdrB1 = sourceSamples[SRC_AATSR_NADIR_SDR_1].getDouble();
            final double aatsrNadirSdrB2 = sourceSamples[SRC_AATSR_NADIR_SDR_2].getDouble();
            final double ndviAatsrNadir = (aatsrNadirSdrB2 - aatsrNadirSdrB1) / (aatsrNadirSdrB2 + aatsrNadirSdrB1);
            final double aatsrNadirNdviMin = aatsrNadirNdviMinMax[0];
            final double aatsrNadirNdviMax = aatsrNadirNdviMinMax[1];
            final double fvc = (ndviAatsrNadir - aatsrNadirNdviMin) / (aatsrNadirNdviMax - aatsrNadirNdviMin);

            final double eB1 = 0.970 * (1 - fvc) + (0.982 + 0.005) * fvc;
            final double eB2 = 0.977 * (1 - fvc) + (0.984 + 0.005) * fvc;
            final double em = 0.5 * (eB1 + eB2);
            final double de = eB1 - eB2;

            final double btNadirDiff = bt2N - bt3N;
            final double bt1NadirFwardDiff = bt2N - bt2O;

            final double lstSw = bt2N +
                    LstConstants.LST_SW_COEFFS[0] +
                    LstConstants.LST_SW_COEFFS[1] * btNadirDiff +
                    LstConstants.LST_SW_COEFFS[2] * btNadirDiff * btNadirDiff +
                    (LstConstants.LST_SW_COEFFS[3] + LstConstants.LST_SW_COEFFS[4] * WATER_VAPOUR_CONTENT) * (1.0 - em) +
                    (LstConstants.LST_SW_COEFFS[5] + LstConstants.LST_SW_COEFFS[6] * WATER_VAPOUR_CONTENT) * de;

            final double lstDa = bt2N +
                    LstConstants.LST_DA_COEFFS[0] +
                    LstConstants.LST_DA_COEFFS[1] * bt1NadirFwardDiff +
                    LstConstants.LST_DA_COEFFS[2] * bt1NadirFwardDiff * bt1NadirFwardDiff +
                    (LstConstants.LST_DA_COEFFS[3] + LstConstants.LST_DA_COEFFS[4] * WATER_VAPOUR_CONTENT) * (1.0 - em) +
                    (LstConstants.LST_DA_COEFFS[5] + LstConstants.LST_DA_COEFFS[6] * WATER_VAPOUR_CONTENT) * de;

            final double lstSwda = bt2N +
                    LstConstants.LST_SWDA_COEFFS[0] +
                    LstConstants.LST_SWDA_COEFFS[1] * btNadirDiff +
                    LstConstants.LST_SWDA_COEFFS[2] * btNadirDiff * btNadirDiff +
                    LstConstants.LST_SWDA_COEFFS[3] * bt1NadirFwardDiff +
                    LstConstants.LST_SWDA_COEFFS[4] * bt1NadirFwardDiff * bt1NadirFwardDiff +
                    (LstConstants.LST_SWDA_COEFFS[5] + LstConstants.LST_SWDA_COEFFS[6] * WATER_VAPOUR_CONTENT) * (1.0 - em) +
                    (LstConstants.LST_SWDA_COEFFS[7] + LstConstants.LST_SWDA_COEFFS[8] * WATER_VAPOUR_CONTENT) * de;

            int targetIndex = 0;

            targetSamples[targetIndex++].set(lstSw);
            targetSamples[targetIndex++].set(lstDa);
            targetSamples[targetIndex].set(lstSwda);
        } else {
            targetSamples[0].set(Double.NaN);
            targetSamples[1].set(Double.NaN);
            targetSamples[2].set(Double.NaN);
        }
    }

    private boolean isAllInputsValid(Sample[] sourceSamples) {
        for (int i = 0; i <= 5; i++) {
            if (sourceSamples[i].getDouble() < 0 || sourceSamples[i].getDouble() > 1) {
                return false;
            }
        }
        for (int i = 6; i <= 9; i++) {
            if (sourceSamples[i].getDouble() < 0) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LstMerisAatsrOp.class);
        }
    }
}
