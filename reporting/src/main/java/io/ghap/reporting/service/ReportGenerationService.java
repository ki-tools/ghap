package io.ghap.reporting.service;

import io.ghap.oauth.OAuthUser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public interface ReportGenerationService {
	String generateUsersReport(String accessToken) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException;

	String generateGroupsReport(String accessToken) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException;

	String generateRolesReport(String accessToken) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException;

	String generateProgramReport(String accessToken) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException;

	String generateGrantReport(String accessToken) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException;

	String generateSystemUsageReport(String accessToken, Date start, Date end, String instanceType, boolean excludeNotCommisioned) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException;

	String generateCPUUsageReport(String accessToken, Date start, Date end) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException;

	String generateDataSubmissionReport(String accessToken, Date start, Date end, boolean excludeDataCurators) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException;

	String generatePermissionMismatchReport(String accessToken) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException;
}
