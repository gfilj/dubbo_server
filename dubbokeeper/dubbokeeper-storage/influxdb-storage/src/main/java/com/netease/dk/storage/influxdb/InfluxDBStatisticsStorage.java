package com.netease.dk.storage.influxdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.dubboclub.dk.storage.StatisticsStorage;
import com.dubboclub.dk.storage.model.ApplicationInfo;
import com.dubboclub.dk.storage.model.MethodMonitorOverview;
import com.dubboclub.dk.storage.model.ServiceInfo;
import com.dubboclub.dk.storage.model.Statistics;
import com.dubboclub.dk.storage.model.StatisticsOverview;

public class InfluxDBStatisticsStorage implements StatisticsStorage {

	private static final ConcurrentHashMap<String, ApplicationIndexWriter> LUCENE_WRITER_MAP = new ConcurrentHashMap<String, ApplicationIndexWriter>();

	private static Logger logger = LoggerFactory.getLogger(InfluxDBStatisticsStorage.class);

	private volatile boolean running = true;

	private InfluxDbDao dkInfluxdbDao;

	public InfluxDbDao getDkInfluxdbDao() {
		return dkInfluxdbDao;
	}

	public void setDkInfluxdbDao(InfluxDbDao dkInfluxdbDao) {
		this.dkInfluxdbDao = dkInfluxdbDao;
	}

	class ApplicationIndexWriter extends Thread {

		private String application;

		private ConcurrentLinkedQueue<Statistics> statisticses;

		private volatile long maxElapsed;

		private volatile long maxConcurrent;

		private volatile int maxFault;

		private volatile int maxSuccess;

		private volatile boolean running = false;

		public ApplicationIndexWriter(String application) {
			this.application = application;
			statisticses = new ConcurrentLinkedQueue<Statistics>();
			running = true;
			init();
			this.setName(application + "-IndexWriter");
		}

		private void init() {
			if (running) {
				long end = System.currentTimeMillis();
				long start = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
				Long[] result = dkInfluxdbDao.getMaxInfo(application, start, end);
				maxConcurrent = result[0] != null ? result[0].longValue() : 0L;
				maxElapsed = result[1] != null ? result[1].longValue() : 0L;
				maxFault = result[2] != null ? result[2].intValue() : 0;
				maxSuccess = result[3] != null ? result[3].intValue() : 0;
			}
		}

		public void offerStatistics(Statistics statistics) {
			statisticses.offer(statistics);
		}

		public void close() {
			running = false;
		}

		@Override
		public void run() {
			while (running) {
				try {
					Statistics statistics = statisticses.poll();
					if (statistics == null) {// queue is empty
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// do nothing
						}
						continue;
					}
					if (maxFault < statistics.getFailureCount()) {
						maxFault = statistics.getFailureCount();
					}
					if (maxSuccess < statistics.getSuccessCount()) {
						maxSuccess = statistics.getSuccessCount();
					}
					if (maxConcurrent < statistics.getConcurrent()) {
						maxConcurrent = statistics.getConcurrent();
					}
					if (maxElapsed < statistics.getElapsed()) {
						maxElapsed = statistics.getElapsed();
					}
					dkInfluxdbDao.saveStatistics(statistics);
					// 未来考虑是否能够做批量保存...

				} catch (Exception e) {
					logger.error("Failed to add statistics to lucene.", e);
				}
			}
		}

		public String getApplication() {
			return application;
		}

		public long getMaxElapsed() {
			return maxElapsed;
		}

		public long getMaxConcurrent() {
			return maxConcurrent;
		}

		public int getMaxFault() {
			return maxFault;
		}

