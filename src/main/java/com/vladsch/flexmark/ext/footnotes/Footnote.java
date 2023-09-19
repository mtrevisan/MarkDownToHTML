package com.vladsch.flexmark.ext.footnotes;

import com.vladsch.flexmark.ast.LinkRendered;
import com.vladsch.flexmark.ext.footnotes.internal.FootnoteRepository;
import com.vladsch.flexmark.util.ast.*;
import com.vladsch.flexmark.util.sequence.BasedSequence;


/**
 * A Footnote referencing node
 */
public class Footnote extends Node implements DelimitedNode, DoNotDecorate, LinkRendered, ReferencingNode<FootnoteRepository,
		FootnoteBlock>{

	protected BasedSequence openingMarker = BasedSequence.NULL;
	protected BasedSequence text = BasedSequence.NULL;
	protected BasedSequence closingMarker = BasedSequence.NULL;
	protected FootnoteBlock footnoteBlock;

	protected int referenceOrdinal;


	public int getReferenceOrdinal(){
		return referenceOrdinal;
	}

	public void setReferenceOrdinal(final int referenceOrdinal){
		this.referenceOrdinal = referenceOrdinal;
	}

	@NotNull
	@Override
	public BasedSequence getReference(){
		return text;
	}

	@Override
	public FootnoteBlock getReferenceNode(final Document document){
		if(footnoteBlock != null || text.isEmpty())
			return footnoteBlock;

		footnoteBlock = getFootnoteBlock(FootnoteExtension.FOOTNOTES.get(document));
		return footnoteBlock;
	}

	@Override
	public FootnoteBlock getReferenceNode(final FootnoteRepository repository){
		if(footnoteBlock != null || text.isEmpty())
			return footnoteBlock;

		footnoteBlock = getFootnoteBlock(repository);
		return footnoteBlock;
	}

	public boolean isDefined(){
		return footnoteBlock != null;
	}

	/**
	 * @return true if this node will be rendered as text because it depends on a reference which is not defined.
	 */
	@Override
	public boolean isTentative(){
		return footnoteBlock == null;
	}

	public FootnoteBlock getFootnoteBlock(final FootnoteRepository footnoteRepository){
		return (text.isEmpty()? null: footnoteRepository.get(text.toString()));
	}

	public FootnoteBlock getFootnoteBlock(){
		return footnoteBlock;
	}

	public void setFootnoteBlock(final FootnoteBlock footnoteBlock){
		this.footnoteBlock = footnoteBlock;
	}

	@NotNull
	@Override
	public BasedSequence[] getSegments(){
		return new BasedSequence[]{openingMarker, text, closingMarker};
	}

	@Override
	public void getAstExtra(@NotNull final StringBuilder out){
		out.append(" ordinal: ")
			.append(footnoteBlock != null? footnoteBlock.getFootnoteOrdinal(): 0)
			.append(" ");
		delimitedSegmentSpanChars(out, openingMarker, text, closingMarker, "text");
	}

	public Footnote(){}

	public Footnote(final BasedSequence chars){
		super(chars);
	}

	public Footnote(final BasedSequence openingMarker, final BasedSequence text, final BasedSequence closingMarker){
		super(openingMarker.baseSubSequence(openingMarker.getStartOffset(), closingMarker.getEndOffset()));

		this.openingMarker = openingMarker;
		this.text = text;
		this.closingMarker = closingMarker;
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
