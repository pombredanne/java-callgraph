package inttest;

import gr.gousiosg.javacg.stat.JCallGraph;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class MphTableIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(MphTableIT.class);
    private final Path mphJar = Paths.get(System.getProperty("user.dir"),"artifacts","output","mph-table", "mph-table-1.0.6-SNAPSHOT.jar");
    private final Path mphTestJar = Paths.get(System.getProperty("user.dir"),"artifacts","output","mph-table","mph-table-1.0.6-SNAPSHOT-tests.jar");
    private final Path mphGraph = Paths.get(System.getProperty("user.dir"),"mph-table_graph");
    private final Path mphSmartByteSerializer = Paths.get(System.getProperty("user.dir"),"output","TestSmartByteSerializer#canRoundTripBytes.dot");
    private final Path mphSmartByteSerializerReachability = Paths.get(System.getProperty("user.dir"),"output","TestSmartByteSerializer#canRoundTripBytes-reachability.dot");
    private final Path mphSmartIntegerSerializer = Paths.get(System.getProperty("user.dir"),"output","TestSmartIntegerSerializer#canRoundTripIntegers.dot");
    private final Path mphSmartIntegerSerializerReachability = Paths.get(System.getProperty("user.dir"),"output","TestSmartIntegerSerializer#canRoundTripIntegers-reachability.dot");
    private final Path mphSmartListSerializer = Paths.get(System.getProperty("user.dir"),"output","TestSmartListSerializer#canRoundTripSerializableLists.dot");
    private final Path mphSmartListSerializerReachability = Paths.get(System.getProperty("user.dir"),"output","TestSmartListSerializer#canRoundTripSerializableLists-reachability.dot");
    private final Path mphSmartLongSerializer = Paths.get(System.getProperty("user.dir"),"output","TestSmartLongSerializer#canRoundTripLongs.dot");
    private final Path mphSmartLongSerializerReachability = Paths.get(System.getProperty("user.dir"),"output","TestSmartLongSerializer#canRoundTripLongs-reachability.dot");
    private final Path mphSmartPairSerializer = Paths.get(System.getProperty("user.dir"),"output","TestSmartPairSerializer#canRoundTripPairs.dot");
    private final Path mphSmartPairSerializerReachability = Paths.get(System.getProperty("user.dir"),"output","TestSmartPairSerializer#canRoundTripPairs-reachability.dot");
    private final Path mphSmartShortSerializer = Paths.get(System.getProperty("user.dir"),"output","TestSmartShortSerializer#canRoundTripShort.dot");
    private final Path mphSmartShortSerializerReachability = Paths.get(System.getProperty("user.dir"),"output","TestSmartShortSerializer#canRoundTripShort-reachability.dot");
    private final Path mphSmartStringSerializer = Paths.get(System.getProperty("user.dir"),"output","TestSmartStringSerializer#canRoundTripStrings.dot");
    private final Path mphSmartStringSerializerReachability = Paths.get(System.getProperty("user.dir"),"output","TestSmartStringSerializer#canRoundTripStrings-reachability.dot");

    @Before
    public void setUp(){
        String outputDirectoryPath = System.getProperty("user.dir") + "/output/mph-table/";
        File outputDir = new File(outputDirectoryPath);
        if(!outputDir.exists())
            outputDir.mkdir();
    }


    // Git Stage
    @Test
    public void testA(){
        String [] args = {"git", "-c", "mph-table"};
        JCallGraph.main(args);
    }
    //Build Stage
    @Test
    public void testB(){
        String [] args = {"build", "-j", "./artifacts/output/mph-table/mph-table-1.0.6-SNAPSHOT.jar",
            "-t", "./artifacts/output/mph-table/mph-table-1.0.6-SNAPSHOT-tests.jar", "-o", "mph-table_graph"};
        JCallGraph.main(args);
    }

    // Test Stage
    @Test
    public void testC(){
        String [] args = {"test", "-c", "mph-table", "-f", "mph-table_graph"};
        JCallGraph.main(args);
    }

    // Validation Checks
    @Test
    public void testD(){
        // Git Stage
        LOGGER.info("Starting Mph-Table Git Verification");
        assertTrue(Files.exists(mphJar));
        assertTrue(Files.exists(mphTestJar));


        // Build Stage
        LOGGER.info("Starting Mph-Table Build Verification");
        assertTrue(Files.exists(mphGraph));

        // Test Stage
        assertTrue(Files.exists(mphSmartByteSerializer));
        assertTrue(Files.exists(mphSmartByteSerializerReachability));
        assertTrue(Files.exists(mphSmartIntegerSerializer));
        assertTrue(Files.exists(mphSmartIntegerSerializerReachability));
        assertTrue(Files.exists(mphSmartListSerializer));
        assertTrue(Files.exists(mphSmartListSerializerReachability));
        assertTrue(Files.exists(mphSmartLongSerializer));
        assertTrue(Files.exists(mphSmartLongSerializerReachability));
        assertTrue(Files.exists(mphSmartPairSerializer));
        assertTrue(Files.exists(mphSmartPairSerializerReachability));
        assertTrue(Files.exists(mphSmartShortSerializer));
        assertTrue(Files.exists(mphSmartShortSerializerReachability));
        assertTrue(Files.exists(mphSmartStringSerializer));
        assertTrue(Files.exists(mphSmartStringSerializerReachability));
    }

    //
    // Create png files for comparison
    @Test
    public void testE() throws IOException, InterruptedException {
        String cmd = "./buildpng.sh";
        String project = "mph-table";
        ProcessBuilder pb = new ProcessBuilder(cmd, project);
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while((line = br.readLine()) != null)
            LOGGER.info(line);
        process.waitFor();
    }

    //
    // Test difference through diffimg
    @Test
    public void testF() throws IOException, InterruptedException {
        String cmd = "./testdiff.sh";
        String project = "mph-table";
        ProcessBuilder pb = new ProcessBuilder(cmd, project);
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while((line = br.readLine()) != null) {
            LOGGER.info(line);
            if(line.contains("%")) {
                String[] values = line.split(" : ");
                Double percentDifference = Double.parseDouble(values[1].replace("%", ""));
                Assert.assertTrue(percentDifference < 0.05);
            }
        }
        process.waitFor();
    }

    @After
    public void cleanUp() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("sh", "rm", "output/*.dot");
        Process process = pb.start();
        process.waitFor();
    }
}
