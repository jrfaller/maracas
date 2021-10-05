package mainclient.methodNowThrowsCheckedException;

import main.methodNowThrowsCheckedException.IMethodNowThrowsCheckedException;
import main.methodNowThrowsCheckedException.MethodNowThrowsCheckedException;

public class MethodNowThrowsCheckedExceptionAnonymous {
	void anonymousCheckedExt() {
		MethodNowThrowsCheckedException c = new MethodNowThrowsCheckedException() {
			@Override
			public void nowThrowsExcep() {

			}
		};
	}

	void anonymousCheckedImp() {
		IMethodNowThrowsCheckedException c = new IMethodNowThrowsCheckedException() {
			@Override
			public int nowThrowsExcep() {
				return 0;
			}
		};
	}
}
