package de.uni_koblenz.west.cidre.common.config.impl;

import java.util.logging.Level;
import java.util.regex.Pattern;

import de.uni_koblenz.west.cidre.common.config.ConfigurableDeserializer;
import de.uni_koblenz.west.cidre.master.dictionary.impl.MapDBCacheOptions;
import de.uni_koblenz.west.cidre.master.dictionary.impl.MapDBStorageOptions;

class ConfigurationDeserializer implements ConfigurableDeserializer {

	public void deserializeMaster(Configuration conf, String master) {
		if (master.indexOf(':') == -1) {
			conf.setMaster(master);
		} else {
			String[] parts = master.split(Pattern.quote(":"));
			conf.setMaster(parts[0], parts[1]);
		}
	}

	public void deserializeSlaves(Configuration conf, String slaves) {
		String[] entries = slaves.split(Pattern.quote(","));
		for (int i = 0; i < entries.length; i++) {
			String entry = entries[i];
			if (entry.indexOf(':') == -1) {
				conf.addSlave(entry);
			} else {
				String[] parts = entry.split(Pattern.quote(":"));
				conf.addSlave(parts[0], parts[1]);
			}
		}
	}

	public void deserializeClientConnection(Configuration conf, String client) {
		if (client.indexOf(':') == -1) {
			conf.setClient(client);
		} else {
			String[] parts = client.split(Pattern.quote(":"));
			conf.setClient(parts[0], parts[1]);
		}
	}

	public void deserializeClientConnectionTimeout(Configuration conf,
			String clientConnectionTimeout) {
		conf.setClientConnectionTimeout(
				Long.parseLong(clientConnectionTimeout));
	}

	public void deserializeLogLevel(Configuration conf, String logLevel) {
		conf.setLoglevel(Level.parse(logLevel));
	}

	public void deserializeLoggingDirectory(Configuration conf,
			String logDirectory) {
		conf.setLogDirectory(logDirectory);
	}

	public void deserializeTmpDir(Configuration conf, String tmpDir) {
		if (tmpDir != null && !tmpDir.isEmpty()) {
			conf.setTmpDir(tmpDir);
		}
	}

	public void deserializeDictionaryStorageType(Configuration conf,
			String storageType) {
		if (storageType != null && !storageType.isEmpty()) {
			try {
				conf.setDictionaryStorageType(
						MapDBStorageOptions.valueOf(storageType.toUpperCase()));
			} catch (IllegalArgumentException e) {

			}
		}
	}

	public void deserializeDictionaryDir(Configuration conf,
			String dictionaryDir) {
		if (dictionaryDir != null && !dictionaryDir.isEmpty()) {
			conf.setDictionaryDir(dictionaryDir);
		}
	}

	public void deserializeEnableTransactionsForDictionary(Configuration conf,
			String enableTransactions) {
		if (enableTransactions != null && !enableTransactions.isEmpty()) {
			conf.setUseTransactionsForDictionary(
					Boolean.parseBoolean(enableTransactions));
		}
	}

	public void deserializeEnableAsynchronousWritesForDictionary(
			Configuration conf, String writeAsynchronously) {
		if (writeAsynchronously != null && !writeAsynchronously.isEmpty()) {
			conf.setDictionaryAsynchronouslyWritten(
					Boolean.parseBoolean(writeAsynchronously));
		}
	}

	public void deserializeDictionaryCacheType(Configuration conf,
			String cacheType) {
		if (cacheType != null && !cacheType.isEmpty()) {
			try {
				conf.setDictionaryCacheType(
						MapDBCacheOptions.valueOf(cacheType.toUpperCase()));
			} catch (IllegalArgumentException e) {

			}
		}
	}

}
