package sen4lst.util.sen3exp;/*
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

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Product;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Program for generating coordinate files for relevant SEN3EXP data.
 *
 * @author Ralf Quast
 */
public class Main {

    public static void main(String args[]) {
        final File dataDir = new File("./data");

        final File[] olciDirs = dataDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().startsWith("S3A_OL_1_EFR");
            }
        });
        final File[] slstrDirs = dataDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().startsWith("S3A_SL_1_SLT");
            }
        });

        for (final File dir : olciDirs) {
            final File[] geoFiles = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().startsWith("GeoCode_Pattern");
                }
            });
            generate(dir.getName().replace(".SAFE", ""), "xeoCoordinates.cdl", geoFiles[0].getPath());
            convert(dir);
        }

        for (final File dir : slstrDirs) {
            File[] geoFiles;
            geoFiles = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().startsWith("GeoCode_Pattern_SEN4LST_TOA_NADIR_CAS");
                }
            });
            generate(dir.getName().replace(".SAFE", ""), "xeodetic_an.cdl", geoFiles[0].getPath());
            geoFiles = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().startsWith("GeoCode_Pattern_SEN4LST_TOA_550_OV_CAS");
                }
            });
            generate(dir.getName().replace(".SAFE", ""), "xeodetic_ao.cdl", geoFiles[0].getPath());
            convert(dir);
        }
    }

    private static void generate(String productName, String cdlResourceName, String geoFilePath) {
        try {
            final LatLonGenerator g = new LatLonGenerator();
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
            final String targetNcFilePath = createTargetNcFilePath(productName, cdlResourceName);
            fileGenerator.setTargetNcFilePath(targetNcFilePath);
            fileGenerator.getProperties().setProperty("LAT", g.getLatFilePath());
            fileGenerator.getProperties().setProperty("LON", g.getLonFilePath());
            fileGenerator.getProperties().setProperty("PRODUCT_NAME", productName);
            fileGenerator.generateDataset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void convert(File dir) {
        final File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".nc") && !name.endsWith("beam.nc") && !name.startsWith("xeoCoordinates");
            }
        });
        for (final File file : files) {
            try {
                Product product = ProductIO.readProduct(file, "NetCDF-CF");
                if (product != null) {
                    if (file.getName().startsWith("x")) {
                        final String subsetName = product.getName() + "_1km";
                        final ProductSubsetDef subsetDef = new ProductSubsetDef(subsetName);
                        subsetDef.setSubSampling(2, 2);
                        product = ProductSubsetBuilder.createProductSubset(product, true, subsetDef, subsetName, null);
                        product.setFileLocation(new File(file.getParentFile(), subsetName + ".nc"));
                    }
                    ProductIO.writeProduct(product, product.getFileLocation().getPath().replace(".nc", ".beam.nc"),
                                           "NetCDF-CF");
                    System.out.println("INFO: Converted file '" + file + "'.");
                    product.dispose();
                }
            } catch (Exception e) {
                System.out.println("WARNING: Failed to convert file '" + file + "'.");
            }
        }
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
