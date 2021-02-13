package gr.gousiosg.javacg.stat;

import gr.gousiosg.javacg.dyn.Pair;
import gr.gousiosg.javacg.stat.support.IgnoredConstants;
import gr.gousiosg.javacg.stat.support.JarMetadata;
import gr.gousiosg.javacg.stat.support.coloring.ColoredNode;
import org.apache.bcel.classfile.ClassParser;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class GraphHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphHelper.class);

    public static Graph<ColoredNode, DefaultEdge> reachability(Graph<String, DefaultEdge> graph, String entrypoint, Optional<Integer> maybeMaximumDepth) {

        if (!graph.containsVertex(entrypoint)) {
            LOGGER.error("---> " + entrypoint + "<---");
            LOGGER.error("The graph doesn't contain the vertex specified as the entry point!");
            throw new InputMismatchException("graph doesn't contain vertex " + entrypoint);
        }

        if (maybeMaximumDepth.isPresent() && (maybeMaximumDepth.get() < 0)) {
            LOGGER.error("Depth " + maybeMaximumDepth.get() + " must be greater than 0!");
            System.exit(1);
        }

        LOGGER.info("Starting at entry point: " + entrypoint);
        maybeMaximumDepth.ifPresent(d -> LOGGER.info("Traversing to depth " + d));

        Graph<ColoredNode, DefaultEdge> subgraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        int currentDepth = 0;

        Deque<String> reachable = new ArrayDeque<>();
        reachable.push(entrypoint);

        Map<String, ColoredNode> subgraphNodes = new HashMap<>();
        Set<String> seenBefore = new HashSet<>();
        Set<String> nextLevel = new HashSet<>();

        while (!reachable.isEmpty()) {

            /* Stop once we've surpassed maximum depth */
            if (maybeMaximumDepth.isPresent() && (maybeMaximumDepth.get() < currentDepth)) {
                break;
            }

            while (!reachable.isEmpty()) {
                /* Visit reachable node */
                String source = reachable.pop();
                ColoredNode sourceNode = subgraphNodes.containsKey(source) ? subgraphNodes.get(source) : new ColoredNode(source);

                /* Keep track of who we've visited */
                seenBefore.add(source);
                if (!subgraphNodes.containsKey(source)) {
                    subgraph.addVertex(sourceNode);
                    subgraphNodes.put(source, sourceNode);
                }

                /* Check if we can add deeper edges or not */
                if (maybeMaximumDepth.isPresent() && (maybeMaximumDepth.get() == currentDepth)) {
                    break;
                }

                graph.edgesOf(source).forEach(edge -> {
                    String target = graph.getEdgeTarget(edge);
                    ColoredNode targetNode = subgraphNodes.containsKey(target) ? subgraphNodes.get(target) : new ColoredNode(target);

                    if (!subgraphNodes.containsKey(target)) {
                        subgraphNodes.put(target, targetNode);
                        subgraph.addVertex(targetNode);
                    }

                    if (graph.containsEdge(source, target) && !subgraph.containsEdge(sourceNode, targetNode)) {
                        subgraph.addEdge(sourceNode, targetNode);
                    }

                    /* Have we visited this vertex before? */
                    if (!seenBefore.contains(target)) {
                        nextLevel.add(target);
                        seenBefore.add(target);
                    }

                });
            }

            currentDepth++;
            reachable.addAll(nextLevel);
            nextLevel.clear();
        }

        subgraphNodes.get(entrypoint).markEntryPoint();
        return subgraph;
    }

    public static <T> void writeGraph(Graph<T, DefaultEdge> graph, DOTExporter<T, DefaultEdge> exporter, String outputName) {
        LOGGER.info("Attempting to store callgraph...");

        /* Write to .dot file in output directory */
        String path = "./output/" + outputName;
        try {
            Writer writer = new FileWriter(path);
            exporter.exportGraph(graph, writer);
            LOGGER.info("Graph written to " + path + "!");
        } catch (IOException e) {
            LOGGER.error("Unable to callgraph write to " + path);
        }
    }

    public static Graph<String, DefaultEdge> staticCallgraph(List<Pair<String, File>> jars) throws InputMismatchException {
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
        URLClassLoader cl = URLClassLoader.newInstance(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
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
                entries.flatMap(e -> {
                    if (e.isDirectory() || !e.getName().endsWith(".class"))
                        return Stream.of();

                    /* Ignore specified JARs */
                    if (shouldIgnoreEntry(e.getName().replace("/", "."))) {
                        return Stream.of();
                    } else {
                        LOGGER.info("Inspecting " + e.getName());
                    }

                    ClassParser cp = new ClassParser(jarPath, e.getName());
                    return getClassVisitor.apply(cp).start().methodCalls().stream();
                }).forEach(p -> {
                    /* Create edges between nodes */
                    calls.putIfAbsent((p.first), new HashSet<>());
                    calls.get(p.first).add(p.second);
                });

            } catch (IOException e) {
                LOGGER.error("Error when analyzing JAR \"" + jarPath + "\": + e.getMessage()");
                e.printStackTrace();
            }
        }

        return intoGraph(calls);
    }

    private static Graph<String, DefaultEdge> intoGraph(Map<String, Set<String>> calls) throws InputMismatchException {
        if (calls.keySet().isEmpty()) {
            throw new InputMismatchException("There is no call graph to look at!");
        }

        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        calls.keySet().forEach(k -> {
            // Add (k) node if not present
            if (!graph.containsVertex(formatNode(k)))
                graph.addVertex(formatNode(k));

            calls.get(k).forEach(v -> {
                // Add (v) node if not present
                if (!graph.containsVertex(formatNode(v)))
                    graph.addVertex(formatNode(v));

                // Edge is guaranteed not to be present
                graph.addEdge(formatNode(k), formatNode(v));
            });
        });

        return graph;
    }

    private static boolean shouldIgnoreEntry(String entry) {
        return IgnoredConstants.IGNORED_CALLING_PACKAGES.stream()
                .anyMatch(entry::startsWith);
    }

    private static String formatNode(String node) {
        return '"' + node + '"';
    }

    public static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
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
                        Spliterator.ORDERED), false);
    }

    public static DOTExporter<String , DefaultEdge> defaultExporter() {
        DOTExporter<String , DefaultEdge> exporter = new DOTExporter<>(id -> id);
        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v));
            return map;
        });
        return exporter;
    }

    public static DOTExporter<ColoredNode, DefaultEdge> coloredExporter() {
        DOTExporter<ColoredNode , DefaultEdge> exporter = new DOTExporter<>(ColoredNode::getLabel);
        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v.getLabel()));
            map.put("style", DefaultAttribute.createAttribute("filled"));
            map.put("fillcolor", DefaultAttribute.createAttribute(v.getColor()));
            return map;
        });
        return exporter;
    }

}
