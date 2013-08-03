zenoss.visualization.chart.line = {
	required : {
		defined : 'nv',
		source : [ 'nv.d3.min.js', 'css/nv.d3.css' ]
	},

	Chart : function() {
		var _model = null;

		this.model = function(_) {
			if (!arguments.length) {
				return _model;
			}
			_model = _;
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

		var _start = new Date(data.startTimeActual);
		var _end = new Date(data.endTimeActual);
		_chart.model().xAxis.tickFormat(function(ts) {
			return zenoss.visualization.tickFormat(_start, _end, ts);
		});

		chart.svg.datum(chart.plots).transition().duration(0).call(
				_chart.model());
	},

	build : function(chart, data) {
		var _chart = new zenoss.visualization.chart.line.Chart();
		var model = nv.models.lineChart();
		_chart.model(model);

		var _start = new Date(data.startTimeActual);
		var _end = new Date(data.endTimeActual);
		model.xAxis.tickFormat(function(ts) {
			return zenoss.visualization.tickFormat(_start, _end, ts);
		});
		model.height($(chart.svgwrapper).height());
		model.width($(chart.svgwrapper).width());
		chart.svg.datum(chart.plots);

		nv.addGraph(function() {
			chart.svg.transition().duration(0).call(model);
			nv.utils.windowResize(function() {
				chart.svg.call(model)
			});
		});

		return _chart;
	},

	render : function() {
	}
}