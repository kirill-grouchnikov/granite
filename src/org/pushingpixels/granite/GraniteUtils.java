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
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.Timeline.TimelineState;
import org.pushingpixels.trident.callback.UIThreadTimelineCallbackAdapter;

public class GraniteUtils {
	public static Image getScaledInstance(Image img, int targetWidth,
			int targetHeight) {
		Image ret = img;
		// Use multi-step technique: start with original size, then
		// scale down in multiple passes with drawImage()
		// until the target size is reached
		int w = img.getImageData().width;
		int h = img.getImageData().height;

		do {
			if (w > targetWidth) {
				w /= 2;
				if (w < targetWidth) {
					w = targetWidth;
				}
			}

			if (h > targetHeight) {
				h /= 2;
				if (h < targetHeight) {
					h = targetHeight;
				}
			}

			Image tmp = new Image(img.getDevice(), w, h);
			GC gc = new GC(tmp);
			// use antialias for better quality
			gc.setAntialias(SWT.ON);
			gc.drawImage(ret, 0, 0, ret.getImageData().width, ret
					.getImageData().height, 0, 0, w, h);

			ret = tmp;
		} while (w != targetWidth || h != targetHeight);

		return ret;
	}

	public static int paintMultilineText(Control control, GC gc, String text,
			int textX, int textWidth, int textY, int maxTextLineCount) {
		int fa = gc.getFontMetrics().getAscent()
				+ gc.getFontMetrics().getDescent();

		if (text.length() == 0)
			return textY;

		SWTLineBreakMeasurer lineBreakMeasurer = new SWTLineBreakMeasurer(
				control, text, gc.getFont());
		int lineCount = 0;
		while (true) {
			String string = lineBreakMeasurer.next(textWidth);
			if (string == null)
				break;

			TextLayout layout = new TextLayout(gc.getDevice());
			layout.setText(string);
			layout.setFont(gc.getFont());
			layout.draw(gc, textX, textY);
			layout.dispose();

			textY += fa;
			lineCount++;
			if ((maxTextLineCount > 0) && (lineCount == maxTextLineCount))
				break;
		}

		return textY;
	}

	public static int getMultilineTextHeight(Control control, GC gc,
			String text, int availableWidth) {
		int fa = gc.getFontMetrics().getAscent()
				+ gc.getFontMetrics().getDescent();

		if (text.length() == 0)
			return 0;

		SWTLineBreakMeasurer lineBreakMeasurer = new SWTLineBreakMeasurer(
				control, text, gc.getFont());
		int lineCount = 0;
		while (true) {
			String string = lineBreakMeasurer.next(availableWidth);
			if (string == null)
				break;

			lineCount++;
		}

		return lineCount * fa;
	}

	private static class SWTLineBreakMeasurer {
		Control control;

		String string;

		Font font;

		int currLocation;

		public SWTLineBreakMeasurer(Control control, String string, Font font) {
			this.control = control;
			this.string = string;
			this.font = font;
			this.currLocation = 0;
		}

		public String next(int availableWidth) {
			GC gc = new GC(this.control);
			gc.setFont(this.font);

			try {
				// skip whitespaces
				while (currLocation < string.length()) {
					char c = string.charAt(currLocation);
					if (!Character.isWhitespace(c)) {
						break;
					}
					currLocation++;
				}

				if (currLocation == string.length())
					return null;

				String curr = "";
				int lastWhiteSpaceLoc = -1;
				int beginIndex = currLocation;
				while (currLocation < string.length()) {
					char c = string.charAt(currLocation);
					if (Character.isWhitespace(c)) {
						if (gc.stringExtent(curr).x >= availableWidth) {
							// try to go back to the last whitespace location
							if (lastWhiteSpaceLoc >= 0) {
								currLocation = lastWhiteSpaceLoc;
								return string.substring(beginIndex,
										currLocation);
							}
							return curr;
						}
						lastWhiteSpaceLoc = currLocation;
					}
					curr += c;
					currLocation++;
				}
				if (gc.stringExtent(curr).x >= availableWidth) {
					// try to go back to the last whitespace location
					if (lastWhiteSpaceLoc >= 0) {
						currLocation = lastWhiteSpaceLoc;
						return string.substring(beginIndex, currLocation);
					}
					return curr;
				}
				return curr;
			} finally {
				gc.dispose();
			}
		}
	}

	public static void fadeOutAndDispose(final Shell shell, int fadeOutDuration) {
		Timeline dispose = new Timeline(shell);
		dispose.addPropertyToInterpolate("alpha", 255, 0);
		dispose.addCallback(new UIThreadTimelineCallbackAdapter() {
			@Override
			public void onTimelineStateChanged(TimelineState oldState,
					TimelineState newState, float durationFraction,
					float timelinePosition) {
				if (newState == TimelineState.DONE) {
					shell.dispose();
				}
			}
		});
		dispose.setDuration(fadeOutDuration);
		dispose.play();
	}

	public static class RoundRectangle extends Path {
		public RoundRectangle(Device device, float x, float y, float width,
				float height, float radius) {
			super(device);
			this.moveTo(x + radius, y);
			this.lineTo(x + width - 1 - radius, y);
			this.addArc(x + width - 1 - 2 * radius, y, 2 * radius, 2 * radius,
					90, -90);
			this.lineTo(x + width - 1, y + height - 1 - radius);
			this
					.addArc(x + width - 1 - 2 * radius, y + height - 1 - 2
							* radius, 2 * radius, 2 * radius, 0, -90);
			this.lineTo(x + radius, y + height - 1);
			this.addArc(x, y + height - 1 - 2 * radius, 2 * radius, 2 * radius,
					-90, -90);
			this.lineTo(x, y + radius);
			this.addArc(x, y, 2 * radius, 2 * radius, 180, -90);
			this.close();
		}
	}
}
