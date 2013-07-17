zenoss.visualization.chart.dc = {
	stacked : {
		build : function(chart) {

			var _chart = null;
			var _groups = [];
			var _ndx = [];
			var _dims = [];
			var _all = [];

			chart.plots.forEach(function(plot) {
				plot.values.forEach(function(e) {
					e.dtimestamp = new Date(e.x);
				})
				_ndx.push(crossfilter(plot.values));
				_dims.push(_ndx[_ndx.length - 1].dimension(function(d) {
					return d3.time.minute(d.dtimestamp);
				}));
				var ___l = _dims.length - 1;
				_groups.push(_dims[_dims.length - 1].group().reduceSum(
						function(d) {
							return d.y;
						}));
				_all.push(_ndx[_ndx.length - 1].groupAll());
			});

			_chart = dc.lineChart('#' + chart.name, "zenoss");

			var l = chart.plots[0].values.length;
			chart.svg.datum(chart.plots);
			_chart.dimension(_dims[0]);
			_chart.group(_groups[0]);
			_chart.width($('#' + chart.name).width());
			_chart.height($('#' + chart.name).height());
			_chart.transitionDuration(500);
			_chart.elasticY(true);
			_chart.elasticX(true);
			_chart.brushOn(false);
			_chart.renderArea(true);
			_chart.title(function(d) {
				return "Value: " + d.value;
			});
			_chart.renderTitle(true);
			_chart.dotRadius(10);
			_chart.round(d3.time.second.round);
			_chart.xUnits(d3.time.second);
			_chart.renderHorizontalGridLines(true);
			_chart.renderVerticalGridLines(true);

			for ( var i = 1; i < _groups.length; ++i) {
				_chart.stack(_groups[i]);
			}

			_chart.x(d3.time.scale().domain(
					[ new Date(chart.plots[0].values[0].x),
							new Date(chart.plots[0].values[l - 1].x) ]))

		},

		render : function() {
			dc.renderAll('zenoss');
		}
	}
}