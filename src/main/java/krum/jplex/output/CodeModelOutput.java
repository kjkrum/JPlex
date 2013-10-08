package krum.jplex.output;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import krum.jplex.input.LexerSpec;
import krum.jplex.input.Rule;
import krum.jplex.input.RuleGroup;
import krum.jplex.input.State;

import dk.brics.automaton.TokenAutomaton;
import dk.brics.automaton.TokenDetails;

import org.slf4j.Logger;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JCase;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JSwitch;
import com.sun.codemodel.JVar;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;

public class CodeModelOutput {
	
	protected LexerSpec spec;
	protected JCodeModel cm;
	protected JDefinedClass	statesEnum,
							stateListenerIfc,
							stateLoggerClass,
							eventListenerIfc,
							eventAdapterClass,
							eventLoggerClass,
							lexerClass,
							underflowClass,
							unmatchedClass;

							
	protected Map<Rule, Integer> actionRules;
	protected boolean anyStateIsStrict = false;

	public CodeModelOutput(LexerSpec spec) throws JClassAlreadyExistsException {
		this.spec = spec;
		
		actionRules = new HashMap<Rule, Integer>();
		int actions = 0;
		for(Rule r : spec.allRules()) {
			if(r.hasEvent() || r.hasStateChange()) {
				actionRules.put(r, actions);
				++actions;
			}
		}
		
		for(State state : spec.states()) {
			if(state.isStrict()) {
				anyStateIsStrict = true;
				break;
			}
		}
		
		cm = new JCodeModel();
		exceptions();
		states();
		stateListener();
		eventListener();
		loggers();
		lexer();
	}
	
	public void build(File javaRoot, File resourceRoot) throws IOException {
		
		// compile automatons for each state
		Map<State, TokenAutomaton> automatons = new HashMap<State, TokenAutomaton>();
		for(State s : spec.states()) {
			List<Rule> rules = new LinkedList<Rule>();
			for(RuleGroup g : spec.ruleGroups()) {
				if(g.appliesToState(s)) {
					rules.addAll(g.getRules());
				}
			}
			Automaton a = new Automaton();
			if(rules.size() > 0) {
				Collections.sort(rules);
				Collections.reverse(rules);
				for(Rule rule : rules) {
					try {
						Automaton b = new RegExp(rule.getExpr()).toAutomaton();
						if(actionRules.containsKey(rule)) {
							for(dk.brics.automaton.State state : b.getAcceptStates()) {
								state.setInfo(actionRules.get(rule));
							}
						}
						a = a.minus(b).union(b);
					}
					catch(RuntimeException e) {
						System.err.println("Expression: \"" + rule.getExpr() + '"');
						throw e;
					}
				}
			}
			else {
				System.err.println("Warning: no rules in state " + s.getName()); // TODO: move this to input validator?
			}
			automatons.put(s, new TokenAutomaton(a, true, true));
		}
		
		// serialize automatons
		File resourceDir = new File(resourceRoot, spec.resourcePath());
		if(!resourceDir.exists()) resourceDir.mkdirs();
		for(State s : spec.states()) {
			System.out.println(spec.resourcePath() + "/" + s.getName() + ".automaton.gz");
			File file = new File(resourceDir, s.getName() + ".automaton.gz");
			FileOutputStream fout = new FileOutputStream(file);
			GZIPOutputStream gzout = new GZIPOutputStream(fout);
			automatons.get(s).store(gzout);
			gzout.close();
		}

		javaRoot.mkdirs();
		cm.build(javaRoot);
	}
	
	
	protected void exceptions() throws JClassAlreadyExistsException {
		String pkg = spec.package_();
		if(!"".equals(pkg)) {
			pkg += ".";
		}
		
		underflowClass = cm._class(JMod.PUBLIC, pkg + "UnderflowException", ClassType.CLASS);
		underflowClass._extends(IOException.class);
		underflowClass.field(JMod.PRIVATE + JMod.STATIC + JMod.FINAL, long.class, "serialVersionUID", JExpr.lit(1L));
		underflowClass.direct("// GENERATED CODE - DO NOT EDIT!");
		
		JMethod ctor = underflowClass.constructor(JMod.PUBLIC);
		ctor.body().directStatement("super();");
		
		ctor = underflowClass.constructor(JMod.PUBLIC);
		ctor.param(String.class, "message");
		ctor.body().directStatement("super(message);");
		
		ctor = underflowClass.constructor(JMod.PUBLIC);
		ctor.param(Throwable.class, "cause");
		ctor.body().directStatement("super(cause);");
		
		ctor = underflowClass.constructor(JMod.PUBLIC);
		ctor.param(String.class, "message");
		ctor.param(Throwable.class, "cause");
		ctor.body().directStatement("super(message, cause);");
		
		if(anyStateIsStrict) {
			unmatchedClass = cm._class(JMod.PUBLIC, pkg + "UnmatchedInputException", ClassType.CLASS);
			unmatchedClass._extends(IOException.class);
			unmatchedClass.field(JMod.PRIVATE + JMod.STATIC + JMod.FINAL, long.class, "serialVersionUID", JExpr.lit(1L));
			unmatchedClass.field(JMod.PROTECTED + JMod.FINAL, int.class, "index");
			unmatchedClass.direct("// GENERATED CODE - DO NOT EDIT!");
			
			ctor = unmatchedClass.constructor(JMod.PUBLIC);
			ctor.param(int.class, "index");
			ctor.body().directStatement("super();");
			ctor.body().directStatement("this.index = index;");
			
			ctor = unmatchedClass.constructor(JMod.PUBLIC);
			ctor.param(int.class, "index");
			ctor.param(String.class, "message");
			ctor.body().directStatement("super(message);");
			ctor.body().directStatement("this.index = index;");
			
			ctor = unmatchedClass.constructor(JMod.PUBLIC);
			ctor.param(int.class, "index");
			ctor.param(Throwable.class, "cause");
			ctor.body().directStatement("super(cause);");
			ctor.body().directStatement("this.index = index;");
			
			ctor = unmatchedClass.constructor(JMod.PUBLIC);
			ctor.param(int.class, "index");
			ctor.param(String.class, "message");
			ctor.param(Throwable.class, "cause");
			ctor.body().directStatement("super(message, cause);");
			ctor.body().directStatement("this.index = index;");
			
			JMethod getter = unmatchedClass.method(JMod.PUBLIC, int.class, "getIndex");
			getter.body().directStatement("return index;");
		}
	}
	
