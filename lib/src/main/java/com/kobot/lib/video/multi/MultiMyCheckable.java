package com.kobot.lib.video.multi;

import android.widget.Checkable;

public abstract class MultiMyCheckable implements Checkable {
	private boolean mChecked = false;
	
	public MultiMyCheckable() {
		this(false);
	}

	public MultiMyCheckable(boolean checked) {
		mChecked = checked;
	}

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void setChecked(boolean checked) {
		if (mChecked != checked) {
			mChecked = checked;
			onCheckedChanged();
		}
	}

	public void onCheckedChanged() {
		onCheckedChanged(mChecked);
	}

	@Override
	public void toggle() {
		setChecked(!mChecked);
	}

	protected abstract void onCheckedChanged(boolean checked);
}