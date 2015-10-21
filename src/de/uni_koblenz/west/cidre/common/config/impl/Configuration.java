package de.uni_koblenz.west.cidre.common.config.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import de.uni_koblenz.west.cidre.common.config.Configurable;
import de.uni_koblenz.west.cidre.common.config.ConfigurableDeserializer;
import de.uni_koblenz.west.cidre.common.config.ConfigurableSerializer;
import de.uni_koblenz.west.cidre.common.config.Property;
import de.uni_koblenz.west.cidre.master.dictionary.impl.MapDBCacheOptions;
import de.uni_koblenz.west.cidre.master.dictionary.impl.MapDBStorageOptions;

public class Configuration implements Configurable {

	private static final String DEFAULT_PORT = "4710";

	@Property(name = "master", description = "The ip and port of the master server, e.g., 192.168.0.1:4710. If no port is specified, the default port "
			+ DEFAULT_PORT + " is used.")
	private String masterIP;

	private String masterPort;

	public String[] getMaster() {
		return new String[] { masterIP, masterPort };
	}

	public void setMaster(String masterIP) {
		setMaster(masterIP, DEFAULT_PORT);
	}

	public void setMaster(String masterIP, String masterPort) {
		this.masterIP = masterIP;
		this.masterPort = masterPort;
	}

	@Property(name = "slaves", description = "The comma separated list of ips and ports of the different slaves, e.g., 192.168.0.2:4712,192.168.0.3,192.168.0.4:4777. If no port is specified, the default port "
			+ DEFAULT_PORT + " is used.")
	private List<String> slaveIPs;

	private List<String> slavePorts;

	public String[] getSlave(int index) {
		return new String[] { slaveIPs.get(index), slavePorts.get(index) };
	}

	public int getNumberOfSlaves() {
		return slaveIPs == null ? 0 : slaveIPs.size();
	}

	public void addSlave(String slaveIP) {
		addSlave(slaveIP, DEFAULT_PORT);
	}

	public void addSlave(String slaveIP, String slavePort) {
		if (slaveIPs == null) {
			slaveIPs = new ArrayList<>();
			slavePorts = new ArrayList<>();
		}
		slaveIPs.add(slaveIP);
		slavePorts.add(slavePort);
	}

	public static final String DEFAULT_CLIENT_PORT = "4711";

	@Property(name = "clientConnection", description = "The ip and port to which clients can connect, e.g., 192.168.0.1:4711. If no port is specified, the default port "
			+ DEFAULT_CLIENT_PORT + " is used.")
	private String clientIP;

	private String clientPort;

	public String[] getClient() {
		return new String[] { clientIP, clientPort };
	}

	public void setClient(String clientIP) {
		setClient(clientIP, DEFAULT_CLIENT_PORT);
	}

	public void setClient(String clientIP, String clientPort) {
		this.clientIP = clientIP;
		this.clientPort = clientPort;
	}

	public static final long CLIENT_CONNECTION_TIMEOUT = 10000;

	public static final long CLIENT_KEEP_ALIVE_INTERVAL = 3000;

	@Property(name = "clientConnectionTimeout", description = "The number of milliseconds the master waits for messages from the client before closing the connection."
			+ " Every " + CLIENT_KEEP_ALIVE_INTERVAL
			+ " milliseconds the client sends a keep alive message to the master. The default value is "
			+ CLIENT_CONNECTION_TIMEOUT + " milliseconds.")
	private long clientConnectionTimeout = CLIENT_CONNECTION_TIMEOUT;

	public long getClientConnectionTimeout() {
		return clientConnectionTimeout;
	}

	public void setClientConnectionTimeout(long clientConnectionTimeout) {
		this.clientConnectionTimeout = clientConnectionTimeout;
	}

	private String romoteLoggerReceiver;

	public String getRomoteLoggerReceiver() {
		return romoteLoggerReceiver;
	}

	public void setRomoteLoggerReceiver(String romoteLoggerReceiver) {
		this.romoteLoggerReceiver = romoteLoggerReceiver;
	}

	@Property(name = "logLevel", description = "Sets the logging level to one of: OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL")
	private Level loglevel = Level.INFO;

	@Property(name = "loggingDirectory", description = "Defines an existing directory in which the log files are written.")
	private String logDirectory = ".";

