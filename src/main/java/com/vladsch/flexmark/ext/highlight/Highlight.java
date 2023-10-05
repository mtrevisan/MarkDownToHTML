package com.vladsch.flexmark.ext.highlight;

import com.vladsch.flexmark.util.ast.DelimitedNode;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;


/**
 * A Highlight node
 */
public class Highlight extends Node implements DelimitedNode{

	protected BasedSequence openingMarker = BasedSequence.NULL;
	protected BasedSequence text = BasedSequence.NULL;
	protected BasedSequence closingMarker = BasedSequence.NULL;
	protected String highlightBlockText;


	public Highlight(){}

	public Highlight(final BasedSequence chars){
		super(chars);
	}

	public Highlight(final BasedSequence openingMarker, final BasedSequence text, final BasedSequence closingMarker){
		super(openingMarker.baseSubSequence(openingMarker.getStartOffset(), closingMarker.getEndOffset()));

		this.openingMarker = openingMarker;
		this.text = text;
		this.closingMarker = closingMarker;
	}

	public Highlight(final BasedSequence chars, final String highlightBlockText){
		super(chars);

		this.highlightBlockText = highlightBlockText;
	}

	@Override
	public BasedSequence[] getSegments(){
		//return EMPTY_SEGMENTS;
		return new BasedSequence[]{openingMarker, text, closingMarker};
	}

	@Override
	public void getAstExtra(final StringBuilder out){
		delimitedSegmentSpanChars(out, openingMarker, text, closingMarker, "text");
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
