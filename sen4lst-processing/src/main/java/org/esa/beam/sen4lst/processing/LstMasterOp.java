package org.esa.beam.sen4lst.processing;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.*;
import javax.media.jai.operator.AddDescriptor;
import javax.media.jai.operator.DivideDescriptor;
import javax.media.jai.operator.SubtractDescriptor;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;

/**
 * Operator for computation of LST from 'MODTRAN' and 'Geolocated' simulation data
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Sen4LST.Lst", version = "1.0",
                  authors = "Olaf Danne, Ralf Quast",
                  copyright = "(c) 2012 Brockmann Consult",
                  description = "Sen4LST master operator for LST retrievals.")
public class LstMasterOp extends Operator {

    @Parameter(defaultValue = "", description = "Sen4LST data directory") // e.g., /data/sen4lst/input
    private String lstDataDir;

    @Parameter(defaultValue = "false", description = "Set to true if old simulation data is used")
    private boolean oldSimulationData;

    @Parameter(defaultValue = "", description = "Timestamp in Modtran simulation data filename, e.g. '090620_0943Z'")
    private String timestampModtran;

    @Parameter(defaultValue = "1",
               interval = "[1,5]",
               description = "ID of file extension (_z1,.., _z5) to be used for Geolocated LST retrieval")
    private int geolocatedFileId;

    @Parameter(defaultValue = "1",
               interval = "[1,66]",
               description = "ID of standard atmosphere to be used for Modtran LST retrieval")
    private int atmosphereId;


    @Override
    public void initialize() throws OperatorException {
        if (oldSimulationData) {
            retrieveLstGeolocated();
        } else {
            retrieveLstModtran();
        }
    }

    private void retrieveLstGeolocated() {
        Product olci300mProduct;
        Product slstrNadir500mProduct;
        Product slstrNadir1kmProduct;
        Product slstrOblique500mProduct;
        Product slstrOblique1kmProduct;

        try {
            olci300mProduct = getGeolocatedInputProduct("OLCI_300m");
            slstrNadir500mProduct = getGeolocatedInputProduct("SLSTRn_500m");
            slstrNadir1kmProduct = getGeolocatedInputProduct("SLSTRn_1km");
            slstrOblique500mProduct = getGeolocatedInputProduct("SLSTRo_500m");
            slstrOblique1kmProduct = getGeolocatedInputProduct("SLSTRo_1km");

            if (olci300mProduct != null && slstrNadir500mProduct != null && slstrNadir1kmProduct != null &&
                    slstrOblique500mProduct != null && slstrOblique1kmProduct != null) {

                // scale all products to 2x10
                final Product olci300mScaledProduct = getScaledProduct(olci300mProduct);
                final Product slstrNadir500mScaledProduct = getScaledProduct(slstrNadir500mProduct);
                final Product slstrOblique500mScaledProduct = getScaledProduct(slstrOblique500mProduct);

                // get minimum NDVIs:
                final Band olciB8Band = olci300mScaledProduct.getBand("Band_8");
                final Band olciB17Band = olci300mScaledProduct.getBand("Band_17");
                final double[] olciNdviMinMax = getNdviMinMax(olciB8Band, olciB17Band);

                final Band slstrNadir500mB2Band = slstrNadir500mScaledProduct.getBand("Band_2");
                final Band slstrNadir500mB3Band = slstrNadir500mScaledProduct.getBand("Band_3");
                final double[] slstrNadir500mNdviMinMax = getNdviMinMax(slstrNadir500mB2Band, slstrNadir500mB3Band);

                LstGeolocatedOp lstOp = new LstGeolocatedOp();
                lstOp.setSourceProduct("olci300mProduct", olci300mScaledProduct);
                lstOp.setSourceProduct("slstrNadir500mProduct", slstrNadir500mScaledProduct);
                lstOp.setSourceProduct("slstrNadir1kmProduct", slstrNadir1kmProduct);
                lstOp.setSourceProduct("slstrOblique500mProduct", slstrOblique500mScaledProduct);
                lstOp.setSourceProduct("slstrOblique1kmProduct", slstrOblique1kmProduct);
                lstOp.setParameter("olciNdviMinMax", olciNdviMinMax);
                lstOp.setParameter("slstrNadir500mNdviMinMax", slstrNadir500mNdviMinMax);

                setTargetProduct(lstOp.getTargetProduct());
            } else {
                throw new OperatorException("Cannot open Geolocation simulation input products (some are NULL).");
            }

        } catch (IOException e) {
            throw new OperatorException("Cannot open Geolocation simulation input products: " + e.getMessage());
        }

    }

    private void retrieveLstModtran() {
        Product inputProduct;
        try {
            inputProduct = getModtranInputProduct();
            if (inputProduct != null) {
                LstModtranOp lstOp = new LstModtranOp();
                lstOp.setSourceProduct("source", inputProduct);
                final double waterVapourContent = StandardAtmosphere.getInstance().getAtmosphereWaterVapour(atmosphereId - 1);  // zero based
                lstOp.setParameter("waterVapourContent", waterVapourContent);
                setTargetProduct(lstOp.getTargetProduct());
                ProductUtils.copyGeoCoding(inputProduct, getTargetProduct());
            } else {
                throw new OperatorException("Cannot open Modtran simulation input product (NULL).");
            }
        } catch (IOException e) {
            throw new OperatorException("Cannot open Modtran simulation input product: " + e.getMessage());
        }
    }

    private Product getGeolocatedInputProduct(String productIdentifier) throws IOException {
        // OLCI_300m_z1, SLSTRn_500m_z1, SLSTRn_1km_z1, SLSTRo_500m_z1, SLSTRo_1km_z1

        final String geolocatedFileSuffix = "_z" + String.format("%01d", atmosphereId) + ".hdr";
        final String[] geolocatedFileNames = (new File(lstDataDir)).list();

        for (String geolocatedFileName : geolocatedFileNames) {
            final String matchingFilename = productIdentifier.toLowerCase() + geolocatedFileSuffix;
            if (geolocatedFileName.toLowerCase().equals(matchingFilename)) {
                String geolocatedProductFileName = lstDataDir + File.separator + geolocatedFileName;
                return ProductIO.readProduct(geolocatedProductFileName);
            }
        }
        throw new OperatorException("No Geolocated simulation input product found in '" + lstDataDir + "'.");
    }

    private Product getModtranInputProduct() throws IOException {
        // SEN4LST_TOA_090620_0943Z_ATM01.HDR

        final String modtranFileSuffix = "_ATM" + String.format("%02d", atmosphereId) + ".HDR";
        final String[] modtranFileNames = (new File(lstDataDir)).list();

        for (String modtranFileName : modtranFileNames) {
            final String matchingFilename = LstConstants.MODTRAN_FILENAME_PREFIX + timestampModtran + modtranFileSuffix;
            if (modtranFileName.toUpperCase().equals(matchingFilename)) {
                String modtranProductFileName = lstDataDir + File.separator + modtranFileName.toUpperCase();
                return ProductIO.readProduct(modtranProductFileName);
            }
        }
        throw new OperatorException("No Modtran simulation input product found in '" + lstDataDir + "'.");
    }

    private static Product getScaledProduct(Product sourceProduct) {
        final float xScale = (float) LstGeolocatedOp.TARGET_WIDTH / sourceProduct.getSceneRasterWidth();
        final float yScale = (float) LstGeolocatedOp.TARGET_HEIGHT / sourceProduct.getSceneRasterHeight();

        final Product scaledProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                            LstGeolocatedOp.TARGET_WIDTH, LstGeolocatedOp.TARGET_HEIGHT);

        final Band[] sourceBands = sourceProduct.getBands();
        for (Band sourceBand : sourceBands) {
            RenderedImage scaledImage = getScaledImage(sourceBand.getGeophysicalImage(), xScale, yScale);
            final Band targetBand = scaledProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
            targetBand.setSourceImage(scaledImage);
        }

        return scaledProduct;
    }

    private static RenderedImage getScaledImage(PlanarImage image, float xScale, float yScale) {

        // first shift by half of the average interval to get finally the same as IDL CONGRID function
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);                   // The source image
        pb.add(0.0f);    // The x translation
        final float yTrans = 1.0f / (2.0f * yScale);
        pb.add(yTrans);    // The y translation
        pb.add(new InterpolationNearest()); // The interpolation

        RenderedImage translImage = JAI.create("translate", pb, null);

        // now do the scale operation
        pb = new ParameterBlock();
        pb.addSource(translImage);           // The source image
        pb.add(xScale);                      // The xScale
        pb.add(yScale);                      // The yScale
        pb.add(0.0F);                        // The x translation
        pb.add(0.0F);                        // The y translation
        pb.add(new InterpolationNearest());  // The interpolation

        return JAI.create("scale", pb, null);
    }

    private static double[] getImageMinMaxValues(PlanarImage image, int inclusionThreshold) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);   // The source image
//        pb.add(null);       // null ROI means whole image
        // this hopefully means that pixel values < inclusionThreshold are not considered: // todo: check and test this!
        pb.add(new ROI(image, inclusionThreshold));
        pb.add(1);          // check every pixel horizontally
        pb.add(1);          // check every pixel vertically

        // Perform the mean operation on the source image.
        RenderedImage minMaxImage = JAI.create("extrema", pb, null);

        // Retrieve and report the mean pixel value.
        final double[][] extrema = (double[][]) minMaxImage.getProperty("extrema");

        return new double[]{extrema[0][0], extrema[1][0]};   // max is extrema[1], min is extrema[0]
    }

    private static double[] getNdviMinMax(Band b1, Band b2) {
        // retrieve minimum of ndvi computed as (b2 - b1)(b2 + b1)
        RenderedImage b1Image = b1.getSourceImage();
        RenderedImage b2Image = b2.getSourceImage();
        RenderedOp diffImage = SubtractDescriptor.create(b2Image, b1Image, null);
        RenderedOp sumImage = AddDescriptor.create(b2Image, b1Image, null);
        RenderedOp ndviImage = DivideDescriptor.create(diffImage, sumImage, null);

        return getImageMinMaxValues(ndviImage, (int) LstConstants.NDVI_MIN);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LstMasterOp.class);
        }
    }
}
