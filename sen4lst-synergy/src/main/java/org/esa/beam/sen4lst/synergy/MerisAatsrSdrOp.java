package org.esa.beam.sen4lst.synergy;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.gpf.operators.standard.BandMathsOp;
import org.esa.beam.synergy.operators.*;
import org.esa.beam.synergy.util.AerosolHelpers;
import org.esa.beam.synergy.util.SynergyConstants;
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @TargetProduct(description = "The target product.")
    private Product targetProduct;

    private String auxdataPath = SynergyConstants.SYNERGY_AUXDATA_HOME_DEFAULT + File.separator +
            "aerosolLUTs" + File.separator + "land";


    private ArrayList<Band> merisBandList;
    private ArrayList<Band> aatsrBandListNad;
    private ArrayList<Band> aatsrBandListFwd;
    private ArrayList<RasterDataNode> merisGeometryBandList;
    private ArrayList<RasterDataNode> aatsrGeometryBandList;
    private float[] merisWvl;
    private float[] aatsrWvl;
    private float[] soilSurfSpec;
    private float[] vegSurfSpec;
    private float[] merisBandWidth;
    private float[] aatsrBandWidth;

    private String[] sdrMerisBandNames;
    private String[][] sdrAatsrBandNames;

    private int rasterWidth;
    private int rasterHeight;

    private Band validBand;
    private Product landUpscaledProduct;


    @Override
    public void initialize() throws OperatorException {

        final int aveBlock = 7;

        // get the aerosol land product..
        Product landProduct;
        Map<String, Product> landInput = new HashMap<String, Product>(1);
        landInput.put("source", preprocessedProduct);
        Map<String, Object> landParams = new HashMap<String, Object>(6);
        landParams.put("soilSpecName", SynergyConstants.SOIL_SPEC_PARAM_DEFAULT);
        landParams.put("vegSpecName", SynergyConstants.VEG_SPEC_PARAM_DEFAULT);
        landParams.put("aveBlock", aveBlock);
        landParams.put("useCustomLandAerosol", false);
        landParams.put("customLandAerosol", SynergyConstants.AEROSOL_MODEL_PARAM_DEFAULT);
        landProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(RetrieveAerosolLandOp.class), landParams, landInput);

        // interpolate the land product..
        Product landInterpolatedProduct;
        Map<String, Product> landOceanInterpolatedInput = new HashMap<String, Product>(2);
        landOceanInterpolatedInput.put("synergy", preprocessedProduct);
        landOceanInterpolatedInput.put("source", landProduct);
        Map<String, Object> landOceanInterpolatedParams = new HashMap<String, Object>();
        landOceanInterpolatedParams.put("aveBlock", aveBlock);
        landInterpolatedProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(AotExtrapOp.class), landOceanInterpolatedParams, landOceanInterpolatedInput);

        // upscale the land product..
        Map<String, Product> landUpscaledInput = new HashMap<String, Product>(2);
        landUpscaledInput.put("synergy", preprocessedProduct);
        landUpscaledInput.put("aerosol", landInterpolatedProduct);
        Map<String, Object> landOceanUpscaledParams = new HashMap<String, Object>(1);
        landOceanUpscaledParams.put("scalingFactor", aveBlock);
        landUpscaledProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(UpscaleOp.class), landOceanUpscaledParams, landUpscaledInput);


        rasterWidth = preprocessedProduct.getSceneRasterWidth();
        rasterHeight = preprocessedProduct.getSceneRasterHeight();

        merisBandList = new ArrayList<Band>();
        aatsrBandListNad = new ArrayList<Band>();
        aatsrBandListFwd = new ArrayList<Band>();
        merisGeometryBandList = new ArrayList<RasterDataNode>();
        aatsrGeometryBandList = new ArrayList<RasterDataNode>();

        AerosolHelpers.getSpectralBandList(preprocessedProduct, SynergyConstants.INPUT_BANDS_PREFIX_MERIS,
                                           SynergyConstants.INPUT_BANDS_SUFFIX_MERIS,
                                           SynergyConstants.EXCLUDE_INPUT_BANDS_MERIS, merisBandList);
        AerosolHelpers.getSpectralBandList(preprocessedProduct, SynergyConstants.INPUT_BANDS_PREFIX_AATSR_NAD,
                                           SynergyConstants.INPUT_BANDS_SUFFIX_AATSR,
                                           SynergyConstants.EXCLUDE_INPUT_BANDS_AATSR, aatsrBandListNad);
        AerosolHelpers.getSpectralBandList(preprocessedProduct, SynergyConstants.INPUT_BANDS_PREFIX_AATSR_FWD,
                                           SynergyConstants.INPUT_BANDS_SUFFIX_AATSR,
                                           SynergyConstants.EXCLUDE_INPUT_BANDS_AATSR, aatsrBandListFwd);
        AerosolHelpers.getGeometryBandList(preprocessedProduct, "MERIS", merisGeometryBandList);
        AerosolHelpers.getGeometryBandList(preprocessedProduct, "AATSR", aatsrGeometryBandList);

        merisWvl = new float[merisBandList.size()];
        aatsrWvl = new float[aatsrBandListNad.size()];
        merisBandWidth = new float[merisBandList.size()];
        aatsrBandWidth = new float[aatsrBandListNad.size()];

        sdrMerisBandNames = new String[merisBandList.size()];
        sdrAatsrBandNames = new String[2][aatsrBandListNad.size()];

        readWavelengthBandw(merisBandList, merisWvl, merisBandWidth);
        readWavelengthBandw(aatsrBandListNad, aatsrWvl, aatsrBandWidth);

        soilSurfSpec = new SurfaceSpec(SynergyConstants.SOIL_SPEC_PARAM_DEFAULT, merisWvl).getSpec();
        vegSurfSpec = new SurfaceSpec(SynergyConstants.VEG_SPEC_PARAM_DEFAULT, merisWvl).getSpec();

        final String validFlagExpression = "( l1_flags_MERIS.LAND_OCEAN && " +
                "!(cloud_flags_synergy.CLOUD || cloud_flags_synergy.CLOUD_FILLED))";
        final BandMathsOp validBandOp = BandMathsOp.createBooleanExpressionBand(validFlagExpression, landUpscaledProduct);
        validBand = validBandOp.getTargetProduct().getBandAt(0);

        createTargetProduct();

    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws
            OperatorException {
        pm.beginTask("SDR retrieval", targetRectangle.width * targetRectangle.height);
        System.out.printf("   SDR Retrieval @ Tile %s\n", targetRectangle.toString());

        // read source tiles
        final Tile[] merisTiles = getSpecTiles(merisBandList, targetRectangle);

        Tile[][] aatsrTiles = new Tile[2][0];
        aatsrTiles[0] = getSpecTiles(aatsrBandListNad, targetRectangle);
        aatsrTiles[1] = getSpecTiles(aatsrBandListFwd, targetRectangle);

        final Tile[] geometryTiles = getGeometryTiles(merisGeometryBandList, aatsrGeometryBandList, targetRectangle);

        final Tile pressureTile = getSourceTile(preprocessedProduct.getTiePointGrid(SynergyConstants.INPUT_PRESSURE_BAND_NAME),
                                                targetRectangle);
        final Tile ozoneTile = getSourceTile(preprocessedProduct.getTiePointGrid(SynergyConstants.INPUT_OZONE_BAND_NAME),
                                             targetRectangle);
        final Tile aotTile = getSourceTile(landUpscaledProduct.getBand(SynergyConstants.OUTPUT_AOT_BAND_NAME + "_filter"),
                                           targetRectangle);
        final Tile validPixelTile = getSourceTile(validBand, targetRectangle);

        float[] merisToaReflec;
        float[][] aatsrToaReflec;

        final Aardvarc aardvarc = new Aardvarc(aatsrWvl, merisWvl);
        aardvarc.setSpecSoil(soilSurfSpec);
        aardvarc.setSpecVeg(vegSurfSpec);

        final int aeroModel = Integer.parseInt(SynergyConstants.AEROSOL_MODEL_PARAM_DEFAULT);
        ReflectanceBinLUT toaLut = new ReflectanceBinLUT(auxdataPath, aeroModel, merisWvl, aatsrWvl);
        final float[] lutAlbedo = toaLut.getAlbDim();
        final float[] lutAot = toaLut.getAotDim();

        float[][][] lutSubsecMeris = new float[merisWvl.length][lutAlbedo.length][lutAot.length];
        float[][][][] lutSubsecAatsr = new float[2][aatsrWvl.length][lutAlbedo.length][lutAot.length];

        for (int iY = targetRectangle.y; iY < targetRectangle.y + targetRectangle.height; iY++) {
            for (int iX = targetRectangle.x; iX < targetRectangle.x + targetRectangle.width; iX++) {
                checkForCancellation();

                if (validPixelTile.getSampleBoolean(iX, iY) && isValidAeroModel(aeroModel)) {
                    final float[] geometry = getGeometries(geometryTiles, iX, iY);

                    merisToaReflec = getSpectra(merisTiles, iX, iY);
                    aatsrToaReflec = getSpectra(aatsrTiles, iX, iY);

                    final float aveMerisPressure = pressureTile.getSampleFloat(iX, iY);
                    final float aveMerisOzone = ozoneTile.getSampleFloat(iX, iY);

                    // this setup is the same as for pure AOD retrieval
                    final int iSza = 0;
                    int iSaa = 1;
                    int iVza = 2;
                    int iVaa = 3;
                    int offset = 0; // MERIS geometry
                    toaLut.subsecLUT("meris", aveMerisPressure, aveMerisOzone, geometry[iVza + offset],
                                     geometry[iVaa + offset],
                                     geometry[iSza + offset], geometry[iSaa + offset], merisWvl, lutSubsecMeris);
                    offset = 4; // AATSR NADIR geometry
                    toaLut.subsecLUT("aatsr", aveMerisPressure, aveMerisOzone, geometry[iVza + offset],
                                     geometry[iVaa + offset],
                                     geometry[iSza + offset], geometry[iSaa + offset], aatsrWvl, lutSubsecAatsr[0]);
                    offset = 8; // AATSR FWARD geometry
                    toaLut.subsecLUT("aatsr", aveMerisPressure, aveMerisOzone, geometry[iVza + offset],
                                     geometry[iVaa + offset],
                                     geometry[iSza + offset], geometry[iSaa + offset], aatsrWvl, lutSubsecAatsr[1]);

                    aardvarc.setSza(geometry[0], geometry[4], geometry[8]);
                    aardvarc.setToaReflMeris(merisToaReflec);
                    aardvarc.setToaReflAatsr(aatsrToaReflec);
                    aardvarc.setLutReflAatsr(lutSubsecAatsr);
                    aardvarc.setLutReflMeris(lutSubsecMeris);
                    aardvarc.setAlbDim(lutAlbedo);
                    aardvarc.setAotDim(lutAot);

                    aardvarc.setNdvi(0.8f);
                    aardvarc.setSurfPres(aveMerisPressure);

                    // now get the SDRs...

                    //
                    // MERIS
                    //
                    float[] surfrefl = new float[merisWvl.length];
                    aardvarc.invLut(aotTile.getSampleFloat(iX, iY), lutSubsecMeris, merisToaReflec, surfrefl);
                    for (int i = 0; i < merisWvl.length; i++) {
                        if (sdrMerisBandNames[i] != null) {
                            Tile targetTile = targetTiles.get(targetProduct.getBand(sdrMerisBandNames[i]));
                            targetTile.setSample(iX, iY, surfrefl[i]);
                        }
                    }
                    //
                    // AATSR
                    //
                    float[][] surfreflAATSR = new float[2][aatsrWvl.length];
                    aardvarc.invLut(aotTile.getSampleFloat(iX, iY), lutSubsecAatsr, aatsrToaReflec, surfreflAATSR);
                    for (int i = 0; i < aatsrWvl.length; i++) {
                        Tile targetTile;
                        if (sdrAatsrBandNames[0][i] != null) {
                            targetTile = targetTiles.get(targetProduct.getBand(sdrAatsrBandNames[0][i]));
                            targetTile.setSample(iX, iY, surfreflAATSR[0][i]);
                        }
                        if (sdrAatsrBandNames[1][i] != null) {
                            targetTile = targetTiles.get(targetProduct.getBand(sdrAatsrBandNames[1][i]));
                            targetTile.setSample(iX, iY, surfreflAATSR[1][i]);
                        }
                    }
                } else {
                    for (int i = 0; i < merisWvl.length; i++) {
                        if (sdrMerisBandNames[i] != null) {
                            Tile targetTile = targetTiles.get(targetProduct.getBand(sdrMerisBandNames[i]));
                            targetTile.setSample(iX, iY, SynergyConstants.OUTPUT_SDR_BAND_NODATAVALUE);
                        }
                    }
                    for (int i = 0; i < aatsrWvl.length; i++) {
                        Tile targetTile;
                        if (sdrAatsrBandNames[0][i] != null) {
                            targetTile = targetTiles.get(targetProduct.getBand(sdrAatsrBandNames[0][i]));
                            targetTile.setSample(iX, iY, SynergyConstants.OUTPUT_SDR_BAND_NODATAVALUE);
                        }
                        if (sdrAatsrBandNames[1][i] != null) {
                            targetTile = targetTiles.get(targetProduct.getBand(sdrAatsrBandNames[1][i]));
                            targetTile.setSample(iX, iY, SynergyConstants.OUTPUT_SDR_BAND_NODATAVALUE);
                        }
                    }
                }
                pm.worked(1);
            }
        }
        pm.done();

    }

    private void createTargetProduct() {

        targetProduct = new Product("SYNERGY SDR", "SYNERGY SDR", rasterWidth, rasterHeight);

        // copy from preprocessed product:
        //    - all flag bands
        //    - AATSR nadir/fward btemps 550, 670
        //    - AATSR latitude, longitude, altitude

        ProductUtils.copyMetadata(preprocessedProduct, targetProduct);
        ProductUtils.copyGeoCoding(preprocessedProduct, targetProduct);
//        ProductUtils.copyTiePointGrids(preprocessedProduct, targetProduct);
        ProductUtils.copyMasks(preprocessedProduct, targetProduct);
        ProductUtils.copyOverlayMasks(preprocessedProduct, targetProduct);
        ProductUtils.copyFlagBands(preprocessedProduct, targetProduct, true);
        for (Band srcBand : preprocessedProduct.getBands()) {
            if (!srcBand.isFlagBand()) {
                if (isAatsrTemperatureBand(srcBand) || isAatsrGeometryBand(srcBand)) {
                    ProductUtils.copyBand(srcBand.getName(), preprocessedProduct, targetProduct, true);
                }
            }
        }

        // add computed bands:
        //    - MERIS SDR bands 7, 12
        //    - AATSR SDR nadir bands 1, 2
        //    - AATSR SDR fward bands 1, 2

        createTargetProductBands();

        targetProduct.setPreferredTileSize(128, 128);
        setTargetProduct(targetProduct);

    }

    private boolean isAatsrGeometryBand(Band srcBand) {
        return srcBand.getName().toUpperCase().equals("LATITUDE_AATSR") ||
                srcBand.getName().toUpperCase().equals("LONGITUDE_AATSR") ||
                srcBand.getName().toUpperCase().equals("ALTITUDE_AATSR");
    }

    private boolean isAatsrTemperatureBand(Band srcBand) {
        return srcBand.getName().toUpperCase().startsWith("BTEMP");
    }

    private void createTargetProductBands() {

        Band targetBand;

        for (int iWL = 0; iWL < merisWvl.length; iWL++) {
            final int thisWvl = Math.round(merisWvl[iWL]);
            if (thisWvl == MerisAatsrConstants.MERIS_BAND7_WVL || thisWvl == MerisAatsrConstants.MERIS_BAND10_WVL) {
                final String wvlSubstring = String.format("_%d", thisWvl);
                sdrMerisBandNames[iWL] = SynergyConstants.OUTPUT_SDR_BAND_NAME + wvlSubstring + "_MERIS";
                targetBand = new Band(sdrMerisBandNames[iWL], ProductData.TYPE_FLOAT32, rasterWidth, rasterHeight);
                targetBand.setDescription(SynergyConstants.OUTPUT_SDR_BAND_DESCRIPTION);
                targetBand.setNoDataValue(SynergyConstants.OUTPUT_SDR_BAND_NODATAVALUE);
                targetBand.setNoDataValueUsed(SynergyConstants.OUTPUT_SDR_BAND_NODATAVALUE_USED);
                targetBand.setValidPixelExpression(sdrMerisBandNames[iWL] + ">= 0 AND " + sdrMerisBandNames[iWL] + "<= 1");
                targetBand.setSpectralBandIndex(iWL);
                targetBand.setSpectralBandwidth(merisBandWidth[iWL]);
                targetBand.setSpectralWavelength(merisWvl[iWL]);
                targetProduct.addBand(targetBand);
            }
        }

        String[] viewString = {"_nadir", "_fward"};
        for (int iView = 0; iView < 2; iView++) {
            for (int iWL = 0; iWL < aatsrWvl.length; iWL++) {
                final int thisWvl = Math.round(aatsrWvl[iWL]);
                if (thisWvl == MerisAatsrConstants.AATSR_BAND1_WVL || thisWvl == MerisAatsrConstants.AATSR_BAND2_WVL) {
                    final String wvlSubstring = String.format("_%d", Math.round(aatsrWvl[iWL]));
                    sdrAatsrBandNames[iView][iWL] = SynergyConstants.OUTPUT_SDR_BAND_NAME
                            + viewString[iView] + wvlSubstring + "_AATSR";
                    targetBand = new Band(sdrAatsrBandNames[iView][iWL], ProductData.TYPE_FLOAT32, rasterWidth,
                                          rasterHeight);
                    targetBand.setDescription(SynergyConstants.OUTPUT_SDR_BAND_DESCRIPTION);
                    targetBand.setNoDataValue(SynergyConstants.OUTPUT_SDR_BAND_NODATAVALUE);
                    targetBand.setNoDataValueUsed(SynergyConstants.OUTPUT_SDR_BAND_NODATAVALUE_USED);
                    targetBand.setValidPixelExpression(
                            sdrAatsrBandNames[iView][iWL] + ">= 0 AND " + sdrAatsrBandNames[iView][iWL] + "<= 1");
                    targetBand.setSpectralBandIndex(iWL);
                    targetBand.setSpectralBandwidth(aatsrBandWidth[iWL]);
                    targetBand.setSpectralWavelength(aatsrWvl[iWL]);
                    targetProduct.addBand(targetBand);
                }
            }
        }
    }

    private boolean isValidAeroModel(int modelNumber) {
        return (modelNumber >= 1 && modelNumber <= 40);
    }

    private float[] getGeometries(Tile[] geometryTiles, int iX, int iY) {

        float[] geometry = new float[geometryTiles.length];
        for (int ig = 0; ig < geometry.length; ig++) {
            geometry[ig] = geometryTiles[ig].getSampleFloat(iX, iY);
            if (geometryTiles[ig].getRasterDataNode().getName().matches(".*elev.*")) {
                geometry[ig] = 90.0f - geometry[ig];
            }
        }
        return geometry;
    }

    private float[][] getSpectra(Tile[][] specTiles, int iTarX, int iTarY) {
        float[][] spectrum = new float[2][0];
        spectrum[0] = getSpectra(specTiles[0], iTarX, iTarY);
        spectrum[1] = getSpectra(specTiles[1], iTarX, iTarY);

        return spectrum;
    }

    private float[] getSpectra(Tile[] specTiles, int iTarX, int iTarY) {
        return getGeometries(specTiles, iTarX, iTarY);
    }


    private Tile[] getGeometryTiles(ArrayList<RasterDataNode> merisGeometryBandList,
                                    ArrayList<RasterDataNode> aatsrGeometryBandList, Rectangle rec) {
        ArrayList<RasterDataNode> bandList = new ArrayList<RasterDataNode>();
        bandList.addAll(merisGeometryBandList);
        bandList.addAll(aatsrGeometryBandList);
        Tile[] geometryTiles = new Tile[bandList.size()];
        for (int i = 0; i < bandList.size(); i++) {
            Tile sourceTile = getSourceTile(bandList.get(i), rec);
            geometryTiles[i] = sourceTile;
        }
        return geometryTiles;
    }

    private Tile[] getSpecTiles(ArrayList<Band> sourceBandList, Rectangle rec) {
        Tile[] sourceTiles = new Tile[sourceBandList.size()];
        for (int i = 0; i < sourceTiles.length; i++) {
            sourceTiles[i] = getSourceTile(sourceBandList.get(i), rec);
        }
        return sourceTiles;
    }

    private void readWavelengthBandw(ArrayList<Band> bandList, float[] wvl, float[] bwidth) {
        for (int i = 0; i < bandList.size(); i++) {
            wvl[i] = bandList.get(i).getSpectralWavelength();
            bwidth[i] = bandList.get(i).getSpectralBandwidth();
        }
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
