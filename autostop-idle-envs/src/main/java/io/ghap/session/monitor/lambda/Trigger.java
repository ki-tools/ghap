package io.ghap.session.monitor.lambda;

import java.util.List;

/**
 */
public class Trigger {

    public String MetricName;
    public String Namespace;
    public String Statistic;
    public String Unit;
    public List<Dimension> Dimensions;
    public Integer Period;
    public Integer EvaluationPeriods;
    public String ComparisonOperator;
    public Integer Threshold;
}
