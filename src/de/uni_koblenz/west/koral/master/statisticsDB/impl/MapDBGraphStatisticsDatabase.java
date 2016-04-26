package de.uni_koblenz.west.koral.master.statisticsDB.impl;

import org.mapdb.BTreeKeySerializer.BasicKeySerializer;

import de.uni_koblenz.west.koral.common.mapDB.BTreeMapWrapper;
import de.uni_koblenz.west.koral.common.mapDB.HashTreeMapWrapper;
import de.uni_koblenz.west.koral.common.mapDB.MapDBCacheOptions;
import de.uni_koblenz.west.koral.common.mapDB.MapDBDataStructureOptions;
import de.uni_koblenz.west.koral.common.mapDB.MapDBMapWrapper;
import de.uni_koblenz.west.koral.common.mapDB.MapDBStorageOptions;
import de.uni_koblenz.west.koral.master.statisticsDB.GraphStatisticsDatabase;

import org.mapdb.Serializer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;

/**
 * A MapDB implementation of {@link GraphStatisticsDatabase}.
 * 
 * @author Daniel Janke &lt;danijankATuni-koblenz.de&gt;
 *
 */
public class MapDBGraphStatisticsDatabase implements GraphStatisticsDatabase {

  private final File persistenceFile;

  private long[] numberOfTriplesPerChunk;

  private long[] ownerLoad;

  private final short numberOfChunks;

  private final MapDBMapWrapper<Long, long[]> map;

  @SuppressWarnings("unchecked")
  public MapDBGraphStatisticsDatabase(MapDBStorageOptions storageType,
          MapDBDataStructureOptions dataStructure, String dir, boolean useTransactions,
          boolean writeAsynchronously, MapDBCacheOptions cacheType, short numberOfChunks) {
    File statisticsDir = new File(dir);
    if (!statisticsDir.exists()) {
      statisticsDir.mkdirs();
    }

    this.numberOfChunks = numberOfChunks;
    persistenceFile = new File(dir + File.separatorChar + "persistence.bin");
    if (persistenceFile.exists()) {
      loadPersistenceFile();
    } else {
      numberOfTriplesPerChunk = new long[numberOfChunks];
      ownerLoad = new long[numberOfChunks];
    }

    try {
      switch (dataStructure) {
        case B_TREE_MAP:
          map = new BTreeMapWrapper<>(storageType,
                  statisticsDir.getAbsolutePath() + File.separatorChar + "statistics.db",
                  useTransactions, writeAsynchronously, cacheType, "decoder",
                  BasicKeySerializer.BASIC, Serializer.LONG_ARRAY, true);
          break;
        case HASH_TREE_MAP:
        default:
          map = new HashTreeMapWrapper<>(storageType,
                  statisticsDir.getAbsolutePath() + File.separatorChar + "statistics.db",
                  useTransactions, writeAsynchronously, cacheType, "decoder", Serializer.LONG,
                  Serializer.LONG_ARRAY);
      }
    } catch (Throwable e) {
      close();
      throw e;
    }
  }

