zenoss.visualization.chart.bar = {

	build : function(chart) {
		nv.addGraph(function() {
			var _chart = nv.models.multiBarChart();

			_chart.xAxis.tickFormat(function(ts) {
				return d3.time.format('%x %X')(new Date(ts));
			});

			chart.svg.datum(chart.plots).transition().duration(500)
					.call(_chart);

			nv.utils.windowResize(_chart.update);
		})
	},
	render : function() {

	}
};
