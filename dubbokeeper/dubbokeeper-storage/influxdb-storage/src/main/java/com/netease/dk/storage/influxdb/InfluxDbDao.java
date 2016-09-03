package com.netease.dk.storage.influxdb;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Series;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.dubboclub.dk.monitor.DubboKeeperMonitorService;
import com.dubboclub.dk.storage.model.BaseItem;
import com.dubboclub.dk.storage.model.ConcurrentItem;
import com.dubboclub.dk.storage.model.ElapsedItem;
import com.dubboclub.dk.storage.model.FaultItem;
import com.dubboclub.dk.storage.model.MethodMonitorOverview;
import com.dubboclub.dk.storage.model.ServiceInfo;
import com.dubboclub.dk.storage.model.Statistics;
import com.dubboclub.dk.storage.model.StatisticsOverview;
import com.dubboclub.dk.storage.model.SuccessItem;
import com.dubboclub.dk.storage.model.Statistics.ApplicationType;

public class InfluxDbDao {

	private String ipaddress;

	private String user;

	private String password;

	private String databaseName;

	private InfluxDB influxDB;

	private String measurementName = "statistics";

	private String applicationTableName = "application";

	private static final int MAX_GROUP_SIZE = 100000;

	private static Logger logger = LoggerFactory.getLogger(InfluxDbDao.class);

	private static ThreadLocal<SimpleDateFormat> dateformat = new ThreadLocal<SimpleDateFormat>() {
		public SimpleDateFormat initialValue() {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			return sdf;
		}
	};
	/*
	 * private static ThreadLocal<SimpleDateFormat> readdateformat = new
	 * ThreadLocal<SimpleDateFormat>() { public SimpleDateFormat initialValue()
	 * { SimpleDateFormat sdf = new
	 * SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	 * sdf.setTimeZone(TimeZone.getTimeZone("UTC")); return sdf; } };
	 */
	/*
	 * private Date computUTCTime(long time){ Calendar
	 * cal=Calendar.getInstance(); cal.setTime(new Date(time)); //2、取得时间偏移量： int
	 * zoneOffset = cal.get(Calendar.ZONE_OFFSET); //3、取得夏令时差： int dstOffset =
	 * cal.get(Calendar.DST_OFFSET); //4、从本地时间里扣除这些差量，即可以取得UTC时间：
	 * cal.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset)); return new
	 * Date(cal.getTimeInMillis()); }
	 */

	public InfluxDbDao(String ipaddress, String user, String password, String databaseName) {
		this.ipaddress = ipaddress;
		this.user = user;
		this.password = password;
		this.databaseName = databaseName;

		influxDB = InfluxDBFactory.connect("http://" + this.ipaddress, this.user, this.password);
		createDatabase();
	}

