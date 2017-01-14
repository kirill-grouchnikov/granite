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
import org.pushingpixels.granite.GraniteUtils;
import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.Timeline.RepeatBehavior;
import org.pushingpixels.trident.Timeline.TimelineState;
import org.pushingpixels.trident.callback.UIThreadTimelineCallbackAdapter;
import org.pushingpixels.trident.swt.SWTRepaintCallback;

/**
 * Adds the following functionality to the album scroller container:
 * 
 * <ul>
 * <li>Overlaying an indefinite load progress indicator on top of the container
 * children</li>
 * <li>Changes in load progress indicator visibility are animated to fade in and
 * fade out</li>
 * </ul>
 * 
 * @author Kirill Grouchnikov
 */
public class Stage1LoadingProgress extends Stage0Base {
	/**
	 * The looping timeline to animate the indefinite load progress. When
	 * {@link #setLoading(boolean)} is called with <code>true</code>, this
	 * timeline is started. When {@link #setLoading(boolean)} is called with
	 * <code>false</code>, this timeline is cancelled at the end of the
	 * {@link #loadingBarFadeTimeline}.
	 */
	Timeline loadingBarLoopTimeline;

	/**
	 * The timeline for showing and hiding the loading progress bar. When
	 * {@link #setLoading(boolean)} is called with <code>true</code>, this
	 * timeline is started. When {@link #setLoading(boolean)} is called with
	 * <code>false</code>, this timeline is started in reverse.
	 */
	Timeline loadingBarFadeTimeline;

	/**
	 * The pixel width of the load progress visuals.
	 */
	static final int PROGRESS_WIDTH = 300;

	/**
	 * The pixel height of the load progress visuals.
	 */
	static final int PROGRESS_HEIGHT = 32;

	/**
	 * Progress indicator.
	 */
	protected ProgressBarIndicator progressIndicator;

	/**
	 * Animated indefinite progress bar indicator.
	 * 
	 * @author Kirill Grouchnikov
	 */
	protected static class ProgressBarIndicator extends Canvas {
		/**
		 * The current position of the {@link #loadingBarLoopTimeline}.
		 */
		float loadingBarLoopPosition;

		/**
		 * The current alpha value of the loading progress bar. Is updated by
		 * the {@link #loadingBarFadeTimeline}.
		 */
		int loadingBarAlpha;

