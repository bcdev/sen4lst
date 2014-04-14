package org.esa.beam.sen4lst.processing;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.sen4lst.synergy.InstrumentCombination;
import org.esa.beam.sen4lst.synergy.MerisAatsrSynergyOp;
import org.esa.beam.sen4lst.synergy.Sen4LstSynergyConstants;
import org.esa.beam.util.ProductUtils;

/**
 * Sen4LST master operator for LST retrievals
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Sen4LST.Sdr",
                  version = "1.1",
                  authors = "A. Heckel, P.R.N. North, O. Danne, R. Quast",
                  copyright = "(c) 2013 European Space Agency",
                  description = "Sen4LST master operator for SDR retrievals.")
public class SdrMasterOp extends Operator {

    @SourceProduct(alias = "Master",
                   description = "The 'Master' instrument L1b source product.")
    Product firstSourceProduct;
    @SourceProduct(alias = "Slave",
                   description = "The 'Slave' instrument L1b source product.")
    Product secondSourceProduct;

    @TargetProduct(description = "The target product (colocated on 'master' grid).")
    private Product targetProduct;

    @Parameter(defaultValue = "MERIS_AATSR", description = "Instrument combination (either MERIS/AATSR or OLCI/SLSTR)")
    private InstrumentCombination instruments;

    @Override
    public void initialize() throws OperatorException {

        if (instruments == InstrumentCombination.MERIS_AATSR) {
            MerisAatsrSynergyOp synergyOp = new MerisAatsrSynergyOp();
            synergyOp.setSourceProduct("MERIS", firstSourceProduct);
            synergyOp.setSourceProduct("AATSR", secondSourceProduct);
            final Product synergyProduct = synergyOp.getTargetProduct();
            setTargetProduct(synergyProduct);
            // copy water vapour band if available in source product (merge from L2):
            if (firstSourceProduct.getBand(Sen4LstSynergyConstants.MERIS_L2_WATER_VAPOUR_BAND_NAME) != null) {
                ProductUtils.copyBand(Sen4LstSynergyConstants.MERIS_L2_WATER_VAPOUR_BAND_NAME,
                                      firstSourceProduct, getTargetProduct(), true);
            }
        } else if (instruments == InstrumentCombination.OLCI_SLSTR) {
            // not yet implemented
            throw new IllegalArgumentException("Instrument combination " + instruments.getLabel() + " not yet implemented.");
        } else {
            throw new IllegalArgumentException("Instrument combination not supported.");
        }
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SdrMasterOp.class);
        }
    }
}
