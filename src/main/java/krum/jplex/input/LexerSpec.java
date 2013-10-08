package krum.jplex.input;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LexerSpec {
	
	protected String package_;
	protected String prefix;
	protected String fqnPrefix;
	protected boolean stateListener;
	protected boolean loggers;
	protected String resourcePath;
	
	protected boolean publicStateChangeMethods;
	protected boolean finalClasses;
	
	protected Map<String, State> states = new HashMap<String, State>();
	protected State initialState;
	protected Map<String, Macro> macros = new HashMap<String, Macro>();
	protected List<RuleGroup> ruleGroups = new LinkedList<RuleGroup>();

	protected LexerSpec(XPath xp, Node node) throws XPathExpressionException, ValidationException {
		package_ = (String) xp.evaluate("LexerSpec/Package/text()", node, XPathConstants.STRING);
		//if("".equals(package_)) package_ = null;
		
		prefix = (String) xp.evaluate("LexerSpec/Prefix/text()", node, XPathConstants.STRING);
		//if("".equals(prefix)) prefix = null;

		StringBuilder sb = new StringBuilder();
		//if(package_ != null) {
		if(!"".equals(package_)) {
			sb.append(package_);
			sb.append('.');
		}
		//if(prefix != null) {
		if(!"".equals(prefix)) {
			sb.append(prefix);
		}
		fqnPrefix = sb.toString();

		stateListener = parseXMLBoolean((String) xp.evaluate("LexerSpec/StateListener/text()", node, XPathConstants.STRING));

		loggers = parseXMLBoolean((String) xp.evaluate("LexerSpec/Loggers/text()", node, XPathConstants.STRING));
		
		resourcePath = (String) xp.evaluate("LexerSpec/ResourcePath/text()", node, XPathConstants.STRING);
		if("".equals(resourcePath)) {
			String[] dirs = package_.split("\\.");
			resourcePath = dirs[0];
			for(int i = 1; i < dirs.length; ++i) {
				resourcePath = resourcePath + '/' + dirs[i];
			}
		}
		
		publicStateChangeMethods = parseXMLBoolean((String) xp.evaluate("LexerSpec/PublicStateChangeMethods/text()", node, XPathConstants.STRING));
		finalClasses = parseXMLBoolean((String) xp.evaluate("LexerSpec/FinalClasses/text()", node, XPathConstants.STRING));
		
		NodeList stateNodes =
				(NodeList) xp.evaluate("LexerSpec/State", node, XPathConstants.NODESET);
		for(int i = 0; i < stateNodes.getLength(); ++i) {
			State state = new State(xp, stateNodes.item(i));
			states.put(state.getName(), state);
		}
		
		initialState = states.get((String) xp.evaluate("LexerSpec/InitialState/text()", node, XPathConstants.STRING));
		
		NodeList macroNodes =
				(NodeList) xp.evaluate("LexerSpec/Macro", node, XPathConstants.NODESET);
		for(int i = 0; i < macroNodes.getLength(); ++i) {
			Macro macro = new Macro(xp, macroNodes.item(i));
			macros.put(macro.getName(), macro);
		}
		
		NodeList ruleGroupNodes =
				(NodeList) xp.evaluate("LexerSpec/RuleGroup", node, XPathConstants.NODESET);
		for(int i = 0; i < ruleGroupNodes.getLength(); ++i) {
			RuleGroup group = new RuleGroup(xp, ruleGroupNodes.item(i), states);
			ruleGroups.add(group);
		}
		
		expandMacros();
		
		validate();
	}		
	
	protected void expandMacros() throws ValidationException {
		if(macros.size() == 0) return;
		
		// pattern is '{' + java identifier + '}'
		Pattern pattern = Pattern.compile("\\{[\\p{L}_$][\\p{L}\\p{N}_$]*\\}");
		Matcher matcher = pattern.matcher("");
		
		// expand macros in macros
		for(Macro macro : macros.values()) {
			matcher.reset(macro.expr);
			while(matcher.find()) {
				String name = macro.expr.substring(matcher.start() + 1, matcher.end() - 1);
				if(!macros.containsKey(name)) {
					throw new ValidationException("undefined macro invocation: " + name);
				}
				macro.expr = macro.expr.replaceAll("\\{" + name + "\\}", Matcher.quoteReplacement(macros.get(name).expr));
				matcher.reset(macro.expr);
			}
		}
		
		// expand macros in rules
		for(Rule rule : allRules()) {
			matcher.reset(rule.expr);
			while(matcher.find()) {
				String name = rule.expr.substring(matcher.start() + 1, matcher.end() - 1);
				if(!macros.containsKey(name)) {
					throw new ValidationException("undefined macro invocation: " + name);
				}
				rule.expr = rule.expr.replaceAll("\\{" + name + "\\}", Matcher.quoteReplacement(macros.get(name).expr));
				matcher.reset(rule.expr);
			}
		}
		
		macros.clear();
	}

	/**
	 * Performs high-level validation that can't be handled in XML Schema.
	 * 
	 * @throws ValidationException 
	 */
	protected void validate() throws ValidationException {
		Set<String> eventNames = new HashSet<String>();
		Set<String> duplicateEventNames = new HashSet<String>();
		
		for(RuleGroup g : ruleGroups) {
			for(Rule r : g.rules) {
				if(r.event != null) {
					if(ReservedWords.contains(r.event)) {
						throw new ValidationException("event name \"" + r.event + "\" is a reserved word");
					}
					if(eventNames.contains(r.event)) duplicateEventNames.add(r.event);
					else eventNames.add(r.event);
				}
			}
		}
		
		for(State s : states.values()) {
			if(ReservedWords.contains(s.name)) {
				throw new ValidationException("state name \"" + s.name + "\" is a reserved word");
			}
		}
		
		for(String name : duplicateEventNames) {
			System.err.printf("Warning: multiple use of event name \"%s\"\n", name);
		}
	}	
	
	protected boolean parseXMLBoolean(String s) {
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	public boolean usesStateListener() {
		return stateListener;
	}

	public boolean usesLoggers() {
		return loggers;
	}

	public String fqnPrefix() {
		return fqnPrefix;
	}
	
	public String package_() {
		return package_;		
	}
	
	public Collection<State> states() {
		return states.values();
	}	
	
	public Collection<Rule> allRules() {
		List<Rule> rules = new LinkedList<Rule>();
		for(RuleGroup g : ruleGroups) {
			rules.addAll(g.getRules());
		}
		return rules;
	}
	
	public Collection<RuleGroup> ruleGroups() {
		return new LinkedList<RuleGroup>(ruleGroups);
	}

	public State initialState() {
		return initialState;
	}

	public String prefix() {
		return prefix;
	}

	public String resourcePath() {
		return resourcePath;
	}
	
	public boolean usesPublicStateChangeMethods() {
		return publicStateChangeMethods;
	}
	
	public boolean usesFinalClasses() {
		return finalClasses;
	}
	
}
