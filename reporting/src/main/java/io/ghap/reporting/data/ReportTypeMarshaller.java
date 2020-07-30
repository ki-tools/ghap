package io.ghap.reporting.data;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMarshaller;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class ReportTypeMarshaller implements DynamoDBMarshaller<ReportType> {

	@Override
	public String marshall(ReportType getterReturnResult) {
		return getterReturnResult == null ? null : getterReturnResult.name();
	}

	@Override
	public ReportType unmarshall(Class<ReportType> clazz, String obj) {
		return obj == null ? null : ReportType.fromName(obj);
	}
}
