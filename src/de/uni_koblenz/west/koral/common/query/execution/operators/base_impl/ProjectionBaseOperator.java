package de.uni_koblenz.west.koral.common.query.execution.operators.base_impl;

import de.uni_koblenz.west.koral.common.query.Mapping;
import de.uni_koblenz.west.koral.common.query.execution.QueryOperatorTask;
import de.uni_koblenz.west.koral.common.query.execution.operators.ProjectionOperator;

import java.io.File;

public class ProjectionBaseOperator extends ProjectionOperator {

  public ProjectionBaseOperator(long id, long coordinatorId, int numberOfSlaves, int cacheSize,
          File cacheDirectory, int emittedMappingsPerRound, long[] resultVars,
          QueryOperatorTask subOperation) {
    super(id, coordinatorId, numberOfSlaves, cacheSize, cacheDirectory, emittedMappingsPerRound,
            resultVars, subOperation);
  }

  public ProjectionBaseOperator(short slaveId, int queryId, short taskId, long coordinatorId,
          int numberOfSlaves, int cacheSize, File cacheDirectory, int emittedMappingsPerRound,
          long[] resultVars, QueryOperatorTask subOperation) {
    super(slaveId, queryId, taskId, coordinatorId, numberOfSlaves, cacheSize, cacheDirectory,
            emittedMappingsPerRound, resultVars, subOperation);
  }

  @Override
  protected void emitMapping(Mapping mapping) {
    numberOfEmittedMappings++;
    if (getParentTask() == null) {
      messageSender.sendQueryMapping(mapping, getID(), getCoordinatorID(), recycleCache);
    } else {
      // send to computer with smallest id
      long parentBaseID = getParentTask().getID() & 0x00_00_FF_FF_FF_FF_FF_FFl;
      messageSender.sendQueryMapping(mapping, getID(), parentBaseID | 0x00_01_00_00_00_00_00_00l,
              recycleCache);
    }
  }

}