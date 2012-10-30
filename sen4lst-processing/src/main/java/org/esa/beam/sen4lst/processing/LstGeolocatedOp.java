package org.esa.beam.sen4lst.processing;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;

/**
 * Operator for computation of LST from geolocated OLCI/SLSTR simulation data
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Sen4LST.LstGeolocated", version = "1.0",
                  authors = "Olaf Danne, Ralf Quast",
                  copyright = "(c) 2012 Brockmann Consult",
                  description = "Computes LST from geolocated OLCI/SLSTR simulation data.")
public class LstGeolocatedOp extends Operator {

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

    @Override
    public void initialize() throws OperatorException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LstGeolocatedOp.class);
        }
    }
}
