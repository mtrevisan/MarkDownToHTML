package com.vladsch.flexmark.ext.attributes;

import com.vladsch.flexmark.util.misc.CharPredicate;
import com.vladsch.flexmark.util.sequence.SequenceUtils;


public enum AttributeValueQuotes{

	AS_IS, NO_QUOTES_SINGLE_PREFERRED, NO_QUOTES_DOUBLE_PREFERRED, SINGLE_PREFERRED, DOUBLE_PREFERRED, SINGLE_QUOTES, DOUBLE_QUOTES;


	final static CharPredicate P_SPACES_OR_QUOTES = CharPredicate.anyOf(" \t\n'\"");
	final static CharPredicate P_SINGLE_QUOTES = CharPredicate.anyOf("'");
	final static CharPredicate P_DOUBLE_QUOTES = CharPredicate.anyOf("\"");

	public String quotesFor(final CharSequence text, final CharSequence defaultQuotes){
		switch(this){
			case NO_QUOTES_SINGLE_PREFERRED:
				if(!SequenceUtils.containsAny(text, P_SPACES_OR_QUOTES))
					return "";
				else if(!SequenceUtils.containsAny(text, P_SINGLE_QUOTES) || SequenceUtils.containsAny(text, P_DOUBLE_QUOTES))
					return "'";
				else
					return "\"";

			case NO_QUOTES_DOUBLE_PREFERRED:
				if(!SequenceUtils.containsAny(text, P_SPACES_OR_QUOTES))
					return "";
				else if(!SequenceUtils.containsAny(text, P_DOUBLE_QUOTES) || SequenceUtils.containsAny(text, P_SINGLE_QUOTES))
					return "\"";
				else
					return "'";

			case SINGLE_PREFERRED:
				return (!SequenceUtils.containsAny(text, P_SINGLE_QUOTES) || SequenceUtils.containsAny(text, P_DOUBLE_QUOTES)? "'": "\"");

			case DOUBLE_PREFERRED:
				return (!SequenceUtils.containsAny(text, P_DOUBLE_QUOTES) || SequenceUtils.containsAny(text, P_SINGLE_QUOTES)? "\"": "'");

			case SINGLE_QUOTES:
				return "'";

			case DOUBLE_QUOTES:
				return "\"";

			case AS_IS:
			default:
				return defaultQuotes.toString();
		}
	}

}
