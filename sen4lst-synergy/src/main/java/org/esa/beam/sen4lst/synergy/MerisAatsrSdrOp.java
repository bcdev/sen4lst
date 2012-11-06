package org.esa.beam.sen4lst.synergy;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.synergy.operators.*;
import org.esa.beam.synergy.util.SynergyConstants;
import org.esa.beam.synergy.util.SynergyUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 06.11.12
 * Time: 16:29
 *
 * @author olafd
 */
@OperatorMetadata(alias = "sen4lst.synergy.merisaatsr.sdr",
                  version = "1.1",
                  authors = "Olaf Danne",
                  copyright = "(c) 2009 by Brockmann Consult",
                  internal = true,
                  description = "Operator for computation of MERIS/AATSR SDRs as needed for LST retrieval.")
public class MerisAatsrSdrOp extends Operator {

    @SourceProduct(alias = "source",
                   label = "MERIS/AATSR synergy preprocessed product with cloud flags)",
                   description = "Select a collocated MERIS AATSR product obtained from preprocessing AND cloudscreening.")
    private Product preprocessedProduct;

    private java.util.List<Integer> aerosolModels;

    private ArrayList<Band> merisBandList;
    private ArrayList<Band> aatsrBandListNad;
    private ArrayList<Band> aatsrBandListFwd;
    private ArrayList<RasterDataNode> merisGeometryBandList;
    private ArrayList<RasterDataNode> aatsrGeometryBandList;
    private float[] merisWvl;
    private float[] aatsrWvl;
    private float[] soilSurfSpec;
    private float[] vegSurfSpec;

    private float[] lutAlbedo;
    private float[] lutAot;
    private float[][][] lutSubsecMeris;
    private float[][][][] lutSubsecAatsr;
    
    private static int aveBlock = 7;

    @Override
    public void initialize() throws OperatorException {

        SynergyUtils.validateCloudScreeningProduct(preprocessedProduct);

        aerosolModels = SynergyUtils.readAerosolLandModelNumbers(false, SynergyConstants.AEROSOL_MODEL_PARAM_DEFAULT);

        merisBandList = new ArrayList<Band>();
        aatsrBandListNad = new ArrayList<Band>();
        aatsrBandListFwd = new ArrayList<Band>();
        merisGeometryBandList = new ArrayList<RasterDataNode>();
        aatsrGeometryBandList = new ArrayList<RasterDataNode>();

        // make sure aveBlock is a odd number
        if ((aveBlock % 2) == 0) {
            aveBlock += 1;
        }

        // get the land product..
        Product landProduct;
        Map<String, Product> landInput = new HashMap<String, Product>(1);
        landInput.put("source", preprocessedProduct);
        Map<String, Object> landParams = new HashMap<String, Object>(6);
        landParams.put("soilSpecName", SynergyConstants.SOIL_SPEC_PARAM_DEFAULT);
        landParams.put("vegSpecName", SynergyConstants.VEG_SPEC_PARAM_DEFAULT);
        landParams.put("aveBlock", aveBlock);
        landParams.put("useCustomLandAerosol", false);
        landParams.put("customLandAerosol", false);
        landProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(RetrieveAerosolLandOp.class), landParams, landInput);