	protected void states() throws JClassAlreadyExistsException {
		statesEnum = cm._class(JMod.PUBLIC, spec.fqnPrefix() + ClassNames.STATES_ENUM, ClassType.ENUM);
		for(State s : spec.states()) {
			statesEnum.enumConstant(s.getName());
		}
	}
	
	protected void stateListener() throws JClassAlreadyExistsException {
		if(!spec.usesStateListener()) return;
		
		stateListenerIfc = cm._class(JMod.PUBLIC, spec.fqnPrefix() + ClassNames.STATE_LISTENER, ClassType.INTERFACE);
		JMethod statePushed = stateListenerIfc.method(JMod.PUBLIC, void.class, "statePushed");
		statePushed.param(statesEnum, "state");
		JMethod statePopped = stateListenerIfc.method(JMod.PUBLIC, void.class, "statePopped");
		statePopped.param(statesEnum, "state");
		JMethod stateJumped = stateListenerIfc.method(JMod.PUBLIC, void.class, "stateJumped");
		stateJumped.param(statesEnum, "state");
	}
	
	protected void eventListener() throws JClassAlreadyExistsException {
		eventListenerIfc = cm._class(JMod.PUBLIC, spec.fqnPrefix() + ClassNames.EVENT_LISTENER, ClassType.INTERFACE);
		eventAdapterClass = cm._class(JMod.PUBLIC, spec.fqnPrefix() + ClassNames.EVENT_ADAPTER, ClassType.CLASS);
		eventAdapterClass._implements(eventListenerIfc);
		
		Set<String> eventSet = new HashSet<String>(); // avoids duplicate methods
		for(Rule r : spec.allRules()) {
			if(r.hasEvent()) {
				String event = r.getEvent();
				if(!eventSet.contains(event)) {
					eventSet.add(event);
					JMethod listenerMethod = eventListenerIfc.method(JMod.PUBLIC, void.class, r.getEvent());
					listenerMethod.param(CharSequence.class, "seq");
					listenerMethod.param(int.class, "off");
					listenerMethod.param(int.class, "len");
					JMethod adapterMethod = eventAdapterClass.method(JMod.PUBLIC, void.class, r.getEvent());
					adapterMethod.annotate(Override.class);
					adapterMethod.param(CharSequence.class, "seq");
					adapterMethod.param(int.class, "off");
					adapterMethod.param(int.class, "len");
				}
			}
		}
	}
	
