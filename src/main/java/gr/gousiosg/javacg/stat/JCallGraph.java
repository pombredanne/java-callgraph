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

import gr.gousiosg.javacg.stat.coverage.ColoredNode;
import gr.gousiosg.javacg.stat.coverage.CoverageStatistics;
import gr.gousiosg.javacg.stat.coverage.JacocoCoverage;
import gr.gousiosg.javacg.stat.graph.*;
import gr.gousiosg.javacg.stat.support.BuildArguments;
import gr.gousiosg.javacg.stat.support.RepoTool;
import gr.gousiosg.javacg.stat.support.TestArguments;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.InputMismatchException;
import java.util.Optional;

/**
 * Constructs a callgraph out of a JAR archive. Can combine multiple archives into a single call
 * graph.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Will Cygan <wcygan3232@gmail.com>
 * @author Alekh Meka <alekhmeka@gmail.com>
 */
public class JCallGraph {

    public static final String OUTPUT_DIRECTORY = "./output/";
    private static final Logger LOGGER = LoggerFactory.getLogger(JCallGraph.class);
    private static final String REACHABILITY = "reachability";
    private static final String COVERAGE = "coverage";
    private static final String ANCESTRY = "ancestry";
    private static final String DELIMITER = "-";
    private static final String DOT_SUFFIX = ".dot";
    private static final String CSV_SUFFIX = ".csv";

    public static void main(String[] args) {
        try {
            LOGGER.info("Starting java-cg!");
            switch (args[0]) {
                case "git": {
                    RepoTool rt = maybeObtainTool(args[1]);
                    rt.cloneRepo();
                    rt.applyPatch();
                    rt.buildJars();
                    rt.moveFiles();
                    break;
                }
                case "build": {
                    // Build and serialize a staticcallgraph object with jar files provided
                    BuildArguments arguments = new BuildArguments(args);
                    StaticCallgraph callgraph = StaticCallgraph.build(arguments);
                    maybeSerializeStaticCallGraph(callgraph, arguments);
                    break;
                }
                case "test": {
                    // 1. Deserialize callgraph
                    TestArguments arguments = new TestArguments(args);
                    StaticCallgraph callgraph = deserializeStaticCallGraph(arguments);
                    // 2. Get coverage
                    JacocoCoverage jacocoCoverage = new JacocoCoverage(arguments.maybeCoverage());
                    Pruning.pruneOriginalGraph(callgraph, jacocoCoverage, arguments);
                    maybeWriteGraph(callgraph.graph, arguments);
                    maybeInspectReachability(callgraph, arguments, jacocoCoverage);
                    maybeInspectAncestry(callgraph, arguments, jacocoCoverage);
                    break;
                }
                default:
                    LOGGER.error("Invalid argument provided!");
                    System.exit(1);
            }
        } catch (InputMismatchException e) {
            LOGGER.error("Unable to load callgraph: " + e.getMessage());
            System.exit(1);
        } catch (JGitInternalException e) {
            LOGGER.error("Cloned directory already exists!");
            System.exit(1);
        } catch (FileNotFoundException e) {
            LOGGER.error("Error obtaining valid yaml folder path: " + e.getMessage());
            System.exit(1);
        } catch (ParserConfigurationException | SAXException | JAXBException | IOException e) {
            LOGGER.error("Error fetching Jacoco coverage: " + e.getMessage());
            System.exit(1);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Error creating class through deserialization");
            System.exit(1);
        } catch (GitAPIException e) {
            LOGGER.error("Error cloning repository");
            System.exit(1);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted during applying patches/building jars");
            System.exit(1);
        }

        LOGGER.info("java-cg is finished! Enjoy!");
    }

    private static void maybeWriteGraph(Graph<String, DefaultEdge> graph, TestArguments arguments) {
        if (arguments.maybeOutput().isPresent()) {
            Utilities.writeGraph(
                    graph, Utilities.defaultExporter(), arguments.maybeOutput().map(JCallGraph::asDot));
        }
    }

