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

import java.util.ArrayList;

import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.pushingpixels.granite.BackendConnector;
import org.pushingpixels.granite.EclipseJobTimelineScenarioActor;
import org.pushingpixels.granite.data.Album;
import org.pushingpixels.granite.data.Track;
import org.pushingpixels.trident.*;
import org.pushingpixels.trident.Timeline.TimelineState;
import org.pushingpixels.trident.callback.UIThreadTimelineCallbackAdapter;
import org.pushingpixels.trident.swing.TimelineSwingWorker;
import org.pushingpixels.trident.swt.SWTRepaintCallback;

/**
 * Shows the details of the selected album, including bigger album art and a
 * scrollable list of album tracks.
 * 
 * @author Kirill Grouchnikov
 */
public class DetailsContentPanel extends Composite {
	/**
	 * Component that shows the album art.
	 */
	private BigAlbumArt albumArt;

	/**
	 * Component that shows the scrollable list of album tracks.
	 */
	private TrackListing trackListing;

	/**
	 * The scenario that is transitioning to the last selected album item.
	 */
	private TimelineScenario currentShowAlbumDetailsScenario;

	/**
	 * 0.0f - the album art and track listing are completely overlayed, 1.0f -
	 * the album art and track listing are completely separate. Is updated in
	 * the {@link #currentShowAlbumDetailsScenario}.
	 */
	private float overlayPosition;

	/**
	 * Creates a new details window.
	 * 
	 * @param shell
	 *            The shell for this window.
	 */
	public DetailsContentPanel(Shell shell) {
		super(shell, SWT.DOUBLE_BUFFERED);

		this.overlayPosition = 0.0f;

		this.albumArt = new BigAlbumArt(this);
		this.trackListing = new TrackListing(this);

		this.albumArt.moveAbove(this.trackListing);

		this.setLayout(new Layout() {
			@Override
			protected void layout(Composite composite, boolean flushCache) {
				int w = composite.getBounds().width;
				int h = composite.getBounds().height;

				// respect the current overlay position to implement the sliding
				// effect in steps 1 and 6 of currentShowAlbumDetailsScenario
				int dim = BigAlbumArt.TOTAL_DIM;
				int dx = (int) (overlayPosition * dim / 2);
				albumArt.setBounds((w - dim) / 2 - dx, (h - dim) / 2, dim, dim);
				trackListing.setBounds((w - dim) / 2 + dx, (h - dim) / 2, dim,
						dim);
			}

			@Override
			protected Point computeSize(Composite composite, int wHint,
					int hHint, boolean flushCache) {
				return new Point(wHint, hHint);
			}
		});
	}

	/**
	 * Signals that details of the specified album item should be displayed in
	 * this window. Note that this window can already display another album item
	 * when this method is called.
	 * 
	 * @param albumItem
	 *            New album item to show in this window.
	 */
	public void setAlbumItem(Album albumItem) {
		if (this.currentShowAlbumDetailsScenario != null)
			this.currentShowAlbumDetailsScenario.cancel();

		this.currentShowAlbumDetailsScenario = this
				.getShowAlbumDetailsScenario(albumItem);
		this.currentShowAlbumDetailsScenario.play();
	}

	/**
	 * Sets the new overlay position of the album art and track listing. This
	 * method will also cause revalidation of the main window content pane.
	 * 
	 * @param overlayPosition
	 *            The new overlay position of the album art and track listing.
	 */
	public void setOverlayPosition(float overlayPosition) {
		this.overlayPosition = overlayPosition;
		this.layout(new Control[] { albumArt, trackListing });
	}

