package rocks.inspectit.oce.eum.server.metrics.percentiles;

import io.opencensus.common.Timestamp;
import io.opencensus.metrics.LabelKey;
import io.opencensus.metrics.export.MetricDescriptor;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Math.round;

/**
 * For the data within this window, smoothed averages can be computed.
 */
public class SmoothedAverageView extends TimeWindowView {

    /**
     * The tag value to use for {@link #PERCENTILE_TAG_KEY} for the "smoothed average" series.
     */
    private static final String SMOOTHED_AVERAGE_METRIC_SUFFIX = "_smoothed_average";

    /**
     * The descriptor of the metric for this view, if smoothed average.
     */
    private MetricDescriptor metricDescriptor;

    @Getter
    private int dropUpper;

    @Getter
    private int dropLower;

    /**
     * Constructor.
     *
     * @param dropUpper        TODO
     * @param dropLower        TODO
     * @param tags             the tags to use for this view
     * @param timeWindowMillis the time range in milliseconds to use for computing minimum / maximum and percentile values
     * @param viewName         the prefix to use for the names of all exposed metrics
     * @param unit             the unit of the measure
     * @param description      the description of this view
     * @param bufferLimit      the maximum number of measurements to be buffered by this view
     */
    SmoothedAverageView(int dropUpper, int dropLower, Set<String> tags, long timeWindowMillis, String viewName, String unit, String description, int bufferLimit) {
        super(tags, timeWindowMillis, viewName, unit, description, bufferLimit);
        validateConfiguration(dropUpper, dropLower);

        this.dropUpper = dropUpper;
        this.dropLower = dropLower;

        List<LabelKey> smoothedAverageLabelKeys = getLabelKeysInOrder();
        metricDescriptor = MetricDescriptor.create(viewName + SMOOTHED_AVERAGE_METRIC_SUFFIX, description, unit, MetricDescriptor.Type.GAUGE_DOUBLE, smoothedAverageLabelKeys);
    }

    private void validateConfiguration(int dropUpper, int dropLower) {
        if (dropUpper < 0 || dropUpper > 100) {
            throw new IllegalArgumentException("cutTop must be greater than 0 and smaller than 50!");
        }
        if (dropLower < 0 || dropLower > 100) {
            throw new IllegalArgumentException("cutBottom must be greater than 0 and smaller than 50!");
        }
    }

    @Override
    Set<String> getSeriesNames() {
        Set<String> result = new HashSet<>();
        result.add(metricDescriptor.getName());
        return result;
    }

    @Override
    protected void computeSeries(List<String> tagValues, double[] data, Timestamp time, ResultSeriesCollector resultSeries) {
        double queueLength = data.length;
        double base = queueLength / 100;

        int ratioBottom = (int) round(base * dropLower);
        int startIndex = (dropLower == 0) ? 0 : Math.min((ratioBottom <= 0) ? 1 : ratioBottom, (int) queueLength - 1);
        int ratioTop = (int) round(base * dropUpper);
        int endIndex = (dropUpper == 0) ? (int) queueLength - 1 : Math.max((ratioTop <= 0) ? (int) queueLength - 2 : (int) queueLength - 1 - ratioTop, startIndex);

        // Note: Can also return sum and count
        double smoothedAverage = round((Arrays.stream(data)
                .sorted()
                .skip(startIndex)
                .limit(endIndex - startIndex + 1)
                .average()
                .orElse(0.0)) * 100.0) / 100.0;
        resultSeries.add(metricDescriptor, smoothedAverage, time, tagValues);
    }

}
