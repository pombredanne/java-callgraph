package inttest;

import gr.gousiosg.javacg.stat.JCallGraph;
import gr.gousiosg.javacg.stat.graph.StaticCallgraph;
import gr.gousiosg.javacg.stat.support.BuildArguments;
import gr.gousiosg.javacg.stat.support.RepoTool;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;

public class MphTableIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(MphTableIT.class);
//    @Test
//    public void testGitCase(){
//        RepoTool repoTool = null;
//        try {
//            repoTool = new RepoTool("mph-table");
//        }
//        catch(FileNotFoundException e){
//            Assert.fail("Repo Tool configuration file missing/undetected - mph-table");
//            LOGGER.error("Could not find valid mph-table configuration file");
//        }
//        Assert.assertNotNull(repoTool);
//        try{
//            repoTool.cloneRepo();
//        } catch (GitAPIException e) {
//            Assert.fail("Error cloning mph-table repository");
//            LOGGER.error("Could not clone mph-table");
//        }
//        try{
//            repoTool.applyPatch();
//        } catch (IOException | InterruptedException e) {
//            Assert.fail("Error applying patch file");
//            LOGGER.error("Failed applying patch file");
//        }
//        try{
//            repoTool.buildJars();
//        } catch (IOException | InterruptedException e) {
//            Assert.fail("Error building jar files");
//            LOGGER.error("Failed build jar/test jar files");
//        }
//    }
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
