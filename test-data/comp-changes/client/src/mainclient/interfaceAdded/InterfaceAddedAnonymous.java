package mainclient.interfaceAdded;

import main.interfaceAdded.IInterfaceAdded;
import main.interfaceAdded.IInterfaceAddedMulti;
import main.interfaceAdded.IInterfaceAddedMultiMulti;
import main.interfaceAdded.InterfaceAdded;
import main.interfaceAdded.InterfaceAddedAbs;

public class InterfaceAddedAnonymous {
	void anonymousClassInterface() {
		IInterfaceAdded anon = new IInterfaceAdded() {
			@Override
			public int methodAbs() {
				return 0;
			}
		};
	}

	void anonymousClassInterfaceExtends() {
		IInterfaceAddedExt anon = new IInterfaceAddedExt() {
			@Override
			public int mMulti() {
				return 0;
			}
		};
	}

	void anonymousClassInterfaceMulti() {
		IInterfaceAddedMulti anon = new IInterfaceAddedMulti() {
			@Override
			public int mMulti() {
				return 0;
			}
		};
	}

	void anonymousClassInterfaceMultiMulti() {
		IInterfaceAddedMulti anon = new IInterfaceAddedMultiMulti() {
			@Override
			public int mMulti() {
				return 0;
			}

			@Override
			public int mMultiMulti() {
				return 0;
			}
		};
	}

	void anonymousClassAbs() {
		InterfaceAddedAbs anon = new InterfaceAddedAbs() {};
	}

	void anonymousClass() {
		InterfaceAdded anon = new InterfaceAdded() {};
	}
}
