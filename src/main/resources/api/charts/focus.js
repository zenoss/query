zenoss.visualization.chart.focus = {
	build : function(chart) {
		var _chart = nv.models.lineWithFocusChart();
		_chart.xAxis.tickFormat(function(ts) {
			return d3.time.format('%x %X')(new Date(ts));
		});
		_chart.x2Axis.tickFormat(function(ts) {
			return d3.time.format('%x %X')(new Date(ts));
		});
		_chart.yAxis.tickFormat(d3.format('f'));
		_chart.y2Axis.tickFormat(d3.format('f'));
		nv.addGraph(function() {
			chart.svg.datum(chart.plots).transition().duration(500)
					.call(_chart);
			nv.utils.windowResize(_chart.update);
		});
	},
	render : function() {

	}
}