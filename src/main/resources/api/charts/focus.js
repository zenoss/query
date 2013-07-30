zenoss.visualization.chart.focus = {
	required : {
		defined : 'nv',
		source : [ 'nv.d3.min.js', 'css/nv.d3.css' ]
	},

	build : function(chart, data) {
		var _chart = nv.models.lineWithFocusChart();
		var _start = new Date(data.startTimeActual);
		var _end = new Date(data.endTimeActual);

		_chart.xAxis.tickFormat(function(ts) {
			return zenoss.visualization.tickFormat(_start, _end, ts);
		});
		_chart.x2Axis.tickFormat(function(ts) {
			return zenoss.visualization.tickFormat(_start, _end, ts);
		});
		_chart.yAxis.tickFormat(d3.format('f'));
		_chart.y2Axis.tickFormat(d3.format('f'));
		_chart.height($(chart.svgwrapper).height());
		_chart.width($(chart.svgwrapper).width());

		nv.addGraph(function() {
			chart.svg.datum(chart.plots).transition().duration(500)
					.call(_chart);
			nv.utils.windowResize(_chart.update);
		});
		return _chart;
	},

	render : function() {

	}
}