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

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.BorderLayout;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;


public class Main{

	public static void main(String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e){
			e.printStackTrace();
		}

		SwingUtilities.invokeLater(() -> {
			final JFrame frame = new JFrame("Convert markdown file");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(400, 300);

			//create the drag and drop listener
			final DragDropListener dragDropListener = new DragDropListener(frame);

			//connect the label with a drag and drop listener
			JLabel dragLabel = new JLabel("Drag markdown file here!", SwingConstants.CENTER);
			new DropTarget(dragLabel, dragDropListener);
			frame.getContentPane()
				.add(BorderLayout.CENTER, dragLabel);

			addCancelByEscapeKey(frame);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

	/**
	 * Force the escape key to call the same action as pressing the Cancel button.
	 *
	 * @param dialog	Dialog to attach the escape key to
	 */
	private static void addCancelByEscapeKey(final JFrame dialog){
		addCancelByEscapeKey(dialog, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -7549108751241576611L;

			@Override
			public void actionPerformed(final ActionEvent e){
				dialog.dispose();
			}


			@Override
			@SuppressWarnings("NewExceptionWithoutArguments")
			protected Object clone() throws CloneNotSupportedException{
				throw new CloneNotSupportedException();
			}

			@SuppressWarnings("unused")
			@Serial
			private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
				throw new NotSerializableException(getClass().getName());
			}

			@SuppressWarnings("unused")
			@Serial
			private void readObject(final ObjectInputStream is) throws NotSerializableException{
				throw new NotSerializableException(getClass().getName());
			}
		});
	}

	/**
	 * Force the escape key to call the same action as pressing the Cancel button.
	 *
	 * @param dialog	Dialog to attach the escape key to
	 * @param cancelAction	Action to be performed on cancel
	 */
	private static void addCancelByEscapeKey(final JFrame dialog, final ActionListener cancelAction){
		final KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
		dialog.getRootPane()
			.registerKeyboardAction(cancelAction, escapeKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

}