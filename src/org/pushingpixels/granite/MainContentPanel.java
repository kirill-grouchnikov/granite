/*
 * Copyright (c) 2009-2010 Granite Kirill Grouchnikov. All Rights Reserved.
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
 *  o Neither the name of Granite Kirill Grouchnikov nor the names of 
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
package org.pushingpixels.granite;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.pushingpixels.granite.content.AlbumOverviewPanel;
import org.pushingpixels.granite.data.Album;

public class MainContentPanel extends Composite {
	AlbumOverviewPanel contentPanel;

	CloseButton closeButton;

	public MainContentPanel(Shell shell) {
		super(shell, SWT.NONE);

		this.contentPanel = new AlbumOverviewPanel(this);
		this.closeButton = new CloseButton(this);
		// move the close button to be painted on top of the content panel
		this.closeButton.moveAbove(this.contentPanel);

		this.setLayout(new Layout() {
			@Override
			protected void layout(Composite composite, boolean flushCache) {
				int width = composite.getBounds().width;
				int height = composite.getBounds().height;
				contentPanel.setBounds(0, 0, width, height);
				int closeButtonDim = 35;
				closeButton.setBounds(width - closeButtonDim, 0,
						closeButtonDim, closeButtonDim);
			}

			@Override
			protected Point computeSize(Composite composite, int wHint,
					int hHint, boolean flushCache) {
				return new Point(wHint, hHint);
			}
		});
	}

	public void setLoading(boolean isLoading) {
		contentPanel.setLoading(isLoading);
	}

	public AlbumOverviewComponent addAlbumItem(Album albumItem) {
		return contentPanel.addAlbumItem(albumItem);
	}
}
