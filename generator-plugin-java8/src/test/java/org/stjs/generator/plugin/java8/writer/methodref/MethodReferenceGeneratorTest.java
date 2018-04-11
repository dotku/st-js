package org.stjs.generator.plugin.java8.writer.methodref;

import org.stjs.generator.utils.AbstractStjsTest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MethodReferenceGeneratorTest extends AbstractStjsTest {
	@Test
	public void testStaticMethodRef() {
		assertCodeContains(MethodRef1.class, "calculate(MethodRef1.inc)");
		assertEquals(1, executeAndReturnNumber(MethodRef1.class), 0);
	}

	@Test
	public void testInstanceMethodRef() {
		assertCodeContains(MethodRef2.class,
				"calculate(stjs.bind(\"inc2\"), new MethodRef2(), 1)");
		assertEquals(3, executeAndReturnNumber(MethodRef2.class), 0);
	}

	@Test
	public void testInstanceMethodRefWithInterface() {
		assertCodeContains(MethodRef9.class,
				"calculate(stjs.bind(\"inc2\"), new MethodRef9_IncImpl(), 1)");
		assertEquals(3, executeAndReturnNumber(MethodRef9.class), 0);
	}

	@Test
	public void testInstanceWithTargetMethodRef() {
		assertCodeContains(MethodRef3.class, "calculate(stjs.bind(ref, \"inc2\"), 1)");
		assertEquals(4, executeAndReturnNumber(MethodRef3.class), 0);
	}

	@Test
	public void testNewMethodRef() {
		assertCodeContains(MethodRef4.class, "calculate(function(){return new MethodRef4(arguments[0]);}, 1)");
		assertEquals(1, executeAndReturnNumber(MethodRef4.class), 0);
	}

	@Test
	public void testUsageOfThisMethodRef() {
		assertCodeContains(MethodRef5.class, "calculate(stjs.bind(this, \"method\"))");
	}

	@Test
	public void testUsageOFieldMethodRef() {
		assertCodeContains(MethodRef6.class, "calculate(stjs.bind(this.field, \"method\"))");
	}

	@Test
	public void testUsageOMethodMethodRef() {
		assertCodeContains(MethodRef7.class, "calculate(stjs.bind(this.method2(), \"method\"))");
	}

	@Test
	public void testUsageOfChainMethodMethodRef() {
		assertCodeContains(MethodRef8.class, "calculate(stjs.bind(this.x.x.method2(), \"method\"))");
	}
}
