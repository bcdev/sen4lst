package org.esa.beam.sen4lst.processing;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.util.ProductUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Operator for computation of LST from MODTRAN simulation data
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
        // todo: implement
    }

    private void retrieveLstModtran() {
        Product inputProduct = null;
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

    @SuppressWarnings({"UnusedDeclaration"})
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LstMasterOp.class);
        }
    }
}
