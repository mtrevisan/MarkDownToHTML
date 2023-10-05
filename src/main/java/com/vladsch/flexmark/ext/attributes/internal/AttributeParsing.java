package com.vladsch.flexmark.ext.attributes.internal;

import com.vladsch.flexmark.ast.util.Parsing;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;

import java.util.regex.Pattern;


class AttributeParsing{

	final Parsing myParsing;
	final Pattern ATTRIBUTES_TAG;
	final Pattern ATTRIBUTE;


	public AttributeParsing(final Parsing parsing){
		this.myParsing = parsing;
		String unquotedValue = myParsing.UNQUOTEDVALUE;//.replace("]+","}{"); // exclude braces
		this.ATTRIBUTE = Pattern.compile("\\s*([#.]" + unquotedValue + "|" + myParsing.ATTRIBUTENAME
			+ ")\\s*(?:=\\s*(" + myParsing.ATTRIBUTEVALUE + ")?" + ")?");

		if(AttributesExtension.USE_EMPTY_IMPLICIT_AS_SPAN_DELIMITER.get(parsing.options))
			this.ATTRIBUTES_TAG = Pattern.compile("^\\{((?:[#.])|(?:" + "\\s*([#.]" + unquotedValue + "|" + myParsing.ATTRIBUTENAME
				+ ")\\s*(?:=\\s*(" + myParsing.ATTRIBUTEVALUE + ")?" + ")?" + ")" + "(?:" + "\\s+([#.]" + unquotedValue + "|"
				+ myParsing.ATTRIBUTENAME + ")\\s*(?:=\\s*(" + myParsing.ATTRIBUTEVALUE + ")?" + ")?" + ")*" + "\\s*)\\}");
		else
			this.ATTRIBUTES_TAG = Pattern.compile("^\\{((?:" + "\\s*([#.]" + unquotedValue + "|" + myParsing.ATTRIBUTENAME
				+ ")\\s*(?:=\\s*(" + myParsing.ATTRIBUTEVALUE + ")?" + ")?" + ")" + "(?:" + "\\s+([#.]" + unquotedValue + "|"
				+ myParsing.ATTRIBUTENAME + ")\\s*(?:=\\s*(" + myParsing.ATTRIBUTEVALUE + ")?" + ")?" + ")*" + "\\s*)\\}");
	}

}
