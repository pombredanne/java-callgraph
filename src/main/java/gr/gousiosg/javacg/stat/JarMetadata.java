package gr.gousiosg.javacg.stat;

import org.reflections.Reflections;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarMetadata {

    // TODO: Figure out a nice way to retain information from previous JARs while also
    //       being able to analyze different JARs
    //       -> Maybe we have to wrap a class around this that stores info from previous JARs

    // TODO: Investigate https://jgrapht.org/ to store/cache complex queries of class information

    // TODO: Analyze each class hierarchy and determine top-level method declarations

    private final JarFile jarFile;
    private final URLClassLoader cl;
    private final Reflections reflections;
    private final HashMap<String, Class<?>> nameToClass = new HashMap<>();

    public JarMetadata(JarFile jarFile, URLClassLoader cl, Reflections reflections) throws IOException {
        this.jarFile = jarFile;
        this.cl = cl;
        this.reflections = reflections;
        load();
    }

    private void load() throws IOException {
        Enumeration<JarEntry> e = jarFile.entries();

        while (e.hasMoreElements()) {
            JarEntry je = e.nextElement();
            if(je.isDirectory() || !je.getName().endsWith(".class")){
                continue;
            }

            // -6 because of .class
            String className = je.getName().substring(0,je.getName().length()-6);
            className = className.replace('/', '.');

            try {
                Class<?> clazz = cl.loadClass(className);
                if (nameToClass.containsKey(className)) {
                    throw new Error("Class already found!" + className);
                }
                nameToClass.put(className, clazz);
            } catch (ClassNotFoundException classNotFoundException) {
                classNotFoundException.printStackTrace();
            }
        }
    }

    public Optional<Class<?>> getClass(String qualifiedName) {
        return  Optional.ofNullable(nameToClass.get(qualifiedName));
    }

    public Reflections getReflections() {
        return this.reflections;
    }
}
