/*******************************************************************************
 * Copyright (c) 2009, 2020 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Evgeny Mandrikov - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.core.internal.instr;

import org.jacoco.core.runtime.IExecutionDataAccessorGenerator;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This strategy for Java 11+ class files uses {@link ConstantDynamic} to hold
 * the probe array and adds bootstrap method requesting the probe array from the
 * runtime.
 */
public class CondyProbeArrayStrategy implements IProbeArrayStrategy {

	/**
	 * Descriptor of the bootstrap method.
	 */
	public static final String B_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)[Z";

	private static final Object[] FRAME_INTEGER = new Object[] {
			Opcodes.INTEGER };
	private static final Object[] FRAME_HITS_ARRAY = new Object[] {
			InstrSupport.DATAFIELD_DESC };

	private final String className;

	private final boolean isInterface;

	private final long classId;

	private final IExecutionDataAccessorGenerator accessorGenerator;

	CondyProbeArrayStrategy(final String className, final boolean isInterface,
			final long classId,
			final IExecutionDataAccessorGenerator accessorGenerator) {
		this.className = className;
		this.isInterface = isInterface;
		this.classId = classId;
		this.accessorGenerator = accessorGenerator;
	}

	public int storeInstance(final MethodVisitor mv, final boolean clinit,
			final int variable) {
		return 0;
	}

	public void addMembers(final ClassVisitor cv, final int probeCount) {
		final MethodVisitor mv = cv.visitMethod(InstrSupport.INITMETHOD_ACC,
				InstrSupport.INITMETHOD_NAME, B_DESC, null, null);
		final int maxStack = accessorGenerator.generateDataAccessor(classId,
				className, probeCount, mv);
		mv.visitInsn(Opcodes.ARETURN);
		mv.visitMaxs(maxStack, 3);
		mv.visitEnd();

		createHitMethod(cv, className, isInterface);
	}

	public void recordHit(final MethodVisitor mv, final int id,
			final int variable) {
		InstrSupport.push(mv, id);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, className,
				InstrSupport.HITMETHOD_NAME, InstrSupport.HITMETHOD_DESC,
				isInterface);
	}

	public boolean useVariable() {
		return false;
	}

	static void createHitMethod(final ClassVisitor cv, final String className,
			final boolean isInterface) {
		final MethodVisitor mv = cv.visitMethod(InstrSupport.INITMETHOD_ACC,
				InstrSupport.HITMETHOD_NAME, InstrSupport.HITMETHOD_DESC, null,
				null);
		mv.visitCode();

		final Handle bootstrapMethod = new Handle(Opcodes.H_INVOKESTATIC,
				className, InstrSupport.INITMETHOD_NAME, B_DESC, isInterface);
		// As a workaround for https://bugs.openjdk.java.net/browse/JDK-8216970
		// constant should have type Object
		mv.visitLdcInsn(new ConstantDynamic(InstrSupport.DATAFIELD_NAME,
				"Ljava/lang/Object;", bootstrapMethod));
		mv.visitTypeInsn(Opcodes.CHECKCAST, "[Z");

		// Stack[0]: [Z

		mv.visitInsn(Opcodes.DUP);

		// Stack[1]: [Z
		// Stack[0]: [Z

		mv.visitVarInsn(Opcodes.ILOAD, 0);

		// Stack[2]: I (index param)
		// Stack[1]: [Z
		// Stack[0]: [Z

		mv.visitInsn(Opcodes.BALOAD);

		// Stack[1]: Z (the previous hit boolean)
		// Stack[0]: [Z

		final Label alreadyHit = new Label();
		mv.visitJumpInsn(Opcodes.IFNE, alreadyHit);

		// Stack[0]: [Z

		mv.visitInsn(Opcodes.DUP);

		// Stack[1]: [Z
		// Stack[0]: [Z

		mv.visitVarInsn(Opcodes.ILOAD, 0);

		// Stack[2]: I (index param)
		// Stack[1]: [Z
		// Stack[0]: [Z

		mv.visitInsn(Opcodes.ICONST_1);

		// Stack[3]: 1 (true)
		// Stack[2]: I (index param)
		// Stack[1]: [Z
		// Stack[0]: [Z

		mv.visitInsn(Opcodes.BASTORE);

		// Stack[0]: [Z

		mv.visitLabel(alreadyHit);
		mv.visitFrame(Opcodes.F_FULL, 1, FRAME_INTEGER, 1, FRAME_HITS_ARRAY);
		mv.visitInsn(Opcodes.POP);
		mv.visitInsn(Opcodes.RETURN);

		mv.visitMaxs(4, 1);

		mv.visitEnd();
	}

}
