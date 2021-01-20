package gr.gousiosg.javacg.stat;

import org.apache.bcel.classfile.ClassParser;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

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
    public void staticCallgraph(String jarName, String entryPoint) {
        BufferedWriter log = new BufferedWriter(new OutputStreamWriter(System.out));

        try {
            File f = new File(jarName);
            if (!f.exists()) {
                System.err.println("Jar file " + jarName + " does not exist");
            }

            try (JarFile jar = new JarFile(f)) {
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

                List<String> methodCalls = entries
                        .flatMap(e -> {
                            if (e.isDirectory() || !e.getName().endsWith(".class"))
                                return (new ArrayList<String>()).stream();

                            ClassParser cp = new ClassParser(jarName, e.getName());
                            return getClassVisitor.apply(cp).start().methodCalls().stream();
                        })
                        .collect(Collectors.toList());

                methodCalls.forEach(System.out::println);

//                log.write("graph callgraph {\n");
//                log.write(methodCalls);
//                log.write("}\n");
                }
        } catch (IOException e) {
            System.err.println("Error while processing jar: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                log.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Oops the logger broke");
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
