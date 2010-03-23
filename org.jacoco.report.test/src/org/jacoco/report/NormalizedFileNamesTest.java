/*******************************************************************************
 * Copyright (c) 2009, 2010 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *    
 * $Id: $
 *******************************************************************************/
package org.jacoco.report;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link NormalizedFileNames}.
 * 
 * @author Marc R. Hoffmann
 * @version $Revision: $
 */
public class NormalizedFileNamesTest {

	private NormalizedFileNames nfn;

	@Before
	public void setup() {
		nfn = new NormalizedFileNames();
	}

	@Test
	public void testKeepLegalCharacters() {
		String id = "Foo-bar_$15.class";
		assertEquals(id, nfn.getFileName(id));
	}

	@Test
	public void testReplaceIllegalCharacters() {
		String id = "A/b C;";
		assertEquals("A_b_C_", nfn.getFileName(id));
	}

	@Test
	public void testReplaceIllegalCharactersNonUnique() {
		assertEquals("F__", nfn.getFileName("F__"));
		assertEquals("F__~1", nfn.getFileName("F**"));
		assertEquals("F__~2", nfn.getFileName("F??"));

		// Mapping must be reproducible
		assertEquals("F__", nfn.getFileName("F__"));
		assertEquals("F__~1", nfn.getFileName("F**"));
		assertEquals("F__~2", nfn.getFileName("F??"));
	}

	@Test
	public void testCaseAware() {
		assertEquals("Hello", nfn.getFileName("Hello"));
		assertEquals("HELLO~1", nfn.getFileName("HELLO"));
		assertEquals("HeLLo~2", nfn.getFileName("HeLLo"));

		// Mapping must be reproducible
		assertEquals("Hello", nfn.getFileName("Hello"));
		assertEquals("HELLO~1", nfn.getFileName("HELLO"));
		assertEquals("HeLLo~2", nfn.getFileName("HeLLo"));
	}

}