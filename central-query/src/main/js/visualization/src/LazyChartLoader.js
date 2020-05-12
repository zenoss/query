/**
 * LazyChartLoader.js
 */
(function () {
    "use strict";

    /**
     * This class is used for manage Chart loading. This class limits the number of graphs
     * that make queries simultaneously. 
     *
     * @constructor
     * @param maxParallelsCharts
     *            the number of graphs that are loaded simultaneously
     */

    LazyChartLoader = function (maxParallelsCharts) {
        this.maxParallelsCharts = maxParallelsCharts;
        this.activeCall = 0;
        this.chartQueue = [];
    };

    LazyChartLoader.prototype = {
        constructor: LazyChartLoader,

        addChart: function (chart) {
            this.chartQueue.push(chart);
            this.__checkQueue();
        },

        __onCompleteOrFailure: function () {
            this.activeCall--;
            this.__checkQueue();
        },

        __onChartUpdate: function (updatePromise) {
            updatePromise.then(
                this.__onCompleteOrFailure.bind(this),
                this.__onCompleteOrFailure.bind(this)
            );
        },

        __checkQueue: function () {
            if (this.chartQueue.length && this.activeCall <= this.maxParallelsCharts) {
                var chart = this.chartQueue.shift();
                if (!chart) {
                    return;
                }
                this.activeCall++;
                chart.onUpdate = this.__onChartUpdate.bind(this);
                chart.update();
            }
        }
    }
})();
