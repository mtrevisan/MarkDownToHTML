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
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Service{

	private static final Random RANDOM = new Random();

	private static final Pattern KATEX_BOUNDARY_PATTERN = Pattern.compile("(\\$\\$?)(.*?)\\\\(.*?)(\\$\\$?)");


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

//			.set(TablesExtension.FOOTNOTE_SORT, ElementPlacement.AS_IS)
			.set(TablesExtension.FOOTNOTE_SORT, ElementPlacement.SORT)

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
		try(final BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))){
			String content = r.lines()
				.collect(Collectors.joining("\n"));
			content = replaceBackslash(content);
			//obfuscate emails
			content = obfuscateEmails(content);

			final Node document = PARSER.parse(content);
			/*
			add this javascript code to manage `eml` elements:
			var allElements = document.getElementsByClassName("eml");
			for(var i = 0; i < allElements.length; i ++)
				updateAnchor(allElements[i])
			function updateAnchor(el){
				el.href = 'mailto:' + decode(el.href);
			}
			function decode(encoded){
				var decoded = "";
				var key = parseInt(encoded.substr(0, 2), 16);
				for(var n = 2; n < encoded.length; n += 2)
					decoded += String.fromCharCode(parseInt(encoded.substr(n, 2), 16) ^ key);
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
							<!-- @font-face{font-family:KaTeX_AMS;src:url(/static/fonts/KaTeX_AMS-Regular.38a68f7.woff2) format("woff2"),url(/static/fonts/KaTeX_AMS-Regular.7d307e8.woff) format("woff"),url(/static/fonts/KaTeX_AMS-Regular.2dbe16b.ttf) format("truetype");font-weight:400;font-style:normal}@font-face{font-family:KaTeX_Caligraphic;src:url(/static/fonts/KaTeX_Caligraphic-Bold.342b296.woff2) format("woff2"),url(/static/fonts/KaTeX_Caligraphic-Bold.9634168.woff) format("woff"),url(/static/fonts/KaTeX_Caligraphic-Bold.33d2688.ttf) format("truetype");font-weight:700;font-style:normal}@font-face{font-family:KaTeX_Caligraphic;src:url(/static/fonts/KaTeX_Caligraphic-Regular.b500497.woff2) format("woff2"),url(/static/fonts/KaTeX_Caligraphic-Regular.00029fb.woff) format("woff"),url(/static/fonts/KaTeX_Caligraphic-Regular.5e7940b.ttf) format("truetype");font-weight:400;font-style:normal}@font-face{font-family:KaTeX_Fraktur;src:url(/static/fonts/KaTeX_Fraktur-Bold.7a3757c.woff2) format("woff2"),url(/static/fonts/KaTeX_Fraktur-Bold.4de87d4.woff) format("woff"),url(/static/fonts/KaTeX_Fraktur-Bold.ed33012.ttf) format("truetype");font-weight:700;font-style:normal}@font-face{font-family:KaTeX_Fraktur;src:url(/static/fonts/KaTeX_Fraktur-Regular.450cc4d.woff2) format("woff2"),url(/static/fonts/KaTeX_Fraktur-Regular.dc4e330.woff) format("woff"),url(/static/fonts/KaTeX_Fraktur-Regular.82d05fe.ttf) format("truetype");font-weight:400;font-style:normal}@font-face{font-family:KaTeX_Main;src:url(/static/fonts/KaTeX_Main-Bold.78b0124.woff2) format("woff2"),url(/static/fonts/KaTeX_Main-Bold.62c6975.woff) format("woff"),url(/static/fonts/KaTeX_Main-Bold.2e1915b.ttf) format("truetype");font-weight:700;font-style:normal}@font-face{font-family:KaTeX_Main;src:url(/static/fonts/KaTeX_Main-BoldItalic.c7213ce.woff2) format("woff2"),url(/static/fonts/KaTeX_Main-BoldItalic.a2e3dcd.woff) format("woff"),url(/static/fonts/KaTeX_Main-BoldItalic.0d817b4.ttf) format("truetype");font-weight:700;font-style:italic}@font-face{font-family:KaTeX_Main;src:url(/static/fonts/KaTeX_Main-Italic.eea3267.woff2) format("woff2"),url(/static/fonts/KaTeX_Main-Italic.081073f.woff) format("woff"),url(/static/fonts/KaTeX_Main-Italic.767e06e.ttf) format("truetype");font-weight:400;font-style:italic}@font-face{font-family:KaTeX_Main;src:url(/static/fonts/KaTeX_Main-Regular.f30e3b2.woff2) format("woff2"),url(/static/fonts/KaTeX_Main-Regular.756fad0.woff) format("woff"),url(/static/fonts/KaTeX_Main-Regular.689bbe6.ttf) format("truetype");font-weight:400;font-style:normal}@font-face{font-family:KaTeX_Math;src:url(/static/fonts/KaTeX_Math-BoldItalic.753ca3d.woff2) format("woff2"),url(/static/fonts/KaTeX_Math-BoldItalic.b3e80ff.woff) format("woff"),url(/static/fonts/KaTeX_Math-BoldItalic.d9377b5.ttf) format("truetype");font-weight:700;font-style:italic}@font-face{font-family:KaTeX_Math;src:url(/static/fonts/KaTeX_Math-Italic.2a39f38.woff2) format("woff2"),url(/static/fonts/KaTeX_Math-Italic.67710bb.woff) format("woff"),url(/static/fonts/KaTeX_Math-Italic.0343f93.ttf) format("truetype");font-weight:400;font-style:italic}@font-face{font-family:KaTeX_SansSerif;src:url(/static/fonts/KaTeX_SansSerif-Bold.59b3773.woff2) format("woff2"),url(/static/fonts/KaTeX_SansSerif-Bold.f28c4fa.woff) format("woff"),url(/static/fonts/KaTeX_SansSerif-Bold.dfcc59a.ttf) format("truetype");font-weight:700;font-style:normal}@font-face{font-family:KaTeX_SansSerif;src:url(/static/fonts/KaTeX_SansSerif-Italic.99ad93a.woff2) format("woff2"),url(/static/fonts/KaTeX_SansSerif-Italic.9d0fdf5.woff) format("woff"),url(/static/fonts/KaTeX_SansSerif-Italic.3ab5188.ttf) format("truetype");font-weight:400;font-style:italic}@font-face{font-family:KaTeX_SansSerif;src:url(/static/fonts/KaTeX_SansSerif-Regular.badf359.woff2) format("woff2"),url(/static/fonts/KaTeX_SansSerif-Regular.6c3bd5b.woff) format("woff"),url(/static/fonts/KaTeX_SansSerif-Regular.d511ebc.ttf) format("truetype");font-weight:400;font-style:normal}@font-face{font-family:KaTeX_Script;src:url(/static/fonts/KaTeX_Script-Regular.af7bc98.woff2) format("woff2"),url(/static/fonts/KaTeX_Script-Regular.4edf4e0.woff) format("woff"),url(/static/fonts/KaTeX_Script-Regular.082640c.ttf) format("truetype");font-weight:400;font-style:normal}@font-face{font-family:KaTeX_Size1;src:url(/static/fonts/KaTeX_Size1-Regular.10ec8be.woff2) format("woff2"),url(/static/fonts/KaTeX_Size1-Regular.35b9977.woff) format("woff"),url(/static/fonts/KaTeX_Size1-Regular.2c2dc3b.ttf) format("truetype");font-weight:400;font-style:normal}@font-face{font-family:KaTeX_Size2;src:url(/static/fonts/KaTeX_Size2-Regular.96a09bf.woff2) format("woff2"),url(/static/fonts/KaTeX_Size2-Regular.9932a08.woff) format("woff"),url(/static/fonts/KaTeX_Size2-Regular.114ad19.ttf) format("truetype");font-weight:400;font-style:normal}@font-face{font-family:KaTeX_Size3;src:url(/static/fonts/KaTeX_Size3-Regular.2c2f0ef.woff2) format("woff2"),url(/static/fonts/KaTeX_Size3-Regular.2afba15.woff) format("woff"),url(/static/fonts/KaTeX_Size3-Regular.a287c06.ttf) format("truetype");font-weight:400;font-style:normal}@font-face{font-family:KaTeX_Size4;src:url(/static/fonts/KaTeX_Size4-Regular.d5822f1.woff2) format("woff2"),url(/static/fonts/KaTeX_Size4-Regular.f961545.woff) format("woff"),url(/static/fonts/KaTeX_Size4-Regular.70174da.ttf) format("truetype");font-weight:400;font-style:normal}@font-face{font-family:KaTeX_Typewriter;src:url(/static/fonts/KaTeX_Typewriter-Regular.641339e.woff2) format("woff2"),url(/static/fonts/KaTeX_Typewriter-Regular.53dcf86.woff) format("woff"),url(/static/fonts/KaTeX_Typewriter-Regular.35fe2cc.ttf) format("truetype");font-weight:400;font-style:normal}.katex{font:normal 1.21em KaTeX_Main,Times New Roman,serif;line-height:1.2;text-indent:0;text-rendering:auto;border-color:currentColor}.katex *{-ms-high-contrast-adjust:none!important}.katex .katex-version:after{content:"0.13.0"}.katex .katex-mathml{position:absolute;clip:rect(1px,1px,1px,1px);padding:0;border:0;height:1px;width:1px;overflow:hidden}.katex .katex-html>.newline{display:block}.katex .base{position:relative;white-space:nowrap;width:-webkit-min-content;width:-moz-min-content;width:min-content}.katex .base,.katex .strut{display:inline-block}.katex .textbf{font-weight:700}.katex .textit{font-style:italic}.katex .textrm{font-family:KaTeX_Main}.katex .textsf{font-family:KaTeX_SansSerif}.katex .texttt{font-family:KaTeX_Typewriter}.katex .mathnormal{font-family:KaTeX_Math;font-style:italic}.katex .mathit{font-family:KaTeX_Main;font-style:italic}.katex .mathrm{font-style:normal}.katex .mathbf{font-family:KaTeX_Main;font-weight:700}.katex .boldsymbol{font-family:KaTeX_Math;font-weight:700;font-style:italic}.katex .amsrm,.katex .mathbb,.katex .textbb{font-family:KaTeX_AMS}.katex .mathcal{font-family:KaTeX_Caligraphic}.katex .mathfrak,.katex .textfrak{font-family:KaTeX_Fraktur}.katex .mathtt{font-family:KaTeX_Typewriter}.katex .mathscr,.katex .textscr{font-family:KaTeX_Script}.katex .mathsf,.katex .textsf{font-family:KaTeX_SansSerif}.katex .mathboldsf,.katex .textboldsf{font-family:KaTeX_SansSerif;font-weight:700}.katex .mathitsf,.katex .textitsf{font-family:KaTeX_SansSerif;font-style:italic}.katex .mainrm{font-family:KaTeX_Main;font-style:normal}.katex .vlist-t{display:inline-table;table-layout:fixed;border-collapse:collapse}.katex .vlist-r{display:table-row}.katex .vlist{display:table-cell;vertical-align:bottom;position:relative}.katex .vlist>span{display:block;height:0;position:relative}.katex .vlist>span>span{display:inline-block}.katex .vlist>span>.pstrut{overflow:hidden;width:0}.katex .vlist-t2{margin-right:-2px}.katex .vlist-s{display:table-cell;vertical-align:bottom;font-size:1px;width:2px;min-width:2px}.katex .vbox{display:inline-flex;flex-direction:column;align-items:baseline}.katex .hbox{width:100%}.katex .hbox,.katex .thinbox{display:inline-flex;flex-direction:row}.katex .thinbox{width:0;max-width:0}.katex .msupsub{text-align:left}.katex .mfrac>span>span{text-align:center}.katex .mfrac .frac-line{display:inline-block;width:100%;border-bottom-style:solid}.katex .hdashline,.katex .hline,.katex .mfrac .frac-line,.katex .overline .overline-line,.katex .rule,.katex .underline .underline-line{min-height:1px}.katex .mspace{display:inline-block}.katex .clap,.katex .llap,.katex .rlap{width:0;position:relative}.katex .clap>.inner,.katex .llap>.inner,.katex .rlap>.inner{position:absolute}.katex .clap>.fix,.katex .llap>.fix,.katex .rlap>.fix{display:inline-block}.katex .llap>.inner{right:0}.katex .clap>.inner,.katex .rlap>.inner{left:0}.katex .clap>.inner>span{margin-left:-50%;margin-right:50%}.katex .rule{display:inline-block;border:0 solid;position:relative}.katex .hline,.katex .overline .overline-line,.katex .underline .underline-line{display:inline-block;width:100%;border-bottom-style:solid}.katex .hdashline{display:inline-block;width:100%;border-bottom-style:dashed}.katex .sqrt>.root{margin-left:.27777778em;margin-right:-.55555556em}.katex .fontsize-ensurer.reset-size1.size1,.katex .sizing.reset-size1.size1{font-size:1em}.katex .fontsize-ensurer.reset-size1.size2,.katex .sizing.reset-size1.size2{font-size:1.2em}.katex .fontsize-ensurer.reset-size1.size3,.katex .sizing.reset-size1.size3{font-size:1.4em}.katex .fontsize-ensurer.reset-size1.size4,.katex .sizing.reset-size1.size4{font-size:1.6em}.katex .fontsize-ensurer.reset-size1.size5,.katex .sizing.reset-size1.size5{font-size:1.8em}.katex .fontsize-ensurer.reset-size1.size6,.katex .sizing.reset-size1.size6{font-size:2em}.katex .fontsize-ensurer.reset-size1.size7,.katex .sizing.reset-size1.size7{font-size:2.4em}.katex .fontsize-ensurer.reset-size1.size8,.katex .sizing.reset-size1.size8{font-size:2.88em}.katex .fontsize-ensurer.reset-size1.size9,.katex .sizing.reset-size1.size9{font-size:3.456em}.katex .fontsize-ensurer.reset-size1.size10,.katex .sizing.reset-size1.size10{font-size:4.148em}.katex .fontsize-ensurer.reset-size1.size11,.katex .sizing.reset-size1.size11{font-size:4.976em}.katex .fontsize-ensurer.reset-size2.size1,.katex .sizing.reset-size2.size1{font-size:.83333333em}.katex .fontsize-ensurer.reset-size2.size2,.katex .sizing.reset-size2.size2{font-size:1em}.katex .fontsize-ensurer.reset-size2.size3,.katex .sizing.reset-size2.size3{font-size:1.16666667em}.katex .fontsize-ensurer.reset-size2.size4,.katex .sizing.reset-size2.size4{font-size:1.33333333em}.katex .fontsize-ensurer.reset-size2.size5,.katex .sizing.reset-size2.size5{font-size:1.5em}.katex .fontsize-ensurer.reset-size2.size6,.katex .sizing.reset-size2.size6{font-size:1.66666667em}.katex .fontsize-ensurer.reset-size2.size7,.katex .sizing.reset-size2.size7{font-size:2em}.katex .fontsize-ensurer.reset-size2.size8,.katex .sizing.reset-size2.size8{font-size:2.4em}.katex .fontsize-ensurer.reset-size2.size9,.katex .sizing.reset-size2.size9{font-size:2.88em}.katex .fontsize-ensurer.reset-size2.size10,.katex .sizing.reset-size2.size10{font-size:3.45666667em}.katex .fontsize-ensurer.reset-size2.size11,.katex .sizing.reset-size2.size11{font-size:4.14666667em}.katex .fontsize-ensurer.reset-size3.size1,.katex .sizing.reset-size3.size1{font-size:.71428571em}.katex .fontsize-ensurer.reset-size3.size2,.katex .sizing.reset-size3.size2{font-size:.85714286em}.katex .fontsize-ensurer.reset-size3.size3,.katex .sizing.reset-size3.size3{font-size:1em}.katex .fontsize-ensurer.reset-size3.size4,.katex .sizing.reset-size3.size4{font-size:1.14285714em}.katex .fontsize-ensurer.reset-size3.size5,.katex .sizing.reset-size3.size5{font-size:1.28571429em}.katex .fontsize-ensurer.reset-size3.size6,.katex .sizing.reset-size3.size6{font-size:1.42857143em}.katex .fontsize-ensurer.reset-size3.size7,.katex .sizing.reset-size3.size7{font-size:1.71428571em}.katex .fontsize-ensurer.reset-size3.size8,.katex .sizing.reset-size3.size8{font-size:2.05714286em}.katex .fontsize-ensurer.reset-size3.size9,.katex .sizing.reset-size3.size9{font-size:2.46857143em}.katex .fontsize-ensurer.reset-size3.size10,.katex .sizing.reset-size3.size10{font-size:2.96285714em}.katex .fontsize-ensurer.reset-size3.size11,.katex .sizing.reset-size3.size11{font-size:3.55428571em}.katex .fontsize-ensurer.reset-size4.size1,.katex .sizing.reset-size4.size1{font-size:.625em}.katex .fontsize-ensurer.reset-size4.size2,.katex .sizing.reset-size4.size2{font-size:.75em}.katex .fontsize-ensurer.reset-size4.size3,.katex .sizing.reset-size4.size3{font-size:.875em}.katex .fontsize-ensurer.reset-size4.size4,.katex .sizing.reset-size4.size4{font-size:1em}.katex .fontsize-ensurer.reset-size4.size5,.katex .sizing.reset-size4.size5{font-size:1.125em}.katex .fontsize-ensurer.reset-size4.size6,.katex .sizing.reset-size4.size6{font-size:1.25em}.katex .fontsize-ensurer.reset-size4.size7,.katex .sizing.reset-size4.size7{font-size:1.5em}.katex .fontsize-ensurer.reset-size4.size8,.katex .sizing.reset-size4.size8{font-size:1.8em}.katex .fontsize-ensurer.reset-size4.size9,.katex .sizing.reset-size4.size9{font-size:2.16em}.katex .fontsize-ensurer.reset-size4.size10,.katex .sizing.reset-size4.size10{font-size:2.5925em}.katex .fontsize-ensurer.reset-size4.size11,.katex .sizing.reset-size4.size11{font-size:3.11em}.katex .fontsize-ensurer.reset-size5.size1,.katex .sizing.reset-size5.size1{font-size:.55555556em}.katex .fontsize-ensurer.reset-size5.size2,.katex .sizing.reset-size5.size2{font-size:.66666667em}.katex .fontsize-ensurer.reset-size5.size3,.katex .sizing.reset-size5.size3{font-size:.77777778em}.katex .fontsize-ensurer.reset-size5.size4,.katex .sizing.reset-size5.size4{font-size:.88888889em}.katex .fontsize-ensurer.reset-size5.size5,.katex .sizing.reset-size5.size5{font-size:1em}.katex .fontsize-ensurer.reset-size5.size6,.katex .sizing.reset-size5.size6{font-size:1.11111111em}.katex .fontsize-ensurer.reset-size5.size7,.katex .sizing.reset-size5.size7{font-size:1.33333333em}.katex .fontsize-ensurer.reset-size5.size8,.katex .sizing.reset-size5.size8{font-size:1.6em}.katex .fontsize-ensurer.reset-size5.size9,.katex .sizing.reset-size5.size9{font-size:1.92em}.katex .fontsize-ensurer.reset-size5.size10,.katex .sizing.reset-size5.size10{font-size:2.30444444em}.katex .fontsize-ensurer.reset-size5.size11,.katex .sizing.reset-size5.size11{font-size:2.76444444em}.katex .fontsize-ensurer.reset-size6.size1,.katex .sizing.reset-size6.size1{font-size:.5em}.katex .fontsize-ensurer.reset-size6.size2,.katex .sizing.reset-size6.size2{font-size:.6em}.katex .fontsize-ensurer.reset-size6.size3,.katex .sizing.reset-size6.size3{font-size:.7em}.katex .fontsize-ensurer.reset-size6.size4,.katex .sizing.reset-size6.size4{font-size:.8em}.katex .fontsize-ensurer.reset-size6.size5,.katex .sizing.reset-size6.size5{font-size:.9em}.katex .fontsize-ensurer.reset-size6.size6,.katex .sizing.reset-size6.size6{font-size:1em}.katex .fontsize-ensurer.reset-size6.size7,.katex .sizing.reset-size6.size7{font-size:1.2em}.katex .fontsize-ensurer.reset-size6.size8,.katex .sizing.reset-size6.size8{font-size:1.44em}.katex .fontsize-ensurer.reset-size6.size9,.katex .sizing.reset-size6.size9{font-size:1.728em}.katex .fontsize-ensurer.reset-size6.size10,.katex .sizing.reset-size6.size10{font-size:2.074em}.katex .fontsize-ensurer.reset-size6.size11,.katex .sizing.reset-size6.size11{font-size:2.488em}.katex .fontsize-ensurer.reset-size7.size1,.katex .sizing.reset-size7.size1{font-size:.41666667em}.katex .fontsize-ensurer.reset-size7.size2,.katex .sizing.reset-size7.size2{font-size:.5em}.katex .fontsize-ensurer.reset-size7.size3,.katex .sizing.reset-size7.size3{font-size:.58333333em}.katex .fontsize-ensurer.reset-size7.size4,.katex .sizing.reset-size7.size4{font-size:.66666667em}.katex .fontsize-ensurer.reset-size7.size5,.katex .sizing.reset-size7.size5{font-size:.75em}.katex .fontsize-ensurer.reset-size7.size6,.katex .sizing.reset-size7.size6{font-size:.83333333em}.katex .fontsize-ensurer.reset-size7.size7,.katex .sizing.reset-size7.size7{font-size:1em}.katex .fontsize-ensurer.reset-size7.size8,.katex .sizing.reset-size7.size8{font-size:1.2em}.katex .fontsize-ensurer.reset-size7.size9,.katex .sizing.reset-size7.size9{font-size:1.44em}.katex .fontsize-ensurer.reset-size7.size10,.katex .sizing.reset-size7.size10{font-size:1.72833333em}.katex .fontsize-ensurer.reset-size7.size11,.katex .sizing.reset-size7.size11{font-size:2.07333333em}.katex .fontsize-ensurer.reset-size8.size1,.katex .sizing.reset-size8.size1{font-size:.34722222em}.katex .fontsize-ensurer.reset-size8.size2,.katex .sizing.reset-size8.size2{font-size:.41666667em}.katex .fontsize-ensurer.reset-size8.size3,.katex .sizing.reset-size8.size3{font-size:.48611111em}.katex .fontsize-ensurer.reset-size8.size4,.katex .sizing.reset-size8.size4{font-size:.55555556em}.katex .fontsize-ensurer.reset-size8.size5,.katex .sizing.reset-size8.size5{font-size:.625em}.katex .fontsize-ensurer.reset-size8.size6,.katex .sizing.reset-size8.size6{font-size:.69444444em}.katex .fontsize-ensurer.reset-size8.size7,.katex .sizing.reset-size8.size7{font-size:.83333333em}.katex .fontsize-ensurer.reset-size8.size8,.katex .sizing.reset-size8.size8{font-size:1em}.katex .fontsize-ensurer.reset-size8.size9,.katex .sizing.reset-size8.size9{font-size:1.2em}.katex .fontsize-ensurer.reset-size8.size10,.katex .sizing.reset-size8.size10{font-size:1.44027778em}.katex .fontsize-ensurer.reset-size8.size11,.katex .sizing.reset-size8.size11{font-size:1.72777778em}.katex .fontsize-ensurer.reset-size9.size1,.katex .sizing.reset-size9.size1{font-size:.28935185em}.katex .fontsize-ensurer.reset-size9.size2,.katex .sizing.reset-size9.size2{font-size:.34722222em}.katex .fontsize-ensurer.reset-size9.size3,.katex .sizing.reset-size9.size3{font-size:.40509259em}.katex .fontsize-ensurer.reset-size9.size4,.katex .sizing.reset-size9.size4{font-size:.46296296em}.katex .fontsize-ensurer.reset-size9.size5,.katex .sizing.reset-size9.size5{font-size:.52083333em}.katex .fontsize-ensurer.reset-size9.size6,.katex .sizing.reset-size9.size6{font-size:.5787037em}.katex .fontsize-ensurer.reset-size9.size7,.katex .sizing.reset-size9.size7{font-size:.69444444em}.katex .fontsize-ensurer.reset-size9.size8,.katex .sizing.reset-size9.size8{font-size:.83333333em}.katex .fontsize-ensurer.reset-size9.size9,.katex .sizing.reset-size9.size9{font-size:1em}.katex .fontsize-ensurer.reset-size9.size10,.katex .sizing.reset-size9.size10{font-size:1.20023148em}.katex .fontsize-ensurer.reset-size9.size11,.katex .sizing.reset-size9.size11{font-size:1.43981481em}.katex .fontsize-ensurer.reset-size10.size1,.katex .sizing.reset-size10.size1{font-size:.24108004em}.katex .fontsize-ensurer.reset-size10.size2,.katex .sizing.reset-size10.size2{font-size:.28929605em}.katex .fontsize-ensurer.reset-size10.size3,.katex .sizing.reset-size10.size3{font-size:.33751205em}.katex .fontsize-ensurer.reset-size10.size4,.katex .sizing.reset-size10.size4{font-size:.38572806em}.katex .fontsize-ensurer.reset-size10.size5,.katex .sizing.reset-size10.size5{font-size:.43394407em}.katex .fontsize-ensurer.reset-size10.size6,.katex .sizing.reset-size10.size6{font-size:.48216008em}.katex .fontsize-ensurer.reset-size10.size7,.katex .sizing.reset-size10.size7{font-size:.57859209em}.katex .fontsize-ensurer.reset-size10.size8,.katex .sizing.reset-size10.size8{font-size:.69431051em}.katex .fontsize-ensurer.reset-size10.size9,.katex .sizing.reset-size10.size9{font-size:.83317261em}.katex .fontsize-ensurer.reset-size10.size10,.katex .sizing.reset-size10.size10{font-size:1em}.katex .fontsize-ensurer.reset-size10.size11,.katex .sizing.reset-size10.size11{font-size:1.19961427em}.katex .fontsize-ensurer.reset-size11.size1,.katex .sizing.reset-size11.size1{font-size:.20096463em}.katex .fontsize-ensurer.reset-size11.size2,.katex .sizing.reset-size11.size2{font-size:.24115756em}.katex .fontsize-ensurer.reset-size11.size3,.katex .sizing.reset-size11.size3{font-size:.28135048em}.katex .fontsize-ensurer.reset-size11.size4,.katex .sizing.reset-size11.size4{font-size:.32154341em}.katex .fontsize-ensurer.reset-size11.size5,.katex .sizing.reset-size11.size5{font-size:.36173633em}.katex .fontsize-ensurer.reset-size11.size6,.katex .sizing.reset-size11.size6{font-size:.40192926em}.katex .fontsize-ensurer.reset-size11.size7,.katex .sizing.reset-size11.size7{font-size:.48231511em}.katex .fontsize-ensurer.reset-size11.size8,.katex .sizing.reset-size11.size8{font-size:.57877814em}.katex .fontsize-ensurer.reset-size11.size9,.katex .sizing.reset-size11.size9{font-size:.69453376em}.katex .fontsize-ensurer.reset-size11.size10,.katex .sizing.reset-size11.size10{font-size:.83360129em}.katex .fontsize-ensurer.reset-size11.size11,.katex .sizing.reset-size11.size11{font-size:1em}.katex .delimsizing.size1{font-family:KaTeX_Size1}.katex .delimsizing.size2{font-family:KaTeX_Size2}.katex .delimsizing.size3{font-family:KaTeX_Size3}.katex .delimsizing.size4{font-family:KaTeX_Size4}.katex .delimsizing.mult .delim-size1>span{font-family:KaTeX_Size1}.katex .delimsizing.mult .delim-size4>span{font-family:KaTeX_Size4}.katex .nulldelimiter{display:inline-block;width:.12em}.katex .delimcenter,.katex .op-symbol{position:relative}.katex .op-symbol.small-op{font-family:KaTeX_Size1}.katex .op-symbol.large-op{font-family:KaTeX_Size2}.katex .accent>.vlist-t,.katex .op-limits>.vlist-t{text-align:center}.katex .accent .accent-body{position:relative}.katex .accent .accent-body:not(.accent-full){width:0}.katex .overlay{display:block}.katex .mtable .vertical-separator{display:inline-block;min-width:1px}.katex .mtable .arraycolsep{display:inline-block}.katex .mtable .col-align-c>.vlist-t{text-align:center}.katex .mtable .col-align-l>.vlist-t{text-align:left}.katex .mtable .col-align-r>.vlist-t{text-align:right}.katex .svg-align{text-align:left}.katex svg{display:block;position:absolute;width:100%;height:inherit;fill:currentColor;stroke:currentColor;fill-rule:nonzero;fill-opacity:1;stroke-width:1;stroke-linecap:butt;stroke-linejoin:miter;stroke-miterlimit:4;stroke-dasharray:none;stroke-dashoffset:0;stroke-opacity:1}.katex svg path{stroke:none}.katex img{border-style:none;min-width:0;min-height:0;max-width:none;max-height:none}.katex .stretchy{width:100%;display:block;position:relative;overflow:hidden}.katex .stretchy:after,.katex .stretchy:before{content:""}.katex .hide-tail{width:100%;position:relative;overflow:hidden}.katex .halfarrow-left{position:absolute;left:0;width:50.2%;overflow:hidden}.katex .halfarrow-right{position:absolute;right:0;width:50.2%;overflow:hidden}.katex .brace-left{position:absolute;left:0;width:25.1%;overflow:hidden}.katex .brace-center{position:absolute;left:25%;width:50%;overflow:hidden}.katex .brace-right{position:absolute;right:0;width:25.1%;overflow:hidden}.katex .x-arrow-pad{padding:0 .5em}.katex .cd-arrow-pad{padding:0 .55556em 0 .27778em}.katex .mover,.katex .munder,.katex .x-arrow{text-align:center}.katex .boxpad{padding:0 .3em}.katex .fbox,.katex .fcolorbox{box-sizing:border-box;border:.04em solid}.katex .cancel-pad{padding:0 .2em}.katex .cancel-lap{margin-left:-.2em;margin-right:-.2em}.katex .sout{border-bottom-style:solid;border-bottom-width:.08em}.katex .angl{box-sizing:border-content;border-top:.049em solid;border-right:.049em solid;margin-right:.03889em}.katex .anglpad{padding:0 .03889em}.katex .eqn-num:before{counter-increment:a;content:"(" counter(a) ")"}.katex .mml-eqn-num:before{counter-increment:b;content:"(" counter(b) ")"}.katex .mtr-glue{width:50%}.katex .cd-vert-arrow{display:inline-block;position:relative}.katex .cd-label-left{display:inline-block;position:absolute;right:calc(50% + .3em);text-align:left}.katex .cd-label-right{display:inline-block;position:absolute;left:calc(50% + .3em);text-align:right}.katex-display{display:block;margin:1em 0;text-align:center}.katex-display>.katex{display:block;text-align:center;white-space:nowrap}.katex-display>.katex>.katex-html{display:block;position:relative}.katex-display>.katex>.katex-html>.tag{position:absolute;right:0}.katex-display.leqno>.katex>.katex-html>.tag{left:0;right:auto}.katex-display.fleqn>.katex{text-align:left;padding-left:2em}body{counter-reset:a b}@font-face{font-family:Lato;font-style:normal;font-weight:400;src:url(/static/fonts/lato-normal.27bd77b.woff) format("woff")}@font-face{font-family:Lato;font-style:italic;font-weight:400;src:url(/static/fonts/lato-normal-italic.f28f2d6.woff) format("woff")}@font-face{font-family:Lato;font-style:normal;font-weight:600;src:url(/static/fonts/lato-black.f80bda6.woff) format("woff")}@font-face{font-family:Lato;font-style:italic;font-weight:600;src:url(/static/fonts/lato-black-italic.798eafd.woff) format("woff")}@font-face{font-family:Roboto Mono;font-style:normal;font-weight:400;src:url(/static/fonts/RobotoMono-Regular.0b6a547.woff) format("woff")}@font-face{font-family:Roboto Mono;font-style:normal;font-weight:600;src:url(/static/fonts/RobotoMono-Bold.819f3b2.woff) format("woff")}.prism *,.token.pre.gfm *{font-weight:inherit!important}.prism .token.cdata,.prism .token.comment,.prism .token.doctype,.prism .token.prolog,.token.pre.gfm .token.cdata,.token.pre.gfm .token.comment,.token.pre.gfm .token.doctype,.token.pre.gfm .token.prolog{color:#708090}.prism .token.punctuation,.token.pre.gfm .token.punctuation{color:#999}.prism .namespace,.token.pre.gfm .namespace{opacity:.7}.prism .token.boolean,.prism .token.constant,.prism .token.deleted,.prism .token.number,.prism .token.property,.prism .token.symbol,.prism .token.tag,.token.pre.gfm .token.boolean,.token.pre.gfm .token.constant,.token.pre.gfm .token.deleted,.token.pre.gfm .token.number,.token.pre.gfm .token.property,.token.pre.gfm .token.symbol,.token.pre.gfm .token.tag{color:#905}.prism .token.attr-name,.prism .token.builtin,.prism .token.char,.prism .token.inserted,.prism .token.selector,.prism .token.string,.token.pre.gfm .token.attr-name,.token.pre.gfm .token.builtin,.token.pre.gfm .token.char,.token.pre.gfm .token.inserted,.token.pre.gfm .token.selector,.token.pre.gfm .token.string{color:#690}.prism .language-css .token.string,.prism .style .token.string,.prism .token.entity,.prism .token.operator,.prism .token.url,.token.pre.gfm .language-css .token.string,.token.pre.gfm .style .token.string,.token.pre.gfm .token.entity,.token.pre.gfm .token.operator,.token.pre.gfm .token.url{color:#a67f59}.prism .token.atrule,.prism .token.attr-value,.prism .token.keyword,.token.pre.gfm .token.atrule,.token.pre.gfm .token.attr-value,.token.pre.gfm .token.keyword{color:#07a}.prism .token.function,.token.pre.gfm .token.function{color:#dd4a68}.prism .token.important,.prism .token.regex,.prism .token.variable,.token.pre.gfm .token.important,.token.pre.gfm .token.regex,.token.pre.gfm .token.variable{color:#e90}.prism .token.bold,.prism .token.important,.token.pre.gfm .token.bold,.token.pre.gfm .token.important{font-weight:500}.prism .token.italic,.token.pre.gfm .token.italic{font-style:italic}/*! normalize-scss | MIT/GPLv2 License | bit.ly/normalize-scss */html{line-height:1.15;-ms-text-size-adjust:100%;-webkit-text-size-adjust:100%}body{margin:0}article,aside,footer,header,nav,section{display:block}h1{font-size:2em;margin:.67em 0}figcaption,figure{display:block}figure{margin:1em 40px}hr{box-sizing:content-box;height:0;overflow:visible}main{display:block}pre{font-family:monospace,monospace;font-size:1em}a{background-color:transparent;-webkit-text-decoration-skip:objects}abbr[title]{border-bottom:none;text-decoration:underline;text-decoration:underline dotted}b,strong{font-weight:inherit;font-weight:bolder}code,kbd,samp{font-family:monospace,monospace;font-size:1em}dfn{font-style:italic}mark{background-color:#ff0;color:#000}small{font-size:80%}sub,sup{font-size:75%;line-height:0;position:relative;vertical-align:baseline}sub{bottom:-.25em}sup{top:-.5em}audio,video{display:inline-block}audio:not([controls]){display:none;height:0}img{border-style:none}svg:not(:root){overflow:hidden}button,input,optgroup,select,textarea{font-family:sans-serif;font-size:100%;line-height:1.15;margin:0}button{overflow:visible}button,select{text-transform:none}[type=reset],[type=submit],button,html [type=button]{-webkit-appearance:button}[type=button]::-moz-focus-inner,[type=reset]::-moz-focus-inner,[type=submit]::-moz-focus-inner,button::-moz-focus-inner{border-style:none;padding:0}[type=button]:-moz-focusring,[type=reset]:-moz-focusring,[type=submit]:-moz-focusring,button:-moz-focusring{outline:1px dotted ButtonText}input{overflow:visible}[type=checkbox],[type=radio]{box-sizing:border-box;padding:0}[type=number]::-webkit-inner-spin-button,[type=number]::-webkit-outer-spin-button{height:auto}[type=search]{-webkit-appearance:textfield;outline-offset:-2px}[type=search]::-webkit-search-cancel-button,[type=search]::-webkit-search-decoration{-webkit-appearance:none}::-webkit-file-upload-button{-webkit-appearance:button;font:inherit}fieldset{padding:.35em .75em .625em}legend{box-sizing:border-box;display:table;max-width:100%;padding:0;color:inherit;white-space:normal}progress{display:inline-block;vertical-align:baseline}textarea{overflow:auto}details{display:block}summary{display:list-item}menu{display:block}canvas{display:inline-block}[hidden],template{display:none}body,html{color:rgba(0,0,0,.75);font-size:16px;font-family:Lato,Helvetica Neue,Helvetica,sans-serif;font-variant-ligatures:common-ligatures;line-height:1.67;-webkit-font-smoothing:antialiased;-moz-osx-font-smoothing:grayscale}.app--dark .layout__panel--editor,.app--dark .layout__panel--preview{color:hsla(0,0%,100%,.75)}blockquote,dl,ol,p,pre,ul{margin:1.2em 0}h1,h2,h3,h4,h5,h6{margin:1.8em 0;line-height:1.33}h1:after,h2:after{content:"";display:block;position:relative;top:.33em;border-bottom:1px solid hsla(0,0%,50%,.33)}ol ol,ol ul,ul ol,ul ul{margin:0}dt{font-weight:700}a{color:#0c93e4;text-decoration:underline;text-decoration-skip:ink}a:focus,a:hover{text-decoration:none}code,pre,samp{font-family:Roboto Mono,Lucida Sans Typewriter,Lucida Console,monaco,Courrier,monospace;font-size:.85em}code *,pre *,samp *{font-size:inherit}blockquote{color:rgba(0,0,0,.5);padding-left:1.5em;border-left:5px solid rgba(0,0,0,.1)}.app--dark .layout__panel--editor blockquote,.app--dark .layout__panel--preview blockquote{color:hsla(0,0%,100%,.4);border-left-color:hsla(0,0%,100%,.1)}code{background-color:rgba(0,0,0,.05);border-radius:3px;padding:2px 4px}hr{border:0;border-top:1px solid hsla(0,0%,50%,.33);margin:2em 0}pre>code{background-color:rgba(0,0,0,.05);display:block;padding:.5em;-webkit-text-size-adjust:none;overflow-x:auto;white-space:pre}.toc ul{list-style-type:none;padding-left:20px}table{background-color:transparent;border-collapse:collapse;border-spacing:0}td,th{border-right:1px solid #dcdcdc;padding:8px 12px}td:last-child,th:last-child{border-right:0}td{border-top:1px solid #dcdcdc}mark{background-color:#f8f840}kbd{font-family:Lato,Helvetica Neue,Helvetica,sans-serif;background-color:#fff;border:1px solid rgba(63,63,63,.25);border-radius:3px;box-shadow:0 1px 0 rgba(63,63,63,.25);color:#333;display:inline-block;font-size:.8em;margin:0 .1em;padding:.1em .6em;white-space:nowrap}abbr[title]{border-bottom:1px dotted #777;cursor:help}img{max-width:100%}.task-list-item{list-style-type:none}.task-list-item-checkbox{margin:0 .2em 0 -1.3em}.footnote{font-size:.8em;position:relative;top:-.25em;vertical-align:top}.page-break-after{page-break-after:always}.abc-notation-block{overflow-x:auto!important}.stackedit__html{margin-bottom:180px;margin-left:auto;margin-right:auto;padding-left:30px;padding-right:30px;max-width:750px}.stackedit__toc ul{padding:0}.stackedit__toc ul a{margin:.5rem 0;padding:.5rem 1rem}.stackedit__toc ul ul{color:#888;font-size:.9em}.stackedit__toc ul ul a{margin:0;padding:.1rem 1rem}.stackedit__toc li{display:block}.stackedit__toc a{display:block;color:inherit;text-decoration:none}.stackedit__toc a:active,.stackedit__toc a:focus,.stackedit__toc a:hover{background-color:rgba(0,0,0,.075);border-radius:3px}.stackedit__left{position:fixed;display:none;width:250px;height:100%;top:0;left:0;overflow-x:hidden;overflow-y:auto;-webkit-overflow-scrolling:touch;-ms-overflow-style:none}@media (min-width:1060px){.stackedit__left{display:block}}.stackedit__right{position:absolute;right:0;top:0;left:0}@media (min-width:1060px){.stackedit__right{left:250px}}.stackedit--pdf blockquote{border-left-color:#ececec}.stackedit--pdf .katex-mathml,.stackedit--pdf annotation{display:none}.stackedit--pdf .stackedit__html{padding-left:0;padding-right:0;max-width:none} -->
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

					<body onload="javascript:var a=document.getElementsByClassName("eml");for(var i=0;i<a.length;i++)b(a[i])function b(el){el.href='mailto:'+c(el.href)}function c(d){var e="";var f=parseInt(d.substr(0, 2),16);for(var n=2;n<d.length;n+=2)e+=String.fromCharCode(parseInt(d.substr(n,2),16)^f);return e;}" class="stackedit">
						<div class="stackedit__html" style="text-align:justify;hyphens:none;">
					""";
			final String htmlEnd = """
					</div>
				</body>

				</html>
				""";
			htmlBegin = htmlBegin.replaceFirst("\\$\\{title\\}", FileUtil.getNameOnly(file));
			final StringJoiner sj = new StringJoiner("", htmlBegin, htmlEnd);
			final String html = RENDERER.render(document);
			sj.add(html);

			return sj.toString();
		}
	}

//	private static String replaceBackslash(final String input){
//		String replacement = input;
//		int start = replacement.indexOf(KATEX_BOUNDARY);
//		while(start != -1){
//			int end = replacement.indexOf(KATEX_BOUNDARY, start + KATEX_BOUNDARY.length());
//			if(end == -1)
//				break;
//
//			String substring = replacement.substring(start, end);
//			substring = substring.replace("\\\\", "\\\\\\\\")
//				.replace("\\%", "\\\\%");
//			replacement = replacement.substring(0, start)
//				+ substring
//				+ replacement.substring(end);
//
//			end = replacement.indexOf(KATEX_BOUNDARY, start + KATEX_BOUNDARY.length());
//			start = replacement.indexOf(KATEX_BOUNDARY, end + KATEX_BOUNDARY.length());
//		}
//		return replacement;
//	}

	/** Replaces `\` inside `$` or `$$` into `\\`. */
	private static String replaceBackslash(final String input){
		return KATEX_BOUNDARY_PATTERN.matcher(input)
			.replaceAll("$1$2\\\\\\\\$3$4");
//		return input.replaceAll("(\\$\\$?)(.*?)\\\\(.*?)(\\$\\$?)", "$1$2\\\\\\\\$3$4");
	}

	private static String obfuscateEmails(final String input){
		String replacement = input;
		int start = replacement.indexOf("<a ");
		while(start != -1){
			int end = replacement.indexOf(">", start + 3);
			if(end == -1)
				break;

			final int startHRef = replacement.indexOf("href=\"", start + 3);
			final int endHRef = replacement.indexOf("\"", startHRef);
			final String href = replacement.substring(startHRef + 6, endHRef);

			replacement = replacement.substring(0, startHRef)
				+ "href=\""
				+ encode(href, RANDOM.nextInt(256))
				+ "\"";

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
		return String.format("%02d", Integer.parseInt(Integer.toHexString(value)));
	}

}
