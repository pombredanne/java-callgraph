package gr.gousiosg.javacg.stat.support;

import gr.gousiosg.javacg.dyn.Pair;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class BuildArguments {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildArguments.class);
    private static final String JAR_SUFFIX = ".jar";
    private static final String JAR_INPUT = "j";
    private static final String JAR_INPUT_LONG = "jarPath";
    private static final String OUTPUT_NAME = "o";
    private static final String OUTPUT_NAME_LONG = "output";

    private final List<Pair<String, File>> jars = new ArrayList<>();
    private Optional<String> maybeOutput = Optional.empty();
    /**
     * Parse command line args into variables
     *
     * @param args the command line args
     */
    public BuildArguments(String[] args){
        LOGGER.info("Parsing command line arguments...");
        /* Setup cmdline argument parsing */
        Set<String> jarPaths = new HashSet<>();
        CommandLineParser parser = new DefaultParser();
        Options options = getOptions();
        CommandLine cmd;
        try{
            cmd = parser.parse(options, args);
            /* Parse JARs */
            if (cmd.hasOption(JAR_INPUT)) {
                jarPaths.addAll(Arrays.asList(cmd.getOptionValues(JAR_INPUT)));
            }
            /* Parse output name */
            if (cmd.hasOption(OUTPUT_NAME)) {
                String name = cmd.getOptionValue(OUTPUT_NAME);
                this.maybeOutput = Optional.of(name);
            }
        } catch (ParseException pe) {
            LOGGER.error("Error parsing command-line arguments: " + pe.getMessage());
            LOGGER.error("Please, follow the instructions below:");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Log messages to sequence diagrams converter", options);
            System.exit(1);
        }
        /* Transform jar paths into (path, file) pairs */
        jarPaths.stream()
                .map(
                        path -> {
                            if (!path.endsWith(JAR_SUFFIX)) {
                                LOGGER.error("---> " + path + " <---");
                                LOGGER.error("Path should end in file of type .jar!");
                                System.exit(1);
                            }

                            File file = new File(path);
                            if (!file.exists()) {
                                LOGGER.error("JAR Path " + path + " doesn't exist!");
                                System.exit(1);
                            }

                            LOGGER.info("Found JAR: " + path);
                            return new Pair<>(path, file);
                        })
                .forEach(this.jars::add);
    }

    private static Options getOptions() {
        Options options = new Options();

        options.addOption(
                Option.builder(JAR_INPUT)
                        .longOpt(JAR_INPUT_LONG)
                        .hasArg(true)
                        .desc("[REQUIRED] specify one or more paths to JARs to analyze")
                        .required(true)
                        .build());

        options.addOption(
                Option.builder(OUTPUT_NAME)
                        .longOpt(OUTPUT_NAME_LONG)
                        .hasArg(true)
                        .desc("[REQUIRED] specify an output name for the bytecode")
                        .required(true)
                        .build());
        return options;
    }

    public List<Pair<String, File>> getJars() { return jars; }

    public Optional<String> maybeOutput() {
        return maybeOutput;
    }
}