		public int getMaxSuccess() {
			return maxSuccess;
		}
	}

	@Override
	public void storeStatistics(Statistics statistics) {
		// TODO Auto-generated method stub
		if (!running) {
			return;
		}
		try {
			if (!LUCENE_WRITER_MAP.containsKey(statistics.getApplication())) {
				ApplicationIndexWriter applicationIndexWriter = new ApplicationIndexWriter(statistics.getApplication());
				applicationIndexWriter = LUCENE_WRITER_MAP.putIfAbsent(statistics.getApplication(),
						applicationIndexWriter);
				if (applicationIndexWriter == null) {
					LUCENE_WRITER_MAP.get(statistics.getApplication()).start();
				}
				dkInfluxdbDao.insertApplicationNameOrNot(statistics.getApplication());
			}
			ApplicationIndexWriter indexWriter = LUCENE_WRITER_MAP.get(statistics.getApplication());
			indexWriter.offerStatistics(statistics);
		} catch (Exception e) {
			logger.error("failed to store statistics info", e);
		}
	}

	@Override
	public List<Statistics> queryStatisticsForMethod(String application, String serviceInterface, String method,
			long startTime, long endTime) {
		// TODO Auto-generated method stub
		return dkInfluxdbDao.queryStatisticsForMethod(application, serviceInterface, method, startTime, endTime);
	}

	@Override
	public Collection<MethodMonitorOverview> queryMethodMonitorOverview(String application, String serviceInterface,
			int methodSize, long startTime, long endTime) {
		// TODO Auto-generated method stub
		return dkInfluxdbDao.queryMethodMonitorOverview(application, serviceInterface, methodSize, startTime, endTime);
	}

	@Override
	public Collection<ApplicationInfo> queryApplications() {
		// TODO Auto-generated method stub
		List<String> names = dkInfluxdbDao.queryAllApplicationName();
		for (String name : names) {
			if (!LUCENE_WRITER_MAP.containsKey(name)) {
				ApplicationIndexWriter applicationIndexWriter = new ApplicationIndexWriter(name);
				applicationIndexWriter = LUCENE_WRITER_MAP.putIfAbsent(name, applicationIndexWriter);
				if (applicationIndexWriter == null) {
					LUCENE_WRITER_MAP.get(name).start();
				}
			}
		}
		Collection<ApplicationIndexWriter> applicationIndexWriters = LUCENE_WRITER_MAP.values();
		List<ApplicationInfo> applicationInfos = new ArrayList<ApplicationInfo>(applicationIndexWriters.size());
		for (ApplicationIndexWriter writer : applicationIndexWriters) {
			ApplicationInfo applicationInfo = new ApplicationInfo();
			applicationInfo.setApplicationName(writer.getApplication());
			applicationInfo.setMaxElapsed(writer.getMaxElapsed());
			applicationInfo.setMaxSuccess(writer.getMaxSuccess());
			applicationInfo.setMaxConcurrent(writer.getMaxConcurrent());
			applicationInfo.setMaxFault(writer.getMaxFault());
			applicationInfo.setApplicationType(queryApplicationType(writer.getApplication()));
			applicationInfos.add(applicationInfo);
		}

		return applicationInfos;
	}

	private int queryApplicationType(String application) {
		return dkInfluxdbDao.queryApplicationType(application);
	}

	@Override
	public ApplicationInfo queryApplicationInfo(String application, long start, long end) {
		ApplicationInfo applicationInfo = new ApplicationInfo();
		applicationInfo.setApplicationName(application);
		Long[] result = dkInfluxdbDao.getMaxInfo(application, start, end);
		applicationInfo.setMaxConcurrent(result[0] != null ? result[0].longValue() : 0L);
		applicationInfo.setMaxElapsed(result[1] != null ? result[1].longValue() : 0L);
		applicationInfo.setMaxFault(result[2] != null ? result[2].intValue() : 0);
		applicationInfo.setMaxSuccess(result[3] != null ? result[3].intValue() : 0);
		applicationInfo.setApplicationType(queryApplicationType(application));
		return applicationInfo;
	}

	@Override
	public StatisticsOverview queryApplicationOverview(String application, long start, long end) {
		// TODO Auto-generated method stub
		return dkInfluxdbDao.queryApplicationOverview(application, start, end);
	}

	@Override
	public StatisticsOverview queryServiceOverview(String application, String service, long start, long end) {
		// TODO Auto-generated method stub
		return dkInfluxdbDao.queryServiceOverview(application, service, start, end);
	}

	@Override
	public Collection<ServiceInfo> queryServiceByApp(String application, long start, long end) {
		// TODO Auto-generated method stub
		return dkInfluxdbDao.queryServiceByApp(application, start, end);
	}

}
