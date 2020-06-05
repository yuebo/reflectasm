/**
 * Copyright (c) 2008, Nathan Sweet
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.esotericsoftware.reflectasm;

import org.objectweb.asm.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

import static org.objectweb.asm.Opcodes.*;

public abstract class MethodAccess {
    private String[] methodNames;
    private Class[][] parameterTypes;
    private Class[] returnTypes;

    /**
     * Invokes the method with the specified name and the specified param types.
     * @param object
     * @param methodIndex
     * @param args
     * @return
     */
    abstract public Object invoke(Object object, int methodIndex, Object... args);

    /** Invokes the method with the specified name and the specified param types. */
    public Object invoke(Object object, String methodName, Class[] paramTypes, Object... args) {
        return invoke(object, getIndex(methodName, paramTypes), args);
    }

    /** Invokes the first method with the specified name and the specified number of arguments. */
    public Object invoke(Object object, String methodName, Object... args) {
        return invoke(object, getIndex(methodName, args == null ? 0 : args.length), args);
    }

    /** Returns the index of the first method with the specified name. */
    public int getIndex(String methodName) {
        for (int i = 0, n = methodNames.length; i < n; i++){
            if (methodNames[i].equals(methodName)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unable to find non-private method: " + methodName);
    }

    /** Returns the index of the first method with the specified name and param types. */
    public int getIndex(String methodName, Class... paramTypes) {
        for (int i = 0, n = methodNames.length; i < n; i++){
            if (methodNames[i].equals(methodName) && Arrays.equals(paramTypes, parameterTypes[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unable to find non-private method: " + methodName + " " + Arrays.toString(paramTypes));
    }

    /** Returns the index of the first method with the specified name and the specified number of arguments. */
    public int getIndex(String methodName, int paramsCount) {
        for (int i = 0, n = methodNames.length; i < n; i++){
            if (methodNames[i].equals(methodName) && parameterTypes[i].length == paramsCount){
                return i;
            }
        }
        throw new IllegalArgumentException(
                "Unable to find non-private method: " + methodName + " with " + paramsCount + " params.");
    }

    public String[] getMethodNames() {
        return methodNames;
    }

    public Class[][] getParameterTypes() {
        return parameterTypes;
    }

    public Class[] getReturnTypes() {
        return returnTypes;
    }

    /** Creates a new MethodAccess for the specified type.
     * @param type Must not be a primitive type, or void. */
    static public MethodAccess get(Class type) {
        boolean isInterface = type.isInterface();
        if (!isInterface && type.getSuperclass() == null && type != Object.class){
            throw new IllegalArgumentException("The type must not be an interface, a primitive type, or void.");
        }

        ArrayList<Method> methods = new ArrayList<Method>();
        if (!isInterface) {
            Class nextClass = type;
            while (nextClass != Object.class) {
                addDeclaredMethodsToList(nextClass, methods);
                nextClass = nextClass.getSuperclass();
            }
        } else{
            recursiveAddInterfaceMethodsToList(type, methods);
        }

        int n = methods.size();
        String[] methodNames = new String[n];
        Class[][] parameterTypes = new Class[n][];
        Class[] returnTypes = new Class[n];
        for (int i = 0; i < n; i++) {
            Method method = methods.get(i);
            methodNames[i] = method.getName();
            parameterTypes[i] = method.getParameterTypes();
            returnTypes[i] = method.getReturnType();
        }

        String className = type.getName();
        String accessClassName = className + "MethodAccess";
        if (accessClassName.startsWith("java.")) {
            accessClassName = "reflectasm." + accessClassName;
        }

        Class accessClass;
        AccessClassLoader loader = AccessClassLoader.get(type);
        synchronized (loader) {
            accessClass = loader.loadAccessClass(accessClassName);
            if (accessClass == null) {
                String accessClassNameInternal = accessClassName.replace('.', '/');
                String classNameInternal = className.replace('.', '/');

                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                FieldVisitor fv;
                MethodVisitor mv;
                cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, accessClassNameInternal, null, "com/esotericsoftware/reflectasm/MethodAccess",
                        null);
                cw.visitInnerClass("java/lang/invoke/MethodHandles$Lookup", "java/lang/invoke/MethodHandles", "Lookup", ACC_PUBLIC + ACC_FINAL + ACC_STATIC);

                {
                    fv = cw.visitField(ACC_PRIVATE, "functions", "[Ljava/util/function/BiFunction;", "[Ljava/util/function/BiFunction<Ljava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;>;", null);
                    fv.visitEnd();
                }
                {
                    mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                    mv.visitCode();
                    Label l0 = new Label();
                    mv.visitLabel(l0);
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKESPECIAL, "com/esotericsoftware/reflectasm/MethodAccess", "<init>", "()V", false);
                    Label l1 = new Label();
                    mv.visitLabel(l1);
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitIntInsn(SIPUSH, n);
                    mv.visitTypeInsn(ANEWARRAY, "java/util/function/BiFunction");
                    mv.visitFieldInsn(PUTFIELD, accessClassNameInternal, "functions", "[Ljava/util/function/BiFunction;");
                    {
                        for (int i = 0; i < n; i++) {
                            Label l3 = new Label();
                            mv.visitLabel(l3);
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitFieldInsn(GETFIELD, accessClassNameInternal, "functions", "[Ljava/util/function/BiFunction;");
                            mv.visitIntInsn(SIPUSH, i);
                            mv.visitInvokeDynamicInsn("apply", "()Ljava/util/function/BiFunction;", new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"), new Object[]{Type.getType("(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), new Handle(Opcodes.H_INVOKESTATIC,
                                    accessClassNameInternal, "lambda$new$" + i, "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;"), Type.getType("(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;")});
                            mv.visitInsn(AASTORE);
                        }

                    }
                    Label l4 = new Label();
                    mv.visitLabel(l4);
                    mv.visitInsn(RETURN);
                    Label l5 = new Label();
                    mv.visitLabel(l5);
                    mv.visitLocalVariable("this", "L+" + accessClassNameInternal + ";", null, l0, l5, 0);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }

                {
                    mv = cw.visitMethod(ACC_PUBLIC + ACC_VARARGS, "invoke", "(Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Object;", null, null);
                    mv.visitCode();
                    Label l0 = new Label();
                    mv.visitLabel(l0);
                    mv.visitLineNumber(45, l0);
                    mv.visitVarInsn(ILOAD, 2);
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, accessClassNameInternal, "functions", "[Ljava/util/function/BiFunction;");
                    mv.visitInsn(ARRAYLENGTH);
                    Label l1 = new Label();
                    mv.visitJumpInsn(IF_ICMPLT, l1);
                    Label l2 = new Label();
                    mv.visitLabel(l2);
                    mv.visitLineNumber(46, l2);
                    mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn("no such method");
                    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
                    mv.visitInsn(ATHROW);
                    mv.visitLabel(l1);
                    mv.visitLineNumber(48, l1);
                    mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, accessClassNameInternal, "functions", "[Ljava/util/function/BiFunction;");
                    mv.visitVarInsn(ILOAD, 2);
                    mv.visitInsn(AALOAD);
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/function/BiFunction", "apply", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                    mv.visitInsn(ARETURN);
                    Label l3 = new Label();
                    mv.visitLabel(l3);
                    mv.visitLocalVariable("this", "L" + accessClassNameInternal + ";", null, l0, l1, 0);
                    mv.visitLocalVariable("object", "Ljava/lang/Object;", null, l0, l3, 1);
                    mv.visitLocalVariable("methodIndex", "I", null, l0, l3, 2);
                    mv.visitLocalVariable("args", "[Ljava/lang/Object;", null, l0, l3, 3);
                    mv.visitMaxs(3, 4);
                    mv.visitEnd();
                }
                {
                    if (!methods.isEmpty()) {
                        StringBuilder buffer = new StringBuilder(128);
                        for (int i = 0; i < n; i++) {
                            mv = cw.visitMethod(Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC + ACC_SYNTHETIC, "lambda$new$" + i, "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", null, null);
                            mv.visitCode();
                            Label l0 = new Label();
                            mv.visitLabel(l0);
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitTypeInsn(CHECKCAST, classNameInternal);
                            mv.visitVarInsn(ASTORE, 2);

                            Label l1 = new Label();
                            mv.visitLabel(l1);

                            buffer.setLength(0);
                            buffer.append('(');

                            Class[] paramTypes = parameterTypes[i];
                            Class returnType = returnTypes[i];
                            mv.visitVarInsn(ALOAD, 2);
                            for (int paramIndex = 0; paramIndex < paramTypes.length; paramIndex++) {
                                mv.visitVarInsn(ALOAD, 1);
                                mv.visitIntInsn(SIPUSH, paramIndex);
                                mv.visitInsn(AALOAD);
                                Type paramType = Type.getType(paramTypes[paramIndex]);
                                switch (paramType.getSort()) {
                                    case Type.BOOLEAN:
                                        mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
                                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
                                        break;
                                    case Type.BYTE:
                                        mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
                                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B");
                                        break;
                                    case Type.CHAR:
                                        mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
                                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C");
                                        break;
                                    case Type.SHORT:
                                        mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
                                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S");
                                        break;
                                    case Type.INT:
                                        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
                                        break;
                                    case Type.FLOAT:
                                        mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
                                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F");
                                        break;
                                    case Type.LONG:
                                        mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
                                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J");
                                        break;
                                    case Type.DOUBLE:
                                        mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
                                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D");
                                        break;
                                    case Type.ARRAY:
                                        mv.visitTypeInsn(CHECKCAST, paramType.getDescriptor());
                                        break;
                                    case Type.OBJECT:
                                        mv.visitTypeInsn(CHECKCAST, paramType.getInternalName());
                                        break;
                                    default:
                                        break;
                                }
                                buffer.append(paramType.getDescriptor());
                            }

                            buffer.append(')');
                            buffer.append(Type.getDescriptor(returnType));
                            int invoke;
                            if (isInterface) {
                                invoke = INVOKEINTERFACE;
                            } else if (Modifier.isStatic(methods.get(i).getModifiers())) {
                                invoke = INVOKESTATIC;
                            } else {
                                invoke = INVOKEVIRTUAL;
                            }

                            mv.visitMethodInsn(invoke, classNameInternal, methodNames[i], buffer.toString());
                            switch (Type.getType(returnType).getSort()) {
                                case Type.VOID:
                                    mv.visitInsn(ACONST_NULL);
                                    break;
                                case Type.BOOLEAN:
                                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
                                    break;
                                case Type.BYTE:
                                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
                                    break;
                                case Type.CHAR:
                                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
                                    break;
                                case Type.SHORT:
                                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
                                    break;
                                case Type.INT:
                                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
                                    break;
                                case Type.FLOAT:
                                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
                                    break;
                                case Type.LONG:
                                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
                                    break;
                                case Type.DOUBLE:
                                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
                                    break;
                                default:
                                    break;
                            }

                            mv.visitInsn(ARETURN);
                            Label l2 = new Label();
                            mv.visitLabel(l2);
                            mv.visitLocalVariable("object", "Ljava/lang/Object;", null, l0, l2, 0);
                            mv.visitLocalVariable("param", "[Ljava/lang/Object;", null, l0, l2, 1);
                            mv.visitLocalVariable("source", "L" + classNameInternal + ";", null, l1, l2, 2);
                            mv.visitMaxs(0, 0);
                            mv.visitEnd();
                        }

                    }
                }
                cw.visitEnd();
                byte[] data = cw.toByteArray();
                accessClass = loader.defineAccessClass(accessClassName, data);
            }
        }
        try {
            MethodAccess access = (MethodAccess) accessClass.newInstance();
            access.methodNames = methodNames;
            access.parameterTypes = parameterTypes;
            access.returnTypes = returnTypes;
            return access;
        } catch (Throwable t) {
            throw new RuntimeException("Error constructing method access class: " + accessClassName, t);
        }
    }

    static private void addDeclaredMethodsToList(Class type, ArrayList<Method> methods) {
        Method[] declaredMethods = type.getDeclaredMethods();
        for (Method method : declaredMethods) {
            int modifiers = method.getModifiers();
            // if (Modifier.isStatic(modifiers)) continue;
            if (Modifier.isPrivate(modifiers)) {
                continue;
            }
            methods.add(method);
        }
    }

    static private void recursiveAddInterfaceMethodsToList(Class interfaceType, ArrayList<Method> methods) {
        addDeclaredMethodsToList(interfaceType, methods);
        for (Class nextInterface : interfaceType.getInterfaces()) {
            recursiveAddInterfaceMethodsToList(nextInterface, methods);
        }
    }
}
