/*******************************************************************************
 * Copyright (c) 2009, 2021 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.core.internal.instr;

import org.jacoco.core.runtime.IExecutionDataAccessorGenerator;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The strategy for regular classes adds a static field to hold the probe array
 * and a static initialization method requesting the probe array from the
 * runtime.
 */
class ClassFieldProbeArrayStrategy implements IProbeArrayStrategy {

	private static final Object[] FRAME_EMPTY = new Object[0];
	private static final Object[] FRAME_INTEGER = new Object[] {
			Opcodes.INTEGER };
	private static final Object[] FRAME_HITS_ARRAY = new Object[] {
			InstrSupport.DATAFIELD_DESC };

	private final String className;
	private final long classId;
	private final boolean withFrames;
	private final IExecutionDataAccessorGenerator accessorGenerator;

	ClassFieldProbeArrayStrategy(final String className, final long classId,
			final boolean withFrames,
			final IExecutionDataAccessorGenerator accessorGenerator) {
		this.className = className;
		this.classId = classId;
		this.withFrames = withFrames;
		this.accessorGenerator = accessorGenerator;
	}

	public int storeInstance(final MethodVisitor mv, final boolean clinit,
			final int variable) {
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, className,
				InstrSupport.INITMETHOD_NAME,
				InstrSupport.INITMETHOD_NORETURN_DESC, false);
		return 0;
	}

	public void addMembers(final ClassVisitor cv, final int probeCount) {
		createDataField(cv);
		createInitMethod(cv, probeCount);
		createHitMethod(cv);
	}

	public void recordHit(final MethodVisitor mv, final int id,
			final int variable) {
		InstrSupport.push(mv, id);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, className,
				InstrSupport.HITMETHOD_NAME, InstrSupport.HITMETHOD_DESC,
				false);
	}

	public boolean useVariable() {
		return false;
	}

	private void createDataField(final ClassVisitor cv) {
		cv.visitField(InstrSupport.DATAFIELD_ACC, InstrSupport.DATAFIELD_NAME,
				InstrSupport.DATAFIELD_DESC, null, null);
	}

	private void createInitMethod(final ClassVisitor cv, final int probeCount) {
		final MethodVisitor mv = cv.visitMethod(InstrSupport.INITMETHOD_ACC,
				InstrSupport.INITMETHOD_NAME,
				InstrSupport.INITMETHOD_NORETURN_DESC, null, null);
		mv.visitCode();

		// Load the value of the static data field:
		mv.visitFieldInsn(Opcodes.GETSTATIC, className,
				InstrSupport.DATAFIELD_NAME, InstrSupport.DATAFIELD_DESC);

		// Stack[0]: [Z

		// Skip initialization when we already have a data array:
		final Label alreadyInitialized = new Label();
		mv.visitJumpInsn(Opcodes.IFNONNULL, alreadyInitialized);

		final int size = accessorGenerator.generateDataAccessor(classId,
				className, probeCount, mv);

		// Stack[0]: [Z

		mv.visitFieldInsn(Opcodes.PUTSTATIC, className,
				InstrSupport.DATAFIELD_NAME, InstrSupport.DATAFIELD_DESC);

		if (withFrames) {
			mv.visitFrame(Opcodes.F_NEW, 0, FRAME_EMPTY, 0, FRAME_EMPTY);
		}
		mv.visitLabel(alreadyInitialized);
		mv.visitInsn(Opcodes.RETURN);

		mv.visitMaxs(Math.max(size, 1), 0); // Maximum local stack size is 2
		mv.visitEnd();
	}

	private void createHitMethod(final ClassVisitor cv) {
		final MethodVisitor mv = cv.visitMethod(InstrSupport.INITMETHOD_ACC,
				InstrSupport.HITMETHOD_NAME, InstrSupport.HITMETHOD_DESC, null,
				null);
		mv.visitCode();

		mv.visitFieldInsn(Opcodes.GETSTATIC, className,
				InstrSupport.DATAFIELD_NAME, InstrSupport.DATAFIELD_DESC);

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
		if (withFrames) {
			mv.visitFrame(Opcodes.F_FULL, 1, FRAME_INTEGER, 1,
					FRAME_HITS_ARRAY);
		}
		mv.visitInsn(Opcodes.POP);
		mv.visitInsn(Opcodes.RETURN);

		mv.visitMaxs(4, 1);

		mv.visitEnd();
	}

}
