package com.vladsch.flexmark.ext.footnotes;

import com.vladsch.flexmark.ext.footnotes.internal.FootnoteBlockParser;
import com.vladsch.flexmark.ext.footnotes.internal.FootnoteLinkRefProcessor;
import com.vladsch.flexmark.ext.footnotes.internal.FootnoteNodeFormatter;
import com.vladsch.flexmark.ext.footnotes.internal.FootnoteNodeRenderer;
import com.vladsch.flexmark.ext.footnotes.internal.FootnoteRepository;
import com.vladsch.flexmark.formatter.Formatter;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.KeepType;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.DataKey;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.format.options.ElementPlacement;
import com.vladsch.flexmark.util.format.options.ElementPlacementSort;


/**
 * Extension for footnotes
 * <p>
 * Create it with {@link #create()} and then configure it on the builders
 * <p>
 * The parsed footnote references in text regions are turned into {@link Footnote} nodes.
 * The parsed footnote definitions are turned into {@link FootnoteBlock} nodes.
 */
public class FootnoteExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension,
		Parser.ReferenceHoldingExtension, Formatter.FormatterExtension{

	public static final DataKey<KeepType> FOOTNOTES_KEEP = new DataKey<>("FOOTNOTES_KEEP", KeepType.FIRST);

	public static final DataKey<FootnoteRepository> FOOTNOTES = new DataKey<>("FOOTNOTES", new FootnoteRepository(null), FootnoteRepository::new);
	public static final DataKey<String> FOOTNOTE_REF_PREFIX = new DataKey<>("FOOTNOTE_REF_PREFIX", "");
	public static final DataKey<String> FOOTNOTE_REF_SUFFIX = new DataKey<>("FOOTNOTE_REF_SUFFIX", "");
	public static final DataKey<String> FOOTNOTE_BACK_REF_STRING = new DataKey<>("FOOTNOTE_BACK_REF_STRING", "&#8617;");
	public static final DataKey<String> FOOTNOTE_LINK_REF_CLASS = new DataKey<>("FOOTNOTE_LINK_REF_CLASS", "footnote-ref");
	public static final DataKey<String> FOOTNOTE_BACK_LINK_REF_CLASS = new DataKey<>("FOOTNOTE_BACK_LINK_REF_CLASS", "footnote-backref");

	// formatter options
	public static final DataKey<ElementPlacement> FOOTNOTE_PLACEMENT = new DataKey<>("FOOTNOTE_PLACEMENT", ElementPlacement.AS_IS);
	public static final DataKey<ElementPlacementSort> FOOTNOTE_SORT = new DataKey<>("FOOTNOTE_SORT", ElementPlacementSort.AS_IS);


	private FootnoteExtension(){}

	public static FootnoteExtension create(){
		return new FootnoteExtension();
	}

	@Override
	public void extend(final Formatter.Builder formatterBuilder){
		formatterBuilder.nodeFormatterFactory(new FootnoteNodeFormatter.Factory());
	}

	@Override
	public void rendererOptions(final MutableDataHolder options){}

	@Override
	public void parserOptions(final MutableDataHolder options){}

	@Override
	public boolean transferReferences(final MutableDataHolder document, final DataHolder included){
		if(document.contains(FOOTNOTES) && included.contains(FOOTNOTES))
			return Parser.transferReferences(FOOTNOTES.get(document), FOOTNOTES.get(included),
				FOOTNOTES_KEEP.get(document) == KeepType.FIRST);

		return false;
	}

	@Override
	public void extend(final Parser.Builder parserBuilder){
		parserBuilder.customBlockParserFactory(new FootnoteBlockParser.Factory());
		parserBuilder.linkRefProcessorFactory(new FootnoteLinkRefProcessor.Factory());
	}

	@Override
	public void extend(final HtmlRenderer.Builder htmlRendererBuilder, final String rendererType){
		if(htmlRendererBuilder.isRendererType("HTML"))
			htmlRendererBuilder.nodeRendererFactory(new FootnoteNodeRenderer.Factory());
		else if(htmlRendererBuilder.isRendererType("JIRA")){}
	}

}
