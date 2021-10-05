package mainclient.methodAbstractAddedToClass;

import main.methodAbstractAddedToClass.MethodAbstractAddedToClass;
import main.methodAbstractAddedToClass.MethodAbstractAddedToClassSub;

public class MethodAbstractAddedToClassAnonymous {
	void createObjectAnonymous1() {
		MethodAbstractAddedToClass c = new MethodAbstractAddedToClass() {
			@Override
			public void abstractOld() {
			}
		};
	}

	void createObjectAnonymous2() {
		MethodAbstractAddedToClassSub c = new MethodAbstractAddedToClassSub() {
			@Override
			public void abstractOld() {
			}
		};
	}

	void createObjectAnonymous3() {
		AbsMethodAbstractAddedToClassExt c = new AbsMethodAbstractAddedToClassExt() {
			@Override
			public void abstractOld() {
			}
		};
	}

	void createObjectAnonymous4() {
		ConcMethodAbstractAddedToClassExt c = new ConcMethodAbstractAddedToClassExt() {};
	}

	void createObjectAnonymous5() {
		MethodAbstractAddedToClassSubExt c = new MethodAbstractAddedToClassSubExt() {};
	}
}
