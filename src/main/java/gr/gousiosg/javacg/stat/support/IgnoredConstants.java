package gr.gousiosg.javacg.stat.support;

import java.util.List;

public class IgnoredConstants {

    public static final List<String> IGNORED_METHOD_NAMES = List.of(
            "<init>",
            "<clinit>"
    );

    public static final List<String> IGNORED_CALLING_PACKAGES = List.of(
            "java.",
            "javax.",
            "javassist.",
            "org.slf4j",
            "org.apache",
            "javassist.",
            "guru.",
            "com.kitfox",
            "org.reflections",
            "org.webjars",
            "net.arnx",
            "com.google",
            "io.cucumber",
            "org.hamcrest",
            "com.eclipsesource",
            "org.checkerframework",
            "org.jgrapht",
            "org.antlr",
            "org.jheaps",
            "org.jgrapht"
    );
}
