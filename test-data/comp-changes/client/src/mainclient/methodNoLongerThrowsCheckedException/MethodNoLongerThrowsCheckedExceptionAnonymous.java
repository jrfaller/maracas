package mainclient.methodNoLongerThrowsCheckedException;

import java.io.IOException;

import main.methodNoLongerThrowsCheckedException.IMethodNoLongerThrowsCheckedException;
import main.methodNoLongerThrowsCheckedException.MethodNoLongerThrowsCheckedException;

public class MethodNoLongerThrowsCheckedExceptionAnonymous {
	void anonymousExt() {
		MethodNoLongerThrowsCheckedException c = new MethodNoLongerThrowsCheckedException() {
			@Override
			public void noLongerThrowsExcep() throws IOException {

			}
		};
	}

	void anonymousImpl() {
		IMethodNoLongerThrowsCheckedException c = new IMethodNoLongerThrowsCheckedException() {
			@Override
			public int noLongerThrowsExcep() throws IOException {
				return 0;
			}
		};
	}
}
