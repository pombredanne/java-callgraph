package inttest;

import gr.gousiosg.javacg.stat.JCallGraph;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MphTableIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(MphTableIT.class);

    @Test
    public void testGitCase(){
        String [] args = {"git", "-c", "mph-table"};
        JCallGraph.main(args);
    }
    @Test
    public void testBuildCase(){
        String [] args = {"build", "-j", "./artifacts/output/mph-table-1.0.6-SNAPSHOT.jar",
            "-t", "./artifacts/output/mph-table-1.0.6-SNAPSHOT-tests.jar", "-o", "mph_table_graph"};
        JCallGraph.main(args);
    }

    @Test
    public void testTestCase(){
        String [] args = {"test", "-c", "mph-table", "-f", "mph_table_graph"};
        JCallGraph.main(args);
    }
}
