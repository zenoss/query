var zenoss = zenoss || {};

// TODO - create util for this?
zenoss.viz = zenoss.viz || {};
zenoss.viz.charts = zenoss.viz.charts || {};

/**
 * sparkpanel_old creates and configures many instances
 * of c3 charts as sparklines and manages them all
 *
 * sparkpanel creates many instances of the sparkline
 * chart type and updates their model, but does not
 * manage the internals
 */
(function(){
	"use strict";

	var delegateSparks = function(data){

		data.results.forEach(function(result){

			var spark,
				id = result.id;

			// if this sparkline hasnt been created yet,
			// create it
			if(!this.sparks[id]){
				console.log("creating spark", id);
				var $sparkWrapper = $("<div>"),
					series;

				this.$el.append($sparkWrapper);

				// TODO - use a unique id here
				// TODO - make sure series exists			
				series = this.config.series.filter(function(el){ return el.id === result.id; });

				this.sparks[id] = new zenoss.ZenVis(zenoss.utils.merge(this.config, {
					$el: $sparkWrapper,
					type: "sparkline",
					_dontFetch: true, // TODO - this property is dumb
					autoUpdate: 0,
					series: series
				}));
			}

			// grab the sparkline and pass it the data
			// specific to it
			console.log("updating spark", id);
			spark = this.sparks[id];
			spark.update(zenoss.utils.merge(data.results, {results: [result]}));
		}.bind(this));
		

	};

	zenoss.viz.charts.sparkpanel = function(){
		this.sparks = {};
		this.on("update", function(data){
			delegateSparks.call(this, data);
		});
	};

})();