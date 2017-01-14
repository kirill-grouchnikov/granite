package org.pushingpixels.granite;

import org.eclipse.core.runtime.jobs.*;
import org.pushingpixels.trident.TimelineScenario.TimelineScenarioActor;

public abstract class EclipseJobTimelineScenarioActor extends Job implements
		TimelineScenarioActor {
	volatile transient boolean isDone = false;

	public EclipseJobTimelineScenarioActor(String name) {
		super(name);
		this.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				isDone = true;
			}
		});
	}

	@Override
	public boolean isDone() {
		return isDone && (this.getState() == Job.NONE);
	}

	@Override
	public void play() {
		this.schedule();
	}

	@Override
	public void resetDoneFlag() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsReplay() {
		return false;
	}
}
