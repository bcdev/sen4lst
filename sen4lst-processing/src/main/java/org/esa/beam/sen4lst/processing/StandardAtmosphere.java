package org.esa.beam.sen4lst.processing;

import org.esa.beam.util.io.CsvReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 31.10.12
 * Time: 12:11
 *
 * @author olafd
 */
public class StandardAtmosphere {

    private static final String ATMPSPHERE_DESCR_FILE_NAME = "atm_description.txt";
    private static final char[] SEPARATOR = new char[]{'\t'};

    private final List<Atmosphere> atmosphereRecords;

    private StandardAtmosphere() {
        this.atmosphereRecords = loadAuxData();
    }

    public static StandardAtmosphere getInstance() {
        return Holder.instance;
    }

    public int getAtmosphereRecordCount() {
        return atmosphereRecords.size();
    }

    public int getAtmosphereId(int index) {
        return atmosphereRecords.get(index).ID;
    }

    public String getAtmosphereModel(int index) {
        return atmosphereRecords.get(index).model;
    }

    public double getAtmosphereWaterVapour(int index) {
        return atmosphereRecords.get(index).waterVapourContent;
    }

    // Initialization on demand holder idiom
    private List<Atmosphere> loadAuxData() {
        InputStream inputStream = StandardAtmosphere.class.getResourceAsStream(ATMPSPHERE_DESCR_FILE_NAME);
        InputStreamReader streamReader = new InputStreamReader(inputStream);
        CsvReader csvReader = new CsvReader(streamReader, SEPARATOR);
        List<String[]> records;
        try {
            records = csvReader.readStringRecords();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load atmosphere auxdata", e);
        }
        List<Atmosphere> atmInfo = new ArrayList<Atmosphere>(records.size());
        for (String[] record : records) {
            String id = record[0].trim();
            String region = record[1].trim();
            String wv = record[2].trim();
            atmInfo.add(new Atmosphere(Integer.parseInt(id), region, Double.parseDouble(wv)));
        }
        return atmInfo;
    }

    private static class Atmosphere {
        private final int ID;
        private final String model;
        private final double waterVapourContent;     // total water vapour content, g/cm^2

        Atmosphere(int ID, String region, double waterVapour) {
            this.ID = ID;
            this.model = region;
            this.waterVapourContent = waterVapour;
        }
    }

    private static class Holder {
        private static final StandardAtmosphere instance = new StandardAtmosphere();
    }
}
