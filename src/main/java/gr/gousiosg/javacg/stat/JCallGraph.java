/*
 * Copyright (c) 2011 - Georgios Gousios <gousiosg@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package gr.gousiosg.javacg.stat;

import gr.gousiosg.javacg.dyn.Pair;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Constructs a callgraph out of a JAR archive. Can combine multiple archives
 * into a single call graph.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
public class JCallGraph {

    private static final Logger LOGGER = LoggerFactory.getLogger(JCallGraph.class);
    private static final String JAR_SUFFIX = ".jar";
    private static final String DOT_SUFFIX = ".dot";

    public static void main(String[] args) {

        LOGGER.info("Parsing command line arguments...");

        /* Setup cmdline options */
        Options options = new Options();

        options.addOption(Option.builder("d")
                .longOpt("dot")
                .hasArg(true)
                .desc("[OPTIONAL] used to specify output for DOT file")
                .required(false)
                .build());

        options.addOption(Option.builder("e")
                .longOpt("entrypoint")
                .hasArg(true)
                .desc("[REQUIRED] describes entrypoint in a JAR")
                .required(true)
                .build());

        options.addOption(Option.builder("j")
                .longOpt("jar")
                .hasArg(true)
                .desc("[REQUIRED] one or more JARs to analyze")
                .required(true)
                .build());

        /* Setup argument variables */
        List<String> jarPaths = new ArrayList<>();
        String entryPoint = null;
        Optional<BufferedWriter> maybeOutfileWriter = Optional.empty();

        /* Parse cmdline arguments */
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);

            /* Parse dotfile */
            if (cmd.hasOption("d")) {
                String outfilePath = cmd.getOptionValue("d");
                if (!outfilePath.endsWith(DOT_SUFFIX))
                    throw new ParseException("Please provide a valid .dot output path!");
                try {
                    maybeOutfileWriter = Optional.of(new BufferedWriter(new FileWriter(outfilePath)));
                } catch (InvalidPathException | IOException e) {
                    throw new ParseException("Couldn't create file at " + outfilePath);
                }
            }

            /* Parse entry point*/
            if (cmd.hasOption("e")) {
                entryPoint = cmd.getOptionValue("e");
            }

            /* Parse JARs  */
            if (cmd.hasOption("j")) {
                jarPaths.addAll(Arrays.asList(cmd.getOptionValues("j")));
            }

        } catch (ParseException pe) {
            LOGGER.error("Error parsing command-line arguments: " + pe.getMessage());
            LOGGER.error("Please, follow the instructions below:");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "Log messages to sequence diagrams converter", options);
            System.exit(1);
        }

        List<Pair<String, File>> jars = jarPaths.stream()
                .map(path -> {
                    if (!path.endsWith(JAR_SUFFIX)) {
                        LOGGER.error("Path should end in file of type .jar!\n"
                                    + "---> " + path + " <---");
                        System.exit(1);
                    }

                    File file = new File(path);
                    if (!file.exists()) {
                        LOGGER.error("JAR Path " + path + " doesn't exist!");
                        System.exit(1);
                    }

                    return new Pair<>(path, file);
                })
                .collect(Collectors.toList());

        LOGGER.info("Beginning callgraph analysis...");
        GraphGenerator.staticCallgraph(jars, entryPoint, maybeOutfileWriter);
    }
}