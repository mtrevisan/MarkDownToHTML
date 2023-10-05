package com.vladsch.flexmark.ext.highlight.internal;

import com.vladsch.flexmark.ext.highlight.Highlight;
import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.core.delimiter.Delimiter;
import com.vladsch.flexmark.parser.delimiter.DelimiterProcessor;
import com.vladsch.flexmark.parser.delimiter.DelimiterRun;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;


public class HighlightDelimiterProcessor implements DelimiterProcessor{

	@Override
	public char getOpeningCharacter(){
		return '=';
	}

	@Override
	public char getClosingCharacter(){
		return '=';
	}

	@Override
	public int getMinLength(){
		return 2;
	}

	@Override
	public boolean canBeOpener(final String before, final String after, final boolean leftFlanking, final boolean rightFlanking,
			final boolean beforeIsPunctuation, final boolean afterIsPunctuation, final boolean beforeIsWhitespace,
			final boolean afterIsWhiteSpace){
		return leftFlanking;
	}

	@Override
	public boolean canBeCloser(final String before, final String after, final boolean leftFlanking, final boolean rightFlanking,
			final boolean beforeIsPunctuation, final boolean afterIsPunctuation, final boolean beforeIsWhitespace,
			final boolean afterIsWhiteSpace){
		return rightFlanking;
	}

	@Override
	public boolean skipNonOpenerCloser(){
		return false;
	}

	@Override
	public int getDelimiterUse(final DelimiterRun opener, final DelimiterRun closer){
		return (opener.length() >= 2 && closer.length() >= 2? 2: 0);
	}

	@Override
	public Node unmatchedDelimiterNode(final InlineParser inlineParser, final DelimiterRun delimiter){
		return null;
	}

	@Override
	public void process(final Delimiter opener, final Delimiter closer, final int delimitersUsed){
		//normal case, wrap nodes between delimiters in strikethrough.
		final Highlight highlight = new Highlight(opener.getTailChars(delimitersUsed), BasedSequence.NULL,
			closer.getLeadChars(delimitersUsed));
		opener.moveNodesBetweenDelimitersTo(highlight, closer);
	}

}
