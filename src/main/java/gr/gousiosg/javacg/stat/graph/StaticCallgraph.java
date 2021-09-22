package gr.gousiosg.javacg.stat.graph;

import gr.gousiosg.javacg.dyn.Pair;
import gr.gousiosg.javacg.stat.ClassVisitor;
import gr.gousiosg.javacg.stat.support.JarMetadata;
import org.apache.bcel.classfile.ClassParser;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static gr.gousiosg.javacg.stat.graph.Utilities.*;

public class StaticCallgraph {

  private static final Logger LOGGER = LoggerFactory.getLogger(StaticCallgraph.class);

  public static Graph<String, DefaultEdge> build(List<Pair<String, File>> jars)
      throws InputMismatchException {
    LOGGER.info("Beginning callgraph analysis...");

    /* Load JAR URLs */
    List<URL> urls = new ArrayList<>();
    try {
      for (Pair<String, File> pair : jars) {
        URL url = new URL("jar:file:" + pair.first + "!/");
        urls.add(url);
      }
    } catch (MalformedURLException e) {
      LOGGER.error("Error loading URLs: " + e.getMessage());
      throw new InputMismatchException("Couldn't load provided JARs");
    }

    if (urls.isEmpty()) {
      LOGGER.error("No URLs to scan!");
      throw new InputMismatchException("There are no URLs to scan!");
    }

    /* Setup infrastructure for analysis */
    URLClassLoader cl =
        URLClassLoader.newInstance(urls.toArray(new URL[0]), Utilities.class.getClassLoader());
    Reflections reflections = new Reflections(cl, new SubTypesScanner(false));
    JarMetadata jarMetadata = new JarMetadata(cl, reflections);

    /* Store method calls (caller -> receiver) */
    Map<String, Set<String>> calls = new HashMap<>();

    for (Pair<String, File> pair : jars) {
      String jarPath = pair.first;
      File file = pair.second;

      try (JarFile jarFile = new JarFile(file)) {
        LOGGER.info("Analyzing: " + jarFile.getName());
        Stream<JarEntry> entries = enumerationAsStream(jarFile.entries());

        Function<ClassParser, ClassVisitor> getClassVisitor =
            (ClassParser cp) -> {
              try {
                return new ClassVisitor(cp.parse(), jarMetadata);
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            };

        /* Analyze each jar entry to find callgraph */
        entries
            .flatMap(
                e -> {
                  if (e.isDirectory() || !e.getName().endsWith(".class")) return Stream.of();

                  /* Ignore specified JARs */
                  if (shouldIgnoreEntry(e.getName().replace("/", "."))) {
                    return Stream.of();
                  } else {
                    LOGGER.info("Inspecting " + e.getName());
                  }

                  ClassParser cp = new ClassParser(jarPath, e.getName());
                  return getClassVisitor.apply(cp).start().methodCalls().stream();
                })
            .forEach(
                p -> {
                  /* Create edges between nodes */
                  calls.putIfAbsent((p.first), new HashSet<>());
                  calls.get(p.first).add(p.second);
                });

      } catch (IOException e) {
        LOGGER.error("Error when analyzing JAR \"" + jarPath + "\": + e.getMessage()");
        e.printStackTrace();
      }
    }

    /* Convert calls into a graph */
    Graph<String, DefaultEdge> graph = buildGraph(calls);

    /* Prune bridge methods from graph */
    jarMetadata
        .getBridgeMethods()
        .forEach(
            bridgeMethod -> {

              /* Fetch the bridge method and make sure it has exactly one outgoing edge */
              String bridgeNode = formatNode(bridgeMethod);
              Optional<DefaultEdge> maybeEdge =
                  graph.outgoingEdgesOf(bridgeNode).stream().findFirst();

              if (graph.outDegreeOf(bridgeNode) != 1 || maybeEdge.isEmpty()) {

                graph.outgoingEdgesOf(bridgeNode).stream()
                    .forEach(
                        e -> {
                          LOGGER.error(
                              "\t" + graph.getEdgeSource(e) + " -> " + graph.getEdgeTarget(e));
                        });
                LOGGER.error(
                    "Found a bridge method that doesn't have exactly 1 outgoing edge: "
                        + bridgeMethod
                        + " : "
                        + graph.outDegreeOf(bridgeNode));
                System.exit(1);
              }

              /* Fetch the bridge method's target */
              String bridgeTarget = graph.getEdgeTarget(maybeEdge.get());

              /* Redirect all edges from the bridge method to its target */
              graph
                  .incomingEdgesOf(bridgeNode)
                  .forEach(
                      edge -> {
                        String sourceNode = graph.getEdgeSource(edge);
                        graph.addEdge(sourceNode, bridgeTarget);
                      });

              /* Remove the bridge method from the graph */
              graph.removeVertex(bridgeNode);
            });

    return graph;
  }

  private static Graph<String, DefaultEdge> buildGraph(Map<String, Set<String>> methodCalls)
      throws InputMismatchException {
    if (methodCalls.keySet().isEmpty()) {
      throw new InputMismatchException("There is no call graph to look at!");
    }

    /* initialize the graph */
    Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

    /* fill the graph with vertices and edges */
    methodCalls
        .keySet()
        .forEach(
            source -> {
              String sourceNode = formatNode(source);
              putIfAbsent(graph, sourceNode);

              methodCalls
                  .get(source)
                  .forEach(
                      destination -> {
                        String destinationNode = formatNode(destination);
                        putIfAbsent(graph, destinationNode);
                        graph.addEdge(sourceNode, destinationNode);
                      });
            });

    return graph;
  }
}
