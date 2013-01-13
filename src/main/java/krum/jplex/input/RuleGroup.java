package krum.jplex.input;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RuleGroup {
	
	protected Set<State> states = new HashSet<State>();
	protected Set<Rule> rules = new HashSet<Rule>();

	protected RuleGroup(XPath xp, Node node, final Map<String, State> stateMap) throws XPathExpressionException {
		NodeList stateNodes =
			(NodeList) xp.evaluate("State", node, XPathConstants.NODESET);
		for(int i = 0; i < stateNodes.getLength(); ++i) {
			String name = (String) xp.evaluate("@name", stateNodes.item(i), XPathConstants.STRING);
			states.add(stateMap.get(name));
		}
			
		NodeList ruleNodes =
			(NodeList) xp.evaluate("Rule", node, XPathConstants.NODESET);
		for(int i = 0; i < ruleNodes.getLength(); ++i) {
			Rule rule = new Rule(xp, ruleNodes.item(i), stateMap);
			rules.add(rule);
		}
	}
	
	public boolean appliesToState(State state) {
		return states.contains(state);
	}
	
	public Collection<Rule> getRules() {
		return new HashSet<Rule>(rules);
	}

}
