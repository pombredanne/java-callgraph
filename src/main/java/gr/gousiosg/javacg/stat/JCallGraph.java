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
import gr.gousiosg.javacg.stat.support.Arguments;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Optional;

/**
 * Constructs a callgraph out of a JAR archive. Can combine multiple archives into a single call
 * graph.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Will Cygan <wcygan3232@gmail.com>
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
      Arguments arguments = new Arguments(args);

      // 1. Build the graph
      StaticCallgraph callgraph = StaticCallgraph.build(arguments.getJars());

      // 2. Get coverage
      JacocoCoverage jacocoCoverage = new JacocoCoverage(arguments.maybeCoverage());

      // 3. Prune the graph with coverage
      Pruning.prune(callgraph, jacocoCoverage);

      // 4. Operate on the graph and write it to output
      maybeWriteGraph(callgraph.graph, arguments);
      maybeInspectReachability(callgraph, arguments, jacocoCoverage);
      maybeInspectAncestry(callgraph, arguments, jacocoCoverage);
    } catch (InputMismatchException e) {
      LOGGER.error("Unable to load callgraph: " + e.getMessage());
      System.exit(1);
    } catch (ParserConfigurationException | SAXException | JAXBException | IOException e) {
      LOGGER.error("Error fetching Jacoco coverage");
      System.exit(1);
    }

    LOGGER.info("java-cg is finished! Enjoy!");
  }

  private static void maybeWriteGraph(Graph<String, DefaultEdge> graph, Arguments arguments) {
    if (arguments.maybeOutput().isPresent()) {
      Utilities.writeGraph(
          graph, Utilities.defaultExporter(), arguments.maybeOutput().map(JCallGraph::asDot));
    }
  }

  private static void maybeInspectReachability(
      StaticCallgraph callgraph, Arguments arguments, JacocoCoverage jacocoCoverage) {
    if (arguments.maybeEntryPoint().isEmpty()) {
      return;
    }

    /* Fetch reachability */
    Graph<ColoredNode, DefaultEdge> reachability =
        Reachability.compute(
            callgraph.graph, arguments.maybeEntryPoint().get(), arguments.maybeDepth());

    /* Apply coverage */
    jacocoCoverage.applyCoverage(reachability, callgraph.metadata);

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
      StaticCallgraph callgraph, Arguments arguments, JacocoCoverage jacocoCoverage) {
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
}
