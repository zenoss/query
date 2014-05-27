/**
 * ZenVis.js
 * Fetches data from the API and wires it to a chart type for
 * actual processing and rendering.
 */

var zenoss = zenoss || {};

(function(){
	"use strict";
	
	var defaults = {
		url: "http://10.87.110.245:8888/api/performance/query",
		// url: "http://localhost:3006/api/performance/query/",
		colors: ["#4878A8", "#A8D8F0", "#F07830", "#781800", "#487890"],
		config: {
			start: "1h-ago",
			end: "now",
			series: [],
			// TODO - default to something useful?
			units: function(x){return x;},
			type: "basic",
			autoupdate: 10000
		},
		series: {
			type: "line",
			aggregator: "avg"
		}
	};
	var ids = 0;

	// TODO - dynamically load/build chartRepo?
	var chartRepo = zenoss.viz.charts;

	function ZenVis(config){
		if(!config){
			throw new Error("Must provide a configuration object for ZenVis");
		}

		this.id = ids++;

		// TODO - get this from user?
		this.url = defaults.url;

		// add default values to config objects
		this.config = zenoss.utils.merge(defaults.config, config);
		// default each series and add an id
		this.config.series = this.config.series.map(function(series){
			series.id = zenoss.utils.randomId();
			return zenoss.utils.merge(defaults.series, series);
		});

		// build stuff
		this.type = config.type;
		this.seriesConfig = buildSeriesConfig(this.config);
		this.apiQuery = buildApiQuery(this.config);

		// prep $el
		// TODO - find a way to create $el if not supplied. d3
		// requires a selector and $el wont be in the dom if its
		// created here
		this.$el = config.$el;
		this.$el.addClass("chartContainer");
		this.$el.append("<div id='zenViz_"+ this.id +"' class='zenchart'></div>");

		// give this chart event emitting powers
		zenoss.utils.eventEmitter.call(this);

		// specialize this chart
		chartRepo[this.config.type].call(this);
		
		// TODO - this property is dumb
		if(!this.config._dontFetch) this.fetch();

		if(this.config.autoUpdate){
			this._autoUpdateFrequency = this.config.autoUpdate;
			this.startAutoUpdate();
		}

		window.c = this;
	}

	ZenVis.prototype = {
		constructor: ZenVis,

		// create an alias of these guys somewhere
		// so you dont access them via ZenVis.prototype.whatever
		types: {
			BASIC: "basic"
		},

		units: {
			BYTES: function(x){
				return x;
			},
			PERCENT: function(x){
				return d3.format(".1f")(x * 100) +"%";
			}
		},

		/**
		 * request latest data from API
		 */
		fetch: function(){

			// TODO - query from last timestamp
			// instead of querying full range?
			
			$.ajax({
				url: this.url,
				data: JSON.stringify(this.apiQuery),
				method: "POST",
				contentType: "application/json",
				success: this.update.bind(this),
				error: this._error.bind(this)
			});
		},

		/**
		 * autofetch every this._autoUpdateFrequency ms
		 */
		startAutoUpdate: function(){
			this.stopAutoUpdate();
			this._shouldAutoUpdate = true;
			this._updateInterval = setInterval(function(){
				console.log("updating");
				if(this._shouldAutoUpdate) this.fetch();
			}.bind(this), this._autoUpdateFrequency);
		},
		stopAutoUpdate: function(){
			this._shouldAutoUpdate = false;
			clearInterval(this._updateInterval);
		},

		/**
		 * emits `update` event when data is returned from api
		 */
		update: function(data){
			this._emit("update", data);
		},

		/**
		 * emits `error` event if things go south
		 */
		_error: function(){
			console.error(arguments);
			this._emit("error", arguments);
		}
	};

	zenoss.ZenVis = ZenVis;


	
	/**
	 * creates a configuration object for each series
	 * in the graph
	 */
	function buildSeriesConfig(config){
		var seriesConfig = {};
		if(config.series){
			config.series.forEach(function(series, i){
				if(!series.metric){
					console.error("Invalid series. Missing metric name", series);
				}

				seriesConfig[series.metric] = {
					name: series.name || series.metric,
					color: series.color || defaults.colors[i],
					type: series.type,
					aggregator: series.aggregator
				};
			});
		}
		return seriesConfig;
	}

	// formats configuration so that the query service
	// can work with it
	function buildApiQuery(config){

		if(!config.series || !config.series.length){
			throw new Error("Must provide at least one series");
		}

		var query = {
			start: config.start,
			end: config.end,
			metrics: [],
			series: true,
			// returnset
			// downsample
			// tags
			// grouping
		};

		// other possible config options:
		// footer: true
		// yAxisLabel: "bits/sec",
		// miny: 0
		// maxy: null
		// format: "%5.2f"
		// timezone: "America/Chicago"
		// autoscale: {factor: 1000, ceiling: 3}

		config.series.forEach(function(series){

			if(!series.metric){
				console.error("Invalid series. Missing metric name", series);
			}

			// merge will strip any keys with value `undefined`
			query.metrics.push(zenoss.utils.merge({
				id: series.id,
				type: series.type,
				metric: series.metric,
				aggregator: series.aggregator,
				tags: series.tags,
				downsample: series.downsample,
				rate: !!series.rate,
				rateOptions: series.rate,
				expression: series.expression
			}));
		});

		return query;
	}

})();