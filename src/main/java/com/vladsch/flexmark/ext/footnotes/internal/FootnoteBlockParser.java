package com.vladsch.flexmark.ext.footnotes.internal;

import com.vladsch.flexmark.ext.footnotes.FootnoteBlock;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.parser.block.*;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.BlockContent;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FootnoteBlockParser extends AbstractBlockParser{

	static String FOOTNOTE_ID = ".*";
	static Pattern FOOTNOTE_ID_PATTERN = Pattern.compile("\\[\\^\\s*(" + FOOTNOTE_ID + ")\\s*\\]");
	static Pattern FOOTNOTE_DEF_PATTERN = Pattern.compile("^\\[\\^\\s*(" + FOOTNOTE_ID + ")\\s*\\]:");

	private final FootnoteBlock block = new FootnoteBlock();
	private final FootnoteOptions options;
	private BlockContent content = new BlockContent();


	public FootnoteBlockParser(final FootnoteOptions options, final int contentOffset){
		this.options = options;
	}

	public BlockContent getBlockContent(){
		return content;
	}

	@Override
	public Block getBlock(){
		return block;
	}

	@Override
	public BlockContinue tryContinue(final ParserState state){
		final int nonSpaceIndex = state.getNextNonSpaceIndex();
		if(state.isBlank()){
			if(block.getFirstChild() == null)
				//blank line after empty list item
				return BlockContinue.none();

			return BlockContinue.atIndex(nonSpaceIndex);
		}

		if(state.getIndent() >= options.contentIndent){
			final int contentIndent = state.getIndex() + options.contentIndent;
			return BlockContinue.atIndex(contentIndent);
		}

		return BlockContinue.none();
	}

	@Override
	public void addLine(final ParserState state, final BasedSequence line){
		content.add(line, state.getIndent());
	}

	@Override
	public void closeBlock(final ParserState state){
		//set the footnote from closingMarker to end
		block.setCharsFromContent();
		block.setFootnote(block.getChars().subSequence(block.getClosingMarker().getEndOffset()
			- block.getStartOffset()).trimStart());
		//add it to the map
		final FootnoteRepository footnoteMap = FootnoteExtension.FOOTNOTES.get(state.getProperties());
		footnoteMap.put(footnoteMap.normalizeKey(block.getText()), block);
		content = null;
	}

	@Override
	public boolean isContainer(){
		return true;
	}

	@Override
	public boolean canContain(final ParserState state, final BlockParser blockParser, final Block block){
		return true;
	}


	public static class Factory implements CustomBlockParserFactory{
		@Override
		public Set<Class<?>> getAfterDependents(){
			return null;
		}

		@Override
		public Set<Class<?>> getBeforeDependents(){
			return null;
		}

		@Override
		public boolean affectsGlobalScope(){
			return false;
		}

		@Override
		public BlockParserFactory apply(final DataHolder options){
			return new BlockFactory(options);
		}
	}


	private static class BlockFactory extends AbstractBlockParserFactory{
		final private FootnoteOptions options;

		private BlockFactory(final DataHolder options){
			super(options);

			this.options = new FootnoteOptions(options);
		}

		@Override
		public BlockStart tryStart(final ParserState state, final MatchedBlockParser matchedBlockParser){
			if(state.getIndent() >= 4)
				return BlockStart.none();

			final BasedSequence line = state.getLine();
			final int nextNonSpace = state.getNextNonSpaceIndex();

			final BasedSequence trySequence = line.subSequence(nextNonSpace, line.length());
			final Matcher matcher = FOOTNOTE_DEF_PATTERN.matcher(trySequence);
			if(matcher.find()){
				//abbreviation definition
				final int openingStart = nextNonSpace + matcher.start();
				final int openingEnd = nextNonSpace + matcher.end();
				final BasedSequence openingMarker = line.subSequence(openingStart, openingStart + 2);
				final BasedSequence text = line.subSequence(openingStart + 2, openingEnd - 2).trim();
				final BasedSequence closingMarker = line.subSequence(openingEnd - 2, openingEnd);

				final int contentOffset = options.contentIndent;

				final FootnoteBlockParser footnoteBlockParser = new FootnoteBlockParser(options, contentOffset);
				footnoteBlockParser.block.setOpeningMarker(openingMarker);
				footnoteBlockParser.block.setText(text);
				footnoteBlockParser.block.setClosingMarker(closingMarker);

				return BlockStart.of(footnoteBlockParser).atIndex(openingEnd);
			}

			return BlockStart.none();
		}
	}

}
