package org.esa.beam.sen4lst.processing;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;

import java.awt.image.PixelGrabber;

/**
 * Operator for computation of LST from geolocated OLCI/SLSTR simulation data
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Sen4LST.LstGeolocated", version = "1.0",
                  authors = "Olaf Danne, Ralf Quast",
                  copyright = "(c) 2012 Brockmann Consult",
                  description = "Computes LST from geolocated OLCI/SLSTR simulation data.")
public class LstGeolocatedOp extends PixelOperator {

    @SourceProduct(alias = "olci300mProduct", description = "OLCI 300m simulation product")
    private Product olci300mProduct;

    @SourceProduct(alias = "slstrNadir500mProduct", description = "SLSTR Nadir 500m simulation product")
    private Product slstrNadir500mProduct;

    @SourceProduct(alias = "slstrNadir1kmProduct", description = "SLSTR Nadir 1km simulation product")
    private Product slstrNadir1kmProduct;

    @SourceProduct(alias = "slstrOblique500mProduct", description = "SLSTR Oblique 500m simulation product")
    private Product slstrOblique500mProduct;

    @SourceProduct(alias = "slstrOblique1kmProduct", description = "SLSTR Oblique 1km simulation product")
    private Product slstrOblique1kmProduct;


    @Parameter(description = "OLCI minimum NDVI value of whole image")
    private double[] olciNdviMinMax;

    @Parameter(description = "SLSTR 500m Nadir minimum NDVI value of whole image")
    private double[] slstrNadir500mNdviMinMax;

    public static final int TARGET_WIDTH = 2;
    public static final int TARGET_HEIGHT = 10;

    private static final double WATER_VAPOUR_CONTENT = 2.0;

    private static final int SRC_OLCI_8 = 0;
    private static final int SRC_OLCI_17 = 1;

    private static final int SRC_SLSTR_NADIR_500m_2 = 2;
    private static final int SRC_SLSTR_NADIR_500m_3 = 3;

    private static final int SRC_SLSTR_NADIR_1km_1 = 4;
    private static final int SRC_SLSTR_NADIR_1km_2 = 5;

    private static final int SRC_SLSTR_OBLIQUE_1km_1 = 6;
    private static final int SRC_SLSTR_OBLIQUE_1km_2 = 7;

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
        sampleConfigurer.defineSample(SRC_OLCI_8, "Band_8", olci300mProduct);
        sampleConfigurer.defineSample(SRC_OLCI_17, "Band_17", olci300mProduct);

        sampleConfigurer.defineSample(SRC_SLSTR_NADIR_500m_2, "Band_2", slstrNadir500mProduct);
        sampleConfigurer.defineSample(SRC_SLSTR_NADIR_500m_3, "Band_3", slstrNadir500mProduct);

        sampleConfigurer.defineSample(SRC_SLSTR_NADIR_1km_1, "Band_2", slstrNadir1kmProduct);
        sampleConfigurer.defineSample(SRC_SLSTR_NADIR_1km_2, "Band_3", slstrNadir1kmProduct);

        sampleConfigurer.defineSample(SRC_SLSTR_OBLIQUE_1km_1, "Band_2", slstrOblique1kmProduct);
        sampleConfigurer.defineSample(SRC_SLSTR_OBLIQUE_1km_2, "Band_3", slstrOblique1kmProduct);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        for (int i = 0; i < LstConstants.LST_BAND_NAMES.length - 1; i++) {
            sampleConfigurer.defineSample(i, LstConstants.LST_BAND_NAMES[i]);
        }
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final double olciB8 = sourceSamples[SRC_OLCI_8].getDouble();
        final double olciB17 = sourceSamples[SRC_OLCI_17].getDouble();

        final double slstrNadir500mB2 = sourceSamples[SRC_SLSTR_NADIR_500m_2].getDouble();
        final double slstrNadir500mB3 = sourceSamples[SRC_SLSTR_NADIR_500m_3].getDouble();

        final double slstrNadir1kmB1 = sourceSamples[SRC_SLSTR_NADIR_1km_1].getDouble();
        final double slstrNadir1kmB2 = sourceSamples[SRC_SLSTR_NADIR_1km_2].getDouble();

        final double slstrOblique1kmB1 = sourceSamples[SRC_SLSTR_OBLIQUE_1km_1].getDouble();
        final double slstrOblique1kmB2 = sourceSamples[SRC_SLSTR_OBLIQUE_1km_2].getDouble();

        final double ndviOlci = (olciB17 - olciB8) / (olciB17 + olciB8);
        final double bt8N = slstrNadir1kmB1;
        final double bt9N = slstrNadir1kmB2;
        final double bt8O = slstrOblique1kmB1;
        final double bt9O = slstrOblique1kmB2;   // not needed? // todo: clarify
        final double olciNdviMin = olciNdviMinMax[0];
        final double olciNdviMax = olciNdviMinMax[1];

        final double fvcOlci = (ndviOlci - olciNdviMin) / (olciNdviMax - olciNdviMin);
        final double eOlciB11 = 0.970 * (1 - fvcOlci) + (0.982 + 0.005) * fvcOlci;
        final double eOlciB12 = 0.977 * (1 - fvcOlci) + (0.984 + 0.005) * fvcOlci;
//        final double emOlci = 0.5 * (eOlciB11 + eOlciB12);   // currently not needed
//        final double deOlci = eOlciB11 - eOlciB12;   // currently not needed

        final double ndviSlstrNadir500m = (slstrNadir500mB3 - slstrNadir500mB2) / (slstrNadir500mB3 + slstrNadir500mB2);
        final double slstrNadir500mNdviMin = slstrNadir500mNdviMinMax[0];
        final double slstrNadir500mNdviMax = slstrNadir500mNdviMinMax[1];

        final double fvcSlstrNadir500m = (ndviSlstrNadir500m - slstrNadir500mNdviMin) / (slstrNadir500mNdviMax - slstrNadir500mNdviMin);
        final double eSlstrNadir500mB11 = 0.970 * (1 - fvcSlstrNadir500m) + (0.982 + 0.005) * fvcSlstrNadir500m;
        final double eSlstrNadir500mB12 = 0.977 * (1 - fvcSlstrNadir500m) + (0.984 + 0.005) * fvcSlstrNadir500m;
        final double emSlstrNadir500m = 0.5 * (eSlstrNadir500mB11 + eSlstrNadir500mB12);
        final double deSlstrNadir500m = eSlstrNadir500mB11 - eSlstrNadir500mB12;

        final double btNadirDiff = bt8N - bt9N;
        final double bt8NadirObliqueDiff = bt8N - bt8O;

        final double lstSw = bt8N +
                LstConstants.LST_SW_COEFFS[0] +
                LstConstants.LST_SW_COEFFS[1] * btNadirDiff +
                LstConstants.LST_SW_COEFFS[2] * btNadirDiff * btNadirDiff +
                (LstConstants.LST_SW_COEFFS[3] + LstConstants.LST_SW_COEFFS[4] * WATER_VAPOUR_CONTENT) * (1.0 - emSlstrNadir500m) +
                (LstConstants.LST_SW_COEFFS[5] + LstConstants.LST_SW_COEFFS[6] * WATER_VAPOUR_CONTENT) * deSlstrNadir500m;

        final double lstDa = bt8N +
                LstConstants.LST_DA_COEFFS[0] +
                LstConstants.LST_DA_COEFFS[1]*bt8NadirObliqueDiff +
                LstConstants.LST_DA_COEFFS[2]*bt8NadirObliqueDiff*bt8NadirObliqueDiff +
                (LstConstants.LST_DA_COEFFS[3] + LstConstants.LST_DA_COEFFS[4]*WATER_VAPOUR_CONTENT)*(1.0-emSlstrNadir500m) +
                (LstConstants.LST_DA_COEFFS[5] + LstConstants.LST_DA_COEFFS[6]*WATER_VAPOUR_CONTENT)*deSlstrNadir500m;

        final double lstSwda = bt8N +
                LstConstants.LST_SWDA_COEFFS[0] +
                LstConstants.LST_SWDA_COEFFS[1]*btNadirDiff +
                LstConstants.LST_SWDA_COEFFS[2]*btNadirDiff*btNadirDiff +
                LstConstants.LST_SWDA_COEFFS[3]*bt8NadirObliqueDiff +
                LstConstants.LST_SWDA_COEFFS[4]*bt8NadirObliqueDiff*bt8NadirObliqueDiff +
                (LstConstants.LST_SWDA_COEFFS[5] + LstConstants.LST_SWDA_COEFFS[6]*WATER_VAPOUR_CONTENT)*(1.0-emSlstrNadir500m) +
                (LstConstants.LST_SWDA_COEFFS[7] + LstConstants.LST_SWDA_COEFFS[8]*WATER_VAPOUR_CONTENT)*deSlstrNadir500m;

        int targetIndex = 0;

        targetSamples[targetIndex++].set(lstSw);
        targetSamples[targetIndex++].set(lstDa);
        targetSamples[targetIndex].set(lstSwda);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LstGeolocatedOp.class);
        }
    }
}
