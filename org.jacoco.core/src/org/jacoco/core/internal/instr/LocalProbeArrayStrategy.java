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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The strategy for interfaces inlines the runtime access directly into the
 * methods as this is the only method without keeping reference within this
 * class. This is very inefficient as the runtime is contacted for every method
 * invocation and therefore only used for static initializers in interfaces.
 */
class LocalProbeArrayStrategy implements IProbeArrayStrategy {

	private final String className;
	private final long classId;
	private final int probeCount;
	private final IExecutionDataAccessorGenerator accessorGenerator;

	LocalProbeArrayStrategy(final String className, final long classId,
			final int probeCount,
			final IExecutionDataAccessorGenerator accessorGenerator) {
		this.className = className;
		this.classId = classId;
		this.probeCount = probeCount;
		this.accessorGenerator = accessorGenerator;
	}

	public int storeInstance(final MethodVisitor mv, final boolean clinit,
			final int variable) {
		final int maxStack = accessorGenerator.generateDataAccessor(classId,
				className, probeCount, mv);
		mv.visitVarInsn(Opcodes.ASTORE, variable);
		return maxStack;
	}

	public void addMembers(final ClassVisitor delegate, final int probeCount) {
		// nothing to do
	}

	public void recordHit(final MethodVisitor mv, final int id,
			final int variable) {
		mv.visitVarInsn(Opcodes.ALOAD, variable);
		// Stack[0]: [Z

		InstrSupport.push(mv, id);

		// Stack[1]: I
		// Stack[0]: [Z

		mv.visitInsn(Opcodes.ICONST_1);

		// Stack[2]: 1
		// Stack[1]: I
		// Stack[0]: [Z

		mv.visitInsn(Opcodes.BASTORE);
	}

	public boolean useVariable() {
		return true;
	}
}
