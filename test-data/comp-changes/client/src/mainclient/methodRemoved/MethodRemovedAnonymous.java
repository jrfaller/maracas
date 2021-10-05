package mainclient.methodRemoved;

import main.methodRemoved.IMethodRemoved;
import main.methodRemoved.MethodRemoved;

public class MethodRemovedAnonymous {
	void anonymousExt() {
		MethodRemoved c = new MethodRemoved() {
			public int methodRemovedClientExt() {
				return methodRemoved();
			}

			public int methodRemovedClientSuper() {
				return super.methodRemoved();
			}

			@Override
			public int methodRemoved() {
				return super.methodRemoved();
			}

			@Override
			public int methodStay() {
				return super.methodStay();
			}
		};
	}

	void anonymousImp() {
		IMethodRemoved c = new IMethodRemoved() {
			@Override
			public int methodRemoved() {
				return 0;
			}

			@Override
			public int methodStay() {
				return 1;
			}
		};
	}
}
