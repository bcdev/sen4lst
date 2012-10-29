package org.esa.beam.sen4lst.util.sen3exp;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;

class LatLonGenerator {

    private String latFilePath = "lat.txt";
    private String lonFilePath = "lon.txt";
    private String geoFilePath = "geo.bin";

    public static void main(String args[]) {
        final LatLonGenerator g = new LatLonGenerator();

        try {
            g.generate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int arrayLength(long streamLength) throws IOException {
        return (int) (streamLength / (Double.SIZE / Byte.SIZE) / 2);
    }

    LatLonGenerator geoFilePath(String path) {
        this.geoFilePath = path;
        return this;
    }

    LatLonGenerator latFilePath(String path) {
        this.latFilePath = path;
        return this;
    }

    LatLonGenerator lonFilePath(String path) {
        this.lonFilePath = path;
        return this;
    }

    void generate() throws IOException {
        final ImageInputStream iis = new FileImageInputStream(new RandomAccessFile(geoFilePath, "r"));
        iis.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        final double[] lons = new double[arrayLength(iis.length())];
        final double[] lats = new double[arrayLength(iis.length())];

        try {
            iis.readFully(lons, 0, lons.length);
            iis.readFully(lats, 0, lats.length);
        } finally {
            try {
                iis.close();
            } catch (IOException e) {
                // ignore
            }
        }

        FileWriter latWriter = null;
        try {
            latWriter = new FileWriter(latFilePath);

            for (int i = 0; i < lats.length; i++) {
                if (i > 0) {
                    latWriter.write(", ");
                }
                latWriter.write(String.valueOf(lats[i]));
            }
        } finally {
            if (latWriter != null) {
                try {
                    latWriter.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        FileWriter lonWriter = null;
        try {
            lonWriter = new FileWriter(lonFilePath);
            for (int i = 0; i < lons.length; i++) {
                if (i > 0) {
                    lonWriter.write(", ");
                }
                lonWriter.write(String.valueOf(lons[i]));
            }
        } finally {
            if (lonWriter != null) {
                try {
                    lonWriter.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    String getLatFilePath() {
        return latFilePath;
    }

    String getLonFilePath() {
        return lonFilePath;
    }
}