        Product landInterpolatedProduct;
        Map<String, Product> landOceanInterpolatedInput = new HashMap<String, Product>(2);
        landOceanInterpolatedInput.put("synergy", preprocessedProduct);
        landOceanInterpolatedInput.put("source", landProduct);
        Map<String, Object> landOceanInterpolatedParams = new HashMap<String, Object>();
        landOceanInterpolatedParams.put("aveBlock", aveBlock);
        landInterpolatedProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(AotExtrapOp.class), landOceanInterpolatedParams, landOceanInterpolatedInput);

        Map<String, Product> landUpscaledInput = new HashMap<String, Product>(2);
        landUpscaledInput.put("synergy", preprocessedProduct);
        landUpscaledInput.put("aerosol", landInterpolatedProduct);
        Map<String, Object> landOceanUpscaledParams = new HashMap<String, Object>(1);
        landOceanUpscaledParams.put("scalingFactor", aveBlock);
        Product landUpscaledProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(UpscaleOp.class), landOceanUpscaledParams, landUpscaledInput);


    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws
            OperatorException {

//        pm.beginTask("aerosol retrieval", aerosolModels.size() * targetRectangle.width * targetRectangle.height + 4);
//        System.out.printf("   Aerosol Retrieval @ Tile %s\n", targetRectangle.toString());
//
//        // define bigger Rectangle for binning of source tiles
//        final int bigWidth = (int) ((2 * aveBlock + 1) * targetRectangle.getWidth());
//        final int bigHeight = (int) ((2 * aveBlock + 1) * targetRectangle.getHeight());
//        final int bigX = (int) ((2 * aveBlock + 1) * targetRectangle.getX());
//        final int bigY = (int) ((2 * aveBlock + 1) * targetRectangle.getY());
//        final Rectangle big = new Rectangle(bigX, bigY, bigWidth, bigHeight);
//
//        // read source tiles
//        final Tile[] merisTiles = getSpecTiles(merisBandList, big);
//
//        Tile[][] aatsrTiles = new Tile[2][0];
//        aatsrTiles[0] = getSpecTiles(aatsrBandListNad, big);
//        aatsrTiles[1] = getSpecTiles(aatsrBandListFwd, big);
//
//        final Tile[] geometryTiles = getGeometryTiles(merisGeometryBandList, aatsrGeometryBandList, big);
//
//        final Tile pressureTile = getSourceTile(
//                preprocessedProduct.getTiePointGrid(SynergyConstants.INPUT_PRESSURE_BAND_NAME), big);
//        final Tile ozoneTile = getSourceTile(
//                preprocessedProduct.getTiePointGrid(SynergyConstants.INPUT_OZONE_BAND_NAME), big);
//
//        final Tile isValidTile = getSourceTile(isValidBand, big);
//        final Tile isLandTile = getSourceTile(isLandBand, big);
//        final Tile isCloudyTile = getSourceTile(isCloudyBand, big);
//
//        // define target tiles
//        Tile vNdviTile = getSourceTile(preprocessedProduct.getBand(virtNdviName), big);
//
//        Tile aerosolTile = targetTiles.get(targetProduct.getBand(SynergyConstants.OUTPUT_AOT_BAND_NAME));
//        Tile aerosolModelTile = targetTiles.get(targetProduct.getBand(SynergyConstants.OUTPUT_AOTMODEL_BAND_NAME));
//        Tile aerosolErrTile = targetTiles.get(targetProduct.getBand(SynergyConstants.OUTPUT_AOTERR_BAND_NAME));
//        Tile aerosolFlagTile = targetTiles.get(targetProduct.getBand(aerosolFlagCodingName));
//
//        float[] merisToaReflec = new float[merisBandList.size()];
//        float[][] aatsrToaReflec = new float[2][aatsrBandListNad.size()];
//
//        ReflectanceBinLUT toaLut = null;
//
//        final Aardvarc aardvarc = new Aardvarc(aatsrWvl, merisWvl);
//        aardvarc.setDoAATSR(true);
//        aardvarc.setDoMERIS(true);
//        aardvarc.setSpecSoil(soilSurfSpec);
//        aardvarc.setSpecVeg(vegSurfSpec);
//
//        double[][] minErr = new double[targetRectangle.height][targetRectangle.width];
//
//        // initialize target aot tile
//        for (int iy = targetRectangle.y; iy < targetRectangle.y + targetRectangle.height; iy++) {
//            for (int ix = targetRectangle.x; ix < targetRectangle.x + targetRectangle.width; ix++) {
//                aerosolTile.setSample(ix, iy, SynergyConstants.OUTPUT_AOT_BAND_NODATAVALUE);
//                aerosolErrTile.setSample(ix, iy, SynergyConstants.OUTPUT_AOTERR_BAND_NODATAVALUE);
//                aerosolModelTile.setSample(ix, iy, SynergyConstants.OUTPUT_AOTMODEL_BAND_NODATAVALUE);
//                minErr[iy - targetRectangle.y][ix - targetRectangle.x] = SynergyConstants.OUTPUT_AOTERR_BAND_NODATAVALUE;
//            }
//        }
//
//        for (Integer aerosolModel : aerosolModels) {
//
//            if ((toaLut == null) || (toaLut.getAerosolModel() != aerosolModel)) {
//                // provide complete LUT:
//                toaLut = new ReflectanceBinLUT(auxdataPath, aerosolModel, merisWvl, aatsrWvl);
//                lutAlbedo = toaLut.getAlbDim();
//                lutAot = toaLut.getAotDim();
//
//                lutSubsecMeris = new float[merisWvl.length][lutAlbedo.length][lutAot.length];
//                lutSubsecAatsr = new float[2][aatsrWvl.length][lutAlbedo.length][lutAot.length];
//            }
//
//            boolean validPixel = true;
//            for (int iY = targetRectangle.y; iY < targetRectangle.y + targetRectangle.height; iY++) {
//                for (int iX = targetRectangle.x; iX < targetRectangle.x + targetRectangle.width; iX++) {
//                    checkForCancellation();
//                    final int iSrcX = (2 * aveBlock + 1) * iX + aveBlock;
//                    final int iSrcY = (2 * aveBlock + 1) * iY + aveBlock;
//
//                    int flagPixel = 0;
//                    final boolean isBorder = (iSrcY + aveBlock >= rasterHeight || iSrcX + aveBlock >= rasterWidth);
//                    if (isBorder) {
//                        flagPixel |= borderMask;
//                    }
//
//                    final boolean isLand = evaluateFlagPixel(isLandTile, iSrcX, iSrcY, true);
//                    final boolean isCloudy = evaluateFlagPixel(isCloudyTile, iSrcX, iSrcY, false);
//                    if (!isLand) {
//                        flagPixel |= oceanMask;
//                    }
//                    if (isCloudy) {
//                        flagPixel |= cloudyMask;
//                    }
//                    // keep previous success
//                    final boolean prevSuccess = aerosolFlagTile.getSampleBit(iX, iY, 2);
//                    if (prevSuccess) {
//                        flagPixel |= successMask;
//                    }
//
//                    validPixel = isLand && !isCloudy;
//
//                    float[] geometry = null;
//                    float aveMerisPressure = 0;
//                    float aveMerisOzone = 0;
//                    float aveNdvi = 0;
//
//                    if (validPixel) {
//                        geometry = getAvePixel(geometryTiles, iSrcX, iSrcY, isValidTile, validPixel);
//
//                        merisToaReflec = getAvePixel(merisTiles, iSrcX, iSrcY, isValidTile, validPixel);
//                        aatsrToaReflec = getAvePixel(aatsrTiles, iSrcX, iSrcY, isValidTile, validPixel);
//
//                        aveMerisPressure = getAvePixel(pressureTile, iSrcX, iSrcY, isValidTile, validPixel);
//                        aveMerisOzone = getAvePixel(ozoneTile, iSrcX, iSrcY, isValidTile, validPixel);
//                        aveNdvi = getAvePixel(vNdviTile, iSrcX, iSrcY, isValidTile, validPixel);
//                    }
//                    if (validPixel) {
//
//                        final int iSza = 0;
//                        int iSaa = 1;
//                        int iVza = 2;
//                        int iVaa = 3;
//                        int offset = 0; // MERIS geometry
//                        toaLut.subsecLUT("meris", aveMerisPressure, aveMerisOzone, geometry[iVza + offset],
//                                         geometry[iVaa + offset],
//                                         geometry[iSza + offset], geometry[iSaa + offset], merisWvl, lutSubsecMeris);
//                        offset = 4; // AATSR NADIR geometry
//                        toaLut.subsecLUT("aatsr", aveMerisPressure, aveMerisOzone, geometry[iVza + offset],
//                                         geometry[iVaa + offset],
//                                         geometry[iSza + offset], geometry[iSaa + offset], aatsrWvl, lutSubsecAatsr[0]);
//                        offset = 8; // AATSR FWARD geometry
//                        toaLut.subsecLUT("aatsr", aveMerisPressure, aveMerisOzone, geometry[iVza + offset],
//                                         geometry[iVaa + offset],
//                                         geometry[iSza + offset], geometry[iSaa + offset], aatsrWvl, lutSubsecAatsr[1]);
//
//                        aardvarc.setSza(geometry[0], geometry[4], geometry[8]);
//                        aardvarc.setSaa(geometry[1], geometry[5], geometry[9]);
//                        aardvarc.setVza(geometry[2], geometry[6], geometry[10]);
//                        aardvarc.setVaa(geometry[3], geometry[7], geometry[11]);
//                        aardvarc.setNdvi(aveNdvi);
//                        aardvarc.setSurfPres(aveMerisPressure);
//                        aardvarc.setToaReflMeris(merisToaReflec);
//                        aardvarc.setToaReflAatsr(aatsrToaReflec);
//                        aardvarc.setLutReflAatsr(lutSubsecAatsr);
//                        aardvarc.setLutReflMeris(lutSubsecMeris);
//                        aardvarc.setAlbDim(lutAlbedo);
//                        aardvarc.setAotDim(lutAot);
//
//                        // now run the retrieval...
//                        aardvarc.runAarvarc();
//
//                        // and these are the retrieval results:
//                        boolean retrievalFailed = aardvarc.isFailed();
//                        final float aot = aardvarc.getOptAOT();    // AOT (tau_550)
//                        final float errMetric = aardvarc.getOptErr();    // E
//                        final float retrievalError = aardvarc.getRetrievalErr();
//                        retrievalFailed = retrievalFailed || aot < 1.0e-3 || (aot > 0.1 && (retrievalError / aot) > 5);
//                        if (!retrievalFailed) {
//                            flagPixel |= successMask;
//                        }
//                        if (aardvarc.isFailed()) {
//                            flagPixel |= negMetricMask;
//                        }
//                        if (aot < 1.0e-5) {
//                            flagPixel |= aotLowMask;
//                        }
//                        if (aot > 0.1 && (retrievalError / aot) > 5) {
//                            flagPixel |= errHighMask;
//                        }
//
//                        final double errTemp = minErr[iY - targetRectangle.y][iX - targetRectangle.x];
//                        if ((Double.compare(errTemp, SynergyConstants.OUTPUT_AOTERR_BAND_NODATAVALUE) == 0
//                                || errMetric < errTemp)) {
//
//                            minErr[iY - targetRectangle.y][iX - targetRectangle.x] = errMetric;
//                            aerosolTile.setSample(iX, iY, aot);
//                            aerosolErrTile.setSample(iX, iY, retrievalError);
//                            aerosolModelTile.setSample(iX, iY, aerosolModel);
//                        }
//
//                    }
//
//                    aerosolFlagTile.setSample(iX, iY, flagPixel);
//                    pm.worked(1);
//                }
//            }
//        }
//        pm.done();
    }



    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MerisAatsrSdrOp.class);
        }
    }
}
