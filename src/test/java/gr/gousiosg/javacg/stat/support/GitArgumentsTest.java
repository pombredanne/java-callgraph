package gr.gousiosg.javacg.stat.support;

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class GitArgumentsTest {

    @Test
    public void testMphTableShortName(){
        String[] args = {"git", "-c", "mph-table"};
        GitArguments ga = new GitArguments(args);
        Optional<String> config = ga.maybeGetConfig();
        Assert.assertEquals(config.get(), "mph-table");
    }

    @Test
    public void testMphTableLongName(){
        String[] args = {"git", "-config", "mph-table"};
        GitArguments ga = new GitArguments(args);
        Optional<String> config = ga.maybeGetConfig();
        Assert.assertEquals(config.get(), "mph-table");
    }

}
