zenoss.visualization.chart.bar = {
	required : {
		defined : 'nv',
		source : [ 'nv.d3.min.js', 'css/nv.d3.css' ]
	},

	color : function(impl, idx) {
		return impl.color()(0, idx);
	},

	build : function(chart, data) {

		// Because we are dealing with stacked charts we need to make sure
		// the the dimension for the data (timestamps) match up. So we cull the
		// data to only those points that are in all the data sets.
		zenoss.visualization.__cull(chart);

		var _chart = nv.models.multiBarChart();
		var _start = new Date(data.startTimeActual);
		var _end = new Date(data.endTimeActual);

		nv.addGraph(function() {
			_chart.xAxis.tickFormat(function(ts) {
				return zenoss.visualization.tickFormat(_start, _end, ts);
			});

			chart.svg.datum(chart.plots).transition().duration(500)
					.call(_chart);
			_chart.height($(chart.svgwrapper).height());
			_chart.width($(chart.svgwrapper).width());

			nv.utils.windowResize(_chart.update);
		});
		return _chart;
	},
	render : function() {

	}
};
