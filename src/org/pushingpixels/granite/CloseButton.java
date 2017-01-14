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
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.pushingpixels.granite.details.DetailsWindowManager;
import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.swt.SWTRepaintCallback;

/**
 * The close button of the Granite demo.
 * 
 * @author Kirill Grouchnikov
 */
public class CloseButton extends Canvas {
	/**
	 * The alpha value of this button. Is updated in the fade-in timeline which
	 * starts when this button becomes a part of the host window hierarchy.
	 */
	int alpha;

	/**
	 * Creates a new close button.
	 * 
	 * @param parent
	 *            Parent composite.
	 */
	public CloseButton(Composite parent) {
		super(parent, SWT.TRANSPARENT);
		this.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		this.alpha = 0;

		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				DetailsWindowManager.disposeCurrentlyShowing();

				// fade out the main shell and dispose it when it is
				// at full transparency
				GraniteUtils.fadeOutAndDispose(getShell(), 500);
			}
		});

		// timeline for the rollover effect (interpolating the
		// button's foreground color)
		final Timeline rolloverTimeline = new Timeline(this);
		rolloverTimeline.addPropertyToInterpolate("foreground", parent
				.getDisplay().getSystemColor(SWT.COLOR_WHITE), new Color(parent
				.getDisplay(), 64, 140, 255));
		rolloverTimeline.setDuration(200);

		// and register a mouse listener to play the rollover
		// timeline
		this.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseEnter(MouseEvent e) {
				rolloverTimeline.play();
			}

			@Override
			public void mouseExit(MouseEvent e) {
				rolloverTimeline.playReverse();
			}
		});

		// fade in the component
		Timeline shownTimeline = new Timeline(CloseButton.this);
		shownTimeline.addPropertyToInterpolate("alpha", 0, 255);
		shownTimeline.addCallback(new SWTRepaintCallback(CloseButton.this));
		shownTimeline.setDuration(500);
		shownTimeline.play();

		this.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				GC gc = e.gc;
				gc.setAntialias(SWT.ON);

				// use the current alpha
				gc.setAlpha(alpha);

				int width = getBounds().width;
				int height = getBounds().height;

				// paint the background - black fill and a dark outline
				// based on the current foreground color
				gc
						.setBackground(gc.getDevice().getSystemColor(
								SWT.COLOR_BLACK));
				gc.fillOval(1, 1, width - 3, height - 3);
				gc.setLineAttributes(new LineAttributes(2.0f));

				Color currFg = getForeground();
				int currR = currFg.getRed();
				int currG = currFg.getGreen();
				int currB = currFg.getBlue();

				Color darkerFg = new Color(gc.getDevice(), currR / 2,
						currG / 2, currB / 2);
				gc.setForeground(darkerFg);
				gc.drawOval(1, 1, width - 3, height - 3);
				darkerFg.dispose();

				// paint the outer cross (always white)
				gc
						.setForeground(gc.getDevice().getSystemColor(
								SWT.COLOR_WHITE));
				gc.setLineAttributes(new LineAttributes(6.0f, SWT.CAP_ROUND,
						SWT.JOIN_ROUND));
				int offset = width / 3;
				gc.drawLine(offset, offset, width - offset - 1, height - offset
						- 1);
				gc.drawLine(width - offset - 1, offset, offset, height - offset
						- 1);

				// paint the inner cross (using the current foreground color)
				gc.setForeground(currFg);
				gc.setLineAttributes(new LineAttributes(4.2f, SWT.CAP_ROUND,
						SWT.JOIN_ROUND));
				gc.drawLine(offset, offset, width - offset - 1, height - offset
						- 1);
				gc.drawLine(width - offset - 1, offset, offset, height - offset
						- 1);
			}
		});
	}

	/**
	 * Sets the alpha value. Used by the fade-in timeline.
	 * 
	 * @param alpha
	 *            Alpha value for this button.
	 */
	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}
}
