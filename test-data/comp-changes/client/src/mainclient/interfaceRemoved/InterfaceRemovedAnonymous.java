package mainclient.interfaceRemoved;

import main.interfaceRemoved.IInterfaceRemoved;
import main.interfaceRemoved.IInterfaceRemovedMulti;
import main.interfaceRemoved.IInterfaceRemovedMultiMulti;
import main.interfaceRemoved.InterfaceRemoved;
import main.interfaceRemoved.InterfaceRemovedAbs;

public class InterfaceRemovedAnonymous {
	void anonymousClassInterface() {
		IInterfaceRemoved anon = new IInterfaceRemoved() {
			@Override
			public int methodAbs() {
				return 0;
			}
		};
	}

	void anonymousClassInterfaceExtends() {
		IInterfaceRemovedExt anon = new IInterfaceRemovedExt() {
			@Override
			public int mMulti() {
				return 0;
			}

			@Override
			public int methodAbs() {
				return 0;
			}
		};
	}

	void anonymousClassInterfaceMulti() {
		IInterfaceRemovedMulti anon = new IInterfaceRemovedMulti() {
			@Override
			public int methodAbs() {
				return 0;
			}

			@Override
			public int mMulti() {
				return 0;
			}
		};
	}

	void anonymousClassInterfaceMultiMulti() {
		IInterfaceRemovedMulti anon = new IInterfaceRemovedMultiMulti() {
			@Override
			public int mMulti() {
				return 0;
			}

			@Override
			public int methodAbs() {
				return 0;
			}

			@Override
			public int mMultiMulti() {
				return 0;
			}
		};
	}

	void anonymousClassAbs() {
		InterfaceRemovedAbs anon = new InterfaceRemovedAbs() {
			@Override
			public int methodAbs() {
				return 0;
			}};
	}

	void anonymousClass() {
		InterfaceRemoved anon = new InterfaceRemoved() {};
	}
}
