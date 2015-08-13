
(function() {
    "use strict";

    function Chart() {
        var _model = null;

        this.model = function(_) {
            if (!arguments.length) {
                return _model;
            }
            _model = _;
        };
    }
    function addHovers(chart) {
        // add hovers
        chart.svg.selectAll('g.nv-series').on('mouseover', function(d){
            console.log('got here');
            var key = d.key;
            chart.svg.selectAll('.nv-group').style('opacity', function(d) {
                if (d.key === key) {
                    return 1;
                }
                return 0.15;
            });
            chart.svg.selectAll('.nv-group').style('stroke-width', function(d) {
                if (d.key === key) {
                    return 4;
                }
                return 1.5;
            });
        });
        chart.svg.selectAll('g.nv-series').on('mouseout', function(d) {
            chart.svg.selectAll('.nv-group').style('opacity', 1);
            chart.svg.selectAll('.nv-group').style('stroke-width', 1.5);
        });

    }

    var line = {
        required : {
            defined : 'nv',
            source : [ 'nv.d3.min.js', 'css/nv.d3.css', 'css/jquery-ui.css' ]
        },

        color : function(chart, impl, idx) {
            return {
                'color' : impl.model().color()(0, idx),
                'opacity' : 1
            };
        },

        update : function(chart, data) {
            var _chart = chart.closure;

            chart.updateXLabels(data.startTimeActual * 1000, data.endTimeActual * 1000, _chart.model().xAxis);

            // if a max or min y are set
            if (chart.maxy !== undefined || chart.miny !== undefined) {
                _chart.model().yDomain(chart.calculateYDomain(chart.miny, chart.maxy, data));
            }
            // find out which series are disabled or not
            var disabled = {};
            chart.svg.selectAll("g.nv-series")
                .each(function(d) {
                    disabled[d.key] = d.disabled;
                });

            // make sure when we update we preserve the disabled behavior
            chart.plots.forEach(function(d) {
                if (disabled[d.key] === true) {
                    d.disabled = true;
                }
            });

            chart.svg
                .datum(chart.plots)
                .transition().duration(0)
                .call(_chart.model());

            this.styleThresholds(chart.div);
            this.styleProjections(chart);
            addHovers(chart);
        },

        build : function(chart, data) {
            var _chart = new Chart();
            var model = nv.models.lineChart();

            _chart.model(model);

            model.useInteractiveGuideline(true);

            chart.updateXLabels(data.startTimeActual * 1000, data.endTimeActual * 1000, _chart.model().xAxis);

            // ensure that there are no duplicate ticks on the y axis
            model.yAxis.tickFormat(chart.dedupeYLabels(model));
            model.yAxis.axisLabel(chart.yAxisLabel);

            // set a different value formatter for tooltip values
            // than for axis labels
            model.valueFormatter(function(d){
                // the second arg to formatValue forces the value's
                // unit to be derived from the value instead of the
                // preferred unit set on the entire chart
                return chart.formatValue(d, true);
            });

            // if a max or min y are set
            if (chart.maxy !== undefined || chart.miny !== undefined) {
                model.yDomain(chart.calculateYDomain(chart.miny, chart.maxy, data));
            }

            // magic to make the yaxis label show up
            // see https://github.com/novus/nvd3/issues/17
            model.margin({
                left : 90
            });

            model.height($(chart.svgwrapper).height());
            model.width($(chart.svgwrapper).width() - 10);

            chart.svg.datum(chart.plots);

            nv.addGraph(function() {
                chart.svg.transition().duration(0).call(model);
                this.styleThresholds(chart.div);
                this.styleProjections(chart);
                nv.utils.windowResize(function() {
                    chart.svg.call(model);
                    this.styleThresholds(chart.div);
                    this.styleProjections(chart);
                }.bind(this));
                addHovers(chart);
            }.bind(this));

            // don't draw null point lines and areas
            model.lines
                .defined(function(d){
                    return d.y !== null;
                })
                .isArea(function(d) {
                    return d.fill;
                });


            return _chart;
        },

        resize : function(chart) {
            var _chart = chart.closure, model = _chart.model();
            model.height($(chart.svgwrapper).height());
            chart.svg.transition().duration(0).call(model);
        },

        render : function() {
        },

        // look for series' that are actually thresholds
        // and style them differently
        styleThresholds: function(el){
            $(el).find(".nv-series .nv-legend-text").each(function(i, legend){
                if(~$(legend).text().indexOf("*")){
                    legend.classList.add("threshold");
                }
            });
        },
        /**
         * All plot series that have the "projection" attribute to true will
         * be styled distinctively
         **/
        styleProjections: function(chart) {
            var i, n;
            for (i = 0; i < chart.plots.length; i ++ ) {
                if (chart.plots[i].projection) {
                    n = i - 1;
                    d3.select('#' + chart.name).selectAll('.nv-series-' +
                                                n.toString()).selectAll('.nv-line').style("stroke-dasharray", ('3, 3'));

                }
            }
        }
    };

    $.extend(true, zenoss.visualization.chart, {
        line : line
    });

}());
