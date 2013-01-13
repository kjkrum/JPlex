package krum.jplex.input;

import java.util.HashSet;
import java.util.Set;

public class ReservedWords {
	
	protected static final Set<String> keywords = new HashSet<String>();
		
	protected ReservedWords() { }
	
	static {
		keywords.add("abstract");
		keywords.add("assert");
		keywords.add("boolean");
		keywords.add("break");
		keywords.add("byte");
		keywords.add("case");
		keywords.add("catch");
		keywords.add("char");
		keywords.add("class");
		keywords.add("const");
		keywords.add("continue");
		keywords.add("default");
		keywords.add("do");
		keywords.add("double");
		keywords.add("else");
		keywords.add("enum");
		keywords.add("extends");
		keywords.add("final");
		keywords.add("finally");
		keywords.add("float");
		keywords.add("for");
		keywords.add("goto");
		keywords.add("if");
		keywords.add("implements");
		keywords.add("import");
		keywords.add("instanceof");
		keywords.add("int");
		keywords.add("interface");
		keywords.add("long");
		keywords.add("native");
		keywords.add("new");
		keywords.add("package");
		keywords.add("private");
		keywords.add("protected");
		keywords.add("public");
		keywords.add("return");
		keywords.add("short");
		keywords.add("static");
		keywords.add("strictfp");
		keywords.add("super");
		keywords.add("switch");
		keywords.add("synchronized");
		keywords.add("this");
		keywords.add("throw");
		keywords.add("throws");
		keywords.add("transient");
		keywords.add("try");
		keywords.add("void");
		keywords.add("volatile");
		keywords.add("while");
		// technically literals, not keywords
		// but anyway, not allowed
		keywords.add("true");
		keywords.add("false");
		keywords.add("null");
	}
	
	public static boolean contains(String string) {
		return keywords.contains(string);
	}
}
