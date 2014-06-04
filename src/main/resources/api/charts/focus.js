(function() {
    "use strict";
    var focus = {
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
            };
        },

        color : function(chart, impl, idx) {
            return {
                'color' : impl.model().color()(0, idx),
                'opacity' : 1
            };
        },

        update : function(chart, data) {
            var _chart = chart.closure;

            _chart.model().xAxis.tickFormat(function(ts) {
                return zenoss.visualization.tickFormat(data.startTimeActual,
                        data.endTimeActual, ts, chart.timezone);
            });
            _chart.model().x2Axis.tickFormat(function(ts) {
                return zenoss.visualization.tickFormat(data.startTimeActual,
                        data.endTimeActual, ts, chart.timezone);
            });

            chart.svg.datum(chart.plots).transition().duration(0).call(
                    _chart.model());
        },

        resize : function(chart) {
            var _chart = chart.closure, model = _chart.model();
            model.height($(chart.svgwrapper).height());
            chart.svg.transition().duration(0).call(model);
        },

        build : function(chart, data) {

            var _chart = new zenoss.visualization.chart.focus.Chart();
            var model = nv.models.lineWithFocusChart();
            _chart.model(model);

            model.xAxis.tickFormat(function(ts) {
                return zenoss.visualization.tickFormat(data.startTimeActual,
                        data.endTimeActual, ts, chart.timezone);
            });
            model.x2Axis.tickFormat(function(ts) {
                return zenoss.visualization.tickFormat(data.startTimeActual,
                        data.endTimeActual, ts, chart.timezone);
            });
            model.yAxis.tickFormat(function(value){
                return chart.formatValue(value);
            });
            model.y2Axis.tickFormat(function(value){
                return chart.formatValue(value);
            });
            model.height($(chart.svgwrapper).height());
            model.width($(chart.svgwrapper).width());
            chart.svg.datum(chart.plots);

            nv.addGraph(function() {
                chart.svg.transition().duration(500).call(model);
                nv.utils.windowResize(model.update);
            });

            return _chart;
        },

        render : function() {

        }
    };

    $.extend(true, zenoss.visualization.chart, {
        focus : focus
    });
}());