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

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.SubscriptExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.KeepType;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.collection.iteration.ReversiblePeekingIterator;
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
			.set(HtmlRenderer.SOFT_BREAK, "<br />")
			.set(HtmlRenderer.GENERATE_HEADER_ID, true)
			.set(HtmlRenderer.HEADER_ID_GENERATOR_NO_DUPED_DASHES, true)
			.set(HtmlRenderer.RENDER_HEADER_ID, true)

			.set(TablesExtension.COLUMN_SPANS, false)
			.set(TablesExtension.APPEND_MISSING_COLUMNS, true)
			.set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
			.set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true);

		PARSER = Parser.builder(options)
			.build();
		RENDERER = HtmlRenderer.builder(options)
			.build();
	}

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
	private static final String HTML_TEMPLATE = """
		<!DOCTYPE html>
		<html lang="it">

		<head>
			<meta charset="utf-8">
			<meta name="viewport" content="width=device-width, initial-scale=1.0">
			<title>${title}</title>
			<link rel="stylesheet" href="https://stackedit.io/style.css" />
			<style>
				._navigation-column { position: fixed; display: none; width: 250px; height: 100%; top: 0; left: 0; padding-left: 1.5em; text-indent:-1.5em; text-align: left; overflow-x: hidden; overflow-y: auto; -webkit-overflow-scrolling: touch; -ms-overflow-style: none; }
				._content-column { position: absolute; right: 0; top: 0; left: 0; }
				._toc ul { padding: 0; }
				._toc ul a { margin: .5rem 0; padding: .5rem 1rem; }
				._toc ul ul { color: #888; font-size: .9em; }
				._toc ul ul a { margin: 0; padding: .1rem 1rem; }
				._toc li { display: block; }
				._toc a { display: block; color: inherit; text-decoration: none; }
				._toc a:active, ._toc a:focus, ._toc a:hover { background-color: rgba(0, 0, 0, .075); border-radius: 3px; }

				body, html { color: rgba(0, 0, 0, .75); font-size: 16px; font-family: Lato, Helvetica Neue, Helvetica, sans-serif; font-variant-ligatures: common-ligatures; line-height: 1.67; -webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale; }
				html { line-height: 1.5; -ms-text-size-adjust: 100%; -webkit-text-size-adjust: 100%; }
				body { counter-reset: katexEqnNo mmlEqnNo; }
				._content { margin: 0 auto 0 auto; padding-left: 30px; padding-right: 30px; max-width: 750px; }
				body._justify { text-align: justify; }
				body._no-hyphens { hyphens: none; }

				article, aside, footer, header, nav, section, figcaption, figure, main, details, menu { display: block; }
				sub, sup { font-size: 75%; line-height: 0; position: relative; vertical-align: baseline; }
				sub { bottom: -.25em; }
				sup { top: -.5em; }
				small { font-size: 80%; }
				audio, video { display: inline-block; }
				audio:not([controls]) { display: none; height: 0; }
				img { border-style: none; max-width: 100%; }
				img._center { display: block; margin: auto; }
				img._separator { max-height: 300px; }
				img._separator-half { max-height: 150px; }
				img._copyright { float:right; }
				svg:not(:root) { overflow: hidden; }
				button, input, optgroup, select, textarea { font-family: sans-serif; font-size: 100%; line-height: 1.15; margin: 0; }
				button { overflow: visible; }
				button, select { text-transform: none; }
				[type=reset], [type=submit], button, html[type=button] { -webkit-appearance: button; }
				[type=button]::-moz-focus-inner, [type=reset]::-moz-focus-inner, [type=submit]::-moz-focus-inner, button::-moz-focus-inner { border-style: none; padding: 0; }
				[type=button]:-moz-focusring, [type=reset]:-moz-focusring, [type=submit]:-moz-focusring, button:-moz-focusring { outline: 1px dotted ButtonText; }
				input { overflow: visible; }
				[type=checkbox], [type=radio] { box-sizing: border-box; padding: 0; }
				[type=number]::-webkit-inner-spin-button, [type=number]::-webkit-outer-spin-button { height: auto; }
				[type=search] { -webkit-appearance: textfield; outline-offset: -2px; }
				[type=search]::-webkit-search-cancel-button, [type=search]::-webkit-search-decoration { -webkit-appearance: none; }
				::-webkit-file-upload-button { -webkit-appearance: button; font: inherit; }
				blockquote, dl, ol, p, pre, ul { margin: 1.2em 0; }
				h1, h2, h3, h4, h5, h6 { margin: 1.8em 0; line-height: 1.33; }
				h1:after, h2:after { content: ""; display: block; position: relative; top: .33em; border-bottom: 1px solid hsla(0, 0%, 50%, .33); }
				h1 { font-size: 2em; margin: .67em 0; }
				hr { border: 0; border-top: 1px solid hsla(0, 0%, 50%, .33); margin: 2em 0; box-sizing: content-box; height: 0; overflow: visible; }
				pre { font-family: monospace, monospace; font-size: 1em; }
				abbr[title] { border-bottom: none; text-decoration: underline; text-decoration: underline dotted; cursor: help; }
				dfn { font-style: italic; }
				mark { background-color: #f8f840; color: #000; }
				b, strong { font-weight: inherit; font-weight: bolder; }
				code, kbd, samp { font-family: monospace, monospace; font-size: 1em; }
				code, pre, samp { font-family: Roboto Mono, Lucida Sans Typewriter, Lucida Console, monaco, Courrier, monospace; font-size: .85em; }
				code *, pre *, samp * { font-size: inherit; }
				code { background-color: rgba(0, 0, 0, .05); border-radius: 3px; padding: 2px 4px; }
				pre>code { background-color: rgba(0, 0, 0, .05); display: block; padding: .5em; -webkit-text-size-adjust: none; overflow-x: auto; white-space: pre; }
				kbd { font-family: Lato, Helvetica Neue, Helvetica, sans-serif; background-color: #fff; border: 1px solid rgba(63, 63, 63, .25); border-radius: 3px; box-shadow: 0 1px 0 rgba(63, 63, 63, .25); color: #333; display: inline-block; font-size: .8em; margin: 0 .1em; padding: .1em .6em; white-space: nowrap; }
				ol ol, ol ul, ul ol, ul ul { margin: 0; }
				a { color: #0c93e4; background-color: transparent; -webkit-text-decoration-skip: objects; text-decoration: underline; text-decoration-skip: ink; }
				a:focus, a:hover { text-decoration: none; }
				table { background-color: transparent; border-collapse: collapse; border-spacing: 0; }
				td, th { border-right: 1px solid #dcdcdc; padding: 8px 12px; page-break-inside: avoid; }
				td:last-child, th:last-child { border-right: 0; }
				td { border-top: 1px solid #dcdcdc; }
				td._no-border { border: 0; padding: 0; }
				span._no-wrap { white-space: nowrap; }
				div._center, figure._center { display: table; margin: auto; }
				div._img-caption, figcaption._default { text-align: right; line-height: 3mm; font-size: x-small; }
				textarea { overflow: auto; }
				summary { display: list-item; }
				canvas { display: inline-block; }
				[hidden], template { display: none; }
				fieldset { padding: .35em .75em .625em; }
				legend { box-sizing: border-box; display: table; max-width: 100%; padding: 0; color: inherit; white-space: normal; }
				progress { display: inline-block; vertical-align: baseline; }
				dt { font-weight: 700 }
				blockquote { color: rgba(0, 0, 0, .5); padding-left: 1.5em; border-left: 5px solid rgba(0, 0, 0, .1); }

				@media (min-width:1060px) {
					._navigation-column { display: block; }
					._content-column { left: 250px; }
				}
				@page {
					size: auto;
					margin: 20mm 20mm 15mm 20mm;
					._navigation-column { display: none; }
					._content-column { left: 0; }
				}
			</style>

			<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css" integrity="sha384-n8MVd4RsNIU0tAv4ct0nTaAbDJwPJzDEaqSD1odI+WdtXRGWt2kTvGFasHpSy3SV" crossorigin="anonymous">
			<!-- The loading of KaTeX is deferred to speed up page rendering -->
			<script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js" integrity="sha384-XjKyOOlGwcjNTAIQHIpgOno0Hl1YQqzUOEleOLALmuqehneUG+vnGctmUb0ZY0l8" crossorigin="anonymous"></script>
			<!-- To automatically render math in text elements, include the auto-render extension: -->
			<script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/contrib/auto-render.min.js" integrity="sha384-+VBxd3r6XgURycqtZ117nYw44OOcIax56Z4dCRWbxyPt0Koah1uHoK0o4+/RRE05" crossorigin="anonymous" onload="renderMathInElement(document.body, {delimiters: [
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

		<body class="_content _justify _no-hyphens">
			${body}
		</body>

		</html>
		""";
	private static final String BODY_TEMPLATE_WITH_TOC = """
		<div class="_navigation-column _toc">
			<ul>
				${toc}
			</ul>
		</div>
		<div class="_content-column _content _justify _no-hyphens">
			${content}
		</div>
		""";
	private static final String TOC_TEMPLATE_BEGIN = """
		<li><a href="#${id}">${heading}</a>
			\t\t<ul>
		""";
	private static final String TOC_TEMPLATE_ITEM = """
		\t\t\t\t<li><a href="#${id}">${heading}</a></li>
		""";
	private static final String TOC_TEMPLATE_END = """
			\t\t</ul>
		\t\t</li>""";


	public static String convert(final File file, final boolean generateTOC) throws IOException{
		try(final BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))){
			String content = r.lines()
				.collect(Collectors.joining("\n"));
			//extract KaTeX code
			final List<String> katexCodes = extractKaTeXCode(content);
			content = replaceKaTeXCodeWithPlaceholders(content, katexCodes);

			//obfuscate emails
			content = obfuscateEmails(content);

			final Node document = PARSER.parse(content);

			final String filename = FileUtil.getNameOnly(file);
			final String template = HTML_TEMPLATE.replace("${title}", filename);
			final String html = RENDERER.render(document);
			String body = reinsertKaTeXCode(html, katexCodes);
			if(generateTOC){
				//extract list of h1 and h2
				final ReversiblePeekingIterator<Node> itr = document.getChildIterator();
				final List<Heading> sectionHeadings = new ArrayList<>();
				while(itr.hasNext()){
					final Node node = itr.next();
					if(node.getClass() == Heading.class && ((Heading)node).getLevel() <= 2)
						sectionHeadings.add((Heading)node);
				}
				if(!sectionHeadings.isEmpty()){
					boolean start = true;
					final StringBuilder toc = new StringBuilder();
					for(final Heading sectionHeading : sectionHeadings){
						final String heading = RENDERER.render(PARSER.parse(sectionHeading.getText()));
						toc.append((sectionHeading.getLevel() == 1? TOC_TEMPLATE_BEGIN: TOC_TEMPLATE_ITEM)
							.replace("${id}", sectionHeading.getAnchorRefId())
							.replace("${heading}", heading.substring("<p>".length(), heading.length() - "</p>".length() - 1)));

						if(sectionHeading.getLevel() == 1 && !start)
							toc.append(TOC_TEMPLATE_END);
						start = false;
					}
					toc.append(TOC_TEMPLATE_END);

					body = BODY_TEMPLATE_WITH_TOC
						.replace("${toc}", toc.toString())
						.replace("${content}", body);
				}
			}
			return template.replace("${body}", body);
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
