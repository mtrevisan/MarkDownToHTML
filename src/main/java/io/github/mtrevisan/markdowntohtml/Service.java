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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Service{

	private static final Random RANDOM = new Random();

	private static final Pattern ID_PATTERN = Pattern.compile("id\\s*=\\s*\"([^\"]*?)\"",
		Pattern.MULTILINE | Pattern.UNICODE_CASE);
	private static final Pattern LOCAL_LINK_PATTERN = Pattern.compile("\\[\\[(.+?)]](?!\\()",
		Pattern.MULTILINE | Pattern.UNICODE_CASE);
	private static final Pattern KATEX_PATTERN = Pattern.compile("(?<!\\\\)(?<!\\\\\\\\)(\\$.+?\\$)(?!\\$)",
		Pattern.DOTALL | Pattern.UNICODE_CASE);

	private static final Pattern PATTERN_MAILTO = Pattern.compile("<a\\b[^>]*href=\"mailto:([^\"]+)\"[^>]*>");

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");


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
	add this javascript code to manage `a[href^="mailto:"]` elements:
	window.onload=function(){
		document.querySelectorAll('a[href^=":"]').forEach(a =>{
			let b = atob(a.getAttribute('href').slice(1));
			let k = b.charCodeAt(0);
			a.href = [...b].slice(1)
				.map(c => String.fromCharCode(c.charCodeAt(0) ^ k))
				.join('');
		})
	}
	*/


	public static List<String> extractIDs(final File file) throws IOException{
		try(final BufferedReader r = getBufferedReader(file)){
			final String content = r.lines()
				.collect(Collectors.joining("\n"));

			return extractIDs(content);
		}
	}

	/**
	 * Converts the content of a file to HTML with optional features.
	 *
	 * @param file	The file to be converted.
	 * @param generateTOC	Flag indicating whether to generate a table of contents.
	 * @param preventCopying	Flag indicating whether to prevent text copying in the generated HTML.
	 * @return	The converted content as HTML.
	 * @throws IOException	If an I/O error occurs while reading the file or loading resources.
	 */
	public static String convert(final File file, final boolean generateTOC, final boolean preventCopying) throws IOException{
		try(final BufferedReader r = getBufferedReader(file)){
			String content = r.lines()
				.collect(Collectors.joining("\n"));

			content = removeLocalLinks(content);

			//extract KaTeX code
			final List<String> katexCodes = extractKaTeXCode(content);
			content = replaceKaTeXCodeWithPlaceholders(content, katexCodes);

			final boolean hasDetailsTag = content.contains("<details");

			//obfuscate emails
			content = obfuscateEmails(content);

			//generate AST
			final Node document = PARSER.parse(content);

			//replace placeholders:
			final Properties properties = loadProperties(file);
			return replacePlaceholders(document, properties, generateTOC, hasDetailsTag, preventCopying, katexCodes);
		}
	}

	/**
	 * Returns a BufferedReader for reading the contents of a file.
	 *
	 * @param file	The file to be read.
	 * @return BufferedReader	for reading the file contents.
	 * @throws FileNotFoundException	If the file does not exist or cannot be opened for reading.
	 */
	private static BufferedReader getBufferedReader(final File file) throws FileNotFoundException{
		return new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
	}

	/**
	 * Generates the body of an HTML document with a table of contents (TOC).
	 *
	 * @param document	The root node of the document.
	 * @return	The generated HTML body with the table of contents or "${content}" if no section headings are found.
	 * @throws IOException	If an I/O error occurs while reading the file or loading resources.
	 */
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

	/**
	 * Extracts the section headings from a document up to a specified maximum level.
	 *
	 * @param document	The root node of the document.
	 * @param maxLevel	The maximum level of headings to extract.
	 * @return	A list of {@link Heading} objects representing the extracted section headings.
	 */
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


	/**
	 * Reads the content of a file from a resource in the classpath.
	 *
	 * @param filename	The filename of the resource to read.
	 * @return	The string content of the file.
	 * @throws IOException	If an I/O error occurs while reading the file.
	 */
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


	private static String removeLocalLinks(final String input){
		final Matcher matcher = LOCAL_LINK_PATTERN.matcher(input);
		final StringBuilder sb = new StringBuilder();
		while(matcher.find())
			matcher.appendReplacement(sb, matcher.group(1));
		matcher.appendTail(sb);
		return sb.toString();
	}


	private static List<String> extractIDs(final String input){
		final List<String> ids = new ArrayList<>();
		final Matcher matcher = ID_PATTERN.matcher(input);
		while(matcher.find())
			ids.add(matcher.group(1));
		return ids;
	}


	/**
	 * Extracts KaTeX code from the given input string.
	 *
	 * @param input	The input string with KaTeX code.
	 * @return	The list of extracted KaTeX codes.
	 */
	private static List<String> extractKaTeXCode(final String input){
		final List<String> katexCodes = new ArrayList<>();
		final Matcher inlineMatcher = KATEX_PATTERN.matcher(input);
		while(inlineMatcher.find())
			katexCodes.add(inlineMatcher.group(1));
		return katexCodes;
	}

	/**
	 * Replaces KaTeX code with placeholders in the given input string.
	 *
	 * @param input	The input string with KaTeX code.
	 * @param katexCodes	The list of KaTeX codes to be replaced.
	 * @return	The input string with KaTeX code replaced with placeholders.
	 */
	private static String replaceKaTeXCodeWithPlaceholders(String input, final List<String> katexCodes){
		final int size = katexCodes.size();
		for(int i = 0; i < size; i ++)
			input = input.replace(katexCodes.get(i), "[$$]{" + i + "}");
		return input;
	}

	/**
	 * Reinserts the KaTeX code into the input string.
	 *
	 * @param input	The input string with placeholders for KaTeX code.
	 * @param katexCodes	The list of KaTeX codes to be reinserted.
	 * @return	The input string with the KaTeX code replaced.
	 */
	private static String reinsertKaTeXCode(String input, final List<String> katexCodes){
		final int size = katexCodes.size();
		for(int i = 0; i < size; i ++)
			input = input.replace("[$$]{" + i + "}", katexCodes.get(i));
		return input;
	}


	/**
	 * Obfuscates emails in a given input string.
	 *
	 * @param input	The input string containing emails.
	 * @return	The input string with obfuscated emails.
	 */
	private static String obfuscateEmails(final String input){
		final Matcher matcher = PATTERN_MAILTO.matcher(input);
		final StringBuilder result = new StringBuilder();
		while(matcher.find()){
			final String emailPart = matcher.group(1);
			final String encrypted = encode(emailPart, RANDOM.nextInt(256));
			final String replacement = matcher.group(0)
				.replaceFirst("href=\"mailto:[^\"]+\"", "href=\":" + encrypted + "\"");
			matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	/**
	 * Encodes a given string using a specified key.
	 *
	 * @param decoded	The string to be encoded.
	 * @param key	The key used for encoding.
	 * @return	The encoded string.
	 */
	private static final java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
	private static String encode(final String decoded, final int key){
//		final StringBuilder sb = new StringBuilder(make2DigitsLong(key));
//		for(int n = 0; n < decoded.length(); n ++)
//			sb.append(make2DigitsLong(decoded.charAt(n) ^ key));
//		return sb.toString();

		final byte[] inputBytes = decoded.getBytes();
		final byte[] result = new byte[inputBytes.length + 1];
		result[0] = (byte)key;
		for(int i = 0; i < inputBytes.length; i ++)
			result[i + 1] = (byte)(inputBytes[i] ^ key);
		return encoder.encodeToString(result);
	}

	/**
	 * Converts an integer value to a 2-digit hexadecimal string.
	 * <p>If the hexadecimal string is shorter than 2 characters, it prepends a 0.</p>
	 *
	 * @param value	The integer value to be converted.
	 * @return	The 2-digit hexadecimal string.
	 */
	private static String make2DigitsLong(final int value){
		final String hex = Integer.toHexString(value);
		return (hex.length() < 2? "0" + hex: hex);
	}


	/**
	 * Replaces placeholders in an HTML template with values from properties and generates the final HTML string.
	 *
	 * @param document	The node representing the parsed HTML document.
	 * @param properties	The properties.
	 * @param generateTOC	Flag indicating whether to generate a table of contents.
	 * @param hasDetailsTag	Flag indicating whether the document contains details tags.
	 * @param preventCopying	Flag indicating whether to prevent text copying in the generated HTML.
	 * @param katexCodes	The list of extracted KaTeX codes.
	 * @return	The generated HTML string with replaced placeholders.
	 * @throws IOException	If an I/O error occurs while loading resources or reading the file.
	 */
	private static String replacePlaceholders(final Node document, final Properties properties, final boolean generateTOC,
			final boolean hasDetailsTag, final boolean preventCopying, final List<String> katexCodes) throws IOException{
		String htmlTemplate = getFileContentFromResource("html-template.html");

		final Set<String> keys = properties.stringPropertyNames();
		for(final String key : keys){
			final String value = properties.getProperty(key);

			htmlTemplate = htmlTemplate.replace("${" + key + "}", value);
		}
		htmlTemplate = htmlTemplate.replace("${modified-datetime}", DATE_TIME_FORMATTER.format(ZonedDateTime.now()));


		final String stylesheet = getFileContentFromResource(preventCopying
				? "stylesheet-prevent-copy.css"
				: "stylesheet.css");
		final String katex = getFileContentFromResource("katex.html");
		final String openDetailsWhenPrintingScript = (hasDetailsTag
			? getFileContentFromResource("open-details-when-printing.html")
			: "");
		final String preventCopyingScript = (preventCopying
			? getFileContentFromResource("prevent-copy.html")
			: "");
		htmlTemplate = htmlTemplate
			.replace("${stylesheet}", stylesheet)
			.replace("${katex}", katex)
			.replace("${scripts}", openDetailsWhenPrintingScript + preventCopyingScript);
		final String html = RENDERER.render(document);
		String body = reinsertKaTeXCode(html, katexCodes);
		if(generateTOC)
			body = generateBodyWithTOC(document)
				.replace("${content}", body);
		return htmlTemplate.replace("${body}", body);
	}

	/**
	 * Loads properties from a file.
	 *
	 * @param file	The file containing the properties.
	 * @return	The loaded properties.
	 */
	private static Properties loadProperties(final File file){
		final Properties properties = new Properties();
		final String filename = file.getAbsolutePath()
			.replaceFirst("\\.[^.]+$", ".properties");
		try(final Reader in = new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8)){
			properties.load(in);
		}
		catch(final IOException ignored){}
		return properties;
	}

}
