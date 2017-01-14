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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.pushingpixels.granite.AlbumOverviewComponent;
import org.pushingpixels.granite.data.Album;
import org.pushingpixels.granite.details.DetailsWindowManager;

/**
 * Adds the following functionality to the album scroller container:
 * 
 * <ul>
 * <li>Adding album overview components</li>
 * <li>Scrolling overview components with mouse wheel and left / right arrow
 * keys</li>
 * </ul>
 * 
 * @author Kirill Grouchnikov
 */
public class Stage2Components extends Stage1LoadingProgress {
	/**
	 * The list of album overview components. Each component added with
	 * {@link #addOverviewComp(Item, ActivationCallback)} is added to this list.
	 */
	List<AlbumOverviewComponent> comps;

	/**
	 * Indicates which album overview component is displayed at the left edge of
	 * this container. Note that while this specific class (in its
	 * {@link #scrollToNext()} and {@link #scrollToPrevious()}) operate on the
	 * integer values, the animated scrolling will result in fractional values
	 * of the leading position.
	 * 
	 * <p>
	 * At the beginning the value is 0.0 - displaying the first entry in
	 * {@link #comps} at the left edge. When scrolling to the next album, the
	 * value will become 1.0 (effectively pushing the first album over the left
	 * edge). If the scrolling is animated, this value will be gradually
	 * interpolated from 0.0 to 1.0.
	 * </p>
	 * 
	 * <p>
	 * This value is respected in the {@link #doLayout()} to provide the
	 * seamless scroll animation.
	 * </p>
	 */
	float leadingPosition;

	/**
	 * Hosts the album components.
	 */
	Composite albumComponentsHolder;

	/**
	 * Creates the new container that can host album overview components.
	 * 
	 * @param parent
	 *            Parent composite.
	 */
	public Stage2Components(Composite parent) {
		super(parent);
		this.comps = new ArrayList<AlbumOverviewComponent>();

		this.albumComponentsHolder = new Composite(this, SWT.TRANSPARENT);
		this.progressIndicator.moveAbove(this.albumComponentsHolder);
		this.setLayout(new Layout() {
			@Override
			protected void layout(Composite composite, boolean flushCache) {
				int w = composite.getBounds().width;
				int h = composite.getBounds().height;

				// set 10 pixel margin to clip the album art components
				albumComponentsHolder.setBounds(10, 10, w - 20, h - 20);

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

		// register the mouse wheel listener for scrolling content
		parent.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				if (e.count < 0) {
					// next
					scrollToNext();
				} else {
					// previous
					scrollToPrevious();
				}
			}
		});

		// use arrow keys for scrolling
		parent.getDisplay().addFilter(SWT.KeyUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				// only scroll when the details window is not showing
				if (event.keyCode == SWT.ARROW_RIGHT) {
					scrollToNext();
				}
				if (event.keyCode == SWT.ARROW_LEFT) {
					scrollToPrevious();
				}
			}
		});

		// add a mouse listener to dispose the album details window
		// when the user clicks outside any album overview component.
		this.addMouseListener(new org.eclipse.swt.events.MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				DetailsWindowManager.disposeCurrentlyShowing();
			}
		});

		this.albumComponentsHolder.setLayout(new Layout() {
			@Override
			protected Point computeSize(Composite composite, int wHint,
					int hHint, boolean flushCache) {
				return new Point(wHint, hHint);
			}

			@Override
			protected void layout(Composite composite, boolean flushCache) {
				if (comps.size() == 0)
					return;

				for (int i = 0; i < comps.size(); i++) {
					float delta = i - leadingPosition;
					// compute the left X based on the current leading position
					int x = (int) (delta * (AlbumOverviewComponent.DEFAULT_WIDTH + 10));
					comps
							.get(i)
							.setBounds(
									x,
									(getBounds().height - AlbumOverviewComponent.DEFAULT_HEIGHT) / 2,
									AlbumOverviewComponent.DEFAULT_WIDTH,
									AlbumOverviewComponent.DEFAULT_HEIGHT);
				}
			}
		});
	}

	/**
	 * Adds the specified album item to this album container.
	 * 
	 * @param albumItem
	 *            Description of the album item from the Amazon backend.
	 * @return Thew matching album overview component.
	 */
	public synchronized AlbumOverviewComponent addAlbumItem(Album albumItem) {
		AlbumOverviewComponent comp = new AlbumOverviewComponent(
				this.albumComponentsHolder, albumItem);
		this.comps.add(comp);
		this.albumComponentsHolder.layout(new Control[] { comp });
		return comp;
	}

	/**
	 * Scrolls the albums to show the next album.
	 */
	protected void scrollToNext() {
		if (this.leadingPosition < (this.comps.size() - 1)) {
			this.leadingPosition++;
			this.albumComponentsHolder.layout(true);
		}
	}

	/**
	 * Scrolls the albums to show the previous album.
	 */
	protected void scrollToPrevious() {
		if (this.leadingPosition > 0) {
			this.leadingPosition--;
			this.albumComponentsHolder.layout(true);
		}
	}
}
