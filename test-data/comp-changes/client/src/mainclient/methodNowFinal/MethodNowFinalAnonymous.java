package mainclient.methodNowFinal;

import main.methodNowFinal.MethodNowFinal;

public class MethodNowFinalAnonymous {
	void anonymous() {
		MethodNowFinal c = new MethodNowFinal() {
			@Override
			public int methodNowFinal() {
				return 1;
			}

			@Override
			public int sMethodNowFinal() {
				return 10;
			}

			public int methodNowFinalClient() {
				return super.methodNowFinal();
			}

			@Override
			public int sMethodNowFinalNoOverride() {
				return 10;
			}
		};
	}
}
