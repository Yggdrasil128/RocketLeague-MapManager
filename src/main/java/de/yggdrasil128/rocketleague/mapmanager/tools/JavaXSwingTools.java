package de.yggdrasil128.rocketleague.mapmanager.tools;

import javax.swing.*;

public abstract class JavaXSwingTools {
	public static JFrame makeModalFrame() {
		JFrame jFrame = new JFrame();
		jFrame.setAlwaysOnTop(true);
		jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		jFrame.setLocationRelativeTo(null);
		jFrame.requestFocus();
		return jFrame;
	}
}
