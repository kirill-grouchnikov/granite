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

import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.pushingpixels.granite.data.Album;
import org.pushingpixels.granite.details.DetailsWindowManager;
import org.pushingpixels.trident.Timeline;

public class DemoApp {
	MainContentPanel mainContentPanel;

	public DemoApp(Shell shell) {
		this.mainContentPanel = new MainContentPanel(shell);
	}

	public void doLoad(final String searchString) throws Exception {
		mainContentPanel.setLoading(true);
		Job job = new Job("Fetch Amazon") {
			@Override
			protected org.eclipse.core.runtime.IStatus run(
					org.eclipse.core.runtime.IProgressMonitor monitor) {
				System.out.println("Searching");
				try {
					final List<Album> result = BackendConnector
							.doAlbumSearch(searchString);
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							for (final Album album : result) {
								// push the album item to the screen
								AlbumOverviewComponent albumOverviewComp = mainContentPanel
										.addAlbumItem(album);
								albumOverviewComp
										.addMouseListener(new MouseAdapter() {
											@Override
											public void mouseUp(
													org.eclipse.swt.events.MouseEvent e) {
												DetailsWindowManager.show(
														mainContentPanel
																.getShell(),
														album);
											};
										});
							}
							System.out.println("Done searching");
							mainContentPanel.setLoading(false);
						}
					});
				} catch (Throwable exc) {
					exc.printStackTrace();
					System.out.println("Failed searching");
					mainContentPanel.setLoading(false);
					return null;
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	public static void main(final String[] args) throws Exception {
		try {
			System.setProperty("java.net.useSystemProxies", "true");
		} catch (SecurityException e) {
		}

		Display display = new Display();
		Shell shell = new Shell(display, SWT.NO_TRIM);
		final DemoApp app = new DemoApp(shell);
		shell.setLayout(new FillLayout());

		// center the shell in the display bounds
		Rectangle pDisplayBounds = shell.getDisplay().getBounds();
		int width = 480;
		int height = 200;
		shell.setBounds((pDisplayBounds.width - width) / 2,
				(pDisplayBounds.height - height) / 2, width, height);

		shell.setAlpha(0);
		shell.open();

		Timeline fadeInShellTimeline = new Timeline(shell);
		fadeInShellTimeline.addPropertyToInterpolate("alpha", 0, 255);
		fadeInShellTimeline.setDuration(500);
		fadeInShellTimeline.play();

		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					app.doLoad("ce58d854-7430-4231-aa44-97f0144b3372");
				} catch (Exception e) {
				}
			}
		});

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}
