(function() {
    "use strict";
    var stacked = {
        required : {
            defined : 'dc',
            source : [ 'crossfilter.min.js', 'dc.min.js', 'css/dc.css' ]
        },

        Chart : function() {
            var _lineChart = null, _datasets = [], i;

            this.lineChart = function(_) {
                if (!arguments.length) {
                    return _lineChart;
                }
                _lineChart = _;
            };

            this.add = function(ds) {
                _datasets.push(ds);
            };

            this.dataset = function(n) {
                return _datasets[n];
            };

            this.stack = function() {
                if (_datasets.length < 2) {
                    return;
                }

                for (i = 1; i < _datasets.length; i += 1) {
                    _lineChart.stack(_datasets[i].group());
                }
            };

            this.forEach = function(f) {
                _datasets.forEach(function(ds) {
                    f(ds);
                });
            };
        },

        DataSet : function(plot) {
            var _plot = null;
            var _crossfilter = null;
            var _dimension = null;
            var _group = null;
            var _lineChart = null;

            this.crossfilter = function(_) {
                if (!arguments.length) {
                    return _crossfilter;
                }

                _crossfilter = crossfilter(_);
                _dimension = _crossfilter.dimension(function(d) {
                    return d.x;
                });

                // Remove the old group
                if (_group !== null) {
                    _group.remove();
                }

                _group = zenoss.visualization.__reduceMax(_dimension
                        .group(function(v) {
                            // Round down to the
                            // nearest 15 second
                            // boundary.
                            var d = new Date(Math.ceil(v / 15000) * 15000);
                            return d;
                        }));

            };

            this.plot = function(_) {
                if (!arguments.length) {
                    return _plot;
                }
                _plot = _;
                this.crossfilter(_plot.values);
            };

            this.lineChart = function(_) {
                if (!arguments.length) {
                    return _lineChart;
                }
                _lineChart = _;
            };

            this.dimension = function() {
                return _dimension;
            };

            this.group = function() {
                return _group;
            };

            this.plot(plot);
        },

        color : function(chart, impl, idx) {
            return {
                'color' : d3.scale.category10().range()[idx],
                'opacity' : 1
            };
        },

        update : function(chart, data) {

            var _chart = chart.closure, i;

            // Cull to common data points so that stacking
            // works correctly
            zenoss.visualization.__cull(chart);
            chart.svg.datum(chart.plots);

            // Clear the existing groups
            _chart.lineChart().getChartStack().clear();

            // Replace the plot data on each of the charts
            // with the new data
            for (i = 0; i < chart.plots.length; i += 1) {
                _chart.dataset(i).plot(chart.plots[i]);
            }
            // Update the primary dimension and group
            var lc = _chart.lineChart();
            lc.dimension(_chart.dataset(0).dimension());
            lc.group(_chart.dataset(0).group());

            // Stack the rest
            _chart.stack();
            dc.renderAll('zenoss');
        },

        resize : function(chart, height) {
            var _chart = chart.closure;
            _chart.lineChart().height(height);
            _chart.lineChart().redraw();
        },

        build : function(chart) {
            // Cull to common data points so that stacking
            // works correctly
            zenoss.visualization.__cull(chart);
            chart.svg.datum(chart.plots);

            var _chart = new zenoss.visualization.chart.dc.stacked.Chart();

            chart.plots.forEach(function(plot) {
                _chart.add(new zenoss.visualization.chart.dc.stacked.DataSet(
                        plot));
            });

            var lc = dc.lineChart(chart.containerSelector, "zenoss");
            _chart.lineChart(lc);

            lc.dimension(_chart.dataset(0).dimension());
            lc.group(_chart.dataset(0).group());
            lc.width($(chart.svgwrapper).width());
            lc.height($(chart.svgwrapper).height());
            lc.transitionDuration(500);
            lc.elasticY(true);
            lc.elasticX(true);
            lc.brushOn(false);
            lc.renderArea(true);
            lc.title(function(d) {
                return d.value + ' at ' + new Date(d.key).toLocaleString();
            });
            lc.renderTitle(true);
            lc.dotRadius(10);
            lc.round(d3.time.second.round);
            lc.xUnits(d3.time.second);
            lc.renderHorizontalGridLines(true);
            lc.renderVerticalGridLines(true);
            lc
                    .x(d3.time
                            .scale()
                            .domain(
                                    [
                                            new Date(chart.plots[0].values[0].x),
                                            new Date(
                                                    chart.plots[0].values[chart.plots[0].values.length - 1].x) ]));

            _chart.stack();

            return _chart;
        },

        render : function(chart) {
            dc.renderAll('zenoss');
        }
    };

    $.extend(true, zenoss.visualization.chart, {
        dc : {
            stacked : stacked
        }
    });
}());
