package org.stjs.generator.writer.enums;

/**
 * (c) Swissquote 05.04.18
 *
 * @author sgoetz
 */
public class Enums11 {
	public enum Value {
		a, b, c;
	}

	public void main() {
		@SuppressWarnings("unused")
		Value[] x = Value.values();
	}
}