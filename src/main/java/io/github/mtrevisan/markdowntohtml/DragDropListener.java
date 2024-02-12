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

import com.vladsch.flexmark.util.misc.FileUtil;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class DragDropListener implements DropTargetListener{

	private final JFrame parent;


	public DragDropListener(final JFrame parent){
		this.parent = parent;
	}

	@Override
	public void dragEnter(final DropTargetDragEvent event){}

	@Override
	public void dragOver(final DropTargetDragEvent event){}

	@Override
	public void dropActionChanged(final DropTargetDragEvent event){}

	@Override
	public void dragExit(final DropTargetEvent event){}

	@Override
	public void drop(final DropTargetDropEvent event){
		//accept copy drops
		event.acceptDrop(DnDConstants.ACTION_COPY);

		//get the transfer which can provide the dropped item data
		final Transferable transferable = event.getTransferable();

		//get the data formats of the dropped item
		final DataFlavor[] flavors = transferable.getTransferDataFlavors();
		//loop through the flavors
		for(final DataFlavor flavor : flavors){
			try{
				//if the drop items are files
				if(flavor.isFlavorJavaFileListType()){
					//get all the dropped files
					@SuppressWarnings("unchecked")
					final List<File> files = (List<File>)transferable.getTransferData(flavor);

					//output directory
					final String currentDir = (!files.isEmpty()? files.get(0).getParent(): ".");
					final File outFolder = new File(currentDir);

					for(final File file : files){
						if(!FileUtil.getDotExtension(file).equals(".md")){
							final JOptionPane outPane = new JOptionPane();
							outPane.setMessage("Cannot process this type of file (" + file.getName() + "), only .md allowed");
							outPane.setMessageType(JOptionPane.WARNING_MESSAGE);
							outPane.setOptionType(JOptionPane.DEFAULT_OPTION);
							final JDialog dialog = outPane.createDialog(null, "Wrong input");
							dialog.setVisible(true);

							continue;
						}

						final ConfigurationDialog configurationDialog = new ConfigurationDialog(parent);
						configurationDialog.setMinimumSize(new Dimension(170, 100));
						configurationDialog.setVisible(true);
						final boolean generateTOC = configurationDialog.isGenerateTOC();
						final boolean preventCopying = configurationDialog.isPreventCopying();

						final List<String> ids = Service.extractIDs(file);
						final HashSet<String> uniqueIDs = new HashSet<>();
						final Set<String> duplicatedIDs = ids.stream()
							.filter(e -> !uniqueIDs.add(e))
							.collect(Collectors.toSet());
						if(!duplicatedIDs.isEmpty())
							JOptionPane.showMessageDialog(null, "There are duplicated IDs: " + duplicatedIDs,
								"Duplicated IDs found", JOptionPane.INFORMATION_MESSAGE);

						final String html = Service.convert(file, generateTOC, preventCopying);

						final JOptionPane outPane = new JOptionPane();
						outPane.setMessage("Output saved");
						outPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
						outPane.setOptionType(JOptionPane.DEFAULT_OPTION);
						//save output
						final File outFile = new File(outFolder, FileUtil.getNameOnly(file) + ".html");
						try(final FileWriter writer = new FileWriter(outFile, StandardCharsets.UTF_8)){
							writer.write(html);
						}
						catch(final IOException e){
							e.printStackTrace();

							outPane.setMessage("Processing error");
							outPane.setMessageType(JOptionPane.ERROR_MESSAGE);
						}
						final JDialog resultDialog = outPane.createDialog(null, "Processing result");
						resultDialog.setVisible(true);
					}
				}
			}
			catch(Exception e){
				//print out the error stack
				e.printStackTrace();
			}
		}

		//inform that the drop is complete
		event.dropComplete(true);
	}

}
