package krum.jplex.input;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLInput {

	protected XMLInput() { }
	
	public static LexerSpec load(Document doc) throws SAXException, IOException, XPathExpressionException, ValidationException {
		StreamSource source =
			new StreamSource(XMLInput.class.getResourceAsStream("/schema/Input.xsd"));
		SchemaFactory sf =
			SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		Schema schema = sf.newSchema(source);
		Validator validator = schema.newValidator();
		// make it throw exceptions for all errors
		validator.setErrorHandler(new ErrorHandler() {
			@Override
			public void error(SAXParseException e) throws SAXException {
				throw e;				
			}
			@Override
			public void fatalError(SAXParseException e) throws SAXException {
				throw e;					
			}
			@Override
			public void warning(SAXParseException e) throws SAXException {
				throw e;					
			}
		});
		validator.validate(new DOMSource(doc));
			
		XPathFactory xpf = XPathFactory.newInstance();
		XPath xp = xpf.newXPath();
		
		return new LexerSpec(xp, doc);
	}
	
	public static LexerSpec load(File file)
			throws ParserConfigurationException, SAXException, IOException, XPathExpressionException, ValidationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = dbf.newDocumentBuilder();
		return load(builder.parse(file));
	}
}
