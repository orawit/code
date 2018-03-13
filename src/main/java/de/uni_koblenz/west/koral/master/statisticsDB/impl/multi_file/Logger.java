package de.uni_koblenz.west.koral.master.statisticsDB.impl.multi_file;

class Logger {

	private static final boolean LOGGING_ENABLED = false;

	private Logger() {
	}

	static void log(String msg) {
		if (LOGGING_ENABLED) {
			System.out.println(msg);
		}
	}

}
