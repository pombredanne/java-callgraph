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
import gr.gousiosg.javacg.stat.support.MethodSignatureUtil;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

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

    // methodCalls helps us build the (caller -> receiver) call graph
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
        visit(i, EXPAND);
    }

    @Override
    public void visitINVOKEINTERFACE(INVOKEINTERFACE i) {
        visit(i, EXPAND);
    }

    @Override
    public void visitINVOKESPECIAL(INVOKESPECIAL i) {
        visit(i, DONT_EXPAND);
    }

    @Override
    public void visitINVOKESTATIC(INVOKESTATIC i) {
        visit(i, DONT_EXPAND);
    }

    @Override
    public void visitINVOKEDYNAMIC(INVOKEDYNAMIC i) {
        visit(i, EXPAND);
    }

    private void visit(InvokeInstruction i, Boolean shouldExpand) {
        /* caller method info */
        String callerClassType = visitedClass.getClassName();
        String callerMethodName = mg.getName();
        Type[] callerArgumentTypes = mg.getArgumentTypes();
        Type callerReturnType = mg.getReturnType();
        String callerSignature = MethodSignatureUtil.fullyQualifiedMethodSignature(callerClassType, callerMethodName, callerArgumentTypes, callerReturnType);

        /* receiver method info */
        String receiverClassType = String.format(format, i.getReferenceType(cp));
        String receiverMethodName = i.getMethodName(cp);
        Type[] receiverArgumentTypes = i.getArgumentTypes(cp);
        Type receiverReturnType = i.getReturnType(cp);
        String receiverSignature = MethodSignatureUtil.fullyQualifiedMethodSignature(receiverClassType, receiverMethodName, receiverArgumentTypes, receiverReturnType);

        /* Record initial method call */
        methodCalls.add(createEdge(callerSignature, receiverSignature));

        if (shouldExpand && !IGNORED_METHOD_NAMES.contains(receiverMethodName)) {
            Optional<Class<?>> maybeReceiverType = jarMetadata.getClass(receiverClassType);
            if (maybeReceiverType.isEmpty()) {
                LOGGER.error("Couldn't find Receiver class: " + receiverClassType);
                return;
            }

            Optional<Class<?>> maybeCallerType = jarMetadata.getClass(callerClassType);
            if (maybeCallerType.isEmpty()) {
                LOGGER.error("Couldn't find Caller class: " + callerClassType);
                return;
            }

            Optional<Method> maybeCallingMethod = jarMetadata.getInspector()
                    .getTopLevelSignature(
                            maybeCallerType.get(),
                            MethodSignatureUtil.namedMethodSignature(callerMethodName, callerArgumentTypes, callerReturnType)
                    );

            if (maybeCallingMethod.isEmpty()) {
                LOGGER.error("Couldn't find top level signature for " + callerSignature);
                return;
            }

            if (maybeCallingMethod.get().isBridge()) {
                jarMetadata.addBridgeMethod(receiverSignature);
            } else {
                /* Expand to all possible receiver class types */
                expand(maybeReceiverType.get(), receiverMethodName, receiverArgumentTypes, receiverReturnType, callerSignature);
            }
        }

    }

    private void expand(Class<?> receiverType, String receiverMethodName, Type[] receiverArgumentTypes, Type receiverReturnType, String callerSignature) {
        ClassHierarchyInspector inspector = jarMetadata.getInspector();
        LOGGER.info("\tExpanding to subtypes of " + receiverType.getName());
        jarMetadata.getReflections().getSubTypesOf(receiverType)
                .stream()
                .map(subtype ->
                        inspector.getTopLevelSignature(
                                subtype,
                                MethodSignatureUtil.namedMethodSignature(receiverMethodName, receiverArgumentTypes, receiverReturnType)
                        )
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(MethodSignatureUtil::fullyQualifiedMethodSignature)
                /* Record expanded method call */
                .forEach(toSubtypeSignature -> methodCalls.add(createEdge(callerSignature, toSubtypeSignature)));

    }

    public Pair<String, String> createEdge(String from, String to) {
        return new Pair<>(from, to);
    }
}
