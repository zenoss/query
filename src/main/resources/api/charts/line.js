zenoss.visualization.chart.line = {
	required : {
		defined : 'nv',
		source : [ 'nv.d3.min.js', 'css/nv.d3.css' ]
	},
	
	color : function(chart, impl, idx) {
		return {
			'color' : impl.color()(0, idx),
			'opacity' : 1,
		}
	},

	build : function(chart, data) {
		var _chart = nv.models.lineChart();
		var _start = new Date(data.startTimeActual);
		var _end = new Date(data.endTimeActual);
		_chart.xAxis.tickFormat(function(ts) {
			return zenoss.visualization.tickFormat(_start, _end, ts);
		});
		_chart.height($(chart.svgwrapper).height());
		_chart.width($(chart.svgwrapper).width());

		nv.addGraph(function() {
			chart.svg.datum(chart.plots).transition().duration(0).call(_chart);
			nv.utils.windowResize(function() {
				chart.svg.call(_chart)
			});
		});
		return _chart;
	},

	render : function() {
	}
}