zenoss.visualization.chart.area = {
	build : function(chart) {
		// Area plots don't seem to do well if there are multiple data point
		// sets and there are not the same number of points in each set, so
		// truncate the data point areas to the same number of points.
		if (chart.plots.length > 1) {
			// get minmum length
			var minLength = chart.plots[0].values.length;
			chart.plots.forEach(function(plot) {
				minLength = Math.min(minLength, plot.values.length);
			});

			// Truncate
			chart.plots.forEach(function(plot) {
				plot.values.length = minLength;
			});

		}
		_chart = nv.models.stackedAreaChart().x(function(v) {
			return v.x;
		}).y(function(v) {
			return v.y;
		}).clipEdge(true);

		_chart.xAxis.tickFormat(function(ts) {
			return d3.time.format('%x %X')(new Date(ts));
		}).axisLabel('Date/Time');
		nv.addGraph(function() {
			chart.svg.datum(chart.plots).transition().duration(500)
					.call(_chart);
			nv.utils.windowResize(function() {
				chart.svg.call(_chart)
			});
		});
	},
	render : function() {

	}
}