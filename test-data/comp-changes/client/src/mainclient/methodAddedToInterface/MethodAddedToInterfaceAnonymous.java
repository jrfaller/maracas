package mainclient.methodAddedToInterface;

import main.methodAddedToInterface.IMethodAddedToInterfaceInner;
import main.methodAddedToInterface.MethodAddedToInterfaceInner;

public class MethodAddedToInterfaceAnonymous {
	void createObjectAnonymousInner() {
		IMethodAddedToInterfaceInner c = new IMethodAddedToInterfaceInner() {};
	}

	void createObjectAnonymousInnerImp() {
		MethodAddedToInterfaceInnerImp c = new MethodAddedToInterfaceInnerImp() {};
	}

	void createObjectAnonymousInnerImpI() {
		MethodAddedToInterfaceInner.I c = new MethodAddedToInterfaceInner.I () {};
	}
}
