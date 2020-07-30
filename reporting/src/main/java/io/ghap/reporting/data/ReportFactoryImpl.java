package io.ghap.reporting.data;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.google.inject.Inject;
import io.ghap.reporting.service.ReportGenerationService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class ReportFactoryImpl implements ReportFactory {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private static final long MAX_SIZE = 400 * 1024;

	private AmazonDynamoDBClient client;

	@Inject
	private ReportGenerationService reportGenerationService;

	@Override
	public List<ReportType> GetAvailableReports() {
		return Arrays.asList(ReportType.values());
	}

	@Override
	public String CreateReport(String accessToken, UUID uuid, ReportType reportType) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
		return CreateReport(accessToken, uuid, reportType, null);
	}

	@Override
	public String CreateReport(String accessToken, UUID uuid, ReportType reportType, List<Constraint> constraints) throws NoSuchMethodException, IOException, IllegalAccessException, InvocationTargetException {
		Report report = new Report();
		report.setOwner(uuid);
		report.setContentType("application/csv");
		report.setCreated(System.currentTimeMillis());

		final DateFormat dateFormatToUse = new SimpleDateFormat("MM-dd-yyyy");
		report.setFilename(reportType.getTypeName() + "_created_on_" + dateFormatToUse.format(new Date()) + ".csv");

		report.setReportType(reportType);
		report.setToken(UUID.randomUUID().toString());
		report.setName(reportType.getTypeName());
		Date start = null;
		Date end = null;
		Object[] objects = extractConstraints(constraints);
		if (objects != null) {
			start = (Date) objects[0];
			if (start != null) {
				start = DateUtils.truncate(start, Calendar.DAY_OF_MONTH);
			}
			end = (Date) objects[1];
			if (end != null) {
				Date endDateMidnight = DateUtils.truncate(end, Calendar.DAY_OF_MONTH);
				end = DateUtils.addMilliseconds(DateUtils.addDays(endDateMidnight, 1), -1);
			}
		}
		if (start != null && end != null) {
			report.setFilename(
					String.format("%s_from_%s_to_%s_created_on_%s.csv",
							reportType.getTypeName(),
							dateFormatToUse.format(start),
							dateFormatToUse.format(end),
							dateFormatToUse.format(new Date())
					)
			);
		}
		switch (reportType) {
			case USER_STATUS:
				report.setContent(reportGenerationService.generateUsersReport(accessToken).getBytes("UTF-8"));
				break;
			case GROUP_STATUS:
				report.setContent(reportGenerationService.generateGroupsReport(accessToken).getBytes("UTF-8"));
				break;
			case ROLE_STATUS:
				report.setContent(reportGenerationService.generateRolesReport(accessToken).getBytes("UTF-8"));
				break;
			case PROGRAM_STATUS:
				report.setContent(reportGenerationService.generateProgramReport(accessToken).getBytes("UTF-8"));
				break;
			case GRANT_STATUS:
				report.setContent(reportGenerationService.generateGrantReport(accessToken).getBytes("UTF-8"));
				break;
			case COMPUTE:
				report.setContent(reportGenerationService.generateSystemUsageReport(accessToken, start, end, null, false).getBytes("UTF-8"));
				break;
			case WINDOWS_COMPUTE:
				String content_str = reportGenerationService.generateSystemUsageReport(accessToken, start, end, "windows", true);
				content_str = content_str == null ? "" : content_str;
				byte[] content = content_str.getBytes("UTF-8");
				report.setContent(content);
				break;
			case DATASUBMISSION:
				//boolean excludeDataCurators = BooleanUtils.toBoolean((Boolean) objects[2]);
				boolean excludeDataCurators = true;
				report.setContent(reportGenerationService.generateDataSubmissionReport(accessToken, start, end, excludeDataCurators).getBytes("UTF-8"));
				break;
			case GRANT_PERMISSION_MISMATCH:
				report.setContent(reportGenerationService.generatePermissionMismatchReport(accessToken).getBytes("UTF-8"));
				break;
		}
		saveReport(report);
		return report.getToken();
	}

	@Override
	public Report GetReport(String token) {
		return getReport(token);
	}

	@Override
	public InputStream GetContent(Report report) {
		if (report == null) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		List<Report> awsReports = getChildrenReports(report);
		if (CollectionUtils.isEmpty(awsReports)) {
			return new ByteArrayInputStream(report.getContent());
		}
		List<Report> reports = new ArrayList<>(awsReports);
		Collections.sort(reports);
		byte[] result = report.getContent();
		for (Report r : reports) {
			result = ArrayUtils.addAll(result, r.getContent());
		}
		return new ByteArrayInputStream(result);
	}



	@Override
	public ReportStatus Status(String token) {
		Report report = getReport(token);
		if (report == null) {
			return ReportStatus.NOT_FOUND;
		} else {
			return ReportStatus.COMPLETE;
		}
	}

	@Override
	public List<ReportStatus> Status(List<String> tokens) {
		final Map<String, ReportStatus> statusMap = new ConcurrentHashMap<>();
		ExecutorService executor = Executors.newFixedThreadPool(20);

		for (final String token : tokens) {
			executor.submit(new Runnable() {
				@Override
				public void run() {
					statusMap.put(token, Status(token));
				}
			});
		}
		executor.shutdown();

		try {
			executor.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
			log.error("Statuses retrieve operation was interrupted");
		}


		List<ReportStatus> statuses = new ArrayList<>(tokens.size());
		for(String token:tokens){
			statuses.add(statusMap.get(token));
		}

		return statuses;
	}

	@Override
	public List<Report> ListReports() {
		AmazonDynamoDBClient client = getClient();
		DynamoDBMapper mapper = new DynamoDBMapper(client);
		DynamoDBScanExpression expression = new DynamoDBScanExpression();
		expression.addFilterCondition("parentToken", new Condition().withComparisonOperator(ComparisonOperator.NULL));

		PaginatedScanList<Report> scan = getScan(mapper, Report.class, expression, 20);
		return getQueryResults(scan, 40);
	}

	@Override
	public List<Report> ListReports(UUID userid) {
		AmazonDynamoDBClient client = getClient();
		DynamoDBMapper mapper = new DynamoDBMapper(client);
		DynamoDBScanExpression expression = new DynamoDBScanExpression();
		expression.addFilterCondition("owner", new Condition().withComparisonOperator(
				ComparisonOperator.EQ)
				.withAttributeValueList(new AttributeValue().withS(userid.toString())));
		expression.addFilterCondition("parentToken", new Condition().withComparisonOperator(ComparisonOperator.NULL));


		PaginatedScanList<Report> scan = getScan(mapper, Report.class, expression, 20);
		return getQueryResults(scan, 40);
	}

	public static <T> List<T> getQueryResults(PaginatedScanList<T> scan, int maxRetries) {
		Iterator<T> it = scan.iterator();

		List<T> result = new ArrayList<>();
		int retries = 0;

		for(;;){
			try {
				result.add( it.next() );
			} catch (ProvisionedThroughputExceededException e){
				if( (retries+1) > maxRetries) {
					throw e;
				}

				long sleepTime = Math.round(Math.pow(2, Math.min(10, retries))/10);
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e1) {
					throw new RuntimeException(e);
				}
				retries++;
			} catch (NoSuchElementException e){
				break;
			}
		}
		return result;
	}

	@Override
	public boolean RemoveReport(String token) {
		Report report = getReport(token);
		if (report != null) {
			AmazonDynamoDBClient client = getClient();
			DynamoDBMapper mapper = new DynamoDBMapper(client);
			List<Report> childrenReports = getChildrenReports(report);
			if (CollectionUtils.isEmpty(childrenReports)) {
				mapper.delete(report);
			} else {
				List<Report> newReports = new ArrayList<>(childrenReports);
				newReports.add(report);
				mapper.batchDelete(newReports);
			}

			return true;
		}
		else {
			return false;
		}
	}

	private void saveReport(Report report) throws IllegalAccessException {
		AmazonDynamoDBClient client = getClient();
		DynamoDBMapper mapper = new DynamoDBMapper(client);
		long size = report.calculateSize();
		List<Report> subReports = new ArrayList<>();
		if (size > MAX_SIZE) {
			int order = Integer.MAX_VALUE;
			while (size > MAX_SIZE) {
				Report r = new Report();
				r.setParentToken(report.getToken());
				r.setOwner(report.getOwner());
				r.setCreated(report.getCreated());
				r.setToken(UUID.randomUUID().toString());
				r.setOrder(order);
				long maxSize = MAX_SIZE - r.calculateSize();
				byte[] content = report.getContent();
				r.setContent(Arrays.copyOfRange(content, (int) (content.length - maxSize), content.length));
				report.setContent(Arrays.copyOfRange(content, 0, (int) (content.length - maxSize)));
				subReports.add(r);
				size = report.calculateSize();
				order--;
			}
		}
		mapper.save(report);
		for (Report r : subReports) {
			mapper.save(r);
		}
	}

	private Report getReport(String token) {
		AmazonDynamoDBClient client = getClient();
		DynamoDBMapper mapper = new DynamoDBMapper(client);

		int retries = 0;
		int maxRetries = 20;

		for(;;){
			try {
				return mapper.load(Report.class, token.toString());
			} catch (ProvisionedThroughputExceededException e){
				if( (retries+1) > maxRetries) {
					throw e;
				}

				long sleepTime = Math.round(Math.pow(2, Math.min(10, retries))/10);
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e1) {
					throw new RuntimeException(e);
				}
				retries++;
			}
		}
	}

	private List<Report> getChildrenReports(Report report) {
		AmazonDynamoDBClient client = getClient();
		DynamoDBMapper mapper = new DynamoDBMapper(client);
		DynamoDBScanExpression expression = new DynamoDBScanExpression();
		expression.addFilterCondition("parentToken", new Condition().withComparisonOperator(
				ComparisonOperator.EQ)
				.withAttributeValueList(new AttributeValue().withS(report.getToken())));

		PaginatedScanList<Report> scan = getScan(mapper, Report.class, expression, 20);
		return getQueryResults( scan, 20);
	}

	public static <T> PaginatedScanList<T> getScan(DynamoDBMapper mapper, Class<T> clazz, DynamoDBScanExpression expression, int maxRetries) {

		int retries = 0;

		for(;;){
			try {
				return mapper.scan(clazz, expression);
			} catch (ProvisionedThroughputExceededException e){
				if( (retries+1) > maxRetries) {
					throw e;
				}

				long sleepTime = Math.round(Math.pow(2, Math.min(10, retries))/10);
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e1) {
					throw new RuntimeException(e);
				}
				retries++;
			}
		}
	}

	private AmazonDynamoDBClient getClient() {
		if (client == null) {
			__initClient();
		}
		return client;
	}

	private synchronized void __initClient() {
		if (client == null) {
			client = new AmazonDynamoDBClient(new DefaultAWSCredentialsProviderChain());
		}
	}

	private Object[] extractConstraints(List<Constraint> constraints) {
		if (CollectionUtils.isEmpty(constraints)) {
			return null;
		}
		Object[] result = new Object[3];
		if (CollectionUtils.isNotEmpty(constraints)) {
			for (Constraint c : constraints) {
				if (c instanceof DateRangeConstraint) {
					result[0] = ((DateRangeConstraint) c).getConstraint().getStart();
					result[1] = ((DateRangeConstraint) c).getConstraint().getEnd();
				} else if (c instanceof ExcludeDataCuratorsConstraint) {
					result[2] = ((ExcludeDataCuratorsConstraint) c).getConstraint();
				}
			}
		}
		return result;
	}
}
