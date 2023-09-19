package com.vladsch.flexmark.ext.footnotes.internal;

import com.vladsch.flexmark.ext.footnotes.Footnote;
import com.vladsch.flexmark.ext.footnotes.FootnoteBlock;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.parser.LinkRefProcessor;
import com.vladsch.flexmark.parser.LinkRefProcessorFactory;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;


public class FootnoteLinkRefProcessor implements LinkRefProcessor{

	static final boolean WANT_EXCLAMATION_PREFIX = false;
	static final int BRACKET_NESTING_LEVEL = 0;

	private final FootnoteRepository footnoteRepository;


	public FootnoteLinkRefProcessor(final Document document){
		this.footnoteRepository = FootnoteExtension.FOOTNOTES.get(document);
	}

	@Override
	public boolean getWantExclamationPrefix(){
		return WANT_EXCLAMATION_PREFIX;
	}

	@Override
	public int getBracketNestingLevel(){
		return BRACKET_NESTING_LEVEL;
	}

	@Override
	public boolean isMatch(@NotNull final BasedSequence nodeChars){
		return (nodeChars.length() >= 3
			&& nodeChars.charAt(0) == '['
			&& nodeChars.charAt(1) == '^'
			&& nodeChars.endCharAt(1) == ']');
	}

	@NotNull
	@Override
	public Node createNode(@NotNull final BasedSequence nodeChars){
		final BasedSequence footnoteId = nodeChars.midSequence(2, - 1)
			.trim();
		final FootnoteBlock footnoteBlock = (!footnoteId.isEmpty()? footnoteRepository.get(footnoteId.toString()): null);

		final Footnote footnote = new Footnote(nodeChars.subSequence(0, 2), footnoteId, nodeChars.endSequence(1));
		footnote.setFootnoteBlock(footnoteBlock);

		if(footnoteBlock != null)
			footnoteRepository.addFootnoteReference(footnoteBlock, footnote);

		return footnote;
	}

	@NotNull
	@Override
	public BasedSequence adjustInlineText(@NotNull final Document document, @NotNull final Node node){
		assert node instanceof Footnote;

		return ((Footnote)node).getText();
	}

	@Override
	public boolean allowDelimiters(@NotNull final BasedSequence chars, @NotNull final Document document, @NotNull final Node node){
		return true;
	}

	@Override
	public void updateNodeElements(@NotNull final Document document, @NotNull final Node node){}


	public static class Factory implements LinkRefProcessorFactory{
		@NotNull
		@Override
		public LinkRefProcessor apply(@NotNull final Document document){
			return new FootnoteLinkRefProcessor(document);
		}

		@Override
		public boolean getWantExclamationPrefix(@NotNull final DataHolder options){
			return WANT_EXCLAMATION_PREFIX;
		}

		@Override
		public int getBracketNestingLevel(@NotNull final DataHolder options){
			return BRACKET_NESTING_LEVEL;
		}
	}

}
