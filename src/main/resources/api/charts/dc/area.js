zenoss.visualization.chart.dc = {
	area : {
		required : {
			defined : 'dc',
			source : [ 'crossfilter.min.js', 'dc.min.js', 'css/dc.css' ]
		},

		color : function(chart, impl, idx) {
			return {
				'color' : d3.scale.category10().range()[idx],
				'opacity' : 1
			}
		},

		build : function(chart) {

			var _chart = null;
			var _groups = [];
			var _ndx = [];
			var _dims = [];

			chart.plots.forEach(function(plot) {
				plot.values.forEach(function(e) {
					e.dtimestamp = new Date(e.x);
				})
				_ndx.push(crossfilter(plot.values));
				_dims.push(_ndx[_ndx.length - 1].dimension(function(d) {
					return d3.time.second(d.dtimestamp);
				}));

				_groups.push(zenoss.visualization
						.__reduceMax(_dims[_dims.length - 1].group()));
			});

			_chart = dc.compositeChart(chart.containerSelector, "zenoss");
			_chart.renderHorizontalGridLines(true);
			_chart.renderVerticalGridLines(true);
			var subs = [];
			for ( var i = 0; i < _groups.length; ++i) {
				var c = dc
						.lineChart(_chart, "zenoss")
						.transitionDuration(500)
						.elasticY(true)
						.elasticX(true)
						.x(
								d3.time
										.scale()
										.domain(
												[
														new Date(
																chart.plots[i].values[0].x),
														new Date(
																chart.plots[i].values[chart.plots[i].values.length - 1].x) ]))
						.round(d3.time.second.round).dimension(_dims[i]).group(
								_groups[i]).xUnits(d3.time.second)
						.renderHorizontalGridLines(false)
						.renderVerticalGridLines(false).renderArea(true).width(
								$(chart.svgwrapper).width()).height(
								$(chart.svgwrapper).height()).brushOn(false)
						.title(function(d) {
							return d.value + ' at ' + d.key;
						}).renderTitle(true).dotRadius(10);
				subs.push(c);
			}
			_chart.compose(subs);

			var l = chart.plots[0].values.length;
			chart.svg.datum(chart.plots);
			_chart.dimension(_dims[0]);
			_chart.group(_groups[0]);
			_chart.width($(chart.svgwrapper).width());
			_chart.height($(chart.svgwrapper).height());
			_chart.transitionDuration(500);
			_chart.elasticY(true);
			_chart.elasticX(true);
			_chart.brushOn(false);

			_chart.x(d3.time.scale().domain(
					[ new Date(chart.plots[0].values[0].x),
							new Date(chart.plots[0].values[l - 1].x) ]));
			return _chart;
		},

		render : function(chart) {
			dc.renderAll('zenoss');
		}
	}
}