package com.vladsch.flexmark.ext.attributes;

import com.vladsch.flexmark.util.ast.DoNotDecorate;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.html.Attribute;
import com.vladsch.flexmark.util.sequence.BasedSequence;


/**
 * An Attribute node representing a single attribute name and value in attributes node
 *
 * @see <a href="https://github.com/vsch/flexmark-java/pull/581">Pull request 581</a>
 */
public class AttributeNode extends Node implements DoNotDecorate{

	protected BasedSequence name = BasedSequence.NULL;
	protected BasedSequence attributeSeparator = BasedSequence.NULL;
	protected BasedSequence openingMarker = BasedSequence.NULL;
	protected BasedSequence value = BasedSequence.NULL;
	protected BasedSequence closingMarker = BasedSequence.NULL;


	@Override
	public BasedSequence[] getSegments(){
		//return EMPTY_SEGMENTS;
		return new BasedSequence[]{name, attributeSeparator, openingMarker, value, closingMarker};
	}

	@Override
	public void getAstExtra(final StringBuilder out){
		segmentSpanChars(out, name, "name");
		segmentSpanChars(out, attributeSeparator, "sep");
		delimitedSegmentSpanChars(out, openingMarker, value, closingMarker, "value");

		if(isImplicitName())
			out.append(" isImplicit");
		if(isClass())
			out.append(" isClass");
		if(isId())
			out.append(" isId");
	}

	public AttributeNode(){}

	public AttributeNode(final BasedSequence chars){
		super(chars);
	}

	public AttributeNode(final BasedSequence name, final BasedSequence attributeSeparator, final BasedSequence openingMarker,
			final BasedSequence value, final BasedSequence closingMarker){
		super(spanningChars(name, attributeSeparator, openingMarker, value, closingMarker));

		this.name = (name != null? name: BasedSequence.NULL);
		this.attributeSeparator = (attributeSeparator != null? attributeSeparator: BasedSequence.NULL);
		this.openingMarker = (openingMarker != null? openingMarker: BasedSequence.NULL);
		this.value = (value != null? value: BasedSequence.NULL);
		this.closingMarker = (closingMarker != null? closingMarker: BasedSequence.NULL);
	}

	public static boolean isImplicitName(final CharSequence text){
		return (!text.isEmpty() && (text.charAt(0) == '.' || text.charAt(0) == '#'));
	}

	public boolean isImplicitName(){
		return (value.isNotNull() && attributeSeparator.isNull() && name.isNotNull());
	}

	public boolean isClass(){
		return (isImplicitName() && name.toString().equals(".")) || !isImplicitName() && name.toString().equals(Attribute.CLASS_ATTR);
	}

	public boolean isId(){
		return (isImplicitName() && name.toString().equals("#")) || !isImplicitName() && name.toString().equals(Attribute.ID_ATTR);
	}

	public BasedSequence getName(){
		return name;
	}

	public void setName(final BasedSequence name){
		this.name = name;
	}

	public BasedSequence getAttributeSeparator(){
		return attributeSeparator;
	}

	public void setAttributeSeparator(final BasedSequence attributeSeparator){
		this.attributeSeparator = attributeSeparator;
	}

	public BasedSequence getValue(){
		return value;
	}

	public void setValue(final BasedSequence value){
		this.value = value;
	}

	public BasedSequence getOpeningMarker(){
		return openingMarker;
	}

	public void setOpeningMarker(final BasedSequence openingMarker){
		this.openingMarker = openingMarker;
	}

	public BasedSequence getClosingMarker(){
		return closingMarker;
	}

	public void setClosingMarker(final BasedSequence closingMarker){
		this.closingMarker = closingMarker;
	}

}
