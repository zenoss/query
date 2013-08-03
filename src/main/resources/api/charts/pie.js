zenoss.visualization.chart.pie = {
	required : {
		defined : 'nv',
		source : [ 'nv.d3.min.js', 'css/nv.d3.css' ]
	},

	Chart : function() {
		var _model = null;
		var _averages = {
			'key' : 'Average',
			'values' : []
		};

		this.model = function(_) {
			if (!arguments.length) {
				return _model;
			}
			_model = _;
		}

		this.averages = function(_) {
			if (!arguments.length) {
				return [ _averages ];
			}
			var plots = _;

			_averages.values = [];
			plots.forEach(function(plot) {
				_averages.values.push({
					'label' : plot.key,
					'value' : d3.mean(plot.values, function(d) {
						return d.y;
					})
				});
			});
			return [ _averages ];
		}
	},

	color : function(chart, impl, idx) {
		return {
			'color' : impl.model().color()(0, idx),
			'opacity' : 1,
		}
	},

	update : function(chart, data) {
		var _chart = chart.closure;

		chart.svg.datum(_chart.averages(chart.plots)).transition().duration(0)
				.call(_chart.model());
	},

	build : function(chart) {
		var _chart = new zenoss.visualization.chart.pie.Chart();
		var model = nv.models.pie();
		_chart.model(model);

		model.x(function(d) {
			return d.label;
		});
		model.y(function(d) {
			return d.value;
		});
		model.showLabels(false);
		model.height($(chart.svgwrapper).height());
		model.width($(chart.svgwrapper).width());
		chart.svg.datum(_chart.averages(chart.plots));

		nv.addGraph(function() {
			chart.svg.transition().duration(500).call(model);
			nv.utils.windowResize(function() {
				chart.svg.call(model.update)
			});
		});

		return _chart;

		// var _chart = nv.models.pie();
		// _chart.x(function(d) {
		// return d.label;
		// }).y(function(d) {
		// return d.value;
		// }).showLabels(true);
		// var __means = [ {
		// 'key' : 'Cumlative',
		// 'values' : []
		// } ];
		// chart.plots.forEach(function(plot) {
		// __means[0].values.push({
		// 'label' : plot.key,
		// 'value' : d3.mean(plot.values, function(d) {
		// return d.y;
		// })
		// });
		// });
		// _chart.height($(chart.svgwrapper).height());
		// _chart.width($(chart.svgwrapper).width());
		// nv.addGraph(function() {
		// chart.svg.datum(__means).transition().duration(500).call(_chart);
		// nv.utils.windowResize(function() {
		// chart.svg.call(_chart)
		// });
		// });
		// return _chart;
	},
	render : function() {
	}
}