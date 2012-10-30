package org.esa.beam.sen4lst.processing;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
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
                  description = "Computes LST from MODTRAN simulation data.")
public class LstModtranOp extends PixelOperator {

    @SourceProduct(alias = "l1b", description = "ENVI source product")
    private Product sourceProduct;

    private static final int SRC_NADIR_BT_8 = 9;
    private static final int SRC_NADIR_BT_9 = 10;
    private static final int SRC_OBLIQUE_RADIANCE_8 = 11;
    private static final int SRC_OBLIQUE_RADIANCE_9 = 12;
    private static final int SRC_OBLIQUE_BT_8 = 13;
    private static final int SRC_OBLIQUE_BT_9 = 14;
    private static final int SRC_LST_INPUT = 15;
    private static final int SRC_NADIR_EMISSIVITY_8 = 16;
    private static final int SRC_OBLIQUE_EMISSIVITY_8 = 17;
    private static final int SRC_NADIR_EMISSIVITY_9 = 18;
    private static final int SRC_OBLIQUE_EMISSIVITY_9 = 19;

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        // todo: implement as given from IDL code below

//        openu, 1, path+'simulation_TOA_SLSTR_modtran\'+name
//        mat=assoc(1, fltarr(sam,fil))
//        T8n=mat(9) & T9n=mat(10) & T8o=mat(13) & T9o=mat(14) & lst_situ=mat(15)
//        e8n=mat(16) & e9n=mat(17) & e8o=mat(18) & e9o=mat(19)
//        close, 1
//
//        emn=0.5*(e8n+e9n) & den=e8n-e9n
//        emo=0.5*(e8o+e9o) & deo=e8o-e9o
//        w=wvec(j)
//
//        c=[-0.268,1.084,0.277,45.11,-0.73,-125.00,16.70];LST SW algorithm coefficients
//        lst_sw=T8n+c(1)*(T8n-T9n)+c(2)*((T8n-T9n)^2)+c(0)+(c(3)+c(4)*w)*(1-emn)+(c(5)+c(6)*w)*den
//
//        c=[-0.441,1.790,0.221,64.26,-7.60,-30.18,3.14];LST DA algorithm coefficients
//        lst_da=T8n+c(1)*(T8n-T8o)+c(2)*((T8n-T8o)^2)+c(0)+(c(3)+c(4)*w)*(1-emo)+(c(5)+c(6)*w)*deo
//
//        c=[-0.510,-0.053,-0.180,2.13,0.377,71.4,-10.04,-5.9,1.01];LST SW-DA algorithm coefficients
//        lst_swda=T8n+c(1)*(T8n-T9n)+c(2)*((T8n-T9n)^2)+c(0)+c(3)*(T8n-T8o)+c(4)*((T8n-T8o)^2)+(c(5)+c(6)*w)*(1-emn)+(c(7)+c(8)*w)*den
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
        sampleConfigurer.defineSample(SRC_NADIR_BT_9, LstConstants.NADIR_BT_8_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_OBLIQUE_RADIANCE_8, LstConstants.OBLIQUE_RADIANCE_8_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_OBLIQUE_RADIANCE_9, LstConstants.OBLIQUE_RADIANCE_9_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_OBLIQUE_BT_8, LstConstants.OBLIQUE_BT_8_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_OBLIQUE_BT_9, LstConstants.OBLIQUE_BT_9_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_LST_INPUT, LstConstants.LST_INPUT_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_NADIR_EMISSIVITY_8, LstConstants.NADIR_EMISSIVITY_8_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_OBLIQUE_EMISSIVITY_8, LstConstants.OBLIQUE_EMISSIVITY_8_BAND_NAME, sourceProduct);
        sampleConfigurer.defineSample(SRC_NADIR_EMISSIVITY_9, LstConstants.NADIR_EMISSIVITY_9_BAND_NAME, sourceProduct);
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
