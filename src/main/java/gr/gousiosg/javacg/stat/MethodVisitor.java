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
import gr.gousiosg.javacg.stat.support.ClassHierarchyInspector;
import gr.gousiosg.javacg.stat.support.JarMetadata;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static gr.gousiosg.javacg.stat.support.IgnoredConstants.IGNORED_METHOD_NAMES;

/**
 * The simplest of method visitors, prints any invoked method
 * signature for all method invocations.
 * 
 * Class copied with modifications from CJKM: http://www.spinellis.gr/sw/ckjm/
 */
public class MethodVisitor extends EmptyVisitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodVisitor.class);
    private static final Boolean EXPAND = true;
    private static final Boolean DONT_EXPAND = false;

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
        visit(String.format(format,i.getReferenceType(cp)), i.getMethodName(cp), argumentList(i.getArgumentTypes(cp)), DONT_EXPAND);
    }

    @Override
    public void visitINVOKESTATIC(INVOKESTATIC i) {
        visit(String.format(format,i.getReferenceType(cp)), i.getMethodName(cp), argumentList(i.getArgumentTypes(cp)), DONT_EXPAND);
    }

    @Override
    public void visitINVOKEDYNAMIC(INVOKEDYNAMIC i) {
        visit(String.format(format,i.getReferenceType(cp)), i.getMethodName(cp), argumentList(i.getArgumentTypes(cp)), EXPAND);
    }

    private void visit(String receiverTypeName, String receiverMethodName, String receiverArgTypeNames, Boolean shouldExpand) {
        String fromSignature = fullyQualifiedMethodSignature(visitedClass.getClassName(), mg.getName(), argumentList(mg.getArgumentTypes()));
        String toSignature = fullyQualifiedMethodSignature(receiverTypeName, receiverMethodName, receiverArgTypeNames);
        methodCalls.add(createEdge(fromSignature, toSignature));

        if (shouldExpand && !IGNORED_METHOD_NAMES.contains(receiverMethodName)) {
            Optional<Class<?>> maybeReceiverType = jarMetadata.getClass(receiverTypeName);
            if (maybeReceiverType.isEmpty()) {
                LOGGER.error("Skipping " + toSignature);
                return;
            }

            Optional<Class<?>> maybeCallerType = jarMetadata.getClass(visitedClass.getClassName());
            if (maybeCallerType.isEmpty()) {
                LOGGER.error("Couldn't find Caller class type: " + visitedClass.getClassName());
                return;
            }

            Optional<Method> maybeCallingMethod = jarMetadata.getInspector()
                    .getTopLevelSignature(
                            maybeCallerType.get(),
                            ClassHierarchyInspector.methodSignature(
                                    mg.getName(),
                                    argumentList(mg.getArgumentTypes())
                            )
                    );

            if (maybeCallingMethod.isEmpty()) {
                LOGGER.error("Couldn't find top level signature for " + fromSignature);
                return;
            }

            if (maybeCallingMethod.get().isBridge()) {
                jarMetadata.addBridgeMethod(fromSignature);
            }

            expand(maybeReceiverType.get(), receiverMethodName, receiverArgTypeNames, fromSignature);
        }
    }

    private void expand(Class<?> receiverType, String receiverMethodName, String receiverArgTypeNames, String fromSignature) {
        ClassHierarchyInspector inspector = jarMetadata.getInspector();
        LOGGER.info("\tExpanding to subtypes of " + receiverType.getName());
        jarMetadata.getReflections().getSubTypesOf(receiverType)
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
