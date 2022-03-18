package gr.gousiosg.javacg.stat.support;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class RepoToolTest {
    @Test
    public void testValidMphTableConfig(){
        try {
            RepoTool rt = new RepoTool("mph-table");
            File repo = new File("mph-table");
            if(!repo.exists())
                rt.cloneRepo();
            rt.applyPatch();
            rt.buildJars();
            rt.testProperty("com.indeed.mph.serializers.TestSmartListSerializer.canRoundTripSerializableLists(Ljava/util/List;Ljava/util/List;Ljava/util/List;)V");
            rt.testProperty("com.indeed.mph.serializers.TestSmartListSerializer.canRoundTripSerializableListsWithGenerator(Ljava/util/List;Ljava/util/List;Ljava/util/List;)V");
            rt.cleanTarget();
        }
        catch(GitAPIException | IOException | InterruptedException exception){
            Assert.fail("Issue with build from valid config file!");
        }
    }

    @Test
    public void testValidConvexConfig(){
        try{
            RepoTool rt = new RepoTool("convex");
            File repo = new File("convex");
            if(!repo.exists())
                rt.cloneRepo();
            rt.applyPatch();
            rt.buildJars();


        } catch (GitAPIException | IOException | InterruptedException e) {
            Assert.fail("Issue with build from valid config file!");
        }
    }
}
