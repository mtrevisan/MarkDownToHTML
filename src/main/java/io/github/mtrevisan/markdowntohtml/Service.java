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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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


	public static String convert(final File file, final boolean generateTOC) throws IOException{
		try(final BufferedReader r = getBufferedReader(file)){
			String content = r.lines()
				.collect(Collectors.joining("\n"));

			//extract KaTeX code
			final List<String> katexCodes = extractKaTeXCode(content);
			content = replaceKaTeXCodeWithPlaceholders(content, katexCodes);

			final boolean hasDetailsTag = content.contains("<details");

			//obfuscate emails
			content = obfuscateEmails(content);

			//generate AST
			final Node document = PARSER.parse(content);

			//replace placeholders:
			final String filename = FileUtil.getNameOnly(file);
			return replacePlaceholders(document, filename, generateTOC, hasDetailsTag, katexCodes);
		}
	}

	private static BufferedReader getBufferedReader(final File file) throws FileNotFoundException{
		return new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
	}

	private static String generateBodyWithTOC(final Node document) throws IOException{
		//extract list of h1 and h2
		final List<Heading> sectionHeadings = extractSectionHeadings(document, 2);
		if(!sectionHeadings.isEmpty()){
			final String tocTemplateBegin = getFileContentFromResource("toc-template-begin.html");
			final String tocTemplateItem = getFileContentFromResource("toc-template-item.html");
			final String tocTemplateEnd = getFileContentFromResource("toc-template-end.html");

			final StringBuilder toc = new StringBuilder();
			for(int i = 0; i < sectionHeadings.size(); i ++){
				final Heading sectionHeading = sectionHeadings.get(i);

				if(sectionHeading.getLevel() == 1 && i > 0)
					toc.append(tocTemplateEnd);

				final String heading = RENDERER.render(PARSER.parse(sectionHeading.getText()));
				toc.append((sectionHeading.getLevel() == 1? tocTemplateBegin: tocTemplateItem)
					.replace("${id}", sectionHeading.getAnchorRefId())
					.replace("${heading}", heading.substring("<p>".length(), heading.length() - "</p>".length() - 1)));
			}
			toc.append(tocTemplateEnd);

			final String bodyTemplateWithTOC = getFileContentFromResource("body-template-with-toc.html");
			return bodyTemplateWithTOC.replace("${toc}", toc.toString());
		}
		return "${content}";
	}

	private static List<Heading> extractSectionHeadings(final Node document, final int maxLevel){
		final List<Heading> sectionHeadings = new ArrayList<>();
		final ReversiblePeekingIterator<Node> itr = document.getChildIterator();
		while(itr.hasNext()){
			final Node node = itr.next();
			if(node.getClass() == Heading.class && ((Heading)node).getLevel() <= maxLevel)
				sectionHeadings.add((Heading)node);
		}
		return sectionHeadings;
	}


	private static String getFileContentFromResource(String filename) throws IOException{
		//the class loader that loaded the class
		final ClassLoader classLoader = Service.class.getClassLoader();
		filename = "resources/" + (filename.charAt(0) == '/'? filename.substring(1): filename);
		try(final InputStream is = classLoader.getResourceAsStream(filename)){
			//the stream holding the file content
			if(is == null)
				throw new IllegalArgumentException("File " + filename + " not found! ");

			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
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


	private static String replacePlaceholders(final Node document, final String filename, final boolean generateTOC,
			final boolean hasDetailsTag, final List<String> katexCodes) throws IOException{
		final String htmlTemplate = getFileContentFromResource("html-template.html");
		final String stylesheet = getFileContentFromResource("stylesheet.css");
		final String katex = getFileContentFromResource("katex.html");
		final String openDetailsWhenPrintingScript = (hasDetailsTag
			? getFileContentFromResource("open-details-when-printing.html")
			: "");
		final String template = htmlTemplate
			.replace("${title}", filename)
			.replace("${stylesheet}", stylesheet)
			.replace("${katex}", katex)
			.replace("${scripts}", openDetailsWhenPrintingScript);
		final String html = RENDERER.render(document);
		String body = reinsertKaTeXCode(html, katexCodes);
		if(generateTOC)
			body = generateBodyWithTOC(document)
				.replace("${content}", body);
		return template.replace("${body}", body);
	}

}