	public InfluxDB createDatabase() {
		influxDB.createDatabase(this.databaseName);
		influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS);
		return influxDB;
	}

	public List<String> queryAllApplicationName() {
		List<String> names = new ArrayList<String>();
		try {
			Query query = new Query("SELECT * from " + this.applicationTableName, databaseName);
			QueryResult result = influxDB.query(query);
			if (result != null && result.getResults() != null && result.getResults().size() > 0
					&& result.getResults().get(0).getSeries() != null
					&& result.getResults().get(0).getSeries().size() > 0
							&& result.getResults().get(0).getSeries().get(0).getValues() != null
							&& result.getResults().get(0).getSeries().get(0).getValues().size() > 0) {
				List<List<Object>> values = result.getResults().get(0).getSeries().get(0).getValues();
				for (List<Object> value : values) {
					names.add((String) value.get(1));
				}
				return names;
			}
		} catch (Exception e) {
			logger.error("Failed to read application table.", e);
		}
		return names;
	}

	public void insertApplicationNameOrNot(String applicationName) {
		try {
			Query query = new Query(
					"SELECT * from " + this.applicationTableName + " where name='" + applicationName + "'",
					databaseName);
			QueryResult result = influxDB.query(query);
			if (result != null && result.getResults() != null && result.getResults().size() > 0
					&& result.getResults().get(0).getSeries() != null
					&& result.getResults().get(0).getSeries().size() > 0
							&& result.getResults().get(0).getSeries().get(0).getValues() != null
							&& result.getResults().get(0).getSeries().get(0).getValues().size() > 0) {
				return;
			}
		} catch (Exception e) {
			logger.error("Failed to read application table.", e);
		}
		BatchPoints batchPoints = BatchPoints.database(this.databaseName).retentionPolicy("default")
				.consistency(ConsistencyLevel.ALL).build();
		Point.Builder pb = Point.measurement(this.applicationTableName).tag("name", applicationName).addField("value", 1);
		batchPoints.point(pb.build());
		this.influxDB.write(batchPoints);
	}

	public void saveStatistics(Statistics statistics) {
		if (statistics == null)
			return;

		Point.Builder pb = Point.measurement(this.measurementName)
				.tag(DubboKeeperMonitorService.APPLICATION, statistics.getApplication())
				.tag(DubboKeeperMonitorService.INTERFACE, statistics.getServiceInterface())
				.tag(DubboKeeperMonitorService.METHOD, statistics.getMethod())
				.tag(DubboKeeperMonitorService.REMOTE_ADDRESS, statistics.getRemoteAddress())
				.tag(DubboKeeperMonitorService.REMOTE_TYPE, statistics.getRemoteType().toString())
				.tag(DubboKeeperMonitorService.APPLICATION_TYPE, statistics.getType().toString())
				.tag(DubboKeeperMonitorService.HOST_KEY, statistics.getHost())
				.time(statistics.getTimestamp(), TimeUnit.MILLISECONDS)
				.addField(DubboKeeperMonitorService.CONCURRENT, statistics.getConcurrent())
				.addField(DubboKeeperMonitorService.ELAPSED, statistics.getElapsed())
				.addField(DubboKeeperMonitorService.INPUT, statistics.getInput())
				.addField(DubboKeeperMonitorService.OUTPUT, statistics.getOutput())
				.addField(DubboKeeperMonitorService.FAILURE, statistics.getFailureCount())
				.addField(DubboKeeperMonitorService.SUCCESS, statistics.getSuccessCount())
				.addField(DubboKeeperMonitorService.FAILURE, statistics.getFailureCount())
				.addField(DubboKeeperMonitorService.TPS, statistics.getTps())
				.addField(DubboKeeperMonitorService.KBPS, statistics.getKbps());
		influxDB.write(this.databaseName, "default", pb.build());
	}

	public Long[] getMaxInfo(String application, long startTime, long endTime) {
		Date start = new Date(startTime);
		Date end = new Date(endTime);
		Query query = new Query("SELECT MAX(" + DubboKeeperMonitorService.CONCURRENT + "), MAX("
				+ DubboKeeperMonitorService.ELAPSED + "), MAX(" + DubboKeeperMonitorService.FAILURE + "), MAX("
				+ DubboKeeperMonitorService.SUCCESS + ") FROM " + this.measurementName + " WHERE "
				+ DubboKeeperMonitorService.APPLICATION + " = '" + application + "' and time >= '"
				+ dateformat.get().format(start) + "' and time <= '" + dateformat.get().format(end) + "'",
				databaseName);
		// System.out.println("getMaxInfo--> " + query.getCommand());
		QueryResult result = influxDB.query(query);
		Long[] ll = new Long[4];
		if (result == null || result.getResults() == null || result.getResults().size() <= 0
				|| result.getResults().get(0).getSeries() == null || result.getResults().get(0).getSeries().size() <= 0
				|| result.getResults().get(0).getSeries().get(0).getValues() == null
				|| result.getResults().get(0).getSeries().get(0).getValues().size() <= 0) {
			return ll;
		}
		List<Object> values = result.getResults().get(0).getSeries().get(0).getValues().get(0);
		if(values.get(1)!=null){
			ll[0] = ((Double) values.get(1)).longValue();
		}else{
			ll[0] = 0L;
		}
		if(values.get(2)!=null){
			ll[1] = ((Double) values.get(2)).longValue();
		}else{
			ll[1] = 0L;
		}
		if(values.get(3)!=null){
			ll[2] = ((Double) values.get(3)).longValue();
		}else{
			ll[2] = 0L;
		}
		if(values.get(4)!=null){
			ll[3] = ((Double) values.get(4)).longValue();
		}else{
			ll[3] = 0L;
		}
		return ll;
	}

	private Map<String, Object> getColumnValueMapping(List<String> columns, List<Object> value) {
		Map<String, Object> map = new HashMap<String, Object>();
		int index = 0;
		for (String colname : columns) {
			map.put(colname, value.get(index));
			index++;
		}
		return map;
	}

	public List<Statistics> queryStatisticsForMethod(String application, String serviceInterface, String method,
			long startTime, long endTime) {
		List<Statistics> statisticsList = new ArrayList<Statistics>();
		Date start = new Date(startTime);
		Date end = new Date(endTime);
		Query query = new Query(
				"SELECT * FROM " + this.measurementName + " WHERE " + DubboKeeperMonitorService.APPLICATION + " = '"
						+ application + "' and " + DubboKeeperMonitorService.INTERFACE + " ='" + serviceInterface
						+ "' and " + DubboKeeperMonitorService.METHOD + " ='" + method + "' and time >= '"
						+ dateformat.get().format(start) + "' and time <= '" + dateformat.get().format(end) + "'",
				databaseName);
		// System.out.println("queryStatisticsForMethod--> " +
		// query.getCommand());
		QueryResult result = influxDB.query(query);
		if (result == null || result.getResults() == null || result.getResults().size() <= 0
				|| result.getResults().get(0).getSeries() == null || result.getResults().get(0).getSeries().size() <= 0
				|| result.getResults().get(0).getSeries().get(0).getValues() == null
				|| result.getResults().get(0).getSeries().get(0).getValues().size() <= 0) {
			return statisticsList;
		}
		List<String> columns = result.getResults().get(0).getSeries().get(0).getColumns();
		List<List<Object>> values = result.getResults().get(0).getSeries().get(0).getValues();
		for (List<Object> value : values) {
			Map<String, Object> map = getColumnValueMapping(columns, value);
			Statistics statistics = new Statistics();
			statistics.setApplication((String) map.get(DubboKeeperMonitorService.APPLICATION));
			statistics.setConcurrent(((Double) map.get(DubboKeeperMonitorService.CONCURRENT)).longValue());
			statistics.setElapsed(((Double) map.get(DubboKeeperMonitorService.ELAPSED)).longValue());
			statistics.setHost((String) map.get(DubboKeeperMonitorService.HOST_KEY));
			statistics.setInput(((Double) map.get(DubboKeeperMonitorService.INPUT)).longValue());
			statistics.setOutput(((Double) map.get(DubboKeeperMonitorService.OUTPUT)).longValue());
			statistics.setRemoteAddress((String) map.get(DubboKeeperMonitorService.REMOTE_ADDRESS));
			statistics.setRemoteType(ApplicationType.valueOf((String) map.get(DubboKeeperMonitorService.REMOTE_TYPE)));
			try {
				statistics.setTimestamp(dateformat.get().parse((String) map.get("time")).getTime());
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				logger.error("Failed to read statistics timestamp.", e);
			}
			statistics.setServiceInterface((String) map.get(DubboKeeperMonitorService.INTERFACE));
			statistics.setKbps((Double) map.get(DubboKeeperMonitorService.KBPS));
			statistics.setTps((Double) map.get(DubboKeeperMonitorService.TPS));
			statistics.setFailureCount(((Double) map.get(DubboKeeperMonitorService.FAILURE)).intValue());
			statistics.setSuccessCount(((Double) map.get(DubboKeeperMonitorService.SUCCESS)).intValue());
			statistics.setMethod((String) map.get(DubboKeeperMonitorService.METHOD));
			statistics.setType(ApplicationType.valueOf((String) map.get(DubboKeeperMonitorService.APPLICATION_TYPE)));
			statisticsList.add(statistics);
		}
		return statisticsList;
	}

	public Collection<MethodMonitorOverview> queryMethodMonitorOverview(String application, String serviceInterface,
			int methodSize, long startTime, long endTime) {
		List<MethodMonitorOverview> methodMonitorOverviews = new ArrayList<MethodMonitorOverview>();
		Date start = new Date(startTime);
		Date end = new Date(endTime);
		int groupsize = methodSize == 0 ? MAX_GROUP_SIZE : methodSize;
		Query query = new Query("SELECT MAX(" + DubboKeeperMonitorService.ELAPSED + "), MIN("
				+ DubboKeeperMonitorService.ELAPSED + "), MAX(" + DubboKeeperMonitorService.CONCURRENT + "), MIN("
				+ DubboKeeperMonitorService.CONCURRENT + "),MAX(" + DubboKeeperMonitorService.INPUT + "), MIN("
				+ DubboKeeperMonitorService.INPUT + "), MAX(" + DubboKeeperMonitorService.OUTPUT + "), MIN("
				+ DubboKeeperMonitorService.OUTPUT + "), MAX(" + DubboKeeperMonitorService.FAILURE + "), MIN("
				+ DubboKeeperMonitorService.FAILURE + "), MAX(" + DubboKeeperMonitorService.SUCCESS + "), MIN("
				+ DubboKeeperMonitorService.SUCCESS + "), MAX(" + DubboKeeperMonitorService.TPS + "), MIN("
				+ DubboKeeperMonitorService.TPS + "), MAX(" + DubboKeeperMonitorService.KBPS + "), MIN("
				+ DubboKeeperMonitorService.KBPS + ") FROM " + this.measurementName + " WHERE "
				+ DubboKeeperMonitorService.APPLICATION + " = '" + application + "' and "
				+ DubboKeeperMonitorService.INTERFACE + " ='" + serviceInterface + "' and time >= '"
				+ dateformat.get().format(start) + "' and time <= '" + dateformat.get().format(end) + "'" + " group by "
				+ DubboKeeperMonitorService.METHOD + " slimit " + groupsize, databaseName);
		// System.out.println("queryMethodMonitorOverview--> " +
		// query.getCommand());
		QueryResult result = influxDB.query(query);
		if (result == null || result.getResults() == null || result.getResults().size() <= 0
				|| result.getResults().get(0).getSeries() == null
				|| result.getResults().get(0).getSeries().size() <= 0) {
			return methodMonitorOverviews;
		}
		List<Series> series = result.getResults().get(0).getSeries();
		for (Series serie : series) {
			List<List<Object>> values = serie.getValues();
			if (values.size() <= 0) {
				continue;
			}
			// System.out.println("values size is:"+values.size()+" value size
			// is:"+values.get(0).size());
			MethodMonitorOverview methodMonitorOverview = new MethodMonitorOverview();
			methodMonitorOverview.setMethod(serie.getTags().get(DubboKeeperMonitorService.METHOD));
			methodMonitorOverview
					.setMaxElapsed(values.get(0).get(1) == null ? 0L : ((Double) values.get(0).get(1)).longValue());
			methodMonitorOverview
					.setMinElapsed(values.get(0).get(2) == null ? 0L : ((Double) values.get(0).get(2)).longValue());
			methodMonitorOverview
					.setMaxConcurrent(values.get(0).get(3) == null ? 0L : ((Double) values.get(0).get(3)).longValue());
			methodMonitorOverview
					.setMinConcurrent(values.get(0).get(4) == null ? 0L : ((Double) values.get(0).get(4)).longValue());
			methodMonitorOverview
					.setMaxInput(values.get(0).get(5) == null ? 0L : ((Double) values.get(0).get(5)).longValue());
			methodMonitorOverview
					.setMinInput(values.get(0).get(6) == null ? 0L : ((Double) values.get(0).get(6)).longValue());
			methodMonitorOverview
					.setMaxOutput(values.get(0).get(7) == null ? 0L : ((Double) values.get(0).get(7)).longValue());
			methodMonitorOverview
					.setMinOutput(values.get(0).get(8) == null ? 0L : ((Double) values.get(0).get(8)).longValue());
			methodMonitorOverview
					.setMaxFailure(values.get(0).get(9) == null ? 0 : ((Double) values.get(0).get(9)).intValue());
			methodMonitorOverview
					.setMinFailure(values.get(0).get(10) == null ? 0 : ((Double) values.get(0).get(10)).intValue());
			methodMonitorOverview
					.setMaxSuccess(values.get(0).get(11) == null ? 0 : ((Double) values.get(0).get(11)).intValue());
			methodMonitorOverview
					.setMinSuccess(values.get(0).get(12) == null ? 0 : ((Double) values.get(0).get(12)).intValue());
			methodMonitorOverview
					.setMaxTps(values.get(0).get(13) == null ? 0L : ((Double) values.get(0).get(13)).longValue());
			methodMonitorOverview
					.setMinTps(values.get(0).get(14) == null ? 0L : ((Double) values.get(0).get(14)).longValue());
			methodMonitorOverview
					.setMaxKbps(values.get(0).get(15) == null ? 0L : ((Double) values.get(0).get(15)).longValue());
			methodMonitorOverview
					.setMinKbps(values.get(0).get(16) == null ? 0L : ((Double) values.get(0).get(16)).longValue());
			methodMonitorOverviews.add(methodMonitorOverview);
		}
		return methodMonitorOverviews;
	}

	public int queryApplicationType(String application) {
		Query query = new Query("SELECT " + DubboKeeperMonitorService.OUTPUT + " FROM " + this.measurementName
				+ " WHERE " + DubboKeeperMonitorService.APPLICATION + " = '" + application + "' group by "
				+ DubboKeeperMonitorService.REMOTE_TYPE + " slimit 2", databaseName);
		// System.out.println("queryApplicationType--> " + query.getCommand());
		QueryResult result = influxDB.query(query);
		if (result == null || result.getResults() == null || result.getResults().size() <= 0
				|| result.getResults().get(0).getSeries() == null
				|| result.getResults().get(0).getSeries().size() <= 0) {
			return -1;
		}
		int size = result.getResults().get(0).getSeries().size();
		if (size == 2) {
			return size;
		}
		for (Series group : result.getResults().get(0).getSeries()) {
			if (Statistics.ApplicationType.PROVIDER.toString()
					.equals(group.getTags().get(DubboKeeperMonitorService.REMOTE_TYPE))) {
				return 0;
			} else {
				return 1;
			}
		}
		return -1;
	}

	public StatisticsOverview queryApplicationOverview(String application, long start, long end) {
		return queryServiceOverview(application, null, start, end);
	}

	public StatisticsOverview queryServiceOverview(String application, String service, long startTime, long endTime) {
		Date start = new Date(startTime);
		Date end = new Date(endTime);
		String serviceFilter = "";
		if (service != null) {
			serviceFilter = " and " + DubboKeeperMonitorService.INTERFACE + " ='" + service + "'";
		}
		int maxSize = 200;
		String query_concurrent = "SELECT TOP(" + DubboKeeperMonitorService.CONCURRENT + "," + maxSize + "),"
				+ DubboKeeperMonitorService.METHOD + "," + DubboKeeperMonitorService.INTERFACE + ","
				+ DubboKeeperMonitorService.REMOTE_TYPE + " FROM " + this.measurementName + " WHERE "
				+ DubboKeeperMonitorService.APPLICATION + " = '" + application + "'" + serviceFilter + " and time >= '"
				+ dateformat.get().format(start) + "' and time <= '" + dateformat.get().format(end) + "'";
		String query_elapsed = "SELECT TOP(" + DubboKeeperMonitorService.ELAPSED + "," + maxSize + "),"
				+ DubboKeeperMonitorService.METHOD + "," + DubboKeeperMonitorService.INTERFACE + ","
				+ DubboKeeperMonitorService.REMOTE_TYPE + " FROM " + this.measurementName + " WHERE "
				+ DubboKeeperMonitorService.APPLICATION + " = '" + application + "'" + serviceFilter + " and time >= '"
				+ dateformat.get().format(start) + "' and time <= '" + dateformat.get().format(end) + "'";
		String query_failure = "SELECT TOP(" + DubboKeeperMonitorService.FAILURE + "," + maxSize + "),"
				+ DubboKeeperMonitorService.METHOD + "," + DubboKeeperMonitorService.INTERFACE + ","
				+ DubboKeeperMonitorService.REMOTE_TYPE + " FROM " + this.measurementName + " WHERE "
				+ DubboKeeperMonitorService.APPLICATION + " = '" + application + "'" + serviceFilter + " and time >= '"
				+ dateformat.get().format(start) + "' and time <= '" + dateformat.get().format(end) + "'";
		String query_success = "SELECT TOP(" + DubboKeeperMonitorService.SUCCESS + "," + maxSize + "),"
				+ DubboKeeperMonitorService.METHOD + "," + DubboKeeperMonitorService.INTERFACE + ","
				+ DubboKeeperMonitorService.REMOTE_TYPE + " FROM " + this.measurementName + " WHERE "
				+ DubboKeeperMonitorService.APPLICATION + " = '" + application + "'" + serviceFilter + " and time >= '"
				+ dateformat.get().format(start) + "' and time <= '" + dateformat.get().format(end) + "'";
		QueryResult result = influxDB.query(new Query(
				query_concurrent + ";" + query_elapsed + ";" + query_failure + ";" + query_success, databaseName));
		// System.out.println("queryServiceOverview--> " + query_concurrent +
		// ";" + query_elapsed + ";" + query_failure
		// + ";" + query_success);
		StatisticsOverview statisticsOverview = new StatisticsOverview();
		// System.out.println("==============================");
		// System.out.println(result.getResults()!=null);
		// System.out.println(result.getResults().get(0)!=null);
		// System.out.println(result.getResults().get(0).getSeries()!=null);
		// System.out.println(result.getResults().get(0).getSeries().get(0)!=null);
		// System.out.println(result.getResults().get(0).getSeries().get(0).getValues()!=null);
		// System.out.println(result.getResults().get(0).getSeries().get(0).getValues().get(0)!=null);
		// System.out.println("==============================");
		if (result == null || result.getResults() == null || result.getResults().size() <= 0) {
			return statisticsOverview;
		}
		List<Object> values = null;
		if (result.getResults().get(0).getSeries().size() > 0
				&& result.getResults().get(0).getSeries().get(0).getValues() != null
				&& result.getResults().get(0).getSeries().get(0).getValues().size() > 0) {
			List<ConcurrentItem> concurrentItems = new ArrayList<ConcurrentItem>(
					result.getResults().get(0).getSeries().get(0).getValues().size());
			statisticsOverview.setConcurrentItems(concurrentItems);
			// System.out.println(concurrentItems.size());
			for (int i = 0; i < result.getResults().get(0).getSeries().get(0).getValues().size(); i++) {
				values = result.getResults().get(0).getSeries().get(0).getValues().get(i);
				ConcurrentItem concurrentItem = new ConcurrentItem();
				convertItem(concurrentItem, values);
				concurrentItem.setConcurrent(((Double) values.get(1)).longValue());
				concurrentItems.add(concurrentItem);
				if (concurrentItem.getConcurrent() < 0) {
					// System.out.println("breaking!");
					break;
				}
			}
		}
		if (result.getResults().get(1).getSeries().size() > 0
				&& result.getResults().get(1).getSeries().get(0).getValues() != null
				&& result.getResults().get(1).getSeries().get(0).getValues().size() > 0) {
			List<ElapsedItem> elapsedItems = new ArrayList<ElapsedItem>(
					result.getResults().get(1).getSeries().get(0).getValues().size());
			statisticsOverview.setElapsedItems(elapsedItems);
			for (int i = 0; i < result.getResults().get(1).getSeries().get(0).getValues().size(); i++) {
				values = result.getResults().get(1).getSeries().get(0).getValues().get(i);
				ElapsedItem elapsedItem = new ElapsedItem();
				convertItem(elapsedItem, values);
				elapsedItem.setElapsed(((Double) values.get(1)).longValue());
				elapsedItems.add(elapsedItem);
				if (elapsedItem.getElapsed() < 0) {
					break;
				}
			}
		}
		if (result.getResults().get(2).getSeries().size() > 0
				&& result.getResults().get(2).getSeries().get(0).getValues() != null
				&& result.getResults().get(2).getSeries().get(0).getValues().size() > 0) {
			List<FaultItem> faultItems = new ArrayList<FaultItem>(
					result.getResults().get(2).getSeries().get(0).getValues().size());
			statisticsOverview.setFaultItems(faultItems);
			for (int i = 0; i < result.getResults().get(2).getSeries().get(0).getValues().size(); i++) {
				values = result.getResults().get(2).getSeries().get(0).getValues().get(i);
				FaultItem faultItem = new FaultItem();
				convertItem(faultItem, values);
				faultItem.setFault(((Double) values.get(1)).intValue());
				faultItems.add(faultItem);
				if (faultItem.getFault() < 0) {
					break;
				}
			}
		}
		if (result.getResults().get(3).getSeries().size() > 0
				&& result.getResults().get(3).getSeries().get(0).getValues() != null
				&& result.getResults().get(3).getSeries().get(0).getValues().size() > 0) {
			List<SuccessItem> successItems = new ArrayList<SuccessItem>(
					result.getResults().get(3).getSeries().get(0).getValues().size());
			statisticsOverview.setSuccessItems(successItems);
			for (int i = 0; i < result.getResults().get(3).getSeries().get(0).getValues().size(); i++) {
				values = result.getResults().get(3).getSeries().get(0).getValues().get(i);
				SuccessItem successItem = new SuccessItem();
				convertItem(successItem, values);
				successItem.setSuccess(((Double) values.get(1)).intValue());
				successItems.add(successItem);
				if (successItem.getSuccess() < 0) {
					break;
				}
			}
		}
		return statisticsOverview;
	}

	private void convertItem(BaseItem item, List<Object> values) {
		try {
			item.setTimestamp(dateformat.get().parse((String) values.get(0)).getTime());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			logger.error("Failed to read service overview's timestamp.", e);
		}
		item.setMethod((String) values.get(2));
		item.setService((String) values.get(3));
		item.setRemoteType((String) values.get(4));
	}

	public Collection<ServiceInfo> queryServiceByApp(String application, long start, long end) {
		List<ServiceInfo> services = new ArrayList<ServiceInfo>();
		Query query = new Query("SELECT * FROM " + this.measurementName + " WHERE "
				+ DubboKeeperMonitorService.APPLICATION + " = '" + application + "' group by "
				+ DubboKeeperMonitorService.INTERFACE + " slimit  " + MAX_GROUP_SIZE, databaseName);
		// System.out.println("queryServiceByApp--> " + query.getCommand());
		QueryResult result = influxDB.query(query);
		if (result.getResults() == null || result.getResults().size() <= 0
				|| result.getResults().get(0).getSeries() == null) {
			return services;
		}
		for (Series group : result.getResults().get(0).getSeries()) {
			if (group.getTags() == null || group.getValues() == null || group.getValues().size() <= 0) {
				continue;
			}
			String service_name = group.getTags().get(DubboKeeperMonitorService.INTERFACE);
			String remote_type = (String) (group.getValues().get(0).get(1));
			ServiceInfo serviceInfo = new ServiceInfo();
			serviceInfo.setName(service_name);
			serviceInfo.setRemoteType(remote_type);
			Long[] rr = getMaxInfo(application, start, end);
			serviceInfo.setMaxConcurrent(rr[0]);
			serviceInfo.setMaxElapsed(rr[1]);
			serviceInfo.setMaxFault(rr[2].intValue());
			serviceInfo.setMaxSuccess(rr[3].intValue());
			services.add(serviceInfo);
		}
		return services;
	}
}
