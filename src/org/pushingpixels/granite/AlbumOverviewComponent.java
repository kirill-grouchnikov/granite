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

import org.eclipse.core.runtime.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.pushingpixels.granite.data.Album;
import org.pushingpixels.trident.*;
import org.pushingpixels.trident.Timeline.RepeatBehavior;
import org.pushingpixels.trident.ease.Spline;
import org.pushingpixels.trident.swt.SWTRepaintCallback;

/**
 * Displays the overview information on the specific album.
 * 
 * @author Kirill Grouchnikov
 */
public class AlbumOverviewComponent extends Canvas {
	/**
	 * The dimensions of the overview image.
	 */
	public static final int OVERVIEW_IMAGE_DIM = 100;

	/**
	 * The original album art.
	 */
	private Image image;

	/**
	 * Indicates whether the image loading is done.
	 */
	private boolean imageLoadedDone;

	/**
	 * The alpha value of the image. Is updated in the fade-in timeline which
	 * starts after the image has been successfully loaded and scaled.
	 */
	private float imageAlpha;

	/**
	 * The alpha value of the border. Is updated in the fade-in timeline which
	 * starts when the mouse moves over this component.
	 */
	private float borderAlpha;

	/**
	 * The album caption.
	 */
	private String caption;

	/**
	 * The album price.
	 */
	private String releaseDate;

	/**
	 * The alpha value of this component. Is updated in the fade-in timeline
	 * which starts when this component becomes a part of the host window
	 * hierarchy.
	 */
	private int alpha;

	/**
	 * Component insets.
	 */
	private static final int INSETS = 7;

	/**
	 * Default width of this component.
	 */
	public static final int DEFAULT_WIDTH = 160;

	/**
	 * Default height of this component.
	 */
	public static final int DEFAULT_HEIGHT = 180;

