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

import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.pushingpixels.granite.GraniteUtils;
import org.pushingpixels.granite.data.Album;
import org.pushingpixels.granite.data.Track;
import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.Timeline.RepeatBehavior;
import org.pushingpixels.trident.swt.SWTRepaintCallback;

/**
 * Component for showing track listing of a single album item from Amazon.
 * 
 * @author Kirill Grouchnikov
 */
public class TrackListing extends Canvas {
	/**
	 * The album item.
	 */
	private Album album;

	/**
	 * The album performer.
	 */
	private String artist;

	/**
	 * The title of {@link #albumItem}.
	 */
	private String albumTitle;

	/**
	 * The release date of {@link #albumItem}.
	 */
	private String released;

	/**
	 * List of the {@link #albumItem} discs.
	 */
	private List<Track> tracks;

	private Font keyFont;

	private Font detailsFont;

	private Timeline scrollerTimeline;

	private int viewportTop;

	/**
	 * Information on a single disc.
	 * 
	 * @author Kirill Grouchnikov
	 */
	private static class DiscInfo {
		/**
		 * Disc caption.
		 */
		private String caption;

		/**
		 * Disc tracks.
		 */
		private List<String> tracks = new ArrayList<String>();
	}

	/**
	 * Creates a new component that shows a list of all album tracks.
	 * 
	 * @param parent
	 *            Parent composite.
	 */
	public TrackListing(Composite parent) {
		super(parent, SWT.DOUBLE_BUFFERED);
		this.viewportTop = 0;
		// this.setBorder(new EmptyBorder(6, 6, 6, 6));

		this.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				paint(e);
			}
		});

		this.setBackground(new Color(parent.getDisplay(), 32, 32, 32));
		FontData fontData = parent.getDisplay().getSystemFont().getFontData()[0];
		this.keyFont = new Font(parent.getDisplay(), fontData.getName(),
				fontData.getHeight() + 4, SWT.BOLD);
		this.detailsFont = new Font(parent.getDisplay(), fontData.getName(),
				fontData.getHeight() + 2, SWT.BOLD);

		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				suspendScrolling();
			}

			@Override
			public void mouseUp(MouseEvent e) {
				int width = getBounds().width;
				int height = getBounds().height;
				if ((e.x >= 0) && (e.x <= width) && (e.y >= 0)
						&& (e.y <= height)) {
					// resume scrolling only if mouse up event is inside
					// the component
					resumeScrolling();
				}
			}
		});

		this.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseEnter(MouseEvent e) {
				if (scrollerTimeline != null) {
					Timeline.TimelineState state = scrollerTimeline.getState();
					if (state == Timeline.TimelineState.IDLE) {
						scrollerTimeline.playLoop(RepeatBehavior.REVERSE);
					} else {
						resumeScrolling();
					}
				}
			}

			@Override
			public void mouseExit(MouseEvent e) {
				suspendScrolling();
			}
		});
	}

	@Override
	public void dispose() {
		this.keyFont.dispose();
		this.detailsFont.dispose();
		super.dispose();
	}

	private void suspendScrolling() {
		if (this.scrollerTimeline != null) {
			Timeline.TimelineState state = this.scrollerTimeline.getState();
			if ((state != Timeline.TimelineState.CANCELLED)
					&& (state != Timeline.TimelineState.SUSPENDED)) {
				this.scrollerTimeline.suspend();
			}
		}
	}

	private void resumeScrolling() {
		if (this.scrollerTimeline != null) {
			Timeline.TimelineState state = this.scrollerTimeline.getState();
			if (state == Timeline.TimelineState.SUSPENDED) {
				this.scrollerTimeline.resume();
			}
		}
	}

	private void abortScrolling() {
		if (this.scrollerTimeline != null) {
			this.scrollerTimeline.abort();
		}
	}

	/**
	 * Sets the specified album item for the track display.
	 * 
	 * @param album
	 *            Album item.
	 */
	public void setAlbumItem(Album album, List<Track> tracks) {
		this.abortScrolling();
		this.scrollerTimeline = null;

		setViewportTop(0);

		this.album = album;
		this.artist = album.artist;
		this.albumTitle = "\"" + this.album.name + "\"";
		this.released = "Released " + this.album.releaseDate;

		this.tracks = Collections.unmodifiableList(tracks);

		this.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				getParent().layout(new Control[] { TrackListing.this });

				int requiredHeight = getRequiredHeight();
				if (requiredHeight > BigAlbumArt.ALBUM_ART_DIM) {
					scrollerTimeline = new Timeline(TrackListing.this);
					scrollerTimeline.addPropertyToInterpolate("viewportTop", 0,
							requiredHeight - BigAlbumArt.ALBUM_ART_DIM);
					// set the duration of the scroller timeline based on how
					// much needs to be scrolled
					scrollerTimeline
							.setDuration((requiredHeight - BigAlbumArt.ALBUM_ART_DIM) * 20);
					// and set the cycle delay to pause the scrolling when it
					// reaches one of the ends
					scrollerTimeline.setCycleDelay(1000);
					scrollerTimeline.addCallback(new SWTRepaintCallback(
							TrackListing.this));
				}
			}
		});
	}

	private int getRequiredHeight() {
		if (this.album == null) {
			return 0;
		}

		GC gc = new GC(this);
		int insets = 4;

		int width = BigAlbumArt.ALBUM_ART_DIM - 2 * insets;

		int height = 0;
		if (this.album != null) {
			gc.setFont(this.keyFont);
			int keyFontHeight = gc.getFontMetrics().getHeight();
			height = insets + gc.getFontMetrics().getAscent() / 2;

			height += GraniteUtils.getMultilineTextHeight(this, gc,
					this.artist, width);
			height += keyFontHeight / 3;
			height += GraniteUtils.getMultilineTextHeight(this, gc,
					this.albumTitle, width);
			height += keyFontHeight / 3;
			height += GraniteUtils.getMultilineTextHeight(this, gc,
					this.released, width);
			height += keyFontHeight / 3;

			// tracks
			gc.setFont(this.detailsFont);
			int detailsFontHeight = gc.getFontMetrics().getHeight();
			height += detailsFontHeight / 2;
			for (Track track : this.tracks) {
				height += GraniteUtils.getMultilineTextHeight(this, gc,
						track.title, width);
				height += detailsFontHeight / 3;
			}
		}

		gc.dispose();
		return height;
	}

	void paint(PaintEvent e) {
		GC gc = e.gc;
		gc.setAntialias(SWT.ON);

		Transform currTransform = new Transform(e.display);
		gc.getTransform(currTransform);
		currTransform.translate(0, -this.viewportTop);
		gc.setTransform(currTransform);

		int w = getBounds().width;
		gc.setFont(this.keyFont);

		int keyFontHeight = gc.getFontMetrics().getHeight();

		int insets = 4;
		int width = w - 2 * insets;
		if (this.album != null) {
			int height = insets + gc.getFontMetrics().getAscent()
					+ gc.getFontMetrics().getDescent();
			// performers
			height += GraniteUtils.getMultilineTextHeight(this, gc,
					this.artist, width);
			height += keyFontHeight / 3;
			// title
			height += GraniteUtils.getMultilineTextHeight(this, gc,
					this.albumTitle, width);
			height += keyFontHeight / 3;
			// release date
			height += GraniteUtils.getMultilineTextHeight(this, gc,
					this.released, width);
			height += keyFontHeight / 3;

			gc.setBackground(e.display.getSystemColor(SWT.COLOR_BLACK));
			gc.fillRectangle(-4, 0, w + 1, height - 2);
		}

		if (this.album != null) {
			int x = insets;

			gc.setFont(this.keyFont);
			gc.setForeground(e.display.getSystemColor(SWT.COLOR_WHITE));
			int y = insets + gc.getFontMetrics().getAscent() / 2;

			y = GraniteUtils.paintMultilineText(this, gc, this.artist, x,
					width, y, -1);
			y += keyFontHeight / 3;
			y = GraniteUtils.paintMultilineText(this, gc, this.albumTitle, x,
					width, y, -1);
			y += keyFontHeight / 3;
			y = GraniteUtils.paintMultilineText(this, gc, this.released, x,
					width, y, -1);
			y += keyFontHeight / 3;

			// tracks
			gc.setFont(this.detailsFont);
			int detailsFontHeight = gc.getFontMetrics().getHeight();
			y += detailsFontHeight / 2;
			Color gray35 = new Color(e.display, 35, 35, 35);
			Color gray44 = new Color(e.display, 44, 44, 44);
			Color gray192 = new Color(e.display, 192, 192, 192);
			for (Track track : this.tracks) {
				gc.setForeground(gray44);
				gc.drawLine(x + 5, y, width - 10, y);
				gc.setForeground(gray35);
				gc.drawLine(x + 5, y + 1, width - 10, y + 1);

				gc.setForeground(gray192);
				y = GraniteUtils.paintMultilineText(this, gc, track.title, x,
						width, y, -1);
				y += detailsFontHeight / 3;
			}
			gray35.dispose();
			gray44.dispose();
			gray192.dispose();
		}

		currTransform.dispose();
	}

	public void setViewportTop(int viewportTop) {
		this.viewportTop = viewportTop;
	}
}
