package ru.babobka.nodeServer.model;

import ru.babobka.nodeServer.Server;
import ru.babobka.nodeServer.exception.EmptyClusterException;
import ru.babobka.nodeServer.thread.ClientThread;
import ru.babobka.nodeServer.util.DistributionUtil;
import ru.babobka.nodeserials.NodeRequest;
import ru.babobka.nodeserials.NodeResponse;
import ru.babobka.subtask.model.SubTask;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Level;

/**
 * Created by dolgopolov.a on 03.08.15.
 */
public final class ResponsesArray {

	private final AtomicInteger size;

	private final int maxSize;

	private final SubTask subTask;

	private final ResponsesArrayMeta meta;

	private static final String TASK = "Task";

	private final AtomicReferenceArray<NodeResponse> responseArray;

	public ResponsesArray(int maxSize, String taskName, SubTask subTask, Map<String, String> params) {
		this.maxSize = maxSize;
		this.responseArray = new AtomicReferenceArray<>(maxSize);
		this.subTask = subTask;
		this.meta = new ResponsesArrayMeta(taskName, params, System.currentTimeMillis());
		size = new AtomicInteger(0);
	}

	public boolean isComplete() {
		for (int i = 0; i < responseArray.length(); i++) {
			if (responseArray.get(i) == null) {
				return false;
			}
		}
		return true;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public synchronized boolean add(NodeResponse response) {

		int corruptedResponseCount = 0;
		if (size.intValue() >= responseArray.length()) {
			return false;
		} else {
			for (int i = 0; i < responseArray.length(); i++) {
				if (responseArray.get(i) == null) {
					responseArray.set(i, response);
					if (response.isStopped()) {
						corruptedResponseCount++;
					}
					size.incrementAndGet();
					if (size.intValue() == responseArray.length()) {
						this.notifyAll();
						if (corruptedResponseCount == size.intValue()) {
							Server.getLogger().log(Level.INFO, TASK + " " + response.getTaskId() + " was canceled");
						} else {
							Server.getLogger().log(Level.INFO, TASK + " " + response.getTaskId() + " is ready ");
						}
					} else if (subTask.isRaceStyle() && subTask.getReducer().isValidResponse(response)) {
						List<ClientThread> clientThreads = Server.getClientThreads()
								.getListByTaskId(response.getTaskId());
						try {
							if (clientThreads.size() > 1) {
								DistributionUtil.broadcastStopRequests(clientThreads,
										new NodeRequest(response.getTaskId(), true, response.getTaskName()));
							}
						} catch (EmptyClusterException e) {
							Server.getLogger().log(Level.SEVERE, e);
						}

					}

					break;

				}
			}
			return true;
		}

	}

	synchronized void fill(NodeResponse response) {

		if (size.intValue() <= responseArray.length()) {
			for (int i = 0; i < responseArray.length(); i++) {
				if (responseArray.get(i) == null) {
					responseArray.set(i, response);
					size.incrementAndGet();
					if (size.intValue() == responseArray.length()) {
						this.notifyAll();
						Server.getLogger().log(Level.INFO,
								TASK + " " + response.getTaskId() + " is ready due to filling");
						break;
					}
				}
			}
		}

	}

	public synchronized List<NodeResponse> getResponseList() {
		LinkedList<NodeResponse> responses = new LinkedList<>();
		try {
			while (size.intValue() != responseArray.length()) {
				this.wait();
			}
			for (int i = 0; i < responseArray.length(); i++) {
				responses.add((NodeResponse) responseArray.get(i));
			}
			return responses;
		} catch (InterruptedException e) {
			Server.getLogger().log(e);
		}
		return responses;

	}

	public ResponsesArrayMeta getMeta() {
		return meta;
	}

	@Override
	public String toString() {
		return meta.getTaskName();
	}

}