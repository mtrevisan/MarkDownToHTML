package com.vladsch.flexmark.ext.footnotes.internal;

import com.vladsch.flexmark.ext.footnotes.Footnote;
import com.vladsch.flexmark.ext.footnotes.FootnoteBlock;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.KeepType;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeRepository;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.DataKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@SuppressWarnings("WeakerAccess")
public class FootnoteRepository extends NodeRepository<FootnoteBlock>{

	private final ArrayList<FootnoteBlock> referencedFootnoteBlocks = new ArrayList<>();


	public static void resolveFootnotes(final Document document){
		final FootnoteRepository footnoteRepository = FootnoteExtension.FOOTNOTES.get(document);

		final boolean[] hadNewFootnotes = {false};
		final NodeVisitor visitor = new NodeVisitor(new VisitHandler<>(Footnote.class, node -> {
			if(!node.isDefined()){
				FootnoteBlock footnoteBlock = node.getFootnoteBlock(footnoteRepository);

				if(footnoteBlock != null){
					footnoteRepository.addFootnoteReference(footnoteBlock, node);
					node.setFootnoteBlock(footnoteBlock);
					hadNewFootnotes[0] = true;
				}
			}
		}));

		visitor.visit(document);
		if(hadNewFootnotes[0])
			footnoteRepository.resolveFootnoteOrdinals();
	}

	public void addFootnoteReference(final FootnoteBlock footnoteBlock, final Footnote footnote){
		if(!footnoteBlock.isReferenced())
			referencedFootnoteBlocks.add(footnoteBlock);

		if(!footnoteBlock.isReferenced())
			footnoteBlock.setFirstReferenceOffset(footnote.getStartOffset());
		else
			footnoteBlock.addFirstReferenceOffset(footnote.getStartOffset());

		final int referenceOrdinal = footnoteBlock.getFootnoteReferences();
		footnoteBlock.setFootnoteReferences(referenceOrdinal + 1);
		footnote.setReferenceOrdinal(referenceOrdinal);
	}

	public void resolveFootnoteOrdinals(){
		//need to sort by first referenced offset then set each to its ordinal position in the array+1
		referencedFootnoteBlocks.sort(Comparator.comparingInt(FootnoteBlock::getFirstReferenceOffset));

		int ordinal = 0;
		for(final FootnoteBlock footnoteBlock : referencedFootnoteBlocks)
			footnoteBlock.setFootnoteOrdinal(++ ordinal);
	}

	public List<FootnoteBlock> getReferencedFootnoteBlocks(){
		return referencedFootnoteBlocks;
	}

	public FootnoteRepository(final DataHolder options){
		super(FootnoteExtension.FOOTNOTES_KEEP.get(options));
	}

	@Override
	public DataKey<FootnoteRepository> getDataKey(){
		return FootnoteExtension.FOOTNOTES;
	}

	@Override
	public DataKey<KeepType> getKeepDataKey(){
		return FootnoteExtension.FOOTNOTES_KEEP;
	}

	@Override
	public Set<FootnoteBlock> getReferencedElements(final Node parent){
		final HashSet<FootnoteBlock> references = new HashSet<>();
		visitNodes(parent, value -> {
			if(value instanceof Footnote){
				final FootnoteBlock reference = ((Footnote)value).getReferenceNode(FootnoteRepository.this);
				if(reference != null)
					references.add(reference);
			}
		}, Footnote.class);
		return references;
	}

}
