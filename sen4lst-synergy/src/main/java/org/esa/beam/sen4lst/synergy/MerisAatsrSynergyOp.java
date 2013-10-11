package org.esa.beam.sen4lst.synergy;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.synergy.operators.CreateSynergyOp;
import org.esa.beam.synergy.operators.SynergyCloudScreeningOp;
import org.esa.beam.synergy.util.SynergyConstants;
import org.esa.beam.synergy.util.SynergyUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Operator for generation of MERIS/AATSR synergy product as needed for LST retrieval
 *
 * @author olafd
 */
@OperatorMetadata(alias = "sen4lst.synergy.merisaatsr",
                  version = "1.1",
                  authors = "Olaf Danne",
                  copyright = "(c) 2009 by Brockmann Consult",
                  internal = true,
                  description = "Operator for generation of MERIS/AATSR synergy product as needed for LST retrieval.")
public class MerisAatsrSynergyOp extends Operator {

    @SourceProduct(alias = "MERIS",
                   description = "MERIS source product.")
    Product merisSourceProduct;

    @SourceProduct(alias = "AATSR",
                   description = "AATSR source product.")
    Product aatsrSourceProduct;

    @TargetProduct(description = "The target product.")
    private Product targetProduct;

    @Override
    public void initialize() throws OperatorException {

        // - from MERIS and AATSR L1b input files, generate Synergy product with all bands
        //   required as input for LST retrieval. These are (see Table 2, Swansea Synergy ATBD, July 2010):
        //     - MERIS SDR b7, b10 (665nm, 753nm, as equivalent to OLCI b8, b17 (665nm, 764nm))
        //     - AATSR nadir SDR b1, b2 (550nm, 665nm, as equivalent to SLSTR nadir b1, b2)
        //     - AATSR oblique SDR b1, b2 (550nm, 665nm, as equivalent to SLSTR oblique b1, b2)
        //     - AATSR nadir BT2, BT3 (10850nm, 12000nm, as equivalent to SLSTR nadir BT3, BT4)
        //
        //   for this, re-use MERIS/AATSR Synergy modules, but clean and strip off SDR retrieval to required bands


        SynergyUtils.validateMerisProduct(merisSourceProduct);
        if (merisSourceProduct.getProductType().equalsIgnoreCase("COLLOCATED")) {
            // L1 was collocated with L2 product to get water vapour info
            merisSourceProduct.setProductType("MER_RR__1P"); // just in case
            merisSourceProduct.setDescription("MERIS L1/L2 collocated product."); // just in case
        }
        SynergyUtils.validateAatsrProduct(aatsrSourceProduct);

        if (!SynergyUtils.validateAuxdata(false, SynergyConstants.AEROSOL_MODEL_PARAM_DEFAULT)) {
            throw new OperatorException(Sen4LstSynergyConstants.AUXDATA_ERROR_MESSAGE);
        }

        // get the colocated 'preprocessing' product...
        Product preprocessingProduct;
        Map<String, Product> preprocessingInput = new HashMap<String, Product>(2);
        preprocessingInput.put("MERIS", merisSourceProduct);
        preprocessingInput.put("AATSR", aatsrSourceProduct);
        Map<String, Object> preprocessingParams = new HashMap<String, Object>();
        preprocessingProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(CreateSynergyOp.class),
                                                 preprocessingParams, preprocessingInput);

        // get the cloud screening product..
        Product cloudScreeningProduct;
        Map<String, Product> cloudScreeningInput = new HashMap<String, Product>(1);
        cloudScreeningInput.put("source", preprocessingProduct);
        Map<String, Object> cloudScreeningParams = new HashMap<String, Object>(4);
        cloudScreeningParams.put("useForwardView", true);
        cloudScreeningParams.put("computeCOT", true);
        cloudScreeningParams.put("computeSF", false);
        cloudScreeningParams.put("computeSH", false);
        cloudScreeningProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SynergyCloudScreeningOp.class),
                                                  cloudScreeningParams, cloudScreeningInput);

        // get the SDR product...
        MerisAatsrSdrOp sdrOp = new MerisAatsrSdrOp();
        sdrOp.setSourceProduct("source", cloudScreeningProduct);
        final Product surfaceReflectanceProduct = sdrOp.getTargetProduct();

        setTargetProduct(surfaceReflectanceProduct);
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MerisAatsrSynergyOp.class);
        }
    }
}
