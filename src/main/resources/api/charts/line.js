zenoss.visualization.chart.line = {
	required : {
		defined : 'nv',
		source : [ 'nv.d3.min.js', 'css/nv.d3.css' ]
	},
	build : function(chart) {
		var _chart = nv.models.lineChart();
		_chart.xAxis.tickFormat(function(ts) {
			return d3.time.format('%x %X')(new Date(ts));
		});
		nv.addGraph(function() {
			chart.svg.datum(chart.plots).transition().duration(0)
					.call(_chart);
			nv.utils.windowResize(function() {
				chart.svg.call(_chart)
			});
		});
	},

	render : function() {
	}
}