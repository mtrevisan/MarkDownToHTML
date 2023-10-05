package com.vladsch.flexmark.ext.attributes;

import com.vladsch.flexmark.ext.attributes.internal.AttributesAttributeProvider;
import com.vladsch.flexmark.ext.attributes.internal.AttributesInlineParserExtension;
import com.vladsch.flexmark.ext.attributes.internal.AttributesNodeFormatter;
import com.vladsch.flexmark.ext.attributes.internal.AttributesNodePostProcessor;
import com.vladsch.flexmark.ext.attributes.internal.AttributesNodeRenderer;
import com.vladsch.flexmark.ext.attributes.internal.NodeAttributeRepository;
import com.vladsch.flexmark.formatter.Formatter;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.RendererBuilder;
import com.vladsch.flexmark.html.RendererExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.KeepType;
import com.vladsch.flexmark.util.data.DataKey;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.format.options.DiscretionaryText;


/**
 * Extension for attributes
 * <p>
 * Create it with {@link #create()} and then configure it on the builders
 * <p>
 * The parsed attributes text is turned into {@link AttributesNode} nodes.
 */
public class AttributesExtension implements Parser.ParserExtension, RendererExtension, HtmlRenderer.HtmlRendererExtension,
	Formatter.FormatterExtension/*, Parser.ReferenceHoldingExtension*/{

	public static final DataKey<NodeAttributeRepository> NODE_ATTRIBUTES = new DataKey<>("NODE_ATTRIBUTES", new NodeAttributeRepository(null), NodeAttributeRepository::new);
	public static final DataKey<KeepType> ATTRIBUTES_KEEP = new DataKey<>("ATTRIBUTES_KEEP", KeepType.FIRST); // standard option to allow control over how to handle duplicates
	public static final DataKey<Boolean> ASSIGN_TEXT_ATTRIBUTES = new DataKey<>("ASSIGN_TEXT_ATTRIBUTES", true); // assign attributes to text if previous is not a space
	public static final DataKey<Boolean> FENCED_CODE_INFO_ATTRIBUTES = new DataKey<>("FENCED_CODE_INFO_ATTRIBUTES", false); // assign attributes found at end of fenced code info strings
	public static final DataKey<FencedCodeAddType> FENCED_CODE_ADD_ATTRIBUTES = new DataKey<>("FENCED_CODE_ADD_ATTRIBUTES", FencedCodeAddType.ADD_TO_PRE_CODE); // assign attributes to pre/code tag
	public static final DataKey<Boolean> WRAP_NON_ATTRIBUTE_TEXT = new DataKey<>("WRAP_NON_ATTRIBUTE_TEXT", true);
	public static final DataKey<Boolean> USE_EMPTY_IMPLICIT_AS_SPAN_DELIMITER = new DataKey<>("USE_EMPTY_IMPLICIT_AS_SPAN_DELIMITER", false);

	public static final DataKey<Boolean> FORMAT_ATTRIBUTES_COMBINE_CONSECUTIVE = new DataKey<>("FORMAT_ATTRIBUTES_COMBINE_CONSECUTIVE", false);
	public static final DataKey<Boolean> FORMAT_ATTRIBUTES_SORT = new DataKey<>("FORMAT_ATTRIBUTES_SORT", false);
	public static final DataKey<DiscretionaryText> FORMAT_ATTRIBUTES_SPACES = new DataKey<>("FORMAT_ATTRIBUTES_SPACES", DiscretionaryText.AS_IS); // add spaces after { and before }
	public static final DataKey<DiscretionaryText> FORMAT_ATTRIBUTE_EQUAL_SPACE = new DataKey<>("FORMAT_ATTRIBUTE_EQUAL_SPACE", DiscretionaryText.AS_IS);
	public static final DataKey<AttributeValueQuotes> FORMAT_ATTRIBUTE_VALUE_QUOTES = new DataKey<>("FORMAT_ATTRIBUTE_VALUE_QUOTES", AttributeValueQuotes.AS_IS);
	public static final DataKey<AttributeImplicitName> FORMAT_ATTRIBUTE_ID = new DataKey<>("FORMAT_ATTRIBUTE_ID", AttributeImplicitName.AS_IS);
	public static final DataKey<AttributeImplicitName> FORMAT_ATTRIBUTE_CLASS = new DataKey<>("FORMAT_ATTRIBUTE_CLASS", AttributeImplicitName.AS_IS);

	private AttributesExtension(){
	}

	public static AttributesExtension create(){
		return new AttributesExtension();
	}

	@Override
	public void parserOptions(final MutableDataHolder options){
		if(options.contains(FENCED_CODE_INFO_ATTRIBUTES) && FENCED_CODE_INFO_ATTRIBUTES.get(options) && !options.contains(FENCED_CODE_ADD_ATTRIBUTES))
			// change default to pre only, to add to code use attributes after info
			options.set(FENCED_CODE_ADD_ATTRIBUTES, FencedCodeAddType.ADD_TO_PRE);
	}

	@Override
	public void extend(final Parser.Builder parserBuilder){
		parserBuilder.postProcessorFactory(new AttributesNodePostProcessor.Factory());
		parserBuilder.customInlineParserExtensionFactory(new AttributesInlineParserExtension.Factory());
	}

	@Override
	public void extend(final Formatter.Builder formatterBuilder){
		formatterBuilder.nodeFormatterFactory(new AttributesNodeFormatter.Factory());
	}

	@Override
	public void rendererOptions(final MutableDataHolder options){

	}

	@Override
	public void extend(final HtmlRenderer.Builder htmlRendererBuilder, final String rendererType){
		if(ASSIGN_TEXT_ATTRIBUTES.get(htmlRendererBuilder))
			htmlRendererBuilder.nodeRendererFactory(new AttributesNodeRenderer.Factory());
		htmlRendererBuilder.attributeProviderFactory(new AttributesAttributeProvider.Factory());
	}

	@Override
	public void extend(final RendererBuilder rendererBuilder, final String rendererType){
		//rendererBuilder.nodeRendererFactory(new AttributesNodeRenderer.Factory());
		rendererBuilder.attributeProviderFactory(new AttributesAttributeProvider.Factory());
	}

}
