package com.vladsch.flexmark.ext.attributes;

import com.vladsch.flexmark.util.ast.DelimitedNode;
import com.vladsch.flexmark.util.ast.DoNotDecorate;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NonRenderingInline;
import com.vladsch.flexmark.util.sequence.BasedSequence;


/**
 * A AttributesNode node
 */
public class AttributesNode extends Node implements DelimitedNode, DoNotDecorate, NonRenderingInline{

	protected BasedSequence openingMarker = BasedSequence.NULL;
	protected BasedSequence text = BasedSequence.NULL;
	protected BasedSequence closingMarker = BasedSequence.NULL;


	@Override
	public BasedSequence[] getSegments(){
		//return EMPTY_SEGMENTS;
		return new BasedSequence[]{openingMarker, text, closingMarker};
	}

	@Override
	public void getAstExtra(final StringBuilder out){
		delimitedSegmentSpanChars(out, openingMarker, text, closingMarker, "text");
	}

	public AttributesNode(){}

	public AttributesNode(final BasedSequence chars){
		super(chars);
	}

	public AttributesNode(final BasedSequence openingMarker, final BasedSequence text, final BasedSequence closingMarker){
		super(openingMarker.baseSubSequence(openingMarker.getStartOffset(), closingMarker.getEndOffset()));

		this.openingMarker = openingMarker;
		this.text = text;
		this.closingMarker = closingMarker;
	}

	public AttributesNode(final BasedSequence chars, final String attributesBlockText){
		super(chars);
	}

	public BasedSequence getOpeningMarker(){
		return openingMarker;
	}

	public void setOpeningMarker(final BasedSequence openingMarker){
		this.openingMarker = openingMarker;
	}

	public BasedSequence getText(){
		return text;
	}

	public void setText(final BasedSequence text){
		this.text = text;
	}

	public BasedSequence getClosingMarker(){
		return closingMarker;
	}

	public void setClosingMarker(final BasedSequence closingMarker){
		this.closingMarker = closingMarker;
	}

}
