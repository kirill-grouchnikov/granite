/*
 * Copyright (c) 2009 Onyx Kirill Grouchnikov. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *     
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *     
 *  o Neither the name of Onyx Kirill Grouchnikov nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */
package org.pushingpixels.granite.details;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.pushingpixels.granite.GraniteUtils;
import org.pushingpixels.granite.data.Album;
import org.pushingpixels.trident.Timeline;

/**
 * Utility manager for handling the functionality related to the details window.
 * 
 * @author Kirill Grouchnikov
 */
public class DetailsWindowManager {
	/**
	 * The currently shown details window.
	 */
	static Shell currentlyShownWindow;

	static DetailsContentPanel currentlyShownContentPanel;

	/**
	 * Disposes the currently shown details window.
	 */
	public static void disposeCurrentlyShowing() {
		if (showsDetailsWindow()) {
			// fade out the main shell and dispose it when it is
			// at full transparency
			GraniteUtils.fadeOutAndDispose(currentlyShownWindow, 500);
		}
	}

	public static boolean showsDetailsWindow() {
		return (currentlyShownWindow != null)
				&& !currentlyShownWindow.isDisposed()
				&& currentlyShownWindow.isVisible();
	}

	/**
	 * Shows the details for the specified album item.
	 * 
	 * @param mainWindow
	 *            The main application window.
	 * @param albumItem
	 *            Album item details from Amazon.
	 */
	public static void show(Shell mainWindow, Album albumItem) {
		if (showsDetailsWindow()) {
			currentlyShownContentPanel.setAlbumItem(albumItem);
			return;
		}

		currentlyShownWindow = new Shell(mainWindow, SWT.NO_TRIM
				| SWT.DOUBLE_BUFFERED);
		currentlyShownWindow.setLayout(new FillLayout());
		// place the details window centered along the bottom edge of the
		// main application window
		Point mainWindowLoc = mainWindow.getLocation();
		Point mainWindowDim = mainWindow.getSize();
		int x = mainWindowLoc.x + mainWindowDim.x / 2 - BigAlbumArt.TOTAL_DIM;
		int y = mainWindowLoc.y + mainWindowDim.y - BigAlbumArt.TOTAL_DIM / 2;
		currentlyShownWindow.setSize(2 * BigAlbumArt.TOTAL_DIM,
				BigAlbumArt.TOTAL_DIM);
		Region region = new Region();
		region.add(BigAlbumArt.TOTAL_DIM / 2, 0, BigAlbumArt.TOTAL_DIM,
				BigAlbumArt.TOTAL_DIM);
		currentlyShownWindow.setRegion(region);
		region.dispose();
		currentlyShownWindow.setLocation(x, y);

		currentlyShownWindow.setAlpha(0);
		currentlyShownWindow.setVisible(true);
		currentlyShownContentPanel = new DetailsContentPanel(
				currentlyShownWindow);
		currentlyShownContentPanel.setAlbumItem(albumItem);
		currentlyShownWindow
				.layout(new Control[] { currentlyShownContentPanel });

		Timeline showWindow = new Timeline(currentlyShownWindow);
		showWindow.addPropertyToInterpolate("alpha", 0, 255);
		showWindow.setDuration(500);
		showWindow.play();
	}
}
