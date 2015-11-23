package de.uni_koblenz.west.cidre.common.executor;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import de.uni_koblenz.west.cidre.common.executor.messagePassing.MessageSenderBuffer;
import de.uni_koblenz.west.cidre.common.query.Mapping;
import de.uni_koblenz.west.cidre.common.query.MappingRecycleCache;
import de.uni_koblenz.west.cidre.common.query.execution.QueryOperatorBase;
import de.uni_koblenz.west.cidre.common.utils.CachedFileReceiverQueue;

/**
 * A base implementation of the {@link WorkerTask}. It implements the following
 * features:
 * <ul>
 * <li>a unique id</li>
 * <li>handling of child tasks</li>
 * <li>providing an arbitrary number of input queues for incoming messages</li>
 * <ul>
 * 
 * @author Daniel Janke &lt;danijankATuni-koblenz.de&gt;
 *
 */
public abstract class WorkerTaskBase implements WorkerTask {

	protected Logger logger;

	private final long id;

	private CachedFileReceiverQueue[] inputQueues;

	private final int cacheSize;

	private final File cacheDirectory;

	private WorkerTask[] children;

	public WorkerTaskBase(long id, int cacheSize, File cacheDirectory) {
		this.id = id;
		this.cacheSize = cacheSize;
		this.cacheDirectory = new File(cacheDirectory.getAbsolutePath()
				+ File.separatorChar + "workerTask_" + this.id);
	}

	@Override
	public void setUp(MessageSenderBuffer messageSender,
			MappingRecycleCache recycleCache, Logger logger) {
		this.logger = logger;
	}

	@Override
	public long getID() {
		return id;
	}

	protected void addInputQueue() {
		if (inputQueues == null || inputQueues.length == 0) {
			inputQueues = new CachedFileReceiverQueue[1];
		} else {
			CachedFileReceiverQueue[] newInputQueues = new CachedFileReceiverQueue[inputQueues.length
					+ 1];
			for (int i = 0; i < inputQueues.length; i++) {
				newInputQueues[i] = inputQueues[i];
			}
			inputQueues = newInputQueues;
		}
		inputQueues[inputQueues.length - 1] = new CachedFileReceiverQueue(
				cacheSize, cacheDirectory, inputQueues.length - 1);
	}

	@Override
	public boolean hasInput() {
		for (CachedFileReceiverQueue queue : inputQueues) {
			if (!queue.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	protected long getSizeOfInputQueue(int inputQueueIndex) {
		return inputQueues[inputQueueIndex].size();
	}

	protected void enqueuMessageInternal(int inputQueueIndex, byte[] message,
			int firstIndex, int length) {
		inputQueues[inputQueueIndex].enqueue(message, firstIndex, length);
	}

	protected Mapping consumeMapping(int inputQueueIndex,
			MappingRecycleCache recycleCache) {
		return inputQueues[inputQueueIndex].dequeue(recycleCache);
	}

	protected boolean isInputQueueEmpty(int inputQueueIndex) {
		return inputQueues[inputQueueIndex].isEmpty();
	}

	public int addChildTask(WorkerTask child) {
		int id = 0;
		if (children == null || children.length == 0) {
			children = new WorkerTask[1];
		} else {
			WorkerTask[] newChildren = new WorkerTask[children.length + 1];
			for (int i = 0; i < children.length; i++) {
				newChildren[i] = children[i];
			}
			children = newChildren;
			id = children.length - 1;
		}
		children[id] = child;
		addInputQueue();
		return id;
	}

	@Override
	public Set<WorkerTask> getPrecedingTasks() {
		Set<WorkerTask> precedingTasks = new HashSet<>();
		for (WorkerTask child : children) {
			precedingTasks.add(child);
		}
		return precedingTasks;
	}

	protected int getIndexOfChild(long childId) {
		for (int childIndex = 0; childIndex < children.length; childIndex++) {
			if (children[childIndex].getID() == childId) {
				return childIndex;
			}
		}
		return -1;
	}

	/**
	 * Called by subclasses of {@link QueryOperatorBase}.
	 * 
	 * @param child
	 * @return <code>true</code> if all {@link Mapping}s of <code>child</code>
	 *         have been processed and the child operation is finished.
	 */
	protected boolean hasChildFinished(int child) {
		return isInputQueueEmpty(child) && children[child].hasFinished();
	}

	protected boolean areAllChildrenFinished() {
		for (WorkerTask child : children) {
			if (!child.hasFinished()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void close() {
		for (CachedFileReceiverQueue queue : inputQueues) {
			queue.close();
		}
	}

	@Override
	public String toString() {
		return getClass().getName() + "[id=" + id + "(slave="
				+ (id >>> (Integer.SIZE + Short.SIZE)) + " query="
				+ ((id << Short.SIZE) >>> (Short.SIZE + Short.SIZE)) + " task="
				+ ((id << (Short.SIZE + Integer.SIZE)) >>> (Short.SIZE
						+ Integer.SIZE))
				+ ")]";
	}

}