	public Level getLoglevel() {
		return loglevel;
	}

	public void setLoglevel(Level loglevel) {
		this.loglevel = loglevel;
	}

	public String getLogDirectory() {
		return logDirectory;
	}

	public void setLogDirectory(String logDirectory) {
		this.logDirectory = logDirectory;
	}

	@Property(name = "tmpDir", description = "Defines the directory where intermediate data is stored. Default directory (i.e., if not set) is the temporary directory of the operating system.")
	private String tmpDir = System.getProperty("java.io.tmpdir");

	public String getTmpDir() {
		return tmpDir;
	}

	public void setTmpDir(String tmpDir) {
		this.tmpDir = tmpDir;
	}

	@Property(name = "dictionaryStorageType", description = "Defines how the dictionary is stored:"
			+ "\nMEMORY = dictionary is only stored in memory"
			+ "\nMEMORY_MAPPED_FILE = dictionary is stored as a file located in dictionaryDir which is mapped to memory. In Linux no additional caching is required."
			+ "\nRANDOM_ACCESS_FILE = dictionary is stored as a file located in dictionaryDir. Each dictionary lookup will result in a file access.")
	private MapDBStorageOptions dictionaryStorageType = MapDBStorageOptions.MEMORY_MAPPED_FILE;

	public MapDBStorageOptions getDictionaryStorageType() {
		return dictionaryStorageType;
	}

	public void setDictionaryStorageType(
			MapDBStorageOptions dictionaryStorageType) {
		this.dictionaryStorageType = dictionaryStorageType;
	}

	@Property(name = "dictionaryDir", description = "Defines the directory where the dictionary is stored.")
	private String dictionaryDir = ".";

	public String getDictionaryDir() {
		return dictionaryDir;
	}

	public void setDictionaryDir(String dictionaryDir) {
		this.dictionaryDir = dictionaryDir;
	}

	@Property(name = "enableTransactionsForDictionary", description = "If set to true, transactions are used. Transactions are only required if processing updates in future work.")
	private boolean useTransactionsForDictionary = false;

	public boolean useTransactionsForDictionary() {
		return useTransactionsForDictionary;
	}

	public void setUseTransactionsForDictionary(boolean useTransactions) {
		useTransactionsForDictionary = useTransactions;
	}

	@Property(name = "enableAsynchronousWritesForDictionary", description = "If set to true, updates are written in a separate thread asynchronously.")
	private boolean isDictionaryAsynchronouslyWritten = true;

	public boolean isDictionaryAsynchronouslyWritten() {
		return isDictionaryAsynchronouslyWritten;
	}

	public void setDictionaryAsynchronouslyWritten(
			boolean isAsynchronousWritten) {
		isDictionaryAsynchronouslyWritten = isAsynchronousWritten;
	}

	@Property(name = "dictionaryCacheType", description = "Defines how the instance cache works:"
			+ "\nNONE = no instances are cached"
			+ "\nHASH_TABLE = a cached instance is deleted, if a hash collision occurs"
			+ "\nLEAST_RECENTLY_USED = the least recently used instance is deleted, if the cache reaches its maximum size"
			+ "\nHARD_REFERENCE = no instance is removed from the cache automatically"
			+ "\nSOFT_REFERENCE = instances are removed from the cache by the garbage collector, if no hard reference exists on them and the memory is full"
			+ "\nWEAK_REFERENCE = instances are removed from the cache by the garbage collector, as soon as no hard reference exists on them")
	private MapDBCacheOptions dictionaryCacheType = MapDBCacheOptions.HASH_TABLE;

	public MapDBCacheOptions getDictionaryCacheType() {
		return dictionaryCacheType;
	}

	public void setDictionaryCacheType(MapDBCacheOptions dictionaryCacheType) {
		this.dictionaryCacheType = dictionaryCacheType;
	}

	// TODO add options

	/*
	 * serialization specific code
	 */

	private ConfigurationSerializer serializer;

	private ConfigurationDeserializer deserializer;

	@Override
	public ConfigurableSerializer getSerializer() {
		if (serializer == null) {
			serializer = new ConfigurationSerializer();
		}
		return serializer;
	}

	@Override
	public ConfigurableDeserializer getDeserializer() {
		if (deserializer == null) {
			deserializer = new ConfigurationDeserializer();
		}
		return deserializer;
	}
}
