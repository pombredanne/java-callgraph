package gr.gousiosg.javacg.stat.support;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitArguments {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitArguments.class);
    private static final String CONFIG_NAME = "c";
    private static final String CONFIG_NAME_LONG = "config";
    private static final String PROPERTY_NAME = "p";
    private static final String PROPERTY_NAME_LONG = "property";

    public GitArguments(String[] args){
        LOGGER.info("Parsing command line arguments...");
        CommandLineParser parser = new DefaultParser();
        Options options = getOptions();
    }

    private static Options getOptions() {
        Options options = new Options();

        options.addOption(
                Option.builder(CONFIG_NAME)
                        .longOpt(CONFIG_NAME_LONG)
                        .hasArg(true)
                        .desc("[REQUIRED] specify configuration to test for coverage")
                        .required(true)
                        .build());

        options.addOption(
                Option.builder(PROPERTY_NAME)
                .longOpt(PROPERTY_NAME_LONG)
                .hasArg(true)
                .desc("[REQUIRED] specify property to test for coverage")
                .required(true)
                .build());

        return options;
    }
}
