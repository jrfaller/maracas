package mainclient.methodReturnTypeChanged;

import java.util.ArrayList;

import main.methodReturnTypeChanged.IMethodReturnTypeChanged;
import main.methodReturnTypeChanged.MethodReturnTypeChanged;

public class MethodReturnTypeChangedAnonymous {
	void anonymousExt() {
		MethodReturnTypeChanged c = new MethodReturnTypeChanged() {
			public long numericClientSuperKey() {
				return super.methodReturnTypeChangedNumeric();
			}

			public ArrayList listClientSuperKey() {
				return super.methodReturnTypeChangedList();
			}

			public long numericClientNoSuperKey() {
				return methodReturnTypeChangedNumeric();
			}

			public ArrayList listClientNoSuperKey() {
				return methodReturnTypeChangedList();
			}
		};
	}

	void anonymousImp() {
		IMethodReturnTypeChanged c = new IMethodReturnTypeChanged() {
			@Override
			public ArrayList methodReturnTypeChangedList() {
				return new ArrayList();
			}

			@Override
			public long methodReturnTypeChangedNumeric() {
				return 1;
			}
		};
	}
}
