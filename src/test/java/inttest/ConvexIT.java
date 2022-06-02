package inttest;

import gr.gousiosg.javacg.stat.JCallGraph;
import org.junit.Test;

public class ConvexIT {

    @Test
    public void testGitCase(){
        String [] args = {"git", "-c", "convex"};
        JCallGraph.main(args);
    }
    @Test
    public void testBuildCase(){
        String [] args = {"build", "-j", "./artifacts/output/convex-core-0.7.1.jar",
                "-t", "./artifacts/output/convex-core-0.7.1-tests.jar", "-o", "convex_core_graph"};
        JCallGraph.main(args);
    }

    @Test
    public void testTestCase(){
        String [] args = {"test", "-c", "convex", "-f", "convex_core_graph"};
        JCallGraph.main(args);
    }
}
