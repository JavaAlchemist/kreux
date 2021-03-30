package at.diwh.kreux.tools;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class SystemOutToSwing extends OutputStream {
	private JTextArea textArea;

	public SystemOutToSwing(JTextArea textArea) throws IOException {
		this.textArea = textArea;
	}

	@Override
	public void write(int i) throws IOException {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				SystemOutToSwing.this.textArea.setText(SystemOutToSwing.this.textArea.getText() + String.valueOf((char) i));
			}
		});
	}

}
