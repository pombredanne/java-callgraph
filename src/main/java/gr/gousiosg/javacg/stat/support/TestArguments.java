package gr.gousiosg.javacg.stat.support;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TestArguments {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestArguments.class);

    private static final String XML_SUFFIX = ".xml";
    private static final String FILE_NAME = "f";
    private static final String FILE_NAME_LONG = "file";
    private static final String CONFIG_NAME = "c";
    private static final String CONFIG_NAME_LONG = "config";
    private static final String OUTPUT_NAME = "o";
    private static final String OUTPUT_NAME_LONG = "output";
    private static final String DEPTH_INPUT = "d";
    private static final String DEPTH_INPUT_LONG = "depth";
    private static final String COVERAGE_INPUT = "c";
    private static final String COVERAGE_INPUT_LONG = "coverage";
    private static final String ENTRYPOINT_INPUT = "e";
    private static final String ENTRYPOINT_INPUT_LONG = "entryPoint";
    private static final String ANCESTRY_INPUT = "a";
    private static final String ANCESTRY_INPUT_LONG = "ancestry";

    private Optional<String> bytecodeFile;
//    private Optional<String> maybeOutput = Optional.empty();
//    private Optional<String> maybeEntryPoint = Optional.empty();
//    private Optional<String> maybeCoverage = Optional.empty();
    private Optional<String> maybeConfig = Optional.empty();
    private Optional<Integer> maybeDepth = Optional.empty();
    private Optional<Integer> maybeAncestry = Optional.empty();

    public TestArguments(String[] args){
        LOGGER.info("Parsing command line arguments...");

        /* Setup cmdline argument parsing */
        CommandLineParser parser = new DefaultParser();
        Options options = getOptions();
        CommandLine cmd;

        try{
            cmd = parser.parse(options, args);

            /*Parse bytecode file */
            if (cmd.hasOption(FILE_NAME)){
                bytecodeFile = Optional.of(cmd.getOptionValue(FILE_NAME));
            }
//
//            /* Parse coverage file */
//            if (cmd.hasOption(COVERAGE_INPUT)) {
//                String coverageFile = cmd.getOptionValue(COVERAGE_INPUT);
//                if (!coverageFile.endsWith(XML_SUFFIX)) {
//                    LOGGER.error("File " + coverageFile + " must be an XML file!");
//                    System.exit(1);
//                }
//                maybeCoverage = Optional.of(coverageFile);
//            }
//
//            /* Parse entry point */
//            if (cmd.hasOption(ENTRYPOINT_INPUT)) {
//                String ep = cmd.getOptionValue(ENTRYPOINT_INPUT);
//                LOGGER.info("Entry Point: " + ep);
//                this.maybeEntryPoint = Optional.of(ep);
//            }
//
//            /* Parse output name */
//            if (cmd.hasOption(OUTPUT_NAME)) {
//                String name = cmd.getOptionValue(OUTPUT_NAME);
//
//                /* Validate output filename */
//                if (!name.matches("^[a-zA-Z0-9_]*$")) {
//                    LOGGER.error("---> " + name + " <---");
//                    LOGGER.error(
//                            "Please specify a valid name (letters, numbers, underscores) for the output. Do not include filetype!");
//                    System.exit(1);
//                }
//
//                this.maybeOutput = Optional.of(name);
//            }
            if(cmd.hasOption(CONFIG_NAME)){
                this.maybeConfig = Optional.of(cmd.getOptionValue(CONFIG_NAME));
            }
            /* Parse ancestry */
            if (cmd.hasOption(ANCESTRY_INPUT)) {
                String val = cmd.getOptionValue(ANCESTRY_INPUT);
                try {
                    this.maybeAncestry = Optional.of(Integer.parseInt(val));
                } catch (NumberFormatException e) {
                    LOGGER.error("---> " + val + " <---");
                    LOGGER.error("Please specify a valid integer for depth!");
                    System.exit(1);
                }
            }

            /* Parse depth */
            if (cmd.hasOption(DEPTH_INPUT)) {
                String val = cmd.getOptionValue(DEPTH_INPUT);
                try {
                    this.maybeDepth = Optional.of(Integer.parseInt(val));
                } catch (NumberFormatException e) {
                    LOGGER.error("---> " + val + " <---");
                    LOGGER.error("Please specify a valid integer for depth!");
                    System.exit(1);
                }
            }
        }
        catch(ParseException pe){
            LOGGER.error("Error parsing command-line arguments: " + pe.getMessage());
            LOGGER.error("Please, follow the instructions below:");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Log messages to sequence diagrams converter", options);
            System.exit(1);
        }

    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption(
                Option.builder(CONFIG_NAME)
                .longOpt(CONFIG_NAME_LONG)
                .hasArg(true)
                .desc("[REQUIRED] specify the config folder")
                .required(true)
                .build());

        options.addOption(
                Option.builder(FILE_NAME)
                        .longOpt(FILE_NAME_LONG)
                        .hasArg(true)
                        .desc("[REQUIRED] specify the bytecode file")
                        .required(true)
                        .build());

//        options.addOption(
//                Option.builder(OUTPUT_NAME)
//                        .longOpt(OUTPUT_NAME_LONG)
//                        .hasArg(true)
//                        .desc("[OPTIONAL] specify an output name for the graph")
//                        .required(false)
//                        .build());

        options.addOption(
                Option.builder(DEPTH_INPUT)
                        .longOpt(DEPTH_INPUT_LONG)
                        .hasArg(true)
                        .desc("[OPTIONAL] specify a depth to explore graph to")
                        .required(false)
                        .build());

//        options.addOption(
//                Option.builder(COVERAGE_INPUT)
//                        .longOpt(COVERAGE_INPUT_LONG)
//                        .hasArg(true)
//                        .desc("[OPTIONAL] specify the coverage to apply to the reachability graph")
//                        .required(false)
//                        .build());
//
//        options.addOption(
//                Option.builder(ENTRYPOINT_INPUT)
//                        .longOpt(ENTRYPOINT_INPUT_LONG)
//                        .hasArg(true)
//                        .desc("[OPTIONAL] specify an entry point into the graph")
//                        .required(false)
//                        .build());

        options.addOption(
                Option.builder(ANCESTRY_INPUT)
                        .longOpt(ANCESTRY_INPUT_LONG)
                        .hasArg(true)
                        .desc("[OPTIONAL] specify a depth to traverse the ancestry of an entrypoint")
                        .required(false)
                        .build());

        return options;
    }

    public Optional<String> maybeBytecodeFile(){ return bytecodeFile; }

//    public Optional<String> maybeOutput() {
//        return maybeOutput;
//    }

//    public Optional<String> maybeEntryPoint() {
//        return maybeEntryPoint;
//    }

    public Optional<Integer> maybeDepth() {
        return maybeDepth;
    }

//    public Optional<String> maybeCoverage() {
//        return maybeCoverage;
//    }

    public Optional<String> maybeGetConfig() {return maybeConfig;}
    public Optional<Integer> maybeAncestry() {
        return maybeAncestry;
    }
}
