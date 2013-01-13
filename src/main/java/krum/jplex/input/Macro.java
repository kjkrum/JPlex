package krum.jplex.input;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;

public class Macro {
	
	protected String name;
	protected String expr;

	protected Macro(XPath xp, Node node) throws XPathExpressionException {
		name = (String) xp.evaluate("@name", node, XPathConstants.STRING);
		expr = (String) xp.evaluate("@expr", node, XPathConstants.STRING);
	}

	public String getName() {
		return name;
	}
	
	public String getExpr() {
		return expr;
	}

}
