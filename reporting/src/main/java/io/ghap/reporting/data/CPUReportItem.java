package io.ghap.reporting.data;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CPUReportItem {

	private String stackId;
	private String instanceId;
	private Double cpuload;
	private Long timestamp;

	public String getStackId() {
		return stackId;
	}
	public void setStackId(String stackId) {
		this.stackId = stackId;
	}
	public String getInstanceId() {
		return instanceId;
	}
	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}
	public Double getCpuload() {
		return cpuload;
	}
	public void setCpuload(Double cpuload) {
		this.cpuload = cpuload;
	}
	public Long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}
}
