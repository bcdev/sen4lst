package org.esa.beam.sen4lst.processing;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.junit.Test;

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;

public class LstMasterOpTest extends TestCase {

    private Band targetBand1;
    private Band targetBand2;
    private Product product;
    private int width = 4;
    private int height = 3;

    private Product target;

    @Override
    protected void setUp() throws Exception {
        product = new Product("p1", "t", width, height);
        targetBand1 = product.addBand("b1", ProductData.TYPE_FLOAT32);
        targetBand2 = product.addBand("b2", ProductData.TYPE_FLOAT32);

        targetBand1.setDataElems(new float[]{
                2, 3, Float.NaN, 5,
                6, Float.NaN, 8, 9,
                10, 11, 12, 13
        });

        targetBand2.setDataElems(new float[]{
                13, 12, 11, 10,
                9, Float.NaN, Float.NaN, 6,
                5, 4, 3, 2
        });
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

    @Test
    public void testGetImageMinMax() throws OperatorException {
        // find min and max of image which includes NaN values
        int inclusionThreshold = -10;
        double[] minMaxValues = LstMasterOp.getImageMinMaxValues(targetBand1.getGeophysicalImage(), inclusionThreshold);
        assertEquals(2.0, minMaxValues[0]);
        assertEquals(13.0, minMaxValues[1]);

        // find min and max of image with inclusion threshold greater than minimum of image
        inclusionThreshold = 5;
        minMaxValues = LstMasterOp.getImageMinMaxValues(targetBand1.getGeophysicalImage(), inclusionThreshold);
        assertEquals(6.0, minMaxValues[0]);
        assertEquals(13.0, minMaxValues[1]);

        // find min and max of image with inclusion threshold greater than both minimum and maximum of image
        inclusionThreshold = 17;
        minMaxValues = LstMasterOp.getImageMinMaxValues(targetBand1.getGeophysicalImage(), inclusionThreshold);
        assertEquals(0.0, minMaxValues[0]);
        assertEquals(0.0, minMaxValues[1]);
    }

    @Test
    public void testGetNdviMinMax() throws OperatorException {

        final double[] ndviMinMax = LstMasterOp.getNdviMinMax(targetBand1, targetBand2);
        double ndviMinExpected = -11. / 15.;
        assertEquals(ndviMinExpected, ndviMinMax[0], 1.E-4);
        double ndviMaxExpected = 11. / 15.;
        assertEquals(ndviMaxExpected, ndviMinMax[1], 1.E-4);
    }

    @Test
    public void testGetVerticalScaledImage() throws OperatorException {

        final Product product = new Product("p1", "t", 1, 20);
        final Band targetBand = product.addBand("b1", ProductData.TYPE_FLOAT32);

        targetBand.setDataElems(new float[]{
                2, 3, Float.NaN, 5, 6,
                7, Float.NaN, 8, 9, 19,
                10, 11, 12, 13, 53,
                15, 16, 17, 23, 33
        });

        final RenderedImage scaledImage = LstMasterOp.getVerticalScaledImage(targetBand.getGeophysicalImage(), 1.0f, 0.25f);

        assertNotNull(scaledImage);
        assertEquals(1, scaledImage.getWidth());
        assertEquals(5, scaledImage.getHeight());

        List<float[]> lst = new ArrayList<float[]>();
        for (int j = 0; j < scaledImage.getHeight(); j++) {
            for (int i = 0; i < scaledImage.getWidth(); i++) {
                lst.add((float[]) scaledImage.getData().getDataElements(i, j, null));
            }
        }
        assertEquals(5, lst.size());
        assertEquals(2.0f, lst.get(0)[0]);
        assertEquals(6.0f, lst.get(1)[0]);
        assertEquals(9.0f, lst.get(2)[0]);
        assertEquals(12.0f, lst.get(3)[0]);
        assertEquals(16.0f, lst.get(4)[0]);

    }
}
