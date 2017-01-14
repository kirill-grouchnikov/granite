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

import java.awt.image.BufferedImage;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.pushingpixels.granite.GraniteUtils;

/**
 * Shows the big album art for Amazon album items.
 * 
 * @author Kirill Grouchnikov
 */
public class BigAlbumArt extends Canvas {
	/**
	 * The previously displayed album art. Is shown during the fade out stage,
	 * controlled by the timeline launched after the call to
	 * {@link #setAlbumArtImage(BufferedImage)}. The alpha value is controlled
	 * by {@link #oldImageAlpha}.
	 */
	private Image oldImage;

	/**
	 * The alpha value for {@link #oldImage}.
	 */
	private int oldImageAlpha;

	/**
	 * The album art image for the currently displayed Amazon album item.
	 */
	private Image image;

	/**
	 * The alpha value for {@link #image}.
	 */
	private int imageAlpha;

	/**
	 * The maximum dimension of the album art.
	 */
	public static final int ALBUM_ART_DIM = 220;

	/**
	 * Album art insets.
	 */
	public static final int INSETS = 3;

	/**
	 * The total dimension required to display album art and track listing side
	 * by side.
	 */
	public static final int TOTAL_DIM = ALBUM_ART_DIM + INSETS * 2;

	/**
	 * Creates a new component that shows album art.
	 * 
	 * @param parent
	 *            Parent composite.
	 */
	public BigAlbumArt(Composite parent) {
		super(parent, SWT.DOUBLE_BUFFERED);
		this.imageAlpha = 0;
		this.oldImageAlpha = 0;

		this.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				GC gc = e.gc;
				gc.setAntialias(SWT.ON);

				int w = getBounds().width;
				int h = getBounds().height;

				Color fill = new Color(e.display, 192, 192, 192);
				gc.setBackground(fill);
				// TODO: fill round rectangle once SWT supports per-pixel
				// translucency
				gc.fillRectangle(0, 0, w, h);
				fill.dispose();

				if (oldImageAlpha > 0) {
					gc.setAlpha(oldImageAlpha);
					if (oldImage != null) {
						// draw the original image
						gc.drawImage(oldImage, INSETS
								+ (w - oldImage.getImageData().width) / 2,
								INSETS
										+ (ALBUM_ART_DIM - oldImage
												.getImageData().height) / 2);
					}
					gc.setAlpha(255);
				}

				if (imageAlpha > 0) {
					gc.setAlpha(imageAlpha);
					if (image != null) {
						// draw the original image
						gc.drawImage(image, INSETS
								+ (w - image.getImageData().width) / 2, INSETS
								+ (ALBUM_ART_DIM - image.getImageData().height)
								/ 2);
					}
					gc.setAlpha(255);
				}

				gc.setForeground(e.display.getSystemColor(SWT.COLOR_WHITE));
				gc.setLineAttributes(new LineAttributes(2.0f));
				gc.drawRoundRectangle(1, 1, w - 2, h - 2, 3, 3);

				Color outer = new Color(e.display, 192, 192, 192);
				gc.setForeground(outer);
				gc.setLineAttributes(new LineAttributes(1.0f));
				gc.drawRoundRectangle(0, 0, w - 1, h - 1, 4, 4);
				outer.dispose();
			}
		});
	}

	/**
	 * Sets the specified album art for the display.
	 * 
	 * @param image
	 *            Album art.
	 */
	public void setAlbumArtImage(Image image) {
		this.oldImage = this.image;
		this.oldImageAlpha = this.imageAlpha;

		this.image = image;
		this.imageAlpha = 0;
		float vFactor = (float) ALBUM_ART_DIM
				/ (float) image.getImageData().height;
		float hFactor = (float) ALBUM_ART_DIM
				/ (float) image.getImageData().width;
		float factor = Math.min(1.0f, Math.min(vFactor, hFactor));
		if (factor < 1.0f) {
			// scaled to fit available area
			this.image = GraniteUtils.getScaledInstance(image,
					(int) (factor * image.getImageData().width),
					(int) (factor * image.getImageData().height));
		}
	}

	/**
	 * Sets the new alpha value for the displayed album art.
	 * 
	 * @param imageAlpha
	 *            The new alpha value for the displayed album art.
	 */
	public void setImageAlpha(int imageAlpha) {
		this.imageAlpha = imageAlpha;
	}

	/**
	 * Sets the new alpha value for the previously displayed album art.
	 * 
	 * @param oldImageAlpha
	 *            The new alpha value for the previously displayed album art.
	 */
	public void setOldImageAlpha(int oldImageAlpha) {
		this.oldImageAlpha = oldImageAlpha;
	}
}
