package mainclient.methodLessAccessible;

import main.methodLessAccessible.IMethodLessAccessible;
import main.methodLessAccessible.MethodLessAccessible;

public class MethodLessAccessibleAnonymous {
	void anonymousAccess1() {
		IMethodLessAccessible c = new IMethodLessAccessible() {
			@Override
			public int methodLessAccessiblePublic2PackPriv() {
				return 0;
			}

			@Override
			public int methodLessAccessiblePackPriv2Public() {
				return 0;
			}
		};
	}

	void anonymousAccess2() {
		MethodLessAccessible c = new MethodLessAccessible() {
			@Override
			public int methodLessAccessiblePublic2Protected() {
				return 100;
			}

			@Override
			public int methodLessAccessiblePublic2PackPriv() {
				return 101;
			}

			@Override
			public int methodLessAccessiblePublic2Private() {
				return 102;
			}

			@Override
			protected int methodLessAccessibleProtected2Public() {
				return 103;
			}

			@Override
			protected int methodLessAccessibleProtected2PackPriv() {
				return 104;
			}

			@Override
			protected int methodLessAccessibleProtected2Private() {
				return 105;
			}
		};
	}
}
