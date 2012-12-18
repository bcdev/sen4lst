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

/**
 * Sen4LST master operator for LST retrievals
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Sen4LST.Sdr",
                  version = "1.1",
                  authors = "Olaf Danne",
                  copyright = "(c) 2009 by Brockmann Consult",
                  internal = true,
                  description = "Sen4LST master operator for SDR retrievals.")
public class SdrMasterOp extends Operator {

    @SourceProduct(alias = "Master",
                   description = "'Master' instrument source product.")
    Product firstSourceProduct;

    @SourceProduct(alias = "Slave",
                   description = "'Slave' instrument source product.")
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
