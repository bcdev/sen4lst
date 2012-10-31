package org.esa.beam.sen4lst.processing;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.ParseException;
import java.util.HashMap;

import static org.junit.Assert.*;

public class LstMasterOpTest {

    private Product target;

    @BeforeClass
    public static void beforeClass() throws ParseException {
    }

    @AfterClass
    public static void afterClass() throws ParseException {
    }

    @After
    public void after() {
        System.gc();
    }

    @Test
    public void testReadAtmosphereAuxdata() throws OperatorException {

        assertNotNull(StandardAtmosphere.getInstance());

        int atmosphereId = 1;
        assertEquals(2, StandardAtmosphere.getInstance().getAtmosphereId(atmosphereId));
        assertEquals("TRO", StandardAtmosphere.getInstance().getAtmosphereModel(atmosphereId));
        assertEquals(2.469, StandardAtmosphere.getInstance().getAtmosphereWaterVapour(atmosphereId), 1.E-3);

        atmosphereId = 31;
        assertEquals(32, StandardAtmosphere.getInstance().getAtmosphereId(atmosphereId));
        assertEquals("MLW", StandardAtmosphere.getInstance().getAtmosphereModel(atmosphereId));
        assertEquals(1.192, StandardAtmosphere.getInstance().getAtmosphereWaterVapour(atmosphereId), 1.E-3);

        atmosphereId = 65;
        assertEquals(66, StandardAtmosphere.getInstance().getAtmosphereId(atmosphereId));
        assertEquals("USS", StandardAtmosphere.getInstance().getAtmosphereModel(atmosphereId));
        assertEquals(2.125, StandardAtmosphere.getInstance().getAtmosphereWaterVapour(atmosphereId), 1.E-3);
    }

}
