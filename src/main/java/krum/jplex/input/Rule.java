package krum.jplex.input;

import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;

public class Rule implements Comparable<Rule> {
	
	protected String expr;
	protected String event;
	protected int pri;
	protected StateChange stateChange;

	protected Rule(XPath xp, Node node, final Map<String, State> stateMap) throws XPathExpressionException {
		expr = (String) xp.evaluate("@expr", node, XPathConstants.STRING);
		event = (String) xp.evaluate("@event", node, XPathConstants.STRING);
		if("".equals(event)) event = null;
		pri = ((Double) xp.evaluate("@pri", node, XPathConstants.NUMBER)).intValue();
		Node stateChangeNode =
				(Node) xp.evaluate("PushState|PopState|JumpState", node, XPathConstants.NODE);
		if(stateChangeNode != null) {
			stateChange = new StateChange(xp, stateChangeNode, stateMap);
		}	
	}
	
	public boolean hasEvent() {
		return event != null;
	}
	
	public String getEvent() {
		return event;
	}
	
	public boolean hasStateChange() {
		return stateChange != null;
	}
	
	public StateChange getStateChange() {
		return stateChange;
	}
	
	public String getExpr() {
		return expr;
	}

	/**
	 * Compares rules according to their priority.
	 */
	@Override
	public int compareTo(Rule other) {
		return pri - other.pri;
	}
}
