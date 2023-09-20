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
import com.vladsch.flexmark.util.misc.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Service{

	private static final Random RANDOM = new Random();

	private static final Pattern KATEX_PATTERN = Pattern.compile("(?:^|[^\\\\])(\\$\\$(?:[^$]|\\\\\\$)*?[^\\\\]\\$\\$|\\$(?:[^$]|\\\\\\$)*?[^\\\\]\\$)",
		Pattern.MULTILINE | Pattern.UNICODE_CASE);


	private static final Parser PARSER;
	private static final HtmlRenderer RENDERER;

	static{
		final MutableDataSet options = new MutableDataSet()
			.set(Parser.REFERENCES_KEEP, KeepType.LAST)
			.set(Parser.EXTENSIONS, List.of(TablesExtension.create(), TypographicExtension.create(), SubscriptExtension.create(),
				FootnoteExtension.create()))

			.set(HtmlRenderer.INDENT_SIZE, 3)
			.set(HtmlRenderer.PERCENT_ENCODE_URLS, true)
			//convert soft-breaks to hard breaks
			.set(HtmlRenderer.SOFT_BREAK, "<br />\n")

			.set(TablesExtension.COLUMN_SPANS, false)
			.set(TablesExtension.APPEND_MISSING_COLUMNS, true)
			.set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
			.set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true);

		PARSER = Parser.builder(options)
			.build();
		RENDERER = HtmlRenderer.builder(options)
			.build();
	}


	public static String convert(final File file) throws IOException{
		try(final BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))){
			String content = r.lines()
				.collect(Collectors.joining("\n"));
			//extract KaTeX code
			final List<String> katexCodes = extractKaTeXCode(content);
			content = replaceKaTeXCodeWithPlaceholders(content, katexCodes);

			//obfuscate emails
			content = obfuscateEmails(content);

			final Node document = PARSER.parse(content);
			/*
			add this javascript code to manage `eml` elements:
			for(var el of document.getElementsByClassName('eml'))
				el.href = decode(el.href.split('/').pop());
			function decode(encoded){
				var decoded = '';
				var key = parseInt(encoded.substr(0, 2), 16);
				for(var i = 2; i < encoded.length; i += 2)
					decoded += String.fromCharCode(parseInt(encoded.substr(i, 2), 16) ^ key);
				return decoded;
			}
			 */
			String htmlBegin = """
				<!DOCTYPE html>
				<html lang="it">

				<head>
					<meta charset="utf-8">
					<meta name="viewport" content="width=device-width, initial-scale=1.0">
					<title>${title}</title>
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
					<script type="text/javascript">window.onload=function(){document.querySelectorAll('.eml').forEach(a=>{var p=a.href.split('/').pop().match(/.{2}/g).map(h=>parseInt(h,16));a.href=String.fromCharCode(...p.slice(1).map(x=>x^p[0]))})}</script>
				</head>

				<body class="stackedit">
					<div class="stackedit__html" style="text-align:justify;hyphens:none;">
				""";
			final String htmlEnd = """
					</div>
				</body>

				</html>
				""";
			htmlBegin = htmlBegin.replace("${title}", FileUtil.getNameOnly(file));
			final String html = RENDERER.render(document);

			return htmlBegin
				+ reinsertKaTeXCode(html, katexCodes)
				+ htmlEnd;
		}
	}


	private static List<String> extractKaTeXCode(String input){
		final List<String> katexCodes = new ArrayList<>();
		final Matcher matcher = KATEX_PATTERN.matcher(input);
		while(matcher.find())
			katexCodes.add(matcher.group(1));
		return katexCodes;
	}

	private static String replaceKaTeXCodeWithPlaceholders(String input, final List<String> katexCodes){
		final int size = katexCodes.size();
		for(int i = 0; i < size; i ++)
			input = input.replace(katexCodes.get(i), "[$$]{" + i + "}");
		return input;
	}

	private static String reinsertKaTeXCode(String input, final List<String> katexCodes){
		final int size = katexCodes.size();
		for(int i = 0; i < size; i ++)
			input = input.replace("[$$]{" + i + "}", katexCodes.get(i));
		return input;
	}


	private static String obfuscateEmails(final String input){
		String replacement = input;
		int start = replacement.indexOf("<a ");
		while(start != -1){
			int end = replacement.indexOf(">", start + 3);
			if(end == -1)
				break;

			final int startHRef = replacement.indexOf("href=\"", start + 3);
			final int endHRef = replacement.indexOf("\"", startHRef + 6);
			if(replacement.substring(start, end).contains("class=\"eml\"")){
				final String href = replacement.substring(startHRef + 6, endHRef);

				replacement = replacement.substring(0, startHRef)
					+ "href=\""
					+ encode(href, RANDOM.nextInt(256))
					+ "\""
					+ replacement.substring(endHRef + 1);
			}

			end = replacement.indexOf(">", start + 3);
			start = replacement.indexOf("<a ", end + 1);
		}
		return replacement;
	}

	private static String encode(final String decoded, final int key){
		final StringBuilder sb = new StringBuilder(make2DigitsLong(key));
		for(int n = 0; n < decoded.length(); n ++)
			sb.append(make2DigitsLong(decoded.charAt(n) ^ key));
		return sb.toString();
	}

	private static String make2DigitsLong(final int value){
		final String hex = Integer.toHexString(value);
		return (hex.length() < 2? "0" + hex: hex);
	}

}
