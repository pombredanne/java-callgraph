package gr.gousiosg.javacg.stat.support;

import org.apache.bcel.generic.Type;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class JarMetadata {

  private static final Logger LOGGER = LoggerFactory.getLogger(JarMetadata.class);

  private final URLClassLoader cl;
  private final Reflections reflections;
  private final ClassHierarchyInspector inspector = new ClassHierarchyInspector();

  /** Methods that are generated from the type erasure process */
  private final Set<String> bridgeMethods = new HashSet<>();

  /**
   * Methods that are created during {@link gr.gousiosg.javacg.stat.MethodVisitor#expand(Class,
   * String, Type[], Type, String)}
   */
  private final Set<String> dynamicMethods = new HashSet<>();

  /**
   * Methods result in a call to {@link gr.gousiosg.javacg.stat.MethodVisitor#expand(Class, String,
   * Type[], Type, String)}
   */
  private final Set<String> staticMethods = new HashSet<>();

  /**
   * Wrapper class used for reflection on class hierarchies
   *
   * @param cl the {@link ClassLoader} containing all provided jars
   * @param reflections the {@link Reflections} using the {@link cl} classloader
   */
  public JarMetadata(URLClassLoader cl, Reflections reflections) {
    this.cl = cl;
    this.reflections = reflections;
  }

  public Optional<Class<?>> getClass(String qualifiedName) {
    qualifiedName = qualifiedName.replace("/", ".");
    try {
      return Optional.of(Class.forName(qualifiedName, false, cl));
    } catch (NoClassDefFoundError | Exception e) {
      LOGGER.error("Unable to load class: " + qualifiedName);
      return Optional.empty();
    }
  }

  public Reflections getReflections() {
    return this.reflections;
  }

  public ClassHierarchyInspector getInspector() {
    return inspector;
  }

  public void addBridgeMethod(String methodSignature) {
    bridgeMethods.add(methodSignature);
  }

  public void addDynamicMethod(String methodSignature) {
    dynamicMethods.add(methodSignature);
  }

  public void addStaticMethod(String methodSignature) {
    staticMethods.add(methodSignature);
  }

  public boolean dynamicMethodsContains(String methodSignature) {
    return dynamicMethods.contains(methodSignature);
  }

  public boolean staticMethodsContains(String methodSignature) {
    return staticMethods.contains(methodSignature);
  }

  public Set<String> getBridgeMethods() {
    return new HashSet<>(bridgeMethods);
  }

  public Set<String> getDynamicMethods() {
    return new HashSet<>(dynamicMethods);
  }

  public Set<String> getStaticMethods() {
    return new HashSet<>(staticMethods);
  }
}
