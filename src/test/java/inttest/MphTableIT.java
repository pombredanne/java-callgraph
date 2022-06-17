package inttest;

import gr.gousiosg.javacg.stat.JCallGraph;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class MphTableIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(MphTableIT.class);

    @Test
    public void testA(){
        String [] args = {"git", "-c", "mph-table"};
        JCallGraph.main(args);
    }
    @Test
    public void testB(){
        String [] args = {"build", "-j", "./artifacts/output/mph-table-1.0.6-SNAPSHOT.jar",
            "-t", "./artifacts/output/mph-table-1.0.6-SNAPSHOT-tests.jar", "-o", "mph_table_graph"};
        JCallGraph.main(args);
    }

    @Test
    public void testC(){
        String [] args = {"test", "-c", "mph-table", "-f", "mph_table_graph"};
        JCallGraph.main(args);
    }

    @Test
    public void testFinalAssertions(){

        // Git Stage
        Path mphJar = Paths.get("/artifacts/output/mph-table-1.0.6-SNAPSHOT.jar");
        Path mphTestJar = Paths.get("/artifacts/output/mph-table-1.0.6-SNAPSHOT-tests.jar");
        LOGGER.info("Starting Mph-Table Git Verification");
        assertTrue(Files.exists(mphJar));
        assertTrue(Files.exists(mphTestJar));

        // Build Stage
        Path mphGraph = Paths.get("mph_table_graph");
        LOGGER.info("Starting Mph-Table Build Verification");
        assertTrue(Files.exists(mphGraph));


        // Test Stage
        Path mphSmartByteSerializer = Paths.get("/output/TestSmartByteSerializer#canRoundTripBytes.dot");
        Path mphSmartByteSerializerReachability = Paths.get("/output/TestSmartByteSerializer#canRoundTripBytes-reachability.dot");
        Path mphSmartIntegerSerializer = Paths.get("/output/TestSmartIntegerSerializer#canRoundTripIntegers.dot");
        Path mphSmartIntegerSerializerReachability = Paths.get("/output/TestSmartIntegerSerializer#canRoundTripIntegers-reachability.dot");
        Path mphSmartListSerializer = Paths.get("/output/TestSmartListSerializer#canRoundTripSerializableLists.dot");
        Path mphSmartListSerializerReachability = Paths.get("/output/TestSmartListSerializer#canRoundTripSerializableLists-reachability.dot");
        Path mphSmartListSerializerWithGenerator = Paths.get("/output/TestSmartListSerializer#canRoundTripSerializableListsWithGenerator.dot");
        Path mphSmartListSerializerWithGeneratorReachability = Paths.get("/output/TestSmartListSerializer#canRoundTripSerializableListsWithGenerator-reachability.dot");
        Path mphSmartLongSerializer = Paths.get("/output/TestSmartLongSerializer#canRoundTripLongs.dot");
        Path mphSmartLongSerializerReachability = Paths.get("/output/TestSmartLongSerializer#canRoundTripLongs-reachability.dot");
        Path mphSmartPairSerializer = Paths.get("/output/TestSmartPairSerializer#canRoundTripPairs.dot");
        Path mphSmartPairSerializerReachability = Paths.get("/output/TestSmartPairSerializer#canRoundTripPairs-reachability.dot");
        Path mphSmartShortSerializer = Paths.get("/output/TestSmartShortSerializer#canRoundTripShort.dot");
        Path mphSmartShortSerializerReachability = Paths.get("/output/TestSmartShortSerializer#canRoundTripShort-reachability.dot");
        Path mphSmartStringSerializer = Paths.get("/output/TestSmartStringSerializer#canRoundTripStrings.dot");
        Path mphSmartStringSerializerReachability = Paths.get("/output/TestSmartStringSerializer#canRoundTripStrings-reachability.dot");
        assertTrue(Files.exists(mphSmartByteSerializer));
        assertTrue(Files.exists(mphSmartByteSerializerReachability));
        assertTrue(Files.exists(mphSmartIntegerSerializer));
        assertTrue(Files.exists(mphSmartIntegerSerializerReachability));
        assertTrue(Files.exists(mphSmartListSerializer));
        assertTrue(Files.exists(mphSmartListSerializerReachability));
        assertTrue(Files.exists(mphSmartListSerializerWithGenerator));
        assertTrue(Files.exists(mphSmartListSerializerWithGeneratorReachability));
        assertTrue(Files.exists(mphSmartLongSerializer));
        assertTrue(Files.exists(mphSmartLongSerializerReachability));
        assertTrue(Files.exists(mphSmartPairSerializer));
        assertTrue(Files.exists(mphSmartPairSerializerReachability));
        assertTrue(Files.exists(mphSmartShortSerializer));
        assertTrue(Files.exists(mphSmartShortSerializerReachability));
        assertTrue(Files.exists(mphSmartStringSerializer));
        assertTrue(Files.exists(mphSmartStringSerializerReachability));

    }
}
