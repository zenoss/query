(function() {
    "use strict";
    var area = {
        required : {
            defined : 'dc',
            source : [ 'crossfilter.min.js', 'dc.min.js', 'css/dc.css' ]
        },

        Chart : function() {
            var _compositeChart = null;
            var _datasets = [];

            this.compositeChart = function(_) {
                if (!arguments.length) {
                    return _compositeChart;
                }
                _compositeChart = _;
            };

            this.add = function(ds) {
                _datasets.push(ds);
            };

            this.dataset = function(n) {
                return _datasets[n];
            };

            this.compose = function() {
                var subs = [];
                this.forEach(function(ds) {
                    subs.push(ds.lineChart());
                });

                _compositeChart.compose(subs);
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
                _group = zenoss.visualization.__reduceMax(_dimension.group());
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

        color : function(chart, closure, idx) {
            return {
                'color' : d3.scale.category10().range()[idx],
                'opacity' : 1
            };
        },

        update : function(chart, data) {
            var _chart = chart.closure, i, cc, ds;

            // Update the svg data
            chart.svg.datum(chart.plots);

            // Replace the plot data on each of the charts
            // with the new data
            for (i = 0; i < chart.plots.length; i += 1) {
                ds = _chart.dataset(i);
                ds.plot(chart.plots[i]);
                ds.lineChart().group(ds.group());
            }

            // Update the primary dimension and group
            cc = _chart.compositeChart();
            cc.dimension(_chart.dataset(0).dimension());
            cc.group(_chart.dataset(0).group());

            dc.renderAll('zenoss');
        },

        resize : function(chart, height) {
            var _chart = chart.closure;
            _chart.compositeChart().height(height);
            _chart.compositeChart().redraw();
        },

        build : function(chart) {
            var _chart = new zenoss.visualization.chart.dc.area.Chart();
            var lc;

            chart.plots.forEach(function(plot) {
                _chart
                        .add(new zenoss.visualization.chart.dc.area.DataSet(
                                plot));
            });

            var cc = dc.compositeChart(chart.containerSelector, "zenoss");
            _chart.compositeChart(cc);

            _chart.forEach(function(ds) {
                lc = dc.lineChart(_chart.compositeChart(), 'zenoss');
                lc.group(ds.group());
                lc.renderArea(true);
                lc.title(function(d) {
                    return d.value + ' at ' + new Date(d.key).toLocaleString();
                });
                lc.renderTitle(true);
                lc.dotRadius(10);
                ds.lineChart(lc);
            });
            _chart.compose();
            cc.renderHorizontalGridLines(true);
            cc.renderVerticalGridLines(true);
            cc.dimension(_chart.dataset(0).dimension());
            cc.group(_chart.dataset(0).group());
            cc.width($(chart.svgwrapper).width());
            cc.height($(chart.svgwrapper).height());
            cc.transitionDuration(500);
            cc.round(d3.time.second);
            cc.xUnits(d3.time.second);
            cc.elasticY(true);
            cc.elasticX(true);
            cc.brushOn(false);
            cc
                    .x(d3.time
                            .scale()
                            .domain(
                                    [
                                            new Date(chart.plots[0].values[0].x),
                                            new Date(
                                                    chart.plots[0].values[chart.plots[0].values.length - 1].x) ]));

            return _chart;
        },

        render : function(chart) {
            dc.renderAll('zenoss');
        }
    };

    $.extend(true, zenoss.visualization.chart, {
        dc : {
            area : area
        }
    });
}());
