package mainclient.methodRemovedInSuperclass;

import main.methodRemovedInSuperclass.MethodRemovedInSuperclass;
import main.methodRemovedInSuperclass.SMethodRemovedInSuperclass;

public class MethodRemovedInSuperClassAnonymous {
	void anonymousExt() {
		MethodRemovedInSuperclass c = new MethodRemovedInSuperclass() {
			@Override
			public int methodRemovedSAbs() {
				return 0;
			}

			@Override
			public int methodRemovedSSAbs() {
				return 0;
			}

			public int callSuperSMethod() {
				return methodRemovedS();
			}

			public int callSuperSSMethod() {
				return methodRemovedSS();
			}
		};
	}

	void anonymousExtSuper() {
		SMethodRemovedInSuperclass c = new SMethodRemovedInSuperclass() {
			@Override
			public int methodRemovedSAbs() {
				return 0;
			}

			@Override
			public int methodRemovedSSAbs() {
				return 0;
			}

			public int callSuperSMethod() {
				return methodRemovedS();
			}

			public int callSuperSSMethod() {
				return methodRemovedSS();
			}
		};
	}
}
