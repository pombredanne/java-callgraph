package gr.gousiosg.javacg.stat.graph;

import gr.gousiosg.javacg.stat.JCallGraph;
import gr.gousiosg.javacg.stat.coverage.ColoredNode;
import gr.gousiosg.javacg.stat.support.IgnoredConstants;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static gr.gousiosg.javacg.stat.graph.Constants.*;

public class Utilities {

  private static final Logger LOGGER = LoggerFactory.getLogger(Utilities.class);

  public static String formatNode(String node) {
    return NODE_DELIMITER + node + NODE_DELIMITER;
  }

  /**
   * Writes a graph to a file `name.dot`
   *
   * @param graph the graph
   * @param exporter the exporter that will write the graph to a file
   * @param maybeOutputName the name to use
   * @param <T> the type of the elements in the graph
   */
  public static <T> void writeGraph(
      Graph<T, DefaultEdge> graph,
      DOTExporter<T, DefaultEdge> exporter,
      Optional<String> maybeOutputName) {
    LOGGER.info("Attempting to store callgraph...");

    if (maybeOutputName.isEmpty()) {
      LOGGER.error("No output name specified!");
      return;
    }

    /* Write to .dot file in output directory */
    String path = JCallGraph.OUTPUT_DIRECTORY + maybeOutputName.get();
    try {
      Writer writer = new FileWriter(path);
      exporter.exportGraph(graph, writer);
      LOGGER.info("Graph written to " + path + "!");
    } catch (IOException e) {
      LOGGER.error("Unable to write callgraph to " + path);
    }
  }

  public static DOTExporter<String, DefaultEdge> defaultExporter() {
    DOTExporter<String, DefaultEdge> exporter = new DOTExporter<>(id -> id);
    exporter.setGraphAttributeProvider(defaultGraphAttributes());
    exporter.setVertexAttributeProvider(
        (v) -> {
          Map<String, Attribute> map = new LinkedHashMap<>();
          map.put(LABEL, DefaultAttribute.createAttribute(v));
          return map;
        });
    return exporter;
  }

  public static DOTExporter<ColoredNode, DefaultEdge> coloredExporter() {
    DOTExporter<ColoredNode, DefaultEdge> exporter = new DOTExporter<>(ColoredNode::getLabel);
    exporter.setGraphAttributeProvider(defaultGraphAttributes());
    exporter.setVertexAttributeProvider(
        (v) -> {
          Map<String, Attribute> map = new LinkedHashMap<>();
          map.put(LABEL, DefaultAttribute.createAttribute(v.getLabel()));
          map.put(STYLE, DefaultAttribute.createAttribute(FILLED));
          map.put(FILLCOLOR, DefaultAttribute.createAttribute(v.getColor()));
          return map;
        });
    return exporter;
  }

  protected static void putIfAbsent(Graph<String, DefaultEdge> graph, String vertex) {
    if (!graph.containsVertex(vertex)) {
      graph.addVertex(vertex);
    }
  }

  protected static boolean shouldIgnoreEntry(String entry) {
    return IgnoredConstants.IGNORED_CALLING_PACKAGES.stream().anyMatch(entry::startsWith);
  }

  protected static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(
            new Iterator<T>() {
              public T next() {
                return e.nextElement();
              }

              public boolean hasNext() {
                return e.hasMoreElements();
              }
            },
            Spliterator.ORDERED),
        false);
  }

  private static Supplier<Map<String, Attribute>> defaultGraphAttributes() {
    return () -> {
      Map<String, Attribute> map = new LinkedHashMap<>();
      map.put(
          RANK_VERTICAL_SEPARATION, DefaultAttribute.createAttribute(VERTICAL_SEPARATION_VALUE));
      map.put(RANK_DIRECTION, DefaultAttribute.createAttribute(LEFT_TO_RIGHT));
      return map;
    };
  }
}
