package gr.gousiosg.javacg.stat.support;

import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class JacocoXMLParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(JacocoXMLParser.class);
    private static final String PACKAGE_TAG = "package";
    private static final String CLASS_TAG = "class";
    private static final String METHOD_TAG = "method";
    private static final String COUNTER_TAG = "counter";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String DESC_ATTRIBUTE = "desc";
    private static final String TYPE_ATTRIBUTE = "type";
    private static final String COVERED_ATTRIBUTE = "covered";
    private static final String METHOD_TYPE_VALUE_NAME = "METHOD";

    public static Set<String> parseCoverage(String path) throws IOException {
        LOGGER.info("Beginning to parse coverage...");
        File file = new File(path);

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

            /* --- https://stackoverflow.com/a/155874 --- */
            dbf.setValidating(false);
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://xml.org/sax/features/namespaces", false);
            dbf.setFeature("http://xml.org/sax/features/validation", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            /* --- without this, we get errors looking for .dtd files --- */

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(file);
            Element root = document.getDocumentElement();
            return inspectXML(root);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new IOException(e.getMessage());
        }
    }

    /* Inspect all packages in an XML file */
    public static Set<String> inspectXML(Element root) {
        Set<String> result = new HashSet<>();
        Set<Node> packages = new HashSet<>();

        /* Find packages */
        NodeList children = root.getElementsByTagName(PACKAGE_TAG);
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeName().equals(PACKAGE_TAG)) {
                packages.add(node);
            }
        }

        /* Inspect packages */
        for (Node packageNode : packages) {
            result.addAll(inspectPackage(packageNode));
        }
        return result;
    }

    /* Inspect all classes in a Java package */
    private static Set<String> inspectPackage(Node pkg) {
        Set<String> result = new HashSet<>();
        Set<Node> classesInPackage = new HashSet<>();

        /* Find classes */
        NodeList children = pkg.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals(CLASS_TAG)) {
                classesInPackage.add(child);
            }
        }

        /* Inspect classes */
        for (Node clazz : classesInPackage) {
            result.addAll(inspectClass(clazz));
        }
        return result;
    }

    /* Inspect all methods in a Java class */
    private static Set<String> inspectClass(Node clazz) {
        String clazzName = clazz.getAttributes().getNamedItem(NAME_ATTRIBUTE).getNodeValue();
        Set<String> result = new HashSet<>();
        Set<Node> methodsInClazz = new HashSet<>();

        /* Find methods */
        NodeList children = clazz.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals(METHOD_TAG)) {
                methodsInClazz.add(child);
            }
        }

        /* Inspect methods */
        for (Node method : methodsInClazz) {
            result.addAll(inspectMethod(clazzName, method));
        }
        return result;
    }

    /* Inspect a method to see if it was covered with JaCoCo or not */
    private static Set<String> inspectMethod(String className, Node method) {
        Set<String> result = new HashSet<>();
        String methodName = method.getAttributes().getNamedItem(NAME_ATTRIBUTE).getNodeValue();
        String args = method.getAttributes().getNamedItem(DESC_ATTRIBUTE).getNodeValue();
        String qualifiedName = qualifiedName(className, methodName, args);
        NodeList children = method.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals(COUNTER_TAG)) {
                if (child.getAttributes().getNamedItem(TYPE_ATTRIBUTE).getNodeValue().equals(METHOD_TYPE_VALUE_NAME)) {
                    String coveredValue = child.getAttributes().getNamedItem(COVERED_ATTRIBUTE).getNodeValue();
                    if (Integer.parseInt(coveredValue) > 0) {
                        result.add(qualifiedName);
                        break;
                    }
                }
            }
        }
        return result;
    }

    /* Return string of form "fully.qualified.class.name:method(arg_types)" */
    private static String qualifiedName(String className, String methodName, String argDescriptors) {
        String argTypes = Arrays.stream(Type.getArgumentTypes(argDescriptors))
                .map(Type::getClassName)
                .collect(Collectors.joining(","));
        return "\"" + className.replace("/", ".") + ":" + methodName + "(" + argTypes + ")\"";
    }
}
