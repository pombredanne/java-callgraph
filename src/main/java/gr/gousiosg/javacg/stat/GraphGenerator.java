package gr.gousiosg.javacg.stat;

import gr.gousiosg.javacg.dyn.Pair;
import org.apache.bcel.classfile.ClassParser;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class GraphGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphGenerator.class);
    private static final String GRAPH_BEGINNING = "graph callgraph {\n";
    private static final String GRAPH_ENDING = "}\n";

    public static void staticCallgraph(List<Pair<String, File>> jars, String entryPoint, Optional<BufferedWriter> maybeOutfileWriter) {
        if (jars.isEmpty()) {
            LOGGER.error("Oops! No JARs to look at! Goodbye!");
            return;
        }

        BufferedWriter writer = maybeOutfileWriter
                .orElse(new BufferedWriter(new OutputStreamWriter(System.out)));

        try {
            // TODO: Support multiple jars and loop through them
            String jarPath = jars.get(0).first;
            File file = jars.get(0).second;

            try (JarFile jar = new JarFile(file)) {

                /* Load JAR specific information */
                URL[] urls = { new URL("jar:file:" + jar.getName() +"!/") };
                URLClassLoader cl = URLClassLoader.newInstance(urls);

                Reflections reflections = new Reflections(entryPoint, cl, new SubTypesScanner(true));
                JarMetadata jarMetadata = new JarMetadata(jar, cl, reflections);

                Stream<JarEntry> entries = enumerationAsStream(jar.entries());

                Function<ClassParser, ClassVisitor> getClassVisitor =
                        (ClassParser cp) -> {
                            try {
                                return new ClassVisitor(cp.parse(), jarMetadata);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        };

                /* Collect callgraph strings of form (a -> b) */
                List<String> methodCalls = entries
                        .flatMap(e -> {
                            if (e.isDirectory() || !e.getName().endsWith(".class"))
                                return Stream.of();

                            ClassParser cp = new ClassParser(jarPath, e.getName());
                            return getClassVisitor.apply(cp).start().methodCalls().stream();
                        })
                        .collect(Collectors.toList());

                /* Write graph */
                LOGGER.info("Writing graph...");
                writeGraph(writer, methodCalls);
            }
        } catch (IOException e) {
            LOGGER.error("Error while processing jar: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                LOGGER.error("Oh no! The writer broke!");
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

    public static void writeGraph(Writer writer, List<String> methodCalls) {
        try {
            writer.write(GRAPH_BEGINNING);
            for (String methodCall : methodCalls) {
                writer.write(methodCall);
            }
            writer.write(GRAPH_ENDING);
        } catch (IOException e) {
            System.err.println("Unable to write graph!");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