	protected void loggers() throws JClassAlreadyExistsException {
		if(!spec.usesLoggers()) return;
		
		eventLoggerClass = cm._class(JMod.PUBLIC, spec.fqnPrefix() + ClassNames.EVENT_LOGGER, ClassType.CLASS);
		eventLoggerClass._implements(eventListenerIfc);
		JFieldVar logger = eventLoggerClass.field(JMod.PROTECTED, Logger.class, "logger");
		logger.init(JExpr.direct("org.slf4j.LoggerFactory.getLogger(" + spec.fqnPrefix() + ClassNames.EVENT_LOGGER + ".class)"));
	
		Set<String> eventSet = new HashSet<String>(); // avoids duplicate methods
		for(Rule r : spec.allRules()) {
			if(r.hasEvent()) {
				String event = r.getEvent();
				if(!eventSet.contains(event)) {
					eventSet.add(event);
					JMethod method = eventLoggerClass.method(JMod.PUBLIC, void.class, r.getEvent());
					method.annotate(Override.class);
					method.param(CharSequence.class, "seq");
					method.param(int.class, "off");
					method.param(int.class, "len");
					method.body().directStatement("logger.debug(\"" + r.getEvent() + ": \" + seq.subSequence(off, off + len));");
				}
			}
		}

		if(spec.usesStateListener()) {
			stateLoggerClass = cm._class(JMod.PUBLIC, spec.fqnPrefix() + ClassNames.STATE_LOGGER, ClassType.CLASS);
			stateLoggerClass._implements(stateListenerIfc);
			JFieldVar logger2 = stateLoggerClass.field(JMod.PROTECTED, Logger.class, "logger");
			logger2.init(JExpr.direct("org.slf4j.LoggerFactory.getLogger(" + spec.fqnPrefix() + ClassNames.STATE_LOGGER + ".class)"));
			
			JMethod logPushed = stateLoggerClass.method(JMod.PUBLIC, void.class, "statePushed");
			logPushed.annotate(Override.class);
			logPushed.param(statesEnum, "state");
			logPushed.body().directStatement("logger.debug(\"State pushed: \" + state.name());");
			
			JMethod logPopped = stateLoggerClass.method(JMod.PUBLIC, void.class, "statePopped");
			logPopped.annotate(Override.class);
			logPopped.param(statesEnum, "state");
			logPopped.body().directStatement("logger.debug(\"State popped: \" + state.name());");
			
			JMethod logJumped = stateLoggerClass.method(JMod.PUBLIC, void.class, "stateJumped");
			logJumped.annotate(Override.class);
			logJumped.param(statesEnum, "state");
			logJumped.body().directStatement("logger.debug(\"State jumped: \" + state.name());");
		}

	}
	
	protected void lexer() throws JClassAlreadyExistsException {
		lexerClass = cm._class(JMod.PUBLIC, spec.fqnPrefix() + ClassNames.LEXER, ClassType.CLASS);
		JMethod ctor = lexerClass.constructor(JMod.PUBLIC);
		ctor._throws(IOException.class);
		ctor._throws(ClassNotFoundException.class);
		ctor.body().directStatement("init();");
		
		lexerFields();
		lexerStateChangeMethods();
		lexerInitMethod();
		lexerResetMethod();
		lexerDispatchMethod();
		lexerLexMethod();
		lexerAddListenerMethods();
	}

