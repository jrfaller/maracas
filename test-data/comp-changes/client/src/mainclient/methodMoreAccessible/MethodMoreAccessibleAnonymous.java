package mainclient.methodMoreAccessible;

import main.methodMoreAccessible.MethodMoreAccessible;

public class MethodMoreAccessibleAnonymous {
	void anonymousExt() {
		MethodMoreAccessible c = new MethodMoreAccessible() {
			@Override
			protected void protected2public() {
				super.protected2public();
			}
		};
	}
}
