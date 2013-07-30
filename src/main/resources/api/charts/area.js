zenoss.visualization.chart.area = {
	required : {
		defined : 'nv',
		source : [ 'nv.d3.min.js', 'css/nv.d3.css' ]
	},

	build : function(chart, data) {
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
		var _start = new Date(data.startTimeActual);
		var _end = new Date(data.endTimeActual);
		var _chart = nv.models.stackedAreaChart().x(function(v) {
			return v.x;
		}).y(function(v) {
			return v.y;
		}).clipEdge(true);

		_chart.xAxis.tickFormat(function(ts) {
			return zenoss.visualization.tickFormat(_start, _end, ts);
		});
		_chart.height($(chart.svgwrapper).height());
		_chart.width($(chart.svgwrapper).width());

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