zenoss.visualization.chart.dc = {
	stacked : {
		required : {
			defined : 'dc',
			source : [ 'crossfilter.min.js', 'dc.min.js', 'css/dc.css' ]
		},

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
				_groups.push(_dims[_dims.length - 1].group().reduce(
						function(p, v) {
							if (typeof p.values[v.y] == 'undefined') {
								p.values[v.y] = 1;
							} else {
								p.values[v.y] += 1;
							}
							p.max = Math.max(p.max, v.y);
							return p;
						}, function(p, v) {
							// need to remove the value from the values array
							p.values[v.y] -= 1;
							if (p.values[v.y] <= 0) {
								delete p.values[v.y];
								if (max == v.y) {
									// pick new max, by iterating over keys
									// finding the largest.
									max = -1;
									for (k in p.values) {
										if (p.values.hasOwnProperty(k)) {
											max = Math.max(max, parseFloat(k));
										}
									}
									p.max = max;
								}
							}
							p.total -= v.y;
							return p;
						}, function() {
							return {
								values : {},
								max : -1,
								toString : function() {
									return this.max;
								}
							};
						}));
				_all.push(_ndx[_ndx.length - 1].groupAll());
			});

			_chart = dc.lineChart(chart.containerSelector, "zenoss");

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