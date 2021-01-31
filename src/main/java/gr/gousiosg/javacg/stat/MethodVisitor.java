/*
 * Copyright (c) 2011 - Georgios Gousios <gousiosg@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package gr.gousiosg.javacg.stat;

import gr.gousiosg.javacg.dyn.Pair;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The simplest of method visitors, prints any invoked method
 * signature for all method invocations.
 * 
 * Class copied with modifications from CJKM: http://www.spinellis.gr/sw/ckjm/
 */
public class MethodVisitor extends EmptyVisitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodVisitor.class);

    private static final String INIT = "<init>";
    private static final List<String> IGNORED_METHOD_NAMES = List.of(INIT);
    private static final Boolean EXPAND = true;
    private static final Boolean DONT_EXPAND = true;

    // TODO move these to a constants file
    private static final String JAVA= "java.";
    private static final String JAVAX= "javax.";
    private static final String JAVA_ASSIST = "javassist.";
    private static final String SLF4J = "org.slf4j";
    private static final String APACHE = "org.apache";
    private static final String REFLECTIONS = "org.reflections";
    private static final String GURU = "guru.";
    private static final String KITFOX = "com.kitfox";
    private static final String WEBJARS = "org.webjars";
    private static final String ARXN = "net.arnx";
    private static final String GOOGLE = "com.google";
    private static final String CUCUMBER = "io.cucumber";
    private static final String HAMCREST = "org.hamcrest";
    private static final String ECLIPSE = "com.eclipsesource";

    private static final List<String> IGNORED_CALLING_PACKAGES = List.of(
            JAVA,
            JAVAX,
            REFLECTIONS,
            SLF4J, APACHE,
            JAVA_ASSIST,
            GURU,
            KITFOX,
            WEBJARS,
            ARXN,
            GOOGLE,
            CUCUMBER,
            HAMCREST,
            ECLIPSE
    );

    JavaClass visitedClass;
    private MethodGen mg;
    private ConstantPoolGen cp;
    private String format;
    private Set<Pair<String, String>> methodCalls = new HashSet<>();
    private final JarMetadata jarMetadata;

    public MethodVisitor(MethodGen m, JavaClass jc, JarMetadata jarMetadata) {
        this.jarMetadata = jarMetadata;
        visitedClass = jc;
        mg = m;
        cp = mg.getConstantPool();
        format = "%s";
    }

    private String argumentList(Type[] arguments) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arguments.length; i++) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(arguments[i].toString());
        }
        return sb.toString();
    }

    public Set<Pair<String, String>> start() {
        if (mg.isAbstract() || mg.isNative())
            return Collections.emptySet();

        for (InstructionHandle ih = mg.getInstructionList().getStart(); 
                ih != null; ih = ih.getNext()) {
            Instruction i = ih.getInstruction();
            
            if (!visitInstruction(i))
                i.accept(this);
        }
        return methodCalls;
    }

    private boolean visitInstruction(Instruction i) {
        short opcode = i.getOpcode();
        return ((InstructionConst.getInstruction(opcode) != null)
                && !(i instanceof ConstantPushInstruction) 
                && !(i instanceof ReturnInstruction));
    }

    @Override
    public void visitINVOKEVIRTUAL(INVOKEVIRTUAL i) {
        visit(String.format(format,i.getReferenceType(cp)), i.getMethodName(cp), argumentList(i.getArgumentTypes(cp)), EXPAND);
    }

    @Override
    public void visitINVOKEINTERFACE(INVOKEINTERFACE i) {
        visit(String.format(format,i.getReferenceType(cp)), i.getMethodName(cp), argumentList(i.getArgumentTypes(cp)), EXPAND);
    }

    @Override
    public void visitINVOKESPECIAL(INVOKESPECIAL i) {
        // Don't expand this
        visit(String.format(format,i.getReferenceType(cp)), i.getMethodName(cp), argumentList(i.getArgumentTypes(cp)), DONT_EXPAND);
    }

    @Override
    public void visitINVOKESTATIC(INVOKESTATIC i) {
        // Don't expand this
        visit(String.format(format,i.getReferenceType(cp)), i.getMethodName(cp), argumentList(i.getArgumentTypes(cp)), DONT_EXPAND);
    }

    @Override
    public void visitINVOKEDYNAMIC(INVOKEDYNAMIC i) {
        visit(String.format(format,i.getReferenceType(cp)), i.getMethodName(cp), argumentList(i.getArgumentTypes(cp)), EXPAND);
    }


    private void visit(String receiverTypeName, String receiverMethodName, String receiverArgTypeNames, Boolean shouldExpand) {

        if (IGNORED_CALLING_PACKAGES.stream().anyMatch(pkg -> visitedClass.getClassName().contains(pkg))) {
            return;
        }

        String fromSignature = fullyQualifiedMethodSignature(visitedClass.getClassName(), mg.getName(), argumentList(mg.getArgumentTypes()));
        String toSignature = fullyQualifiedMethodSignature(receiverTypeName, receiverMethodName, receiverArgTypeNames);
        methodCalls.add(createEdge(fromSignature, toSignature));

        if (shouldExpand && !IGNORED_METHOD_NAMES.contains(receiverMethodName)) {
            Optional<Class<?>> maybeReceiverClass = jarMetadata.getClass(receiverTypeName);
            if (maybeReceiverClass.isEmpty()) {
                LOGGER.info("Error from: " + fromSignature + " -> " + toSignature);
                LOGGER.error("\tCouldn't find " + receiverTypeName);
                return;
            }

            expand(maybeReceiverClass.get(), receiverMethodName, receiverArgTypeNames, fromSignature);
        }
    }

    private void expand(Class<?> receiver, String receiverMethodName, String receiverArgTypeNames, String fromSignature) {
        ClassHierarchyInspector inspector = jarMetadata.getInspector();
        jarMetadata.getReflections().getSubTypesOf(receiver)
                .stream()
                .map(subtype ->
                        inspector.getTopLevelSignature(
                                subtype,
                                ClassHierarchyInspector.methodSignature(
                                        receiverMethodName,
                                        receiverArgTypeNames)
                        )
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::fullyQualifiedMethodSignature)
                .forEach(toSubtypeSignature -> methodCalls.add(createEdge(fromSignature, toSubtypeSignature)));
    }

    private String fullyQualifiedMethodSignature(Method method) {
        return fullyQualifiedMethodSignature(
                method.getDeclaringClass().getName(),
                method.getName(),
                Arrays.stream(method.getParameterTypes())
                        .map(Class::getName)
                        .collect(Collectors.joining(","))
        );
    }

    public String fullyQualifiedMethodSignature(String className, String methodName, String argTypeNames) {
        return className + ":" + methodName + "(" + argTypeNames + ")";
    }

    public Pair<String, String> createEdge(String from, String to) {
        return new Pair<>(from, to);
    }
}
