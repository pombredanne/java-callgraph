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

    Map<Class<?>, Map<String, Method>> methodHierarchy = new HashMap<>();

    // TODO: Maybe we can remove our local storage and reduce this to use only the reflection API
    private void loadHierarchy(Class<?> clazz) {
        try {
            if (methodHierarchy.containsKey(clazz)) {
                return;
            } else {
                methodHierarchy.put(clazz, new HashMap<>());
            }

            Method[] declaredMethods = clazz.getDeclaredMethods();
            for (Method declaredMethod : declaredMethods) {
                String signature = methodSignature(declaredMethod);
                methodHierarchy.get(clazz).put(signature, declaredMethod);
            }

            // Load information from hierarchy
            Optional<Class<?>> maybeParent = Optional.ofNullable(clazz.getSuperclass());
            maybeParent.ifPresent(this::loadHierarchy);
        } catch (Exception | NoClassDefFoundError e) {
            LOGGER.error("Trouble loading hierarchy for " + clazz.getName());
        }
    }

    public Optional<Method> getTopLevelSignature(Class<?> clazz, String methodSignature) {
        try {
            loadHierarchy(clazz);
            while (clazz != null) {
                if (methodHierarchy.get(clazz).containsKey(methodSignature)) {
                    return Optional.of(
                            methodHierarchy.get(clazz).get(methodSignature)
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
