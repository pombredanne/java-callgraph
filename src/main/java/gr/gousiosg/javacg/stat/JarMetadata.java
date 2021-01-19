package gr.gousiosg.javacg.stat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.StringJoiner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class JarMetadata {

    private final JarFile jarFile;
    private final URLClassLoader cl;

    public JarMetadata(JarFile jarFile, URLClassLoader cl) {
        this.jarFile = jarFile;
        this.cl = cl;
    }

    public void load() throws IOException {
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
                Class<?> c = cl.loadClass(className);
                System.out.println("Inspecting " + c.getName());
                for (Method m : c.getMethods()) {

                    Class<?> declaringClass = m.getDeclaringClass();

                    String params = Arrays.stream(m.getParameterTypes())
                            .map(Class::getName)
                            .collect(Collectors.joining(","));

                    String methodSignature = m.getName() + "(" + params + ")";

                    System.out.println("\t"+ declaringClass + ":" + methodSignature);
                }

            } catch (ClassNotFoundException classNotFoundException) {
                classNotFoundException.printStackTrace();
            }
        }
    }


}
