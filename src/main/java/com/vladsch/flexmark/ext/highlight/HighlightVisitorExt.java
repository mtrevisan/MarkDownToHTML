package com.vladsch.flexmark.ext.highlight;

import com.vladsch.flexmark.util.ast.VisitHandler;


public class HighlightVisitorExt{

	public static <V extends HighlightVisitor> VisitHandler<?>[] visitHandlers(final V visitor){
		return new VisitHandler<?>[]{
			new VisitHandler<>(Highlight.class, visitor::visit)
		};
	}

}