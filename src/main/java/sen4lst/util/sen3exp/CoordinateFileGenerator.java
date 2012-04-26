package sen4lst.util.sen3exp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

class CoordinateFileGenerator {

    final Properties properties = new Properties();

    public static void main(String[] args) {
        final CoordinateFileGenerator g = new CoordinateFileGenerator();

        if (args.length > 0) {
            FileInputStream is = null;
            try {
                is = new FileInputStream(args[0]);
                g.getProperties().load(is);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
        try {
            g.generateDataset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Properties getProperties() {
        return properties;
    }

    void setGeneratorExecutablePath(String path) {
        properties.setProperty("ncgen", path);
    }

    void setSourceCdlFilePath(String path) {
        properties.setProperty("sourceCdlFile", path);
    }

    void setTargetCdlFilePath(String path) {
        properties.setProperty("targetCdlFile", path);
    }

    void setTargetNcFilePath(String path) {
        properties.setProperty("targetNcFile", path);
    }

    void generateDataset() throws Exception {
        final TemplateResolver resolver = new TemplateResolver(properties);
        final File sourceCdlFile = new File(properties.getProperty("sourceCdlFile"));
        final File targetCdlFile = new File(properties.getProperty("targetCdlFile"));
        final File targetNcFile = new File(properties.getProperty("targetNcFile"));

        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(sourceCdlFile));
            writer = new BufferedWriter(new FileWriter(targetCdlFile));
            String line = reader.readLine();
            while (line != null) {
                line = resolver.resolve(line, writer);
                writer.write(line + "\n");
                line = reader.readLine();
            }
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
        generateNcFile(properties.getProperty("ncgen"), targetCdlFile, targetNcFile);
    }

    private void generateNcFile(String ncgen, File cdlFile, File ncFile) throws Exception {
        final String command = ncgen + " -k 1 -o " + ncFile.getPath() + " " + cdlFile.getPath();
        System.out.println(command);
        final Process process = Runtime.getRuntime().exec(command);
        if (process.waitFor() != 0) {
            throw new Exception(
                    MessageFormat.format("process <code>{0}</code> terminated with exit value {1}",
                                         command, process.exitValue()));
        }
    }
}