	/**
	 * Creates a new component that shows overview informartion on the specified
	 * album.
	 * 
	 * @param parent
	 *            Parent composite.
	 * @param album
	 *            Information on an album.
	 */
	public AlbumOverviewComponent(Composite parent, final Album album) {
		super(parent, SWT.DOUBLE_BUFFERED | SWT.TRANSPARENT);
		this.caption = album.name;
		this.releaseDate = album.releaseDate;
		this.imageLoadedDone = false;
		this.imageAlpha = 0.0f;

		this.setCursor(new Cursor(Display.getDefault(), SWT.CURSOR_HAND));
		this.alpha = 0;

		final Timeline rolloverTimeline = new Timeline(this);
		rolloverTimeline.addPropertyToInterpolate("borderAlpha", 0.0f, 0.6f);
		rolloverTimeline.addCallback(new SWTRepaintCallback(
				AlbumOverviewComponent.this));
		rolloverTimeline.setEase(new Spline(0.7f));
		rolloverTimeline.setDuration(800);
		this.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseEnter(MouseEvent e) {
				rolloverTimeline.playLoop(RepeatBehavior.REVERSE);
			}

			@Override
			public void mouseExit(MouseEvent e) {
				rolloverTimeline.playReverse();
			}
		});

		this.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				if (borderAlpha > 0.0f)
					rolloverTimeline.playReverse();
			}
		});

		Timeline shownTimeline = new Timeline(AlbumOverviewComponent.this);
		shownTimeline.addPropertyToInterpolate("alpha", 0, 255);
		shownTimeline.addCallback(new SWTRepaintCallback(
				AlbumOverviewComponent.this));
		shownTimeline.setDuration(1000);
		shownTimeline.play();

		this.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				GC gc = e.gc;
				gc.setAlpha(alpha);
				gc.setAntialias(SWT.ON);

				Pattern pattern = new Pattern(e.display, 0, 0, 0,
						DEFAULT_HEIGHT, e.display
								.getSystemColor(SWT.COLOR_BLACK), 196,
						e.display.getSystemColor(SWT.COLOR_BLACK), 0);
				gc.setBackgroundPattern(pattern);
				gc.setForegroundPattern(pattern);
				gc.fillRoundRectangle(0, 0, DEFAULT_WIDTH - 1,
						DEFAULT_HEIGHT - 1, 18, 18);
				gc.drawRoundRectangle(0, 0, DEFAULT_WIDTH - 1,
						DEFAULT_HEIGHT - 1, 18, 18);
				pattern.dispose();

				if (borderAlpha > 0.0f) {
					// show the pulsating bluish outline of the rollover album
					Color borderColor = new Color(e.display, 64, 140, 255);
					Pattern borderPattern = new Pattern(e.display, 0, 0, 0,
							DEFAULT_HEIGHT, borderColor,
							(int) (196 * borderAlpha), borderColor, 0);
					LineAttributes currLineAttr = gc.getLineAttributes();
					gc.setLineAttributes(new LineAttributes(2.0f,
							SWT.CAP_ROUND, SWT.JOIN_ROUND));
					gc.setForegroundPattern(borderPattern);
					gc.drawRoundRectangle(1, 1, DEFAULT_WIDTH - 2,
							DEFAULT_HEIGHT - 2, 18, 18);
					gc.setLineAttributes(currLineAttr);
					borderPattern.dispose();
					borderColor.dispose();
				}

				if (imageLoadedDone) {
					gc.setAlpha((int) (alpha * imageAlpha));
					// draw the album art image
					gc.drawImage(image, (getBounds().width - image
							.getImageData().width) / 2,
							INSETS
									+ (OVERVIEW_IMAGE_DIM - image
											.getImageData().height) / 2);
					gc.setAlpha(alpha);
				}

				FontData fontData = gc.getDevice().getSystemFont()
						.getFontData()[0];
				gc.setFont(new Font(gc.getDevice(), fontData.getName(), 9,
						SWT.NORMAL));

				FontMetrics fontMetrics = gc.getFontMetrics();
				int textY = INSETS + OVERVIEW_IMAGE_DIM
						+ fontMetrics.getDescent();
				int textX = INSETS;
				int textWidth = DEFAULT_WIDTH - INSETS - textX;

				gc
						.setForeground(gc.getDevice().getSystemColor(
								SWT.COLOR_WHITE));
				GraniteUtils.paintMultilineText(AlbumOverviewComponent.this,
						gc, caption, textX, textWidth, textY, 2);

				gc.setForeground(new Color(gc.getDevice(), 64, 140, 255));
				GraniteUtils.paintMultilineText(AlbumOverviewComponent.this,
						gc, releaseDate, textX, textWidth, textY + 2
								* fontMetrics.getHeight(), 1);
			}
		});

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				getLoadImageScenario(album).play();
			}
		});
	}

	/**
	 * Returns the timeline scenario that loads, scaled and fades in the
	 * associated album art.
	 * 
	 * @param albumItem
	 *            Album item.
	 * @return The timeline scenario that loads, scaled and fades in the
	 *         associated album art.
	 */
	private TimelineScenario getLoadImageScenario(final Album albumItem) {
		TimelineScenario loadScenario = new TimelineScenario.Sequence();

		// load the image
		EclipseJobTimelineScenarioActor imageLoadWorker = new EclipseJobTimelineScenarioActor(
				"Load image") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					image = new Image(Display.getDefault(), BackendConnector
							.getAlbumArt(albumItem.asin));
					return Status.OK_STATUS;
				} catch (Throwable t) {
					t.printStackTrace();
					return Status.CANCEL_STATUS;
				}
			}
		};
		loadScenario.addScenarioActor(imageLoadWorker);

		// scale if necessary
		TimelineRunnable scaler = new TimelineRunnable() {
			@Override
			public void run() {
				if (image != null) {
					float vFactor = (float) OVERVIEW_IMAGE_DIM
							/ (float) image.getImageData().height;
					float hFactor = (float) OVERVIEW_IMAGE_DIM
							/ (float) image.getImageData().width;
					float factor = Math.min(1.0f, Math.min(vFactor, hFactor));
					if (factor < 1.0f) {
						// scaled to fit available area
						image = GraniteUtils.getScaledInstance(image,
								(int) (factor * image.getImageData().width),
								(int) (factor * image.getImageData().height));
					}

					imageLoadedDone = true;
				}
			}
		};
		loadScenario.addScenarioActor(scaler);

		// and fade it in
		Timeline imageFadeInTimeline = new Timeline(AlbumOverviewComponent.this);
		imageFadeInTimeline.addPropertyToInterpolate("imageAlpha", 0.0f, 1.0f);
		imageFadeInTimeline.addCallback(new SWTRepaintCallback(
				AlbumOverviewComponent.this));
		imageFadeInTimeline.setDuration(500);
		loadScenario.addScenarioActor(imageFadeInTimeline);

		return loadScenario;
	}

	/**
	 * Sets the alpha value for the image. Used by the image fade-in timeline.
	 * 
	 * @param imageAlpha
	 *            Alpha value for the image.
	 */
	public void setImageAlpha(float imageAlpha) {
		this.imageAlpha = imageAlpha;
	}

	/**
	 * Sets the alpha value for the border. Used by the rollover timeline.
	 * 
	 * @param borderAlpha
	 *            Alpha value for the border.
	 */
	public void setBorderAlpha(float borderAlpha) {
		this.borderAlpha = borderAlpha;
	}

	/**
	 * Sets the alpha value. Used by the fade-in timeline.
	 * 
	 * @param alpha
	 *            Alpha value for this component.
	 */
	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}
}
