zenoss.visualization.chart.bar = {
	required : {
		defined : 'nv',
		source : [ 'nv.d3.min.js', 'css/nv.d3.css' ]
	},

	build : function(chart, data) {
		nv.addGraph(function() {
			var _chart = nv.models.multiBarChart();
			var _start = new Date(data.startTimeActual);
			var _end = new Date(data.endTimeActual);

			_chart.xAxis.tickFormat(function(ts) {
				return zenoss.visualization.tickFormat(_start, _end, ts);
			});

			chart.svg.datum(chart.plots).transition().duration(500)
					.call(_chart);
			_chart.height($(chart.svgwrapper).height());
			_chart.width($(chart.svgwrapper).width());

			nv.utils.windowResize(_chart.update);
		})
	},
	render : function() {

	}
};
