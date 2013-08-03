zenoss.visualization.chart.discretebar = {
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
				var label = plot.key;
				if (label.indexOf('{') > -1) {
					label = label.substring(0, 50);
				}

				_averages.values.push({
					'label' : label,
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
		var _chart = new zenoss.visualization.chart.discretebar.Chart();
		var model = nv.models.discreteBarChart();
		_chart.model(model);

		model.x(function(d) {
			return d.label;
		});
		model.y(function(d) {
			return d.value;
		});
		model.staggerLabels(true);
		model.tooltips(true);
		model.showValues(true);
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
	},
	render : function() {

	}
}