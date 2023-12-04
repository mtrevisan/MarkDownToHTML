package com.vladsch.flexmark.ext.footnotes.internal;

import com.vladsch.flexmark.ext.footnotes.Footnote;
import com.vladsch.flexmark.ext.footnotes.FootnoteBlock;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.html.renderer.PhasedNodeRenderer;
import com.vladsch.flexmark.html.renderer.RenderingPhase;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.data.DataHolder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;


public class FootnoteNodeRenderer implements PhasedNodeRenderer{

	private final FootnoteRepository footnoteRepository;
	private final FootnoteOptions options;
	private final boolean recheckUndefinedReferences;


	public FootnoteNodeRenderer(final DataHolder options){
		this.options = new FootnoteOptions(options);
		footnoteRepository = FootnoteExtension.FOOTNOTES.get(options);
		recheckUndefinedReferences = HtmlRenderer.RECHECK_UNDEFINED_REFERENCES.get(options);
		footnoteRepository.resolveFootnoteOrdinals();
	}

	@Override
	public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers(){
		return new HashSet<>(Arrays.asList(
			new NodeRenderingHandler<>(Footnote.class, this::render),
			new NodeRenderingHandler<>(FootnoteBlock.class, this::render)
		));
	}

	@Override
	public Set<RenderingPhase> getRenderingPhases(){
		final Set<RenderingPhase> set = new HashSet<>(2);
		set.add(RenderingPhase.BODY_TOP);
		set.add(RenderingPhase.BODY_BOTTOM);
		return set;
	}

	@Override
	public void renderDocument(final NodeRendererContext context, final HtmlWriter html,
			final Document document, final RenderingPhase phase){
		if(phase == RenderingPhase.BODY_TOP){
			if(recheckUndefinedReferences){
				//need to see if we have undefined footnotes that were defined after parsing
				final boolean[] hadNewFootnotes = {false};
				final NodeVisitor visitor = new NodeVisitor(new VisitHandler<>(Footnote.class, node -> {
					if(!node.isDefined()){
						final FootnoteBlock footnoteBlock = node.getFootnoteBlock(footnoteRepository);

						if(footnoteBlock != null){
							footnoteRepository.addFootnoteReference(footnoteBlock, node);
							node.setFootnoteBlock(footnoteBlock);
							hadNewFootnotes[0] = true;
						}
					}
				}));

				visitor.visit(document);
				if(hadNewFootnotes[0])
					this.footnoteRepository.resolveFootnoteOrdinals();
			}
		}
		else if(phase == RenderingPhase.BODY_BOTTOM){
			//here we dump the footnote blocks that were referenced in the document body, i.e. ones with footnoteOrdinal > 0
			if(!footnoteRepository.getReferencedFootnoteBlocks().isEmpty()){
				html.attr("class", "footnotes").withAttr().tagIndent("div", () -> {
					html.tagIndent("ol", () -> {
						for(final FootnoteBlock footnoteBlock : footnoteRepository.getReferencedFootnoteBlocks()){
							final int footnoteOrdinal = footnoteBlock.getFootnoteOrdinal();
							html.attr("id", "fn-" + footnoteOrdinal);
							html.withAttr().tagIndent("li", () -> {
								context.renderChildren(footnoteBlock);

								final int iMax = footnoteBlock.getFootnoteReferences();
								for(int i = 0; i < iMax; i ++){
									html.attr("href", "#fnref-" + footnoteOrdinal
										+ (i == 0? "": String.format(Locale.US, "-%d", i)));
									if(!options.footnoteBackLinkRefClass.isEmpty())
										html.attr("class", options.footnoteBackLinkRefClass);
									html.line().withAttr().tag("a");
									html.raw(options.footnoteBackRefString);
									html.tag("/a");
								}
							});
						}
					});
				});
			}
		}
	}

	private void render(final FootnoteBlock node, final NodeRendererContext context, final HtmlWriter html){}

	private void render(final Footnote node, final NodeRendererContext context, final HtmlWriter html){
		final FootnoteBlock footnoteBlock = node.getFootnoteBlock();
		if(footnoteBlock == null){
			//just text
			html.raw("[^");
			context.renderChildren(node);
			html.raw("]");
		}
		else{
			final int footnoteOrdinal = footnoteBlock.getFootnoteOrdinal();
			final int i = node.getReferenceOrdinal();
			html.attr("id", "fnref-" + footnoteOrdinal + (i == 0? "": String.format(Locale.US, "-%d", i)));
			html.srcPos(node.getChars())
				.withAttr()
				.tag("sup", false, false, () -> {
					if(!options.footnoteLinkRefClass.isEmpty())
						html.attr("class", options.footnoteLinkRefClass);
					html.attr("href", "#fn-" + footnoteOrdinal);
					html.withAttr().tag("a");
					html.raw(options.footnoteRefPrefix + footnoteOrdinal + options.footnoteRefSuffix);
					html.tag("/a");
				});
		}
	}

	public static class Factory implements NodeRendererFactory{
		@Override
		public NodeRenderer apply(final DataHolder options){
			return new FootnoteNodeRenderer(options);
		}
	}

}
