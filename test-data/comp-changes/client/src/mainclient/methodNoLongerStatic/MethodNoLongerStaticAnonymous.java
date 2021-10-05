package mainclient.methodNoLongerStatic;

import main.methodNoLongerStatic.IMethodNoLongerStatic;
import main.methodNoLongerStatic.MethodNoLongerStatic;

public class MethodNoLongerStaticAnonymous {
	void anonymousExt() {
		MethodNoLongerStatic c = new MethodNoLongerStatic() {
			public int methodNoLongerStaticSuperKeyAccess() {
				return super.methodNoLongerStatic();
			}

			public int methodNoLongerStaticNoSuperKeyAccess() {
				return methodNoLongerStatic();
			}
		};
	}

	void anonymousImp() {
		IMethodNoLongerStatic c = new IMethodNoLongerStatic() {
			public int methodNoLongerStaticClient() {
				return IMethodNoLongerStatic.methodNoLongerStatic();
			}
		};
	}
}