		/**
		 * Creates a new progress bar indicator.
		 * 
		 * @param parent
		 *            Parent composite.
		 */
		public ProgressBarIndicator(Composite parent) {
			super(parent, SWT.TRANSPARENT | SWT.DOUBLE_BUFFERED
					| SWT.NO_BACKGROUND);
			this.loadingBarAlpha = 0;

			this.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent e) {
					if (loadingBarAlpha > 0) {
						int width = getBounds().width;
						int height = getBounds().height;

						GC gc = e.gc;
						gc.setAntialias(SWT.ON);
						gc.setAlpha(loadingBarAlpha);

						Region clipping = new Region(e.display);
						gc.getClipping(clipping);

						int contourRadius = 8;

						// create a round rectangle clip to paint the inner part
						// of the progress indicator
						Path clipPath = new GraniteUtils.RoundRectangle(
								e.display, 0, 0, width, height, contourRadius);

						gc.setClipping(clipPath);

						Color fill1 = new Color(e.display, 156, 208, 221);
						Color fill2 = new Color(e.display, 101, 183, 243);
						Pattern pFill1 = new Pattern(e.display, 0, 0, 0,
								height / 2.0f, fill1, loadingBarAlpha, fill2,
								loadingBarAlpha);
						gc.setBackgroundPattern(pFill1);
						gc.fillRectangle(0, 0, width, height / 2);
						fill1.dispose();
						fill2.dispose();
						pFill1.dispose();

						Color fill3 = new Color(e.display, 67, 169, 241);
						Color fill4 = new Color(e.display, 138, 201, 247);
						Pattern pFill2 = new Pattern(e.display, 0,
								height / 2.0f, 0, height, fill3,
								loadingBarAlpha, fill4, loadingBarAlpha);
						gc.setBackgroundPattern(pFill2);
						gc.fillRectangle(0, height / 2, width, height / 2);
						fill3.dispose();
						fill4.dispose();
						pFill2.dispose();

						int stripeCellWidth = 25;
						Color stripe1 = new Color(e.display, 36, 155, 239);
						Color stripe2 = new Color(e.display, 17, 145, 238);
						Pattern pStripe1 = new Pattern(e.display, 0, 0, 0,
								height / 2.0f, stripe1, loadingBarAlpha,
								stripe2, loadingBarAlpha);
						Color stripe3 = new Color(e.display, 15, 56, 200);
						Color stripe4 = new Color(e.display, 3, 133, 219);
						Pattern pStripe2 = new Pattern(e.display, 0, 0, 0,
								height / 2.0f, stripe3, loadingBarAlpha,
								stripe4, loadingBarAlpha);

						int stripeWidth = 10;
						gc.setLineAttributes(new LineAttributes(9.0f));
						for (int stripeX = (int) (loadingBarLoopPosition * stripeCellWidth); stripeX < width
								+ height; stripeX += stripeCellWidth) {
							gc.setBackgroundPattern(pStripe1);
							gc.fillPolygon(new int[] {
									stripeX - stripeWidth / 2,
									0,
									stripeX + stripeWidth / 2,
									0,
									stripeX - stripeCellWidth / 2 + stripeWidth
											/ 2,
									height / 2,
									stripeX - stripeCellWidth / 2 - stripeWidth
											/ 2, height / 2 });
							gc.setBackgroundPattern(pStripe2);
							gc
									.fillPolygon(new int[] {
											stripeX - stripeCellWidth / 2
													- stripeWidth / 2,
											height / 2,
											stripeX - stripeCellWidth / 2
													+ stripeWidth / 2,
											height / 2,
											stripeX - stripeCellWidth
													+ stripeWidth / 2,
											height,
											stripeX - stripeCellWidth
													- stripeWidth / 2, height });
						}
						stripe1.dispose();
						stripe2.dispose();
						stripe3.dispose();
						stripe4.dispose();
						pStripe1.dispose();
						pStripe2.dispose();

						// restore the original clipping to paint the contour
						gc.setClipping(clipping);
						clipping.dispose();

						gc.setForeground(e.display
								.getSystemColor(SWT.COLOR_GRAY));
						float lineWeight = 1.6f;
						gc.setLineAttributes(new LineAttributes(lineWeight));

						Path outline = new GraniteUtils.RoundRectangle(
								e.display, lineWeight / 2.0f - 1,
								lineWeight / 2.0f - 1, width - 1 - lineWeight
										+ 2, height - 1 - lineWeight + 2,
								contourRadius - lineWeight / 2);

						gc.drawPath(outline);

						outline.dispose();
					}
				}
			});
		}

		/**
		 * Sets the new alpha value of the loading progress bar. Is called by
		 * the {@link #loadingBarFadeTimeline}.
		 * 
		 * @param loadingBarAlpha
		 *            The new alpha value of the loading progress bar.
		 */
		public void setLoadingBarAlpha(int loadingBarAlpha) {
			this.loadingBarAlpha = loadingBarAlpha;
		}

		/**
		 * Sets the new loop position of the loading progress bar. Is called by
		 * the {@link #loadingBarLoopTimeline}.
		 * 
		 * @param loadingBarLoopPosition
		 *            The new loop position of the loading progress bar.
		 */
		public void setLoadingBarLoopPosition(float loadingBarLoopPosition) {
			this.loadingBarLoopPosition = loadingBarLoopPosition;
		}
	}

	/**
	 * Creates a container with support for showing load progress.
	 * 
	 * @param parent
	 *            Parent composite.
	 */
	public Stage1LoadingProgress(Composite parent) {
		super(parent);

		this.progressIndicator = new ProgressBarIndicator(this);
		this.setLayout(new Layout() {
			@Override
			protected void layout(Composite composite, boolean flushCache) {
				int w = composite.getBounds().width;
				int h = composite.getBounds().height;
				// put the progress indication in the center
				progressIndicator.setBounds((w - PROGRESS_WIDTH) / 2,
						(h - PROGRESS_HEIGHT) / 2, PROGRESS_WIDTH,
						PROGRESS_HEIGHT);
			}

			@Override
			protected Point computeSize(Composite composite, int wHint,
					int hHint, boolean flushCache) {
				return new Point(wHint, hHint);
			}
		});

		this.loadingBarLoopTimeline = new Timeline(this.progressIndicator);
		this.loadingBarLoopTimeline.addPropertyToInterpolate(
				"loadingBarLoopPosition", 0.0f, 1.0f);
		this.loadingBarLoopTimeline.addCallback(new SWTRepaintCallback(this));
		this.loadingBarLoopTimeline.setDuration(750);

		// create the fade timeline
		this.loadingBarFadeTimeline = new Timeline(this.progressIndicator);
		this.loadingBarFadeTimeline.addPropertyToInterpolate("loadingBarAlpha",
				0, 255);
		this.loadingBarFadeTimeline
				.addCallback(new UIThreadTimelineCallbackAdapter() {
					@Override
					public void onTimelineStateChanged(TimelineState oldState,
							TimelineState newState, float durationFraction,
							float timelinePosition) {
						if (oldState == TimelineState.PLAYING_REVERSE
								&& newState == TimelineState.DONE) {
							// after the loading progress is faded out, stop the
							// loading animation
							loadingBarLoopTimeline.cancel();
							if (!progressIndicator.isDisposed())
								progressIndicator.setVisible(false);
						}
					}
				});
		this.loadingBarFadeTimeline.setDuration(500);
	}

	/**
	 * Starts or stops the loading progress animation.
	 * 
	 * @param isLoading
	 *            if <code>true</code>, this container will display a loading
	 *            progress animation, if <code>false</code>, the loading
	 *            progress animation will be stopped.
	 */
	public void setLoading(boolean isLoading) {
		if (isLoading) {
			this.loadingBarFadeTimeline.play();
			this.loadingBarLoopTimeline.playLoop(RepeatBehavior.LOOP);
		} else {
			this.loadingBarFadeTimeline.playReverse();
		}
	}
}
