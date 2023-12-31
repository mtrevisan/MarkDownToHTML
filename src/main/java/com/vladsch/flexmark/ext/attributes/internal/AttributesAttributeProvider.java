package com.vladsch.flexmark.ext.attributes.internal;

import com.vladsch.flexmark.ast.AnchorRefTarget;
import com.vladsch.flexmark.ext.attributes.AttributeNode;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.attributes.AttributesNode;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.CoreNodeRenderer;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.html.Attribute;
import com.vladsch.flexmark.util.html.MutableAttributes;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.ArrayList;


public class AttributesAttributeProvider implements AttributeProvider{

	final private NodeAttributeRepository nodeAttributeRepository;
	final private AttributesOptions attributeOptions;


	public AttributesAttributeProvider(final LinkResolverContext context){
		final DataHolder options = context.getOptions();
		attributeOptions = new AttributesOptions(options);
		nodeAttributeRepository = AttributesExtension.NODE_ATTRIBUTES.get(options);
	}

	@Override
	public void setAttributes(final Node node, final AttributablePart part, final MutableAttributes attributes){
		// regression bug, issue #372, add option, default to both as before
		if(part == CoreNodeRenderer.CODE_CONTENT? attributeOptions.fencedCodeAddAttributes.addToCode:
				attributeOptions.fencedCodeAddAttributes.addToPre){
			ArrayList<AttributesNode> nodeAttributesList = nodeAttributeRepository.get(node);
			if(nodeAttributesList != null){
				// add these as attributes
				for(final AttributesNode nodeAttributes : nodeAttributesList){
					for(final Node attribute : nodeAttributes.getChildren()){
						if(!(attribute instanceof AttributeNode attributeNode))
							continue;

						if(!attributeNode.isImplicitName()){
							final BasedSequence attributeNodeName = attributeNode.getName();
							if(attributeNodeName.isNotNull() && !attributeNodeName.isBlank()){
								if(!attributeNodeName.toString().equals(Attribute.CLASS_ATTR))
									attributes.remove(attributeNodeName);
								attributes.addValue(attributeNodeName, attributeNode.getValue());
							}
							else{
								// empty then ignore
							}
						}
						else{
							// implicit
							if(attributeNode.isClass())
								attributes.addValue(Attribute.CLASS_ATTR, attributeNode.getValue());
							else if(attributeNode.isId()){
								if(node instanceof AnchorRefTarget){
									// was already provided via setAnchorRefId, we only have to read it
									attributes.remove(Attribute.ID_ATTR);
									attributes.addValue(Attribute.ID_ATTR, ((AnchorRefTarget) node).getAnchorRefId());
								}
								else{
									attributes.remove(Attribute.ID_ATTR);
									attributes.addValue(Attribute.ID_ATTR, attributeNode.getValue());
								}
							}
							else
								// unknown
								throw new IllegalStateException("Implicit attribute yet not class or id");
						}
					}
				}
			}
		}
	}

	public static class Factory extends IndependentAttributeProviderFactory{
		//@Override
		//public Set<Class<?>> getAfterDependents(){
		//    return null;
		//}
		//
		//@Override
		//public Set<Class<?>> getBeforeDependents(){
		//    return null;
		//}
		//
		//@Override
		//public boolean affectsGlobalScope(){
		//    return false;
		//}

		@Override
		public AttributeProvider apply(final LinkResolverContext context){
			return new AttributesAttributeProvider(context);
		}
	}

}
