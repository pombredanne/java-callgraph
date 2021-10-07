package gr.gousiosg.javacg.stat.graph;

import gr.gousiosg.javacg.dyn.Pair;
import gr.gousiosg.javacg.stat.ClassVisitor;
import gr.gousiosg.javacg.stat.coverage.JacocoCoverage;
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

  /**
   * Builds a static callgraph from the provided jars
   *
   * @param jars the jars to inspect
   * @return a {@link Graph} representing the static callgraph of the combined jars
   * @throws InputMismatchException
   */
  public static Graph<String, DefaultEdge> build(
      List<Pair<String, File>> jars, JacocoCoverage coverage) throws InputMismatchException {
    LOGGER.info("Beginning callgraph analysis...");

    // 1. SETTING UP FOR GRAPH INSPECTION
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

    // 2. GRAPH INSPECTION
    /* iterate over all provided jars */
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
        inspectJarEntries(entries, jarPath, getClassVisitor, calls);

      } catch (IOException e) {
        LOGGER.error("Error when analyzing JAR \"" + jarPath + "\": + e.getMessage()");
        e.printStackTrace();
      }
    }

    /* Convert calls into a graph */
    Graph<String, DefaultEdge> graph = buildGraph(calls);

    // 3. GRAPH POSTPROCESSING
    Pruning.markConcreteBridgeTargets(graph, jarMetadata);
    Pruning.pruneBridgeMethods(graph, jarMetadata);
    Pruning.pruneConcreteMethods(graph, jarMetadata, coverage);

    return graph;
  }

  /**
   * Takes all (source, destination) method call pairs and stitches them together to create a graph
   *
   * @param methodCalls the method calls
   * @return a {@link Graph}
   * @throws InputMismatchException
   */
  private static Graph<String, DefaultEdge> buildGraph(Map<String, Set<String>> methodCalls)
      throws InputMismatchException {
    if (methodCalls.keySet().isEmpty()) {
      throw new InputMismatchException("There is no call graph to look at!");
    }

    Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
    methodCalls
        .keySet()
        .forEach(
            source -> {
              /* create source vertex */
              putIfAbsent(graph, source);

              methodCalls
                  .get(source)
                  .forEach(
                      destination -> {
                        /* create destination vertex */
                        putIfAbsent(graph, destination);

                        /* connect every (source, destination) pair with an edge */
                        graph.addEdge(source, destination);
                      });
            });

    return graph;
  }

  /**
   * Finds all (source, destination) method call pairs within a jar
   *
   * @param entries the entries of the jar
   * @param jarPath the path to the jar
   * @param getClassVisitor a {@link ClassVisitor}
   * @param calls the data structure containing all (source, destination) method call pairs
   */
  private static void inspectJarEntries(
      Stream<JarEntry> entries,
      String jarPath,
      Function<ClassParser, ClassVisitor> getClassVisitor,
      Map<String, Set<String>> calls) {

    /* Analyze each jar entry to find callgraph */
    entries
        .flatMap(
            e -> {
              /* Only inspect directories and `*.class` files */
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
  }
}
