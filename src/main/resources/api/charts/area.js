zenoss.visualization.chart.area = {
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
		var _chart = null;

		// OK. Area charts really want data points to match up on keys, which
		// makes sense as this is how they stack things. To make this work we
		// going to walk the points and make sure they match
		zenoss.visualization.__cull(chart);

		var _start = new Date(data.startTimeActual);
		var _end = new Date(data.endTimeActual);
		var _chart = nv.models.stackedAreaChart().clipEdge(true);

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
		return _chart;
	},
	render : function() {

	}
}