  private void loadPersistenceFile() {
    try (DataInputStream in = new DataInputStream(
            new BufferedInputStream(new FileInputStream(persistenceFile)));) {
      numberOfTriplesPerChunk = readLongArray(in);
      ownerLoad = readLongArray(in);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  private long[] readLongArray(DataInputStream in) throws IOException {
    long[] result;
    short length = in.readShort();
    result = new long[length];
    for (int i = 0; i < result.length; i++) {
      result[i] = in.readLong();
    }
    return result;
  }

  @Override
  public void incrementSubjectCount(long subject, int chunk) {
    incrementValue(subject, chunk);
  }

  @Override
  public void incrementPropertyCount(long property, int chunk) {
    incrementValue(property, numberOfChunks + chunk);
  }

  @Override
  public void incrementObjectCount(long object, int chunk) {
    incrementValue(object, 2 * numberOfChunks + chunk);
  }

  @Override
  public void incrementRessourceOccurrences(long resource, int chunk) {
    incrementValue(resource, 3 * numberOfChunks);
  }

  private void incrementValue(long resourceID, int column) {
    try {
      long[] statistics = map.get(resourceID);
      if (statistics == null) {
        statistics = new long[3 * numberOfChunks + 1];
        int owner = (int) (resourceID >>> (Long.SIZE - Short.SIZE));
        ownerLoad[owner]++;
      }
      statistics[column]++;
      // MapDB does not detect changes in array automatically
      map.put(resourceID, statistics);
    } catch (Throwable e) {
      close();
      throw e;
    }
  }

  @Override
  public void incrementNumberOfTriplesPerChunk(int chunk) {
    numberOfTriplesPerChunk[chunk]++;
  }

  @Override
  public long[] getStatisticsForResource(long id) {
    long[] statistics = map.get(id);
    if (statistics == null) {
      return null;
    }
    return Arrays.copyOf(statistics, statistics.length);
  }

  @Override
  public long[] getChunkSizes() {
    return Arrays.copyOf(numberOfTriplesPerChunk, numberOfTriplesPerChunk.length);
  }

  @Override
  public long[] getOwnerLoad() {
    return Arrays.copyOf(ownerLoad, ownerLoad.length);
  }

  @Override
  public long setOwner(long oldID, short owner) {
    short oldOwner = (short) (oldID >>> 48);
    if (oldOwner != 0 && oldOwner != owner) {
      throw new IllegalArgumentException(
              "the first two bytes of the id must be 0 or equal to the new owner " + owner);
    }
    if (oldOwner == owner) {
      return oldID;
    }
    long newID = owner;
    newID = newID << 48;
    newID |= oldID;

    try {
      long[] statistics = map.get(oldID);
      map.remove(oldID);
      map.put(newID, statistics);
    } catch (Throwable e) {
      close();
      throw e;
    }
    ownerLoad[oldOwner & 0x00_00_ff_ff]--;
    ownerLoad[owner & 0x00_00_ff_ff]++;

    return newID;
  }

  @Override
  public void clear() {
    if (persistenceFile.exists()) {
      persistenceFile.delete();
    }
    for (int i = 0; i < numberOfTriplesPerChunk.length; i++) {
      numberOfTriplesPerChunk[i] = 0;
      ownerLoad[i] = 0;
    }
    map.clear();
  }

  @Override
  public void close() {
    persistNumberOfTriplesPerChunk();
    map.close();
  }

  private void persistNumberOfTriplesPerChunk() {
    try (DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(persistenceFile)));) {
      // persist number of triples per chunk
      out.writeShort((short) numberOfTriplesPerChunk.length);
      for (long l : numberOfTriplesPerChunk) {
        out.writeLong(l);
      }
      // persist ownerLoad
      out.writeShort((short) ownerLoad.length);
      for (long l : ownerLoad) {
        out.writeLong(l);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("TriplesPerChunk ");
    for (long l : numberOfTriplesPerChunk) {
      sb.append("\t").append(l);
    }
    sb.append("\n");
    sb.append("OwnerLoad ");
    for (long l : ownerLoad) {
      sb.append("\t").append(l);
    }
    sb.append("\n");
    sb.append("ResourceID");
    for (int i = 0; i < numberOfChunks; i++) {
      sb.append(";").append("subjectInChunk").append(i);
    }
    for (int i = 0; i < numberOfChunks; i++) {
      sb.append(";").append("propertyInChunk").append(i);
    }
    for (int i = 0; i < numberOfChunks; i++) {
      sb.append(";").append("objectInChunk").append(i);
    }
    sb.append(";").append("overallOccurrance");
    for (Entry<Long, long[]> entry : map.entrySet()) {
      sb.append("\n");
      sb.append(entry.getKey());
      for (long value : entry.getValue()) {
        sb.append(";").append(value);
      }
    }
    return sb.toString();
  }

}