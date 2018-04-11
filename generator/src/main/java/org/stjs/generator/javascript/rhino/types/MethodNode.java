package org.stjs.generator.javascript.rhino.types;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;

import java.util.List;

public class MethodNode extends AstNode {

	private String name;
	private List<AstNode> params;
	private AstNode body;
	private boolean isStatic;
	private boolean isAbstract;

	{
		type = TSToken.METHODS;
	}

	public MethodNode(String name, List<AstNode> params, AstNode body, boolean isStatic, boolean isAbstract) {
		this.name = name;
		this.params = params;
		this.body = body;
		this.isStatic = isStatic;
		this.isAbstract = isAbstract;
	}

	@Override
	public String toSource(int depth) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public void visit(NodeVisitor visitor) {

	}

	public String getName() {
		return name;
	}

	public List<AstNode> getParams() {
		return params;
	}

	/**
	 * Returns function body.  Normally a {@link org.mozilla.javascript.ast.Block},
	 * but can be a plain {@link AstNode} if it's a function closure.
	 *
	 * @return the body.  Can be {@code null} only if the AST is malformed.
	 */
	public AstNode getBody() {
		return body;
	}

	public boolean isStatic() { return isStatic; }

	public boolean isAbstract() { return isAbstract; }
}
