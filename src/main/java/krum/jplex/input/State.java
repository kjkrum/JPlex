package krum.jplex.input;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;

public class State {
	
	protected String name;
	protected boolean strict;

	protected State(XPath xp, Node node) throws XPathExpressionException {
		name = (String) xp.evaluate("@name", node, XPathConstants.STRING);
		strict = (Boolean) xp.evaluate("@strict", node, XPathConstants.BOOLEAN);
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isStrict() {
		return strict;
	}

}
