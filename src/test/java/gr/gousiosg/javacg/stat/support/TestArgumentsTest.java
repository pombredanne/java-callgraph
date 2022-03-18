package gr.gousiosg.javacg.stat.support;

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class TestArgumentsTest {

    @Test
    public void testValidPath(){
        String[] args = {"test", "test", "-c", "mph-table", "-f", "mph_table_bin"};
        TestArguments ta = new TestArguments(args);
        Optional<String> config = ta.maybeGetConfig();
        Optional<String> bytecodeFile = ta.maybeBytecodeFile();
        Assert.assertEquals("mph-table", config.get());
        Assert.assertEquals("mph_table_bin", bytecodeFile.get());
    }

}
