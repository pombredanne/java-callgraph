package gr.gousiosg.javacg.stat.support.parsing;

import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JacocoCoverage {

    private static final Logger LOGGER = LoggerFactory.getLogger(JacocoCoverage.class);
    private static final String METHOD_TYPE_VALUE_NAME = "METHOD";
    private static final String LINE_TYPE_VALUE_NAME = "LINE";
    private static final String BRANCH_TYPE_VALUE_NAME = "BRANCH";

    private Map<String, Report.Package.Class.Method> methodCoverage = new HashMap<>();

    public JacocoCoverage(String filepath) throws IOException, ParserConfigurationException, JAXBException, SAXException {
        Report report = JacocoCoverageParser.getReport(filepath);

        /* Iterate all packages in report */
        for (Report.Package pkg : report.getPackage()) {

            /* Iterate all classes in a package */
            for (Report.Package.Class clazz : pkg.getClazz()) {

                /* Find all methods in a class */
                List<Report.Package.Class.Method> methods = clazz.getContent().stream()
                        .filter(s -> s instanceof JAXBElement)
                        .map(s -> (JAXBElement<?>) s)
                        .filter(je -> je.getValue() instanceof Report.Package.Class.Method)
                        .map(je -> (Report.Package.Class.Method) je.getValue())
                        .collect(Collectors.toList());

                /* Store "covered" methods in methodCoverage */
                methods.stream()
                        .filter(method ->
                            method.getCounter().stream().anyMatch(c ->
                                c.type.equals(METHOD_TYPE_VALUE_NAME) && (c.covered > 0))
                            )
                        .forEach(method -> {
                            String qualifiedName = qualifiedName(
                                    clazz.getName(),
                                    method.getName(),
                                    method.getDesc()
                            );
                            methodCoverage.putIfAbsent(qualifiedName, method);
                        });
            }
        }
    }

    private static String qualifiedName(String className, String methodName, String argDescriptors) {
        String argTypes = Arrays.stream(Type.getArgumentTypes(argDescriptors))
                .map(Type::getClassName)
                .collect(Collectors.joining(","));
        return "\"" + className.replace("/", ".") + ":" + methodName + "(" + argTypes + ")\"";
    }
}
