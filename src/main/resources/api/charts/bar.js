zenoss.visualization.chart.bar = {
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
		// OK. Area charts really want data points to match up on keys, which
		// makes sense as this is how they stack things. To make this work we
		// going to walk the points and make sure they match
		zenoss.visualization.__cull(chart);

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
		// Because we are dealing with stacked charts we need to make sure
		// the the dimension for the data (timestamps) match up. So we cull the
		// data to only those points that are in all the data sets.
		zenoss.visualization.__cull(chart);

		var _chart = new zenoss.visualization.chart.bar.Chart();
		var model = nv.models.multiBarChart();
		_chart.model(model);

		var _start = new Date(data.startTimeActual);
		var _end = new Date(data.endTimeActual);
		model.xAxis.tickFormat(function(ts) {
			return zenoss.visualization.tickFormat(_start, _end, ts);
		});
		model.height($(chart.svgwrapper).height());
		model.width($(chart.svgwrapper).width());
		chart.svg.datum(chart.plots)

		nv.addGraph(function() {
			chart.svg.transition().duration(500).call(model);
			nv.utils.windowResize(model.update);
		});

		return _chart;

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
