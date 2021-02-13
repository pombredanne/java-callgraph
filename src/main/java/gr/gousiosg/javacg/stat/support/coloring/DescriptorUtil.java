package gr.gousiosg.javacg.stat.support.coloring;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/* From: https://github.com/joewhaley/joeq/blob/master/joeq_core/joeq/Util/DescriptorUtil.java */
public class DescriptorUtil {
    private static Map _paramTypeMap = new HashMap();

    private static Map _returnTypeMap = new HashMap();

    static {
        _paramTypeMap.put("byte", "B");
        _paramTypeMap.put("char", "C");
        _paramTypeMap.put("double", "D");
        _paramTypeMap.put("float", "F");
        _paramTypeMap.put("int", "I");
        _paramTypeMap.put("long", "J");

        //todo: make generic...look for 'dots' of package. that algorithm doesn't handle
        // packageless (default package)
        // classes though..
        _paramTypeMap.put("java.lang.Object", "Ljava/lang/Object;");
        _paramTypeMap.put("short", "S");
        _paramTypeMap.put("boolean", "Z");

        //todo
        _paramTypeMap.put("array reference", "[");
        _returnTypeMap.put("void", "V");
    }

    /**
     * Converts from the Java/Javadoc method signature the JVM spec format.
     *
     * @param javadocSig        method signature as returned via Javadoc API.
     * @param javadocReturnType return type as returned via Javadoc API.
     * @return mtehod signature as defined in the JVM spec.
     */
    public static String convert(String javadocSig, String javadocReturnType) {
        //remove the leading and trailing parens
        String javadocSigTrim = javadocSig.substring(1, javadocSig.length() - 1);
        StringTokenizer st = new StringTokenizer(javadocSigTrim, ",");
        StringBuffer jvmBuff = new StringBuffer("(");
        while (st.hasMoreTokens()) {
            //remove the leading space character.
            String sigElement = st.nextToken().trim();
            if (_paramTypeMap.containsKey(sigElement)) {
                jvmBuff.append(_paramTypeMap.get(sigElement));
            }
        }
        jvmBuff.append(")");
        if (_returnTypeMap.containsKey(javadocReturnType)) {
            jvmBuff.append(_returnTypeMap.get(javadocReturnType));
        }
        return jvmBuff.toString();
    }

    /**
     * Convert a JVM signature as defined in the JVM spec to that used in the Java.
     *
     * @param jvmSignature The JVM format signature.
     * @return a <code>String[]</code> containing the method parameter as elements of the array.
     */
    public static String[] getParameters(final String jvmSignature) {
        int i = 0;
        if (jvmSignature.charAt(i) != '(') {
            return null;
        }
        int j = 0;
        StringBuffer stringbuffer = new StringBuffer();
        for (i++; i < jvmSignature.length();) {
            if (jvmSignature.charAt(i) == ')') {
                i++;
                break; //we are at the end of the signature.
            }
            if (i > 1) {
                //put in spaces to later tokenize on.
                stringbuffer.append(" ");
            }
            i = jvmFormatToJavaFormat(jvmSignature, i, stringbuffer);

            //count number of elements parsed.
            j++;
        }

        //convert to string array.
        String convertedString = stringbuffer.toString();
        String[] as = new String[j];
        int k = 0;
        StringTokenizer st = new StringTokenizer(convertedString);
        while (st.hasMoreTokens()) {
            as[k++] = st.nextToken();
        }
        return as;
    }

    /**
     * The utility method that does the real work of parsing through the JVM formatted string and adding an converted
     * method parameter description to the StringBuffer.
     *
     * @param jvmFormat    The JVM formatted string that is being parsed.
     * @param i            The offset into the string being parsed.
     * @param stringbuffer The storage for building the converted method signature.
     * @return new offset location for parsing.
     * @TODO this an extremely ugly method (the int an stringbuffer params must be removed)
     */
    public static int jvmFormatToJavaFormat(final String jvmFormat, int i, StringBuffer stringbuffer) {;
        String s1 = "";

        //arrays.
        for (; jvmFormat.charAt(i) == '['; i++) {
            s1 = s1 + "[]";
        }
        startover: switch (jvmFormat.charAt(i)) {
            case 66: // 'B'
                stringbuffer.append("byte");
                break;
            case 67: // 'C'
                stringbuffer.append("char");
                break;
            case 68: // 'D'
                stringbuffer.append("double");
                break;
            case 70: // 'F'
                stringbuffer.append("float");
                break;
            case 73: // 'I'
                stringbuffer.append("int");
                break;
            case 74: // 'J'
                stringbuffer.append("long");
                break;
            case 83: // 'S'
                stringbuffer.append("short");
                break;
            case 90: // 'Z'
                stringbuffer.append("boolean");
                break;
            case 86: // 'V'
                stringbuffer.append("void");
                break;
            case 76: // 'L'

                //special case for objects.
                for (i++; i < jvmFormat.length(); i++) {
                    if (jvmFormat.charAt(i) == '/') {
                        //convert to period
                        stringbuffer.append('.');
                    } else {
                        if (jvmFormat.charAt(i) == ';') {
                            //we reached the end
                            break startover;
                        }

                        //copy contents.
                        stringbuffer.append(jvmFormat.charAt(i));
                    }
                }
                break;
            default:
                return jvmFormat.length();
        }
        stringbuffer = stringbuffer.append(s1);
        return ++i;
    }
}