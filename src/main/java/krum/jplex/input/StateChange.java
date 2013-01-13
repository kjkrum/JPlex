package krum.jplex.input;

import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;

public class StateChange {
	
	public static enum Type {
		PUSH, POP, JUMP
	}
	
	protected Type type;
	protected State state;
	
	public StateChange(XPath xp, Node node, final Map<String, State> stateMap) throws XPathExpressionException {
		String name = node.getNodeName();
		if("PushState".equals(name)) {
			type = Type.PUSH;
			state = stateMap.get((String) xp.evaluate("@name", node, XPathConstants.STRING));
		}
		else if("PopState".equals(name)) {
			type = Type.POP;
			state = null;
		}
		else if("JumpState".equals(name)) {
			type = Type.JUMP;
			state = stateMap.get((String) xp.evaluate("@name", node, XPathConstants.STRING));
		}
		else {
			// TODO: log error?
		}
	}
	
	public Type getType() {
		return type;
	}
	
	public State getState() {
		return state;
	}
	
}

