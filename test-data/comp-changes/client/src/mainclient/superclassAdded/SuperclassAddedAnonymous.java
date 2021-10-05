package mainclient.superclassAdded;

import java.util.List;

import main.interfaceAdded.IInterfaceAdded;
import main.superclassAdded.SuperSuperclassAddedMulti;
import main.superclassAdded.SuperSuperclassAddedMultiMulti;
import main.superclassAdded.SuperclassAdded;
import main.superclassAdded.SuperclassAddedAbs;

public class SuperclassAddedAnonymous {
	void anonymousExt() {
		SuperclassAdded c = new SuperclassAdded() {
			public void intConsInter() {
				int ii = IInterfaceAdded.CTE;
			}

			public void listConsInter() {
				List<String> li = IInterfaceAdded.LIST;
			}

			public void staticM() {
				//int ia = InterfaceRemoved.staticMeth(); Cannot happen
				int ii = IInterfaceAdded.staticMeth();
				//int in = staticMethos(); Cannot happen
			}
		};
	}

	void anonymousExtAbs() {
		SuperclassAddedAbs c = new SuperclassAddedAbs() {};
	}

	void anonymousImp() {
		SuperSuperclassAddedMulti c = new SuperSuperclassAddedMulti() {
			@Override
			public int mMulti() {
				return 0;
			}
		};
	}

	void anonymousImpMulti() {
		SuperSuperclassAddedMultiMulti c = new SuperSuperclassAddedMultiMulti() {
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

	void anonymousSuper() {
		SuperSuperclassAddedMulti c = new SuperSuperclassAddedMulti() {
			@Override
			public int mMulti() {
				return 0;
			}
		};
	}
}
