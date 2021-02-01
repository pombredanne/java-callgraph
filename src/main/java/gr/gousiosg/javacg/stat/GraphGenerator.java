package gr.gousiosg.javacg.stat;

import gr.gousiosg.javacg.dyn.Pair;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import org.apache.bcel.classfile.ClassParser;
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
import java.util.stream.StreamSupport;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;

public class GraphGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphGenerator.class);

    public static void staticCallgraph(List<Pair<String, File>> jars, String outputName) {
        List<URL> urls = new ArrayList<>();

        try {
            for (Pair<String, File> pair : jars) {
                URL url = new URL("jar:file:" + pair.first + "!/");
                urls.add(url);
            }
        } catch (MalformedURLException e) {
            LOGGER.error("Error loading URLs: " + e.getMessage());
            return;
        }

        if (urls.isEmpty()) {
            LOGGER.error("No URLs to scan!");
            return;
        }

        /* Setup infrastructure for analysis */
        URLClassLoader cl = URLClassLoader.newInstance(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
        Reflections reflections = new Reflections(cl, new SubTypesScanner(false));
        JarMetadata jarMetadata = new JarMetadata(cl, reflections);

        /* Store graph */
        Set<Pair<String, String>> methodCalls = new HashSet<>();

        try {
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

                    /* Analyze each jar entry */
                    entries.flatMap(e -> {
                        if (e.isDirectory() || !e.getName().endsWith(".class"))
                            return Stream.of();

                        ClassParser cp = new ClassParser(jarPath, e.getName());
                        return getClassVisitor.apply(cp).start().methodCalls().stream();
                    }).forEach(methodCalls::add);

                } catch (IOException e) {
                    LOGGER.error("Error when analyzing JAR \"" + jarPath + "\": + e.getMessage()");
                    e.printStackTrace();
                }
            }
        } finally {
            if (methodCalls.isEmpty()) {
                LOGGER.error("No method calls to look at!");
            } else {


                methodCalls.forEach(pair -> {
                    LOGGER.info(pair.first + " -> " + pair.second);
                });

//                LOGGER.info("Adding edges to the graph...");
//                MutableGraph graph = mutGraph(outputName).setDirected(true);
//                methodCalls.forEach(pair -> graph.add(mutNode(pair.first).addLink(mutNode(pair.second))));
//                try {
//                    Graphviz.fromGraph(graph).render(Format.PNG).toFile(new File("./output/" + outputName + ".png"));
//                    Graphviz.fromGraph(graph).render(Format.DOT).toFile(new File("./output/" + outputName + ".dot"));
//                } catch (IOException e) {
//                    LOGGER.error("Trouble writing graph: " + e.getMessage());
//                }
            }
        }
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
}
