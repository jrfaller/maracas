package mainclient.methodNowStatic;

import main.methodNowStatic.IMethodNowStatic;
import main.methodNowStatic.MethodNowStatic;

public class MethodNowStaticAnonymous {
	void anonymousExt() {
		MethodNowStatic c = new MethodNowStatic() {
			public int methodNowStaticClientSuperKeyAccess() {
				return super.methodNowStatic();
			}

			public int methodNowStaticClientNoSuperKeyAccess() {
				return methodNowStatic();
			}
		};
	}

	void anonymousExtOverriden() {
		MethodNowStatic c = new MethodNowStatic() {
			@Override
			public int methodNowStatic() {
				return 1;
			}
		};
	}

	void anonymousImp() {
		IMethodNowStatic c = new IMethodNowStatic() {
			public int methodNowStaticClient() {
				return methodNowStatic();
			}
		};
	}
}
