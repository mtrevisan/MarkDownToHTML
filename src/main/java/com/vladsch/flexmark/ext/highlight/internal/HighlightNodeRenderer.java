package com.vladsch.flexmark.ext.highlight.internal;

import com.vladsch.flexmark.ext.highlight.Highlight;
import com.vladsch.flexmark.ext.highlight.HighlightExtension;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;

import java.util.HashSet;
import java.util.Set;


public class HighlightNodeRenderer implements NodeRenderer{

	final private String highlightStyleHtmlOpen;
	final private String highlightStyleHtmlClose;


	public HighlightNodeRenderer(final DataHolder options){
		highlightStyleHtmlOpen = HighlightExtension.HIGHLIGHT_STYLE_HTML_OPEN.get(options);
		highlightStyleHtmlClose = HighlightExtension.HIGHLIGHT_STYLE_HTML_CLOSE.get(options);
	}

	@Override
	public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers(){
		final Set<NodeRenderingHandler<?>> set = new HashSet<>();
		set.add(new NodeRenderingHandler<>(Highlight.class, HighlightNodeRenderer.this::render));
		return set;
	}

	private void render(final Highlight node, final NodeRendererContext context, final HtmlWriter html){
		if(highlightStyleHtmlOpen == null || highlightStyleHtmlClose == null){
			if(context.getHtmlOptions().sourcePositionParagraphLines)
				html.withAttr().tag("mark");
			else
				html.srcPos(node.getText()).withAttr().tag("mark");
			context.renderChildren(node);
			html.tag("/mark");
		}
		else{
			html.raw(highlightStyleHtmlOpen);
			context.renderChildren(node);
			html.raw(highlightStyleHtmlClose);
		}
	}

	public static class Factory implements NodeRendererFactory{
		@Override
		public NodeRenderer apply(final DataHolder options){
			return new HighlightNodeRenderer(options);
		}
	}

}
