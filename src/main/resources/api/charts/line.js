zenoss.visualization.chart.line = {
	build : function(chart) {
		var _chart = nv.models.lineChart();
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