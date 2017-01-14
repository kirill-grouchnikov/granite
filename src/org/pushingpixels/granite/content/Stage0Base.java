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
package org.pushingpixels.granite.content;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.swt.SWTRepaintCallback;

/**
 * The basis of the album scroller container. Provides the functionality of:
 * 
 * <ul>
 * <li>Fading in once it becomes a part of the host window hierarchy</li>
 * <li>Dragging the host window with the mouse</li>
 * <li>Painting a rounded translucent background</li>
 * </ul>
 * 
 * @author Kirill Grouchnikov
 */
public class Stage0Base extends Canvas {
	/**
	 * The alpha value for this container. Is updated in the fade-in timeline
	 * which starts when this container becomes a part of the host window
	 * hierarchy.
	 */
	int alpha;

	/**
	 * Creates the basic container.
	 * 
	 * @param parent
	 *            Parent composite.
	 */
	public Stage0Base(final Composite parent) {
		super(parent, SWT.DOUBLE_BUFFERED);
		this.alpha = 0;

		Listener l = new Listener() {
			Point origin;

			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.MouseDown:
					origin = new Point(e.x, e.y);
					break;
				case SWT.MouseUp:
					origin = null;
					break;
				case SWT.MouseMove:
					if (origin != null) {
						Shell shell = parent.getShell();
						Point p = shell.getDisplay().map(shell, null, e.x, e.y);
						shell.setLocation(p.x - origin.x, p.y - origin.y);
					}
					break;
				}
			}
		};
		this.addListener(SWT.MouseDown, l);
		this.addListener(SWT.MouseUp, l);
		this.addListener(SWT.MouseMove, l);

		// fade in the container
		Timeline shownTimeline = new Timeline(Stage0Base.this);
		shownTimeline.addPropertyToInterpolate("alpha", 0, 255);
		shownTimeline.addCallback(new SWTRepaintCallback(Stage0Base.this));
		shownTimeline.setDuration(500);
		shownTimeline.play();

		this.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				GC gc = e.gc;

				int width = getBounds().width;
				int height = getBounds().height;

				// TODO: respect alpha once SWT supports per-pixel translucency
				// on the shell level
				gc.setAlpha(255);
				gc.setAntialias(SWT.OFF);

				gc.setBackground(new Color(gc.getDevice(), 40, 40, 40));
				// TODO: use round rectangle for soft corners
				gc.fillRectangle(0, 0, width, height);
			}
		});
	}

	/**
	 * Sets the alpha value. Used by the fade-in timeline.
	 * 
	 * @param alpha
	 *            Alpha value for this container.
	 */
	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}
}
