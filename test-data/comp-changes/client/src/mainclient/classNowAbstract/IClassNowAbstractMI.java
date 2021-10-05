package mainclient.classNowAbstract;

import main.classNowAbstract.IClassNowAbstract;

public class IClassNowAbstractMI {

	public void createObject() {
		IClassNowAbstract c = new IClassNowAbstract();
	}

	public void createObjectParams() {
		IClassNowAbstract c = new IClassNowAbstract(3);
	}

	public void createObjectAnonymous() {
		IClassNowAbstract c = new IClassNowAbstract() {};
	}

}
