zenoss.visualization.chart.pie = {
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

	build : function(chart) {
		var _chart = nv.models.pie();
		_chart.x(function(d) {
			return d.label;
		}).y(function(d) {
			return d.value;
		}).showLabels(true);
		var __means = [ {
			'key' : 'Cumlative',
			'values' : []
		} ];
		chart.plots.forEach(function(plot) {
			__means[0].values.push({
				'label' : plot.key,
				'value' : d3.mean(plot.values, function(d) {
					return d.y;
				})
			});
		});
		_chart.height($(chart.svgwrapper).height());
		_chart.width($(chart.svgwrapper).width());
		nv.addGraph(function() {
			chart.svg.datum(__means).transition().duration(500).call(_chart);
			nv.utils.windowResize(function() {
				chart.svg.call(_chart)
			});
		});
		return _chart;
	},
	render : function() {
	}
}