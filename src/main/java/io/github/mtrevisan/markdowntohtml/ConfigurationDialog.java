package io.github.mtrevisan.markdowntohtml;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;


public class ConfigurationDialog extends JDialog{

	private boolean generateTOC;
	private boolean preventCopying;


	public ConfigurationDialog(final String filename, final Frame owner){
		super(owner, "Configuration", true);

		final JLabel filenameLabel = new JLabel(filename);
		final JCheckBox generateTOCCheckBox = new JCheckBox("Generate TOC");
		final JCheckBox preventCopyingCheckBox = new JCheckBox("Prevent copying");
		final JButton confirmButton = new JButton("Confirm");
		confirmButton.addActionListener(e -> {
			generateTOC = generateTOCCheckBox.isSelected();
			preventCopying = preventCopyingCheckBox.isSelected();

			dispose();
		});

		final JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		final JPanel contentPanel = new JPanel(new GridLayout(3, 1, 5, 5));
		contentPanel.add(filenameLabel);
		contentPanel.add(generateTOCCheckBox);
		contentPanel.add(preventCopyingCheckBox);
		mainPanel.add(contentPanel, BorderLayout.CENTER);

		final JPanel buttonBar = new JPanel(new GridBagLayout());
		buttonBar.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
		final GridBagLayout buttonBarLayout = (GridBagLayout)buttonBar.getLayout();
		buttonBarLayout.columnWidths = new int[]{0, 80, 0};
		buttonBarLayout.columnWeights = new double[]{1., 0., 1.};
		buttonBar.add(confirmButton, new GridBagConstraints(1, 0, 1, 1, 0., 0.,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		mainPanel.add(buttonBar, BorderLayout.SOUTH);
		confirmButton.setFocusPainted(false);

		getContentPane()
			.add(mainPanel, BorderLayout.CENTER);
		pack();

		setLocationRelativeTo(owner);
	}

	public boolean isGenerateTOC(){
		return generateTOC;
	}

	public boolean isPreventCopying(){
		return preventCopying;
	}


	public static void main(String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e){
			e.printStackTrace();
		}

		ConfigurationDialog dialog = new ConfigurationDialog("Config", null);
		dialog.setMinimumSize(new Dimension(170, 100));
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

}
