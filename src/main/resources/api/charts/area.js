(function() {
    "use strict";
    var area = {
        required : {
            defined : 'nv',
            source : [ 'nv.d3.min.js', 'css/nv.d3.css' ]
        },

        color : function(chart, impl, idx) {
            return {
                'color' : impl.model().color()(0, idx),
                'opacity' : 1
            };
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

        update : function(chart, data) {
            // OK. Area charts really want data points to match up on keys,
            // which
            // makes sense as this is how they stack things. To make this work
            // we
            // going to walk the points and make sure they match
            zenoss.visualization.__cull(chart);

            var _chart = chart.closure;

            _chart.model().xAxis.tickFormat(function(ts) {
                return zenoss.visualization.tickFormat(data.startTimeActual,
                        data.endTimeActual, ts);
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
            // OK. Area charts really want data points to match up on keys,
            // which
            // makes sense as this is how they stack things. To make this work
            // we
            // going to walk the points and make sure they match
            zenoss.visualization.__cull(chart);

            var _chart = new zenoss.visualization.chart.area.Chart();
            var model = nv.models.stackedAreaChart();
            _chart.model(model);

            model.xAxis.tickFormat(function(ts) {
                return zenoss.visualization.tickFormat(data.startTimeActual,
                        data.endTimeActual, ts);
            });
            model.clipEdge(true);
            model.height($(chart.svgwrapper).height());
            model.width($(chart.svgwrapper).width());
            chart.svg.datum(chart.plots);

            nv.addGraph(function() {
                chart.svg.transition().duration(500).call(model);
                nv.utils.windowResize(function() {
                    chart.svg.call(model);
                });
            });
            return _chart;
        },
        render : function() {

        }
    };
    
    

    $.extend(true, zenoss.visualization.chart, {
        area : area
    });

}());