	protected void lexerFields() {
		// state->automaton map
		JClass automatonClass = cm.ref(TokenAutomaton.class);
		JClass stateMapIfc = cm.ref(Map.class).narrow(statesEnum, automatonClass);
		JVar stateMapField = lexerClass.field(JMod.PROTECTED, stateMapIfc, "automatonMap");
		JClass stateMapClass = cm.ref(HashMap.class).narrow(statesEnum, automatonClass);
		stateMapField.init(JExpr._new(stateMapClass));
		
		// state->strictness map
		JClass booleanClass = cm.ref(Boolean.class);
		JClass strictnessMapIfc = cm.ref(Map.class).narrow(statesEnum, booleanClass);
		JVar strictnessMapField = lexerClass.field(JMod.PROTECTED, strictnessMapIfc, "strictMap");
		JClass strictnessMapClass = cm.ref(HashMap.class).narrow(statesEnum, booleanClass);
		strictnessMapField.init(JExpr._new(strictnessMapClass));
				
		// state stack
		JClass stackIfc = cm.ref(Deque.class).narrow(statesEnum);
		JVar stackField = lexerClass.field(JMod.PROTECTED, stackIfc, "stateStack");
		JClass stackClass = cm.ref(LinkedList.class).narrow(statesEnum);
		stackField.init(JExpr._new(stackClass));
					
		// current state
		JVar stateField = lexerClass.field(JMod.PROTECTED, statesEnum, "state");
		stateField.init(JExpr._null());
		
		// current strictness
		JVar strictnessField = lexerClass.field(JMod.PROTECTED, boolean.class, "strict");
		strictnessField.init(JExpr.FALSE);
		
		// current automaton
		JVar automatonField = lexerClass.field(JMod.PROTECTED, automatonClass, "automaton");
		automatonField.init(JExpr._null());
						
		// event listeners
		JClass eventListenerIfc = cm._getClass(spec.fqnPrefix() + "EventListener");
		JClass eventListIfc = cm.ref(List.class).narrow(eventListenerIfc);
		JVar eventListenersField = lexerClass.field(JMod.PROTECTED, eventListIfc, "eventListeners");
		JClass eventListClass = cm.ref(ArrayList.class).narrow(eventListenerIfc);
		eventListenersField.init(JExpr._new(eventListClass));
								
		// state listeners
		if(spec.usesStateListener()) {
			JClass stateListenerIfc = cm._getClass(spec.fqnPrefix() + "StateListener");
			JClass stateListIfc = cm.ref(List.class).narrow(stateListenerIfc);
			JVar stateListenersField = lexerClass.field(JMod.PROTECTED, stateListIfc, "stateListeners");
			JClass stateListClass = cm.ref(ArrayList.class).narrow(stateListenerIfc);
			stateListenersField.init(JExpr._new(stateListClass));
		}
		
		// token details
		JVar tokenField = lexerClass.field(JMod.PROTECTED, TokenDetails.class, "token");
		tokenField.init(JExpr._new(cm.ref(TokenDetails.class)));
	}
	
	protected void lexerStateChangeMethods() {
		int visibility;
		if(spec.usesPublicStateChangeMethods()) visibility = JMod.PUBLIC;
		else visibility = JMod.PROTECTED;
		
		// pushState
		JMethod pushState = lexerClass.method(visibility, void.class, "pushState");
		pushState.param(statesEnum, "state");
		pushState.body().directStatement("stateStack.push(this.state);");
		pushState.body().directStatement("this.state = state;");
		pushState.body().directStatement("automaton = automatonMap.get(state);");
		pushState.body().directStatement("strict = strictMap.get(state);");
		if(spec.usesStateListener()) {
			//pushState.body().directStatement("for(" + stateListenerIfc.name() + " l : stateListeners) l.statePushed(state);");
			pushState.body().directStatement("for(int i = 0; i < stateListeners.size(); ++i) { stateListeners.get(i).statePushed(state); }");
		}
				
		// popState
		JMethod popState = lexerClass.method(visibility, void.class, "popState");
		popState.body().directStatement("state = stateStack.pop();");
		popState.body().directStatement("automaton = automatonMap.get(state);");
		popState.body().directStatement("strict = strictMap.get(state);");
		if(spec.usesStateListener()) {
			//popState.body().directStatement("for(" + stateListenerIfc.name() + " l : stateListeners) l.statePopped(state);");
			popState.body().directStatement("for(int i = 0; i < stateListeners.size(); ++i) { stateListeners.get(i).statePopped(state); }");
		}
				
		// jumpState
		JMethod jumpState = lexerClass.method(visibility, void.class, "jumpState");
		jumpState.param(statesEnum, "state");
		jumpState.body().directStatement("stateStack.clear();");
		jumpState.body().directStatement("this.state = state;");
		jumpState.body().directStatement("automaton = automatonMap.get(state);");
		jumpState.body().directStatement("strict = strictMap.get(state);");
		if(spec.usesStateListener()) {
			//jumpState.body().directStatement("for(" + stateListenerIfc.name() + " l : stateListeners) l.stateJumped(state);");
			jumpState.body().directStatement("for(int i = 0; i < stateListeners.size(); ++i) { stateListeners.get(i).stateJumped(state); }");
		}
	}

	protected void lexerInitMethod() {		
		JMethod init = lexerClass.method(JMod.PROTECTED, void.class, "init");
		init._throws(IOException.class);
		init._throws(ClassNotFoundException.class);
		
		// deserialize automatons
		String path = "/" + spec.resourcePath() + "/";
		// no runtime state collection to iterate - unroll it here
		for(State s : spec.states()) {
			init.body().directStatement("automatonMap.put(" + spec.prefix() + ClassNames.STATES_ENUM + '.' + s.getName() + ", " +
					"TokenAutomaton.load(new java.util.zip.GZIPInputStream(" + spec.prefix() + ClassNames.LEXER +
					".class.getResourceAsStream(\"" + path + s.getName() + ".automaton.gz\"))));");
			init.body().directStatement("strictMap.put(" + spec.prefix() + ClassNames.STATES_ENUM + '.' + s.getName() + ", " +
					Boolean.toString(s.isStrict()) + ");");
		}
		
		// initialize current state, automaton, and strictness
		init.body().directStatement("state = " + spec.prefix() + ClassNames.STATES_ENUM + '.' + spec.initialState().getName() + ';');
		init.body().directStatement("automaton = automatonMap.get(state);");
		init.body().directStatement("strict = strictMap.get(state);");
	}
	
	protected void lexerResetMethod() {
		JMethod reset = lexerClass.method(JMod.PUBLIC, void.class, "reset");
		reset.body().directStatement("eventListeners.clear();");
		reset.body().directStatement("jumpState(" + spec.prefix() + ClassNames.STATES_ENUM + "." + spec.initialState().getName() + ");");
	}
	
	protected void lexerDispatchMethod() {
		// what the generated method needs to do:
		// takes a TokenDetails
		// switches on (Integer) token.info.intValue()
		// case labels are action rule values assigned in init method
		// add rule expressions as comments
		// each case calls event listeners and/or state change methods, and breaks
		
		JMethod dispatch = lexerClass.method(JMod.PROTECTED, void.class, "dispatch");
		dispatch.param(TokenDetails.class, "token");
		
		dispatch.body().directStatement("int action = (Integer) token.info;");
		JSwitch bigSwitch = dispatch.body()._switch(JExpr.direct("action"));
		
		List<Map.Entry<Rule, Integer>> entryList =
				new LinkedList<Map.Entry<Rule, Integer>>(actionRules.entrySet());
		Collections.sort(entryList, new Comparator<Map.Entry<Rule, Integer>>() {
			@Override
			public int compare(Entry<Rule, Integer> arg0, Entry<Rule, Integer> arg1) {
				return arg0.getValue() - arg1.getValue();
			}
		});
		
		for(Map.Entry<Rule, Integer> entry : entryList) {
			JCase ruleCase = bigSwitch._case(JExpr.lit(entry.getValue()));
			Rule rule = entry.getKey();
			// replace certain character literals with their entity expressions
			// otherwise the comment isn't valid Java and the lexer won't compile
			// (tab is replaced just for niceness)
			String comment = rule.getExpr();
			comment = comment.replace("\n", "\\n");
			comment = comment.replace("\r", "\\r");
			comment = comment.replace("\t", "\\t");
			ruleCase.body().directStatement("// \"" + comment + '\"');
			if(rule.hasEvent()) {
				ruleCase.body().directStatement("for(int i = 0; i < eventListeners.size(); ++i) { eventListeners.get(i)." + rule.getEvent() + "(token.seq, token.off, token.len); }");
			}
			if(rule.hasStateChange()) {
				switch(rule.getStateChange().getType()) {
				case PUSH:
					ruleCase.body().directStatement("pushState(" + spec.fqnPrefix() + ClassNames.STATES_ENUM + '.' + rule.getStateChange().getState().getName() + ");");
					break;
				case POP:
					ruleCase.body().directStatement("popState();");
					break;
				case JUMP:
					//System.out.println("rule.getStateChange(): " + rule.getStateChange());
					//System.out.println("rule.getStateChange().getState(): " + rule.getStateChange().getState());
					//System.out.println("rule.getStateChange().getState().getName(): " + rule.getStateChange().getState().getName());
					ruleCase.body().directStatement("jumpState(" + spec.fqnPrefix() + ClassNames.STATES_ENUM + '.' + rule.getStateChange().getState().getName() + ");");
					break;
				}
			}			
			ruleCase.body()._break();
		}
		
		// TODO: default case for debugging?
		
	}
	
	protected void lexerLexMethod() {
		// what the generated method needs to do:
		// takes a CharSequence
		// start at index 0
		// automaton.find(...)
		// if token found, dispatch and index += token.length
		// else if token.info is UNDERFLOW, return
		// else if current state is strict, throw something
		// else ++index
		// repeat to end of char seq
		// return number of characters lexed
		
		JMethod lex = lexerClass.method(JMod.PUBLIC, int.class, "lex");
		lex._throws(underflowClass);
		if(anyStateIsStrict) lex._throws(unmatchedClass);
		lex.param(CharSequence.class, "seq");
		lex.param(int.class, "off");
		lex.param(int.class, "len");
		lex.param(boolean.class, "endOfInput");
		
		lex.body().directStatement("if(off < 0 || len < 0 || off + len > seq.length()) throw new IndexOutOfBoundsException();");
		// lex.body().directStatement("TokenDetails token = new TokenDetails();"); // made this a class member
		lex.body().directStatement("int index = off;");
		lex.body().directStatement("while(index < off + len) {");
		lex.body().directStatement("	if(automaton.find(seq, index, endOfInput, token)) {");
		lex.body().directStatement("		if(token.info != null) dispatch(token);");
		lex.body().directStatement("		index += token.len;");
		lex.body().directStatement("	}");
		lex.body().directStatement("	else if(token.info == TokenDetails.UNDERFLOW) {");
		lex.body().directStatement("		if(index == 0) throw new UnderflowException();");
		lex.body().directStatement("		else return index;");
		lex.body().directStatement("	}");
		if(anyStateIsStrict) {
			lex.body().directStatement("	else if(strict) {");
			lex.body().directStatement("		throw new UnmatchedInputException(index);");
			lex.body().directStatement("	}");
		}
		lex.body().directStatement("	else ++index;");
		lex.body().directStatement("}");
		lex.body().directStatement("return index;");
	}

	
	private void lexerAddListenerMethods() {
		JMethod addEventListener = lexerClass.method(JMod.PUBLIC, void.class, "addEventListener");
		addEventListener.param(eventListenerIfc, "eventListener");
		addEventListener.body().directStatement("eventListeners.add(eventListener);");
		
		JMethod removeEventListener = lexerClass.method(JMod.PUBLIC, boolean.class, "removeEventListener");
		removeEventListener.param(eventListenerIfc, "eventListener");
		removeEventListener.body().directStatement("return eventListeners.remove(eventListener);");
		
		if(spec.usesStateListener()) {
			JMethod addStateListener = lexerClass.method(JMod.PUBLIC, void.class, "addStateListener");
			addStateListener.param(stateListenerIfc, "stateListener");
			addStateListener.body().directStatement("stateListeners.add(stateListener);");
			
			JMethod removeStateListener = lexerClass.method(JMod.PUBLIC, boolean.class, "removeStateListener");
			removeStateListener.param(stateListenerIfc, "stateListener");
			removeStateListener.body().directStatement("return stateListeners.remove(stateListener);");
		}
	}
	
}
