package gr.gousiosg.javacg.stat.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClassHierarchyInspector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassHierarchyInspector.class);

    // memoize class -> method name -> return type -> method
    Map<Class<?>, Map<String, Map<String, Method>>> hierarchy = new HashMap<>();

    private void loadHierarchy(Class<?> clazz) {
        try {
            if (hierarchy.containsKey(clazz)) {
                return;
            } else {
                hierarchy.put(clazz, new HashMap<>());
            }

            Method[] declaredMethods = clazz.getDeclaredMethods();
            for (Method declaredMethod : declaredMethods) {
                String signature = methodSignature(declaredMethod);
                hierarchy.get(clazz).putIfAbsent(signature, new HashMap<>());
                String rt = declaredMethod.getReturnType().getCanonicalName();
                hierarchy.get(clazz).get(signature).put(rt, declaredMethod);
            }

            Optional<Class<?>> maybeParent = Optional.ofNullable(clazz.getSuperclass());
            maybeParent.ifPresent(this::loadHierarchy);
        } catch (Exception | NoClassDefFoundError e) {
            LOGGER.error("Trouble loading hierarchy for " + clazz.getName());
        }
    }

    public Optional<Method> getTopLevelSignature(Class<?> clazz, String methodSignature, String returnType) {
        try {
            loadHierarchy(clazz);
            while (clazz != null) {
                if (hierarchy.get(clazz).containsKey(methodSignature) && hierarchy.get(clazz).get(methodSignature).containsKey(returnType)) {
                    return Optional.of(
                            hierarchy.get(clazz).get(methodSignature).get(returnType)
                    );
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception | NoClassDefFoundError e) {
            LOGGER.error("Unable to find method " + methodSignature + " in class " + clazz.getName());
        }
        return Optional.empty();
    }

    public static String methodSignature(Method method) {
        String params = Arrays.stream(method.getParameterTypes())
                .map(Class::getName)
                .collect(Collectors.joining(","));
        return methodSignature(method.getName(), params);
    }

    public static String methodSignature(String name, String parameterTypes) {
        return name + "(" + parameterTypes + ")";
    }
}