    private static void maybeInspectReachability(
            StaticCallgraph callgraph, TestArguments arguments, JacocoCoverage jacocoCoverage) {
        if (arguments.maybeEntryPoint().isEmpty()) {
            return;
        }

        /* Fetch reachability */
        Graph<ColoredNode, DefaultEdge> reachability =
                Reachability.compute(
                        callgraph.graph, arguments.maybeEntryPoint().get(), arguments.maybeDepth());

        /* Apply coverage */
        jacocoCoverage.applyCoverage(reachability, callgraph.metadata);

        Pruning.pruneReachabilityGraph(reachability, callgraph.metadata, jacocoCoverage, arguments);

        /* Should we write the graph to a file? */
        Optional<String> outputName =
                arguments.maybeOutput().isPresent()
                        ? Optional.of(arguments.maybeOutput().get() + DELIMITER + REACHABILITY)
                        : Optional.empty();

        /* Attach depth to name if present */
        outputName =
                outputName.map(
                        name -> {
                            if (arguments.maybeDepth().isPresent()) {
                                return name + DELIMITER + arguments.maybeDepth().get();
                            } else {
                                return name;
                            }
                        });

        /* Store reachability in file? */
        if (outputName.isPresent()) {
            Utilities.writeGraph(
                    reachability, Utilities.coloredExporter(), outputName.map(JCallGraph::asDot));
        }

        /* Analyze reachability coverage? */
        if (jacocoCoverage.hasCoverage()) {
            CoverageStatistics.analyze(
                    reachability, outputName.map(name -> asCsv(name + DELIMITER + COVERAGE)));
        }
    }

    private static void maybeInspectAncestry(
            StaticCallgraph callgraph, TestArguments arguments, JacocoCoverage jacocoCoverage) {
        if (arguments.maybeAncestry().isEmpty() || arguments.maybeEntryPoint().isEmpty()) {
            return;
        }

        Graph<ColoredNode, DefaultEdge> ancestry =
                Ancestry.compute(
                        callgraph.graph, arguments.maybeEntryPoint().get(), arguments.maybeAncestry().get());
        jacocoCoverage.applyCoverage(ancestry, callgraph.metadata);

        /* Should we store the ancestry in a file? */
        if (arguments.maybeOutput().isPresent()) {
            String subgraphOutputName =
                    arguments.maybeOutput().get()
                            + DELIMITER
                            + ANCESTRY
                            + DELIMITER
                            + arguments.maybeAncestry().get();
            Utilities.writeGraph(
                    ancestry, Utilities.coloredExporter(), Optional.of(asDot(subgraphOutputName)));
        }
    }

    private static String asDot(String name) {
        return name.endsWith(DOT_SUFFIX) ? name : (name + DOT_SUFFIX);
    }

    private static String asCsv(String name) {
        return name.endsWith(CSV_SUFFIX) ? name : (name + CSV_SUFFIX);
    }

    //
    // serializeStaticCallGraph creates a file that contains the bytecode data of the StaticCallgraph object
    // Throws: IOException when the file cannot be written to disk
    private static void maybeSerializeStaticCallGraph(StaticCallgraph callgraph, BuildArguments arguments) throws IOException {
        if (arguments.maybeOutput().isPresent()) {
            File filename = new File(arguments.maybeOutput().get());
            FileOutputStream file = new FileOutputStream(filename);
            ObjectOutputStream out = new ObjectOutputStream(file);
            out.writeObject(callgraph);
            out.close();
            file.close();
        }
    }

    //
    // deserializeStaticCallGraph reads bytecode and creates a StaticCallgraph object to be returned
    // Throws: IOException when file cannot be read
    // Throws: ClassNotFoundException when object cannot be read properly
    private static StaticCallgraph deserializeStaticCallGraph(TestArguments arguments) throws IOException, ClassNotFoundException {
        File filename = new File(arguments.getBytecodeFile());
        FileInputStream file = new FileInputStream(filename);
        ObjectInputStream ois = new ObjectInputStream(file);
        StaticCallgraph scg = (StaticCallgraph) ois.readObject();
        ois.close();
        file.close();
        return scg;
    }

    private static RepoTool maybeObtainTool(String folderName) throws FileNotFoundException {
        Optional<RepoTool> rt = RepoTool.obtainTool(folderName);
        if (rt.isPresent())
            return rt.get();
        throw new FileNotFoundException("folderName path is incorrect! Please provide a valid folder");
    }
}