	/**
	 * Returns the timeline scenario that implements a transition from the
	 * currently shown album item (which may be <code>null</code>) to the
	 * specified album item.
	 * 
	 * @param album
	 *            The new album item to be shown in this window.
	 * @return The timeline scenario that implements a transition from the
	 *         currently shown album item (which may be <code>null</code>) to
	 *         the specified album item.
	 */
	private TimelineScenario getShowAlbumDetailsScenario(final Album album) {
		// final Point mainWindowLoc = mainWindow.getLocation();
		// final Point mainWindowDim = mainWindow.getSize();
		// final Point currDetailsShellDim = getShell().getSize();

		TimelineScenario.RendezvousSequence scenario = new TimelineScenario.RendezvousSequence();

		// step 1 - move album art and track listing to the same location
		Timeline collapseArtAndTracks = new Timeline(this);
		collapseArtAndTracks.addPropertyToInterpolate("overlayPosition",
				this.overlayPosition, 0.0f);
		collapseArtAndTracks.addCallback(new UIThreadTimelineCallbackAdapter() {
			int startingRegionWidth;

			@Override
			public void onTimelineStateChanged(TimelineState oldState,
					TimelineState newState, float durationFraction,
					float timelinePosition) {
				if (newState == TimelineState.READY) {
					startingRegionWidth = (int) (BigAlbumArt.TOTAL_DIM * (1 + overlayPosition));
				}
				updateShellRegion(timelinePosition);
			}

			@Override
			public void onTimelinePulse(float durationFraction,
					float timelinePosition) {
				updateShellRegion(timelinePosition);
			}

			private void updateShellRegion(float timelinePosition) {
				int regionWidth = (int) (startingRegionWidth + timelinePosition
						* (BigAlbumArt.TOTAL_DIM - startingRegionWidth));
				Region newRegion = new Region();
				newRegion.add(BigAlbumArt.TOTAL_DIM - regionWidth / 2, 0,
						regionWidth, BigAlbumArt.TOTAL_DIM);
				Shell shell = getShell();
				if (!shell.isDisposed())
					getShell().setRegion(newRegion);
			}
		});
		// collapseArtAndTracks.addPropertyToInterpolate(Timeline
		// .<Point> property("size").on(this.getShell()).fromCurrent().to(
		// new Point(BigAlbumArt.ALBUM_ART_DIM,
		// BigAlbumArt.ALBUM_ART_DIM)));
		// collapseArtAndTracks
		// .addPropertyToInterpolate(Timeline.<Point> property("location")
		// .on(this.getShell()).fromCurrent().to(
		// new Point(mainWindowLoc.x + mainWindowDim.x / 2
		// - BigAlbumArt.TOTAL_DIM / 2,
		// mainWindowLoc.y + mainWindowDim.y
		// - BigAlbumArt.TOTAL_DIM / 2)));
		collapseArtAndTracks.setDuration((int) (500 * this.overlayPosition));
		scenario.addScenarioActor(collapseArtAndTracks);

		// step 2 (in parallel) - load the new album art
		final Image[] albumArtHolder = new Image[1];
		EclipseJobTimelineScenarioActor loadNewAlbumArt = new EclipseJobTimelineScenarioActor(
				"Load album art") {
			@Override
			protected org.eclipse.core.runtime.IStatus run(
					org.eclipse.core.runtime.IProgressMonitor arg0) {
				try {
					albumArtHolder[0] = new Image(Display.getDefault(),
							BackendConnector.getLargeAlbumArt(album.asin));
					return Status.OK_STATUS;
				} catch (Throwable t) {
					t.printStackTrace();
					return Status.CANCEL_STATUS;
				}
			}
		};
		scenario.addScenarioActor(loadNewAlbumArt);

		// step 3 (in parallel) - load the track listing
		final java.util.List<Track> tracks = new ArrayList<Track>();
		TimelineSwingWorker<Void, Void> loadNewAlbumTrackList = new TimelineSwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				tracks.addAll(BackendConnector.doTrackSearch(album.releaseID));
				return null;
			}
		};
		scenario.addScenarioActor(loadNewAlbumTrackList);
		scenario.rendezvous();

		// step 4 (wait for steps 1-3) - replace album art
		TimelineRunnable replaceAlbumArt = new TimelineRunnable() {
			@Override
			public void run() {
				albumArt.setAlbumArtImage(albumArtHolder[0]);
			}
		};
		scenario.addScenarioActor(replaceAlbumArt);

		// step 5 (in parallel) - replace the track listing
		TimelineRunnable replaceTrackListing = new TimelineRunnable() {
			@Override
			public void run() {
				trackListing.setAlbumItem(album, tracks);
			}
		};
		scenario.addScenarioActor(replaceTrackListing);
		scenario.rendezvous();

		// step 6 (wait for steps 4 and 5) - cross fade album art from old to
		// new
		Timeline albumArtCrossfadeTimeline = new Timeline(this.albumArt);
		albumArtCrossfadeTimeline.addPropertyToInterpolate("oldImageAlpha",
				255, 0);
		albumArtCrossfadeTimeline
				.addPropertyToInterpolate("imageAlpha", 0, 255);
		albumArtCrossfadeTimeline.addCallback(new SWTRepaintCallback(
				this.albumArt));
		albumArtCrossfadeTimeline.setDuration(400);

		scenario.addScenarioActor(albumArtCrossfadeTimeline);
		scenario.rendezvous();

		// step 7 (wait for step 6) - move new album art and track listing to
		// be side by side.
		Timeline separateArtAndTracks = new Timeline(this);
		separateArtAndTracks.addPropertyToInterpolate("overlayPosition", 0.0f,
				1.0f);
		separateArtAndTracks.addCallback(new UIThreadTimelineCallbackAdapter() {
			@Override
			public void onTimelinePulse(float durationFraction,
					float timelinePosition) {
				updateShellRegion(timelinePosition);
			}

			@Override
			public void onTimelineStateChanged(TimelineState oldState,
					TimelineState newState, float durationFraction,
					float timelinePosition) {
				updateShellRegion(timelinePosition);
			}

			private void updateShellRegion(float timelinePosition) {
				int regionWidth = (int) (BigAlbumArt.TOTAL_DIM * (1.0 + timelinePosition));
				Region newRegion = new Region();
				newRegion.add(BigAlbumArt.TOTAL_DIM - regionWidth / 2, 0,
						regionWidth, BigAlbumArt.TOTAL_DIM);
				Shell shell = getShell();
				if (!shell.isDisposed())
					getShell().setRegion(newRegion);
			}
		});
		//
		// separateArtAndTracks.addPropertyToInterpolate(Timeline
		// .<Point> property("size").on(this.getShell()).fromCurrent().to(
		// new Point(2 * BigAlbumArt.ALBUM_ART_DIM,
		// BigAlbumArt.ALBUM_ART_DIM)));
		// separateArtAndTracks
		// .addPropertyToInterpolate(Timeline.<Point> property("location")
		// .on(this.getShell()).fromCurrent().to(
		// new Point(mainWindowLoc.x + mainWindowDim.x / 2
		// - BigAlbumArt.TOTAL_DIM,
		// mainWindowLoc.y + mainWindowDim.y
		// - BigAlbumArt.TOTAL_DIM / 2)));
		separateArtAndTracks.setDuration(500);
		scenario.addScenarioActor(separateArtAndTracks);

		return scenario;
	}
}
