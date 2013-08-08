zenoss.visualization.chart.focus = {
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
		_chart.model().x2Axis.tickFormat(function(ts) {
			return zenoss.visualization.tickFormat(_start, _end, ts);
		});

		chart.svg.datum(chart.plots).transition().duration(0).call(
				_chart.model());
	},

	build : function(chart, data) {

		var _chart = new zenoss.visualization.chart.focus.Chart();
		var model = nv.models.lineWithFocusChart();
		_chart.model(model);

		var _start = new Date(data.startTimeActual);
		var _end = new Date(data.endTimeActual);

		model.xAxis.tickFormat(function(ts) {
			return zenoss.visualization.tickFormat(_start, _end, ts);
		});
		model.x2Axis.tickFormat(function(ts) {
			return zenoss.visualization.tickFormat(_start, _end, ts);
		});

		model.yAxis.tickFormat(d3.format('f'));
		model.y2Axis.tickFormat(d3.format('f'));
		model.height($(chart.svgwrapper).height());
		model.width($(chart.svgwrapper).width());
		chart.svg.datum(chart.plots);

		nv.addGraph(function() {
			chart.svg.transition().duration(500).call(model);
			nv.utils.windowResize(model.update);
		});

		return _chart;

		// var _chart = nv.models.lineWithFocusChart();
		// var _start = new Date(data.startTimeActual);
		// var _end = new Date(data.endTimeActual);
		//
		// _chart.xAxis.tickFormat(function(ts) {
		// return zenoss.visualization.tickFormat(_start, _end, ts);
		// });
		// _chart.x2Axis.tickFormat(function(ts) {
		// return zenoss.visualization.tickFormat(_start, _end, ts);
		// });
		// _chart.yAxis.tickFormat(d3.format('f'));
		// _chart.y2Axis.tickFormat(d3.format('f'));
		// _chart.height($(chart.svgwrapper).height());
		// _chart.width($(chart.svgwrapper).width());
		//
		// nv.addGraph(function() {
		// chart.svg.datum(chart.plots).transition().duration(500)
		// .call(_chart);
		// nv.utils.windowResize(_chart.update);
		// });
		// return _chart;
	},

	render : function() {

	}
}