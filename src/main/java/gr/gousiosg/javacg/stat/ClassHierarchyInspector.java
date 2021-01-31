package gr.gousiosg.javacg.stat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClassHierarchyInspector {

    Map<Class<?>, Map<String, Method>> methodHierarchy = new HashMap<>();

    private void loadHierarchy(Class<?> clazz) {

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

    }

    public Optional<Method> getTopLevelSignature(Class<?> clazz, String methodSignature) {
        loadHierarchy(clazz);

        while (clazz != null) {
            if (methodHierarchy.get(clazz).containsKey(methodSignature)) {
                return Optional.of(
                        methodHierarchy.get(clazz).get(methodSignature)
                );
            }
            clazz = clazz.getSuperclass();
        }

        return Optional.empty();
    }

    public Map<String, Method> fetchHierarchy(Class<?> clazz) {
        loadHierarchy(clazz);

        Map<String, Method> myHierarchy = new HashMap<>();

        // Can exclude object class here if needed
        while (clazz != null) {

            methodHierarchy.get(clazz).values()
                    .forEach(m -> {
                        String signature = methodSignature(m);
                        myHierarchy.putIfAbsent(signature, m);
                    });

            clazz = clazz.getSuperclass();
        }

        return myHierarchy;
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
