package mainclient.superclassRemoved;

import java.util.List;

import main.superclassRemoved.SuperSuperclassRemoved;
import main.superclassRemoved.SuperSuperclassRemovedMulti;
import main.superclassRemoved.SuperSuperclassRemovedMultiMulti;
import main.superclassRemoved.SuperclassRemoved;
import main.superclassRemoved.SuperclassRemovedAbs;

public class SuperclassRemovedAnonymous {
	void anonymousExt() {
		SuperclassRemoved c = new SuperclassRemoved() {
			public void cast() {
				SuperclassRemoved a = new SuperclassRemoved();
				SuperSuperclassRemoved i = a;
			}

			public void intCons() {
				int ia = SuperclassRemoved.CTE;
			}

			public void intConsSuper() {
				int ii = SuperSuperclassRemoved.CTE;
			}

			public void intConsDirect() {
				int in = CTE;
			}

			public void listCons() {
				List<String> la = SuperclassRemoved.LIST;
			}

			public void listConsSuper() {
				List<String> li = SuperSuperclassRemoved.LIST;
			}

			public void listConsDirect() {
				List<String> ln = LIST;
			}

			public void staticM() {
				int ia = SuperclassRemoved.staticMeth();
			}

			public void staticMSuper() {
				int ii = SuperSuperclassRemoved.staticMeth();
			}

			public void staticMDirect() {
				int in = staticMeth();
			}
		};
	}

	void anonymousExtAbs() {
		SuperclassRemovedAbs c = new SuperclassRemovedAbs() {
			@Override
			public int methodAbs() {
				return 0;
			}
		};
	}

	void anonymousImp() {
		SuperSuperclassRemovedMulti c = new SuperSuperclassRemovedMulti() {
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

	void anonymousImpMulti() {
		SuperSuperclassRemovedMultiMulti c = new SuperSuperclassRemovedMultiMulti() {
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

	void anonymousExtSuper() {
		SuperSuperclassRemovedMulti c = new SuperSuperclassRemovedMulti() {
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
}
