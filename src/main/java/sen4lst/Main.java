package sen4lst;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Main {

    public static void main(String args[]) {
        generate(
                "S3A_OL_1_EFR_20090622T095200_20090622T100200_000001_00600_001_ESR_TEST_01",
                "GeoCode_Pattern_SEN4LST_TOA_NADIR_CAS_090622_0952Z_P01SF_OLCI_LAYER_300_mask_sub_all.bin",
                "xeoCoordinates.cdl"
        );

        generate(
                "S3A_OL_1_EFR_20090622T101100_20090622T102100_000001_00600_001_ESR_TEST_01",
                "GeoCode_Pattern_SEN4LST_TOA_NADIR_CAS_090622_1011Z_P03SF_OLCI_LAYER_300_mask_sub_all.bin",
                "xeoCoordinates.cdl"
        );

        generate(
                "S3A_OL_1_EFR_20090622T101900_20090622T102900_000001_00600_001_ESR_TEST_01",
                "GeoCode_Pattern_SEN4LST_TOA_NADIR_CAS_090622_1019Z_P04SF_OLCI_LAYER_300_mask_sub_all.bin",
                "xeoCoordinates.cdl"
        );

        generate(
                "S3A_OL_1_EFR_20090622T102900_20090622T103900_000001_00600_001_ESR_TEST_01",
                "GeoCode_Pattern_SEN4LST_TOA_NADIR_CAS_090622_1029Z_P05SF_OLCI_LAYER_300_mask_sub_all.bin",
                "xeoCoordinates.cdl"
        );

        generate(
                "S3A_SL_1_SLT_20090622T095200_20090622T100200_000001_00600_001_ESR_TEST_01",
                "GeoCode_Pattern_SEN4LST_TOA_NADIR_CAS_090622_0952Z_P01SF_SLSTR_LAYER_ALL_500_mask_sub_all.bin",
                "xeodetic_an.cdl"
        );
        generate(
                "S3A_SL_1_SLT_20090622T095200_20090622T100200_000001_00600_001_ESR_TEST_01",
                "GeoCode_Pattern_SEN4LST_TOA_550_OV_CAS_090622_0952Z_P01SF_SLSTR_LAYER_ALL_500_mask_sub_all.bin",
                "xeodetic_ao.cdl"
        );

        generate(
                "S3A_SL_1_SLT_20090622T101100_20090622T102100_000001_00600_001_ESR_TEST_01",
                "GeoCode_Pattern_SEN4LST_TOA_NADIR_CAS_090622_1011Z_P03SF_SLSTR_LAYER_ALL_500_mask_sub_all.bin",
                "xeodetic_an.cdl"
        );
        generate(
                "S3A_SL_1_SLT_20090622T101100_20090622T102100_000001_00600_001_ESR_TEST_01",
                "GeoCode_Pattern_SEN4LST_TOA_550_OV_CAS_090622_1011Z_P03SF_SLSTR_LAYER_ALL_500_mask_sub_all.bin",
                "xeodetic_ao.cdl"
        );

        generate(
                "S3A_SL_1_SLT_20090622T101900_20090622T102900_000001_00600_001_ESR_TEST_01",
                "GeoCode_Pattern_SEN4LST_TOA_NADIR_CAS_090622_1019Z_P04SF_SLSTR_LAYER_ALL_500_mask_sub_all.bin",
                "xeodetic_an.cdl"
        );
        generate(
                "S3A_SL_1_SLT_20090622T101900_20090622T102900_000001_00600_001_ESR_TEST_01",
                "GeoCode_Pattern_SEN4LST_TOA_550_OV_CAS_090622_1019Z_P04SF_SLSTR_LAYER_ALL_500_mask_sub_all.bin",
                "xeodetic_ao.cdl"
        );

        generate(
                "S3A_SL_1_SLT_20090622T102900_20090622T103900_000001_00600_001_ESR_TEST_01",
                "GeoCode_Pattern_SEN4LST_TOA_NADIR_CAS_090622_1029Z_P05SF_SLSTR_LAYER_ALL_500_mask_sub_all.bin",
                "xeodetic_an.cdl"
        );
        generate(
                "S3A_SL_1_SLT_20090622T102900_20090622T103900_000001_00600_001_ESR_TEST_01",
                "GeoCode_Pattern_SEN4LST_TOA_550_OV_CAS_090622_1029Z_P05SF_SLSTR_LAYER_ALL_500_mask_sub_all.bin",
                "xeodetic_ao.cdl"
        );
    }

    private static void generate(String productName, String geoFileName, String cdlResourceName) {
        try {
            final LatLonGenerator g = new LatLonGenerator();
            final String geoFilePath = createGeoFilePath(productName, geoFileName);
            g.geoFilePath(geoFilePath);
            g.latFilePath(createTempFile("lat", ".txt").getPath());
            g.lonFilePath(createTempFile("lon", ".txt").getPath());

            try {
                g.generate();
            } catch (IOException e) {
                e.printStackTrace();
            }
            final CoordinateFileGenerator fileGenerator = new CoordinateFileGenerator();
            fileGenerator.setGeneratorExecutablePath("/usr/local/bin/ncgen");
            fileGenerator.setSourceCdlFilePath(createSourceCdlFilePath(cdlResourceName));
            fileGenerator.setTargetCdlFilePath(createTempFile("geo", ".cdl").getPath());
            fileGenerator.setTargetNcFilePath(createTargetNcFilePath(productName, cdlResourceName));
            fileGenerator.getProperties().setProperty("LAT", g.getLatFilePath());
            fileGenerator.getProperties().setProperty("LON", g.getLonFilePath());
            fileGenerator.getProperties().setProperty("PRODUCT_NAME", productName);
            fileGenerator.generateDataset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String createGeoFilePath(String productName, String geoFileName) {
        return new File(new File("data", productName + ".SAFE"), geoFileName).getPath();
    }

    private static String createTargetNcFilePath(String productName, String cdlResourceName) {
        return new File(new File("data", productName + ".SAFE"), cdlResourceName.replace(".cdl", ".nc")).getPath();
    }

    private static String createSourceCdlFilePath(String cdlResourceName) throws URISyntaxException {
        final URI uri = Main.class.getResource(cdlResourceName).toURI();
        final File file = new File(uri);
        return file.getPath();
    }

    private static File createTempFile(String prefix, String suffix) throws IOException {
        final File file = File.createTempFile(prefix, suffix, new File("data"));
        file.deleteOnExit();

        return file;
    }
}
