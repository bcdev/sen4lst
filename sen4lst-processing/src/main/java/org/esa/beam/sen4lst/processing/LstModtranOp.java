package org.esa.beam.sen4lst.processing;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;

/**
 * Operator for computation of LST from MODTRAN simulation data
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Sen4LST.Modtran", version = "1.0",
                  authors = "Olaf Danne, Ralf Quast",
                  copyright = "(c) 2012 Brockmann Consult",
                  internal = true,
                  description = "Computes LST from MODTRAN simulation data.")
public class LstModtranOp extends PixelOperator {

    @SourceProduct(alias = "source", description = "ENVI source product")
    private Product sourceProduct;

    @Parameter(description = "Water vapour content value from a distinct standard atmosphere, taken from auxiliary data")
    private double waterVapourContent;

    private static final int SRC_NADIR_BT_8 = 9;
    private static final int SRC_NADIR_BT_9 = 10;
    private static final int SRC_OBLIQUE_RADIANCE_8 = 11;
    private static final int SRC_OBLIQUE_RADIANCE_9 = 12;
    private static final int SRC_OBLIQUE_BT_8 = 13;
    private static final int SRC_OBLIQUE_BT_9 = 14;
    private static final int SRC_LST_INSITU = 15;
    private static final int SRC_NADIR_EMISSIVITY_8 = 16;
    private static final int SRC_NADIR_EMISSIVITY_9 = 17;
    private static final int SRC_OBLIQUE_EMISSIVITY_8 = 18;
    private static final int SRC_OBLIQUE_EMISSIVITY_9 = 19;

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        final double bt8N = sourceSamples[SRC_NADIR_BT_8].getDouble();
        final double bt9N = sourceSamples[SRC_NADIR_BT_9].getDouble();
        final double bt8O = sourceSamples[SRC_OBLIQUE_BT_8].getDouble();
        final double bt9O = sourceSamples[SRC_OBLIQUE_BT_9].getDouble();  // not needed? // todo: clarify
        final double lstInsitu = sourceSamples[SRC_LST_INSITU].getDouble();
        final double emiss8N = sourceSamples[SRC_NADIR_EMISSIVITY_8].getDouble();
        final double emiss9N = sourceSamples[SRC_NADIR_EMISSIVITY_9].getDouble();
        final double emiss8O = sourceSamples[SRC_OBLIQUE_EMISSIVITY_8].getDouble();
        final double emiss9O = sourceSamples[SRC_OBLIQUE_EMISSIVITY_9].getDouble();

        final double btNadirDiff = bt8N - bt9N;
        final double bt8NadirObliqueDiff = bt8N - bt8O;
        final double emissNadir = 0.5 * (emiss8N + emiss9N);
        final double emissNadirDiff = emiss8N - emiss9N;
        final double emissOblique = 0.5 * (emiss8O + emiss9O);
        final double emissObliqueDiff = emiss8O - emiss9O;

        final double lstSw = bt8N +
                LstConstants.LST_SW_COEFFS[0] +
                LstConstants.LST_SW_COEFFS[1]*btNadirDiff +
                LstConstants.LST_SW_COEFFS[2]*btNadirDiff*btNadirDiff +
                (LstConstants.LST_SW_COEFFS[3] + LstConstants.LST_SW_COEFFS[4]*waterVapourContent)*(1.0-emissNadir) +
                (LstConstants.LST_SW_COEFFS[5] + LstConstants.LST_SW_COEFFS[6]*waterVapourContent)*emissNadirDiff;

        final double lstDa = bt8N +
                LstConstants.LST_DA_COEFFS[0] +
                LstConstants.LST_DA_COEFFS[1]*bt8NadirObliqueDiff +
                LstConstants.LST_DA_COEFFS[2]*bt8NadirObliqueDiff*bt8NadirObliqueDiff +
                (LstConstants.LST_DA_COEFFS[3] + LstConstants.LST_DA_COEFFS[4]*waterVapourContent)*(1.0-emissOblique) +
                (LstConstants.LST_DA_COEFFS[5] + LstConstants.LST_DA_COEFFS[6]*waterVapourContent)*emissObliqueDiff;

        final double lstSwda = bt8N +
                LstConstants.LST_SWDA_COEFFS[0] +
                LstConstants.LST_SWDA_COEFFS[1]*btNadirDiff +
                LstConstants.LST_SWDA_COEFFS[2]*btNadirDiff*btNadirDiff +
                LstConstants.LST_SWDA_COEFFS[3]*bt8NadirObliqueDiff +
                LstConstants.LST_SWDA_COEFFS[4]*bt8NadirObliqueDiff*bt8NadirObliqueDiff +
                (LstConstants.LST_SWDA_COEFFS[5] + LstConstants.LST_SWDA_COEFFS[6]*waterVapourContent)*(1.0-emissNadir) +
                (LstConstants.LST_SWDA_COEFFS[7] + LstConstants.LST_SWDA_COEFFS[8]*waterVapourContent)*emissNadirDiff;

        int targetIndex = 0;

        targetSamples[targetIndex++].set(lstSw);
        targetSamples[targetIndex++].set(lstDa);
        targetSamples[targetIndex++].set(lstSwda);
        targetSamples[targetIndex].set(lstInsitu);
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) throws OperatorException {
        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        for (String lstBandName : LstConstants.LST_BAND_NAMES) {
            Band band = targetProduct.addBand(lstBandName, ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        // Nadir radiances:
        for (int i = 0; i < LstConstants.NADIR_RADIANCE_BAND_NAMES.length; i++) {
            sampleConfigurer.defineSample(i, LstConstants.NADIR_RADIANCE_BAND_NAMES[i], sourceProduct);
        }

        sampleConfigurer.defineSample(SRC_NADIR_BT_8, LstConstants.NADIR_BT_8_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_NADIR_BT_9, LstConstants.NADIR_BT_9_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_OBLIQUE_RADIANCE_8, LstConstants.OBLIQUE_RADIANCE_8_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_OBLIQUE_RADIANCE_9, LstConstants.OBLIQUE_RADIANCE_9_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_OBLIQUE_BT_8, LstConstants.OBLIQUE_BT_8_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_OBLIQUE_BT_9, LstConstants.OBLIQUE_BT_9_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_LST_INSITU, LstConstants.LST_INSITU_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_NADIR_EMISSIVITY_8, LstConstants.NADIR_EMISSIVITY_8_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_NADIR_EMISSIVITY_9, LstConstants.NADIR_EMISSIVITY_9_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_OBLIQUE_EMISSIVITY_8, LstConstants.OBLIQUE_EMISSIVITY_8_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_OBLIQUE_EMISSIVITY_9, LstConstants.OBLIQUE_EMISSIVITY_9_BAND_NAME, sourceProduct);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        // LST bands:
        for (int i = 0; i < LstConstants.LST_BAND_NAMES.length; i++) {
            sampleConfigurer.defineSample(i, LstConstants.LST_BAND_NAMES[i]);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LstModtranOp.class);
        }
    }
}
