/**
 * Copyright (c) 2023 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.markdowntohtml;

import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.SubscriptExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.KeepType;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;


public class Service{

	private static final Parser PARSER;
	private static final HtmlRenderer RENDERER;
	static{
		final MutableDataSet options = new MutableDataSet()
			.set(Parser.REFERENCES_KEEP, KeepType.LAST)
			.set(HtmlRenderer.INDENT_SIZE, 3)
			.set(HtmlRenderer.PERCENT_ENCODE_URLS, true)

			.set(TablesExtension.COLUMN_SPANS, false)
			.set(TablesExtension.APPEND_MISSING_COLUMNS, true)
			.set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
			.set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true)
			.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));

		final List<Parser.ParserExtension> opts = Arrays.asList(
			TablesExtension.create(),
			TypographicExtension.create(),
			SubscriptExtension.create(),
			FootnoteExtension.create()
		);
		PARSER = Parser.builder(options)
			.extensions(opts)
			.build();
		RENDERER = HtmlRenderer.builder(options)
			.extensions(opts)
			.build();
	}


	public static String convert(final File file) throws IOException{
		try(final Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)){
			final Node document = PARSER.parseReader(r);
			String htmlBegin = """
					<!DOCTYPE html>
					<html lang="it">

					<head>
						<meta charset="utf-8">
						<meta name="viewport" content="width=device-width, initial-scale=1.0">
						<title>dhonklada</title>
						<link rel="stylesheet" href="https://stackedit.io/style.css" />
						<style>
							@page {
								size: auto;
								margin: {20mm 20mm 15mm 20mm};
							}
							img.separator{ max-height: 300px; }
							img.separator-half{ max-height: 150px; }
							img.copyright{ float:right; }
							span.no-wrap{ white-space: nowrap; }
							td.no-border{ border: 0; padding: 0; }
						</style>

						<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css" integrity="sha384-GvrOXuhMATgEsSwCs4smul74iXGOixntILdUW9XmUC6+HX0sLNAK3q71HotJqlAn" crossorigin="anonymous" />
						<!-- The loading of KaTeX is deferred to speed up page rendering -->
						<script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js" integrity="sha384-cpW21h6RZv/phavutF+AuVYrr+dA8xD9zs6FwLpaCct6O9ctzYFfFr4dgmgccOTx" crossorigin="anonymous"></script>
						<!-- To automatically render math in text elements, include the auto-render extension: -->
						<script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js" integrity="sha384-+VBxd3r6XgURycqtZ117nYw44OOcIax56Z4dCRWbxyPt0Koah1uHoK0o4+/RRE05" crossorigin="anonymous" onload="renderMathInElement(document.body, {delimiters: [
							{left: '$$', right: '$$', display: true},
							{left: '$', right: '$', display: false},
							{left: '\\\\(', right: '\\\\)', display: false},
							{left: '\\\\begin{equation}', right: '\\\\end{equation}', display: true},
							{left: '\\\\begin{align}', right: '\\\\end{align}', display: true},
							{left: '\\\\begin{alignat}', right: '\\\\end{alignat}', display: true},
							{left: '\\\\begin{gather}', right: '\\\\end{gather}', display: true},
							{left: '\\\\begin{CD}', right: '\\\\end{CD}', display: true},
							{left: '\\\\[', right: '\\\\]', display: true}
						], throwOnError: false});"></script>
					</head>

					<body class="stackedit">
						<div class="stackedit__html" style="text-align:justify;hyphens:auto;">
					""";
			final String htmlEnd = """
					</div>
				</body>

				</html>
				""";
			final StringJoiner sj = new StringJoiner("", htmlBegin, htmlEnd);
			final String html = RENDERER.render(document);
			sj.add(html);

			return sj.toString();
		}
	}

}
