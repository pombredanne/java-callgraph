package inttest;

import gr.gousiosg.javacg.stat.support.RepoTool;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;

public class RepoToolIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(RepoToolIT.class);
    @Test
    public void testMphTable(){
        RepoTool repoTool = null;
        try {
            repoTool = new RepoTool("mph-table");
        }
        catch(FileNotFoundException e){
            Assert.fail("Repo Tool configuration file missing/undetected - mph-table");
            LOGGER.error("Could not find valid mph-table configuration file");
        }
        Assert.assertNotNull(repoTool);
        try{
            repoTool.cloneRepo();
        } catch (GitAPIException e) {
            Assert.fail("Error cloning mph-table repository");
            LOGGER.error("Could not clone mph-table");
        }
        try{
            repoTool.applyPatch();
        } catch (IOException | InterruptedException e) {
            Assert.fail("Error applying patch file");
            LOGGER.error("Failed applying patch file");
        }
        try{
            repoTool.buildJars();
        } catch (IOException | InterruptedException e) {
            Assert.fail("Error building jar files");
            LOGGER.error("Failed build jar/test jar files");
        }



    }
}
