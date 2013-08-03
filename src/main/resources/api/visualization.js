/**
 * @overview Provides objects and convenience methods to construct and
 *           manipulate Zenoss visualization graphs.
 * @copyright 2013, Zenoss, Inc; All rights reserved
 */
/**
 * @namespace
 */
var zenoss = {
	/**
	 * @namespace
	 */
	visualization : {
		/**
		 * Used to enable (true) or disable (false) debug output to the browser
		 * console
		 * 
		 * @default false
		 */
		debug : false,

		/**
		 * Used to specify the base URL that is the endpoint for the Zenoss
		 * metric service.
		 * 
		 * @access public
		 * @default http://localhost:8080
		 */
		url : "http://localhost:8080",

		/**
		 * Used to format dates for the output display in the footer of a chart.
		 * 
		 * @param {Date}
		 *            date the date to be formated
		 * @returns a string representation of the date
		 * @access public
		 */
		dateFormatter : function(date) {
			return date.toLocaleString();
		},

		/**
		 * Used to generate the date/time to be displayed on a tick mark. This
		 * takes into account the range of times being displayed so that common
		 * data can be removed.
		 * 
		 * @param {Date}
		 *            start the start date of the time range being considered
		 * @param {Date}
		 *            end the end of the time range being considerd
		 * @param {timestamp}
		 *            ts the timestamp to be formated in ms since epoch
		 * @returns string representation of the timestamp
		 * @access public
		 */
		tickFormat : function(start, end, ts) {
			if (start.getFullYear() == end.getFullYear()) {
				if (start.getMonth() == end.getMonth()) {
					if (start.getDate() == end.getDate()) {
						if (start.getHours() == end.getHours()) {
							if (start.getMinutes() == end.getMinutes()) {
								return d3.time.format('::%S')(new Date(ts));
							}
							return d3.time.format(':%M:%S')(new Date(ts));
						}
						return d3.time.format('%H:%M:%S')(new Date(ts));
					}
				}
				return d3.time.format('%m/%d %H:%M:%S')(new Date(ts));
			}
			return d3.time.format('%x %X')(new Date(ts));
		},

		/**
		 * Culls the plots in a chart so that only data points with a common
		 * time stamp remain.
		 * 
		 * @param the
		 *            chart that contains the plots to cull
		 * @access private
		 */
		__cull : function(chart) {
			// If there is only one plot in the chart we are done, there is
			// nothing
			// to do.
			if (chart.plots.length < 2) {
				return;
			}

			var keys = [];
			chart.plots.forEach(function(plot) {
				plot.values.forEach(function(v) {
					if (typeof keys[v.x] == 'undefined') {
						keys[v.x] = 1;
					} else {
						keys[v.x] += 1;
					}
				});
			});

			// At this point, any entry in the keys array with a count of
			// chart.plots.length is a key in every plot and we can use, so now
			// we walk through the plots again removing any invalid key
			chart.plots.forEach(function(plot) {
				for ( var i = plot.values.length - 1; i >= 0; --i) {
					if (keys[plot.values[i].x] != chart.plots.length) {
						plot.values.splice(i, 1);
					}
				}
			});
		},

		__reduceMax : function(group) {
			return group.reduce(function(p, v) {
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
			});
		},

		/**
		 * Used to augment the div element with an error message when an error
		 * is encountered while creating a chart.
		 * 
		 * @access private
		 * @param {string}
		 *            name the ID of the HTML div element to augment
		 * @param {object}
		 *            err the error object
		 * @param {string}
		 *            detail the detailed error message
		 */
		__showError : function(name, err, detail) {
			$('#' + name).html('<span class="zenerror">' + detail + '</span>');
		},

		/**
		 * This class should not be instantiated directly unless the caller
		 * really understand what is going on behind the scenes as there is a
		 * lot of concurrent processing involved as many components are loaded
		 * dynamically with a delayed creation or realization.
		 * 
		 * Instead instance of this class are better created with the
		 * zenoss.visualization.chart.create method.
		 * 
		 * @access private
		 * @constructor
		 * @param {string}
		 *            name the name of the HTML div element to augment with the
		 *            chart
		 * @param {object}
		 *            config the values specified as the configuration will
		 *            augment / override options loaded from any chart template
		 *            that is specified, thus if no chart template is specified
		 *            this configuration parameter can be used to specify the
		 *            entire chart definition.
		 */
		Chart : function(name, config) {
			var self = this;
			this.name = name;
			this.config = config;
			this.div = $('#' + this.name);

			this.svgwrapper = document.createElement('div');
			$(this.svgwrapper).addClass('zenchart');
			$(this.div).append($(this.svgwrapper));
			this.containerSelector = '#' + name + ' .zenchart';

			this.footer = document.createElement('div');
			$(this.footer).addClass('zenfooter');
			$(this.div).append($(this.footer));

			/*
			 * $(self.svgwrapper).outerHeight( $(self.div).height() -
			 * $(self.footer).outerHeight());
			 */

			this.svg = d3.select(this.svgwrapper).append('svg');
			this.request = this.__buildDataRequest(this.config);

			if (zenoss.visualization.debug) {
				console.groupCollapsed('POST Request Object');
				console.log(zenoss.visualization.url + '/query/performance')
				console.log(this.request);
				console.groupEnd();
			}

			// Sanity Check. If the request contained no metrics to query then
			// log this information as a warning, as it really does not make
			// sense.
			if (typeof this.request.metrics == 'undefined') {
				console
						.warn('Chart configuration contains no metric sepcifications. No data will be displayed.');
			} else {
				$
						.ajax({
							'url' : zenoss.visualization.url
									+ '/query/performance',
							'type' : 'POST',
							'data' : JSON.stringify(this.request),
							'dataType' : 'json',
							'contentType' : 'application/json',
							'success' : function(data) {
								self.plots = self.__processResult(self.request,
										data);
								// Set default type of the chart if it was not
								// set
								if (typeof self.config.type == 'undefined') {
									self.config.type = 'line';
								}
								self.__render(data);
							},
							'error' : function(res) {
								// Many, many reasons that we might have gotten
								// here, with most of them we are not able to
								// detect why.
								// If we have a readystate of 4 and an response
								// code in the
								// 200s that likely means we were unable to
								// parse the JSON
								// returned from the server. If not that then
								// who knows
								// ....
								if (res.readyState == 4
										&& Math.floor(res.status / 100) == 2) {
									var detail = 'Severe: Unable to parse data returned from Zenoss metric service as JSON object. Please copy / paste the REQUEST and RESPONSE written to your browser\'s Java Console into an email to Zenoss Support';
									console
											.group('Severe error, please report');
									console
											.error('REQUEST : POST /query/performance: '
													+ JSON.stringify(request));
									console.error('RESPONSE: '
											+ res.responseText);
									console.groupEnd();
									zenoss.visualization.__showError(self.name,
											{}, detail);
								} else {
									try {
										var err = JSON.parse(res.responseText);
										var detail = 'An unexpected failure response was received from the server. The reported message is: '
												+ err.errorSource
												+ ' : '
												+ err.errorMessage;
										console.error(detail);
										zenoss.visualization.__showError(
												self.name, err, detail);
									} catch (e) {
										var detail = 'An unexpected failure response was received from the server. The reported message is: '
												+ res.statusText
												+ ' : '
												+ res.status;
										console.error(detail);
										zenoss.visualization.__showError(
												self.name, err, detail);
									}
								}
							}
						});
			}
		},

		/**
		 * @namespace
		 */
		chart : {
			/**
			 * Looks up a chart instance by the given name and, if found,
			 * updates the chart instance with the given changes. To remove an
			 * item (at the first level or the change structure) set its values
			 * to the negative '-' symbol.
			 * 
			 * @param {string}
			 *            name the name of the chart to update
			 * @param {object}
			 *            changes a configuration object that holds the changes
			 *            to the chart
			 */
			update : function(name, changes) {
				var found = zenoss.visualization.__charts[name];
				if (typeof found == 'undefined') {
					console.warn('Attempt to modify (range) a chart, "' + name
							+ '", that does not exist.');
					return;
				}
				found.update(changes);
			},

			/**
			 * Constructs a zenoss.visualization.Chart object, but first
			 * dynamically loading any chart definition required, then
			 * dynamically loading all dependencies, and finally creating the
			 * chart object. This method should be used to create a chart as
			 * opposed to calling "new" directly on the class.
			 * 
			 * @param {string}
			 *            name the name of the HTML div element to augment with
			 *            the chart
			 * @param {string}
			 *            [template] the name of the chart template to load. The
			 *            chart template will be looked up as a resource against
			 *            the Zenoss metric service.
			 * @param {object}
			 *            [config] the values specified as the configuration
			 *            will augment / override options loaded from any chart
			 *            template that is specified, thus if no chart template
			 *            is specified this configuration parameter can be used
			 *            to specify the entire chart definition.
			 * @param {callback}
			 *            [success] this callback will be called when a
			 *            zenoss.visualization.Chart object is successfully
			 *            created. The reference to the Chart object will be
			 *            passed as a parameter to the callback.
			 * @param {callback}
			 *            [fail] this callback will be called when an error is
			 *            encountered during the creation of the chart. The
			 *            error that occurred will be passed as a parameter to
			 *            the callback.
			 */
			create : function(name, arg1, arg2, success, fail) {

				function loadChart(name, callback, onerror) {
					if (zenoss.visualization.debug) {
						console.log('Loading chart from: '
								+ zenoss.visualization.url + '/chart/name/'
								+ name)
					}
					$
							.ajax({
								'url' : zenoss.visualization.url
										+ '/chart/name/' + name,
								'type' : 'GET',
								'dataType' : 'json',
								'contentType' : 'application/json',
								'success' : function(data) {
									callback(data);
								},
								'error' : function(response) {
									console.error(response.responseText);
									var err = JSON.parse(response.responseText);
									var detail = 'Error while attempting to fetch chart resource with the name "'
											+ name
											+ '", via the URL "'
											+ zenoss.visualization.url
											+ '/chart/name/'
											+ name
											+ '", the reported error was "'
											+ err.errorSource
											+ ':'
											+ err.errorMessage + '"';
									if (typeof onerror != 'undefined') {
										onerror(err, detail);
									}
								}
							});
				}

				if (typeof arg1 == 'string') {
					// A chart template name was specified, so we need to first
					// load that template and then create the chart based on
					// that.
					var config = arg2;
					if (typeof jQuery == 'undefined') {
						zenoss.visualization.__bootstrap(function() {
							loadChart(arg1, function(template) {
								var result = new zenoss.visualization.Chart(
										name, zenoss.visualization.__merge(
												template, config));
								zenoss.visualization.__charts[name] = result;
								return result;
							}, function(err, detail) {
								zenoss.visualization.__showError(name, err,
										detail);
							});
						});
						return;
					}
					loadChart(arg1,
							function(template) {
								var result = new zenoss.visualization.Chart(
										name, zenoss.visualization.__merge(
												template, config));
								zenoss.visualization.__charts[name] = result;
								return result;
							}, function(err, detail) {
								zenoss.visualization.__showError(name, err,
										detail);
							});
					return;
				}

				var template = null;
				var config = arg1;

				if (typeof jQuery == 'undefined') {
					zenoss.visualization
							.__bootstrap(function() {
								var result = new zenoss.visualization.Chart(
										name, zenoss.visualization.__merge(
												template, config));
								zenoss.visualization.__charts[name] = result;
								return result;
							});
					return;
				}
				var result = new zenoss.visualization.Chart(name,
						zenoss.visualization.__merge(template, config));
				zenoss.visualization.__charts[name] = result;
				return result;
			}
		},

		/**
		 * Used to track dependency loading, including the load state (loaded /
		 * loading) as well as the callback that will be called when a
		 * dependency load has been completed.
		 * 
		 * @access private
		 */
		__dependencies : {},

		/**
		 * Used to track the charts that have been created and the names to
		 * which they are associated
		 * 
		 * @access private
		 */
		__charts : {},

		/**
		 * Main entry point for web pages. This method is used to first
		 * bootstrap the library and then call the callback to create charts.
		 * Because of the updated dependency loading capability, this method is
		 * not strictly needed any more, but will be left around for posterity.
		 * 
		 * @param {callback}
		 *            callback method called after all the pre-requisite
		 *            JavaScript libraries are loaded.
		 */
		load : function(callback) {
			zenoss.visualization.__bootstrap(callback);
		}
	}
}

if (typeof String.prototype.endsWith !== 'function') {
	String.prototype.endsWith = function(suffix) {
		return this.indexOf(suffix, this.length - suffix.length) !== -1;
	};
}

if (typeof String.prototype.startsWith != 'function') {
	String.prototype.startsWith = function(str) {
		return this.slice(0, str.length) == str;
	};
}

/**
 * Sets the box in the footer for the given plot (specified by index) to the
 * specified color. The implementation of this is dependent on how the footer is
 * constructed (see __buildFooter).
 * 
 * @access private
 * @param {int}
 *            idx the index of the plot whose color should be set, corresponds
 *            to which row in the table + 2
 * @param {color}
 *            the color to which the box should be set.
 */
zenoss.visualization.Chart.prototype.__setFooterBoxColor = function(idx, color) {
	var box = $($(this.table).find('.zenfooter_box')[idx]);
	box.css('background-color', color.color);
	box.css('opacity', color.opacity);
}

zenoss.visualization.Chart.prototype.__updateFooter = function(data) {

	// The first table row is for the dates, the second is a header and then
	// a row for each plot.
	var rows = $(this.table).find('tr');

	$($(rows[0]).find('td')).html(
			zenoss.visualization.dateFormatter(new Date(data.startTimeActual))
					+ ' to '
					+ zenoss.visualization.dateFormatter(new Date(
							data.endTimeActual)) + ' ('
					+ jstz.determine().name() + ')');

	// Calculate the summary values from the data and place the date in the
	// the table.
	for ( var i = 0; i < this.plots.length; ++i) {
		var plot = this.plots[i];
		var vals = [ 0, -1, -1, 0 ];
		var cur = 0;
		var min = 1;
		var max = 2;
		var avg = 3;
		var init = false;
		plot.values.forEach(function(v) {
			if (!init) {
				vals[min] = v.y;
				vals[max] = v.y;
				init = true;
			} else {
				vals[min] = Math.min(vals[min], v.y);
				vals[max] = Math.max(vals[max], v.y);
			}
			vals[avg] += v.y;
			vals[cur] = v.y;
		});
		vals[avg] = vals[avg] / plot.values.length;

		// The first column is the color, the second is the metric name,
		// followed byt the values
		var cols = $(rows[2 + i]).find('td');

		// Metric name
		var label = plot.key;
		if (label.indexOf('{') > -1) {
			label = label.substring(0, label.indexOf('{')) + '{*}'
		}
		$(cols[1]).html(label);

		for ( var v = 0; v < vals.length; ++v) {
			$(cols[2 + v]).html(vals[v].toFixed(2));
		}
	}
}

/**
 * Constructs the chart footer for a given chart. The footer will contain
 * information such as the date range and key values (ending, min, max, avg) of
 * each plot on the chart.
 * 
 * @access private
 * @param {object}
 *            config the charts configuration
 * @param {object}
 *            data the data returned from the metric service that contains the
 *            data to be charted
 */
zenoss.visualization.Chart.prototype.__buildFooter = function(config, data) {
	this.table = document.createElement('table');
	$(this.table).addClass('zenfooter_content');
	$(this.table).addClass('zenfooter_text');
	$(this.footer).append($(this.table));

	// One row for the date range of the chart
	var tr = document.createElement('tr');
	var td = document.createElement('td');
	var dates = document.createElement('span');
	$(td).addClass('zenfooter_dates');
	$(td).attr('colspan', 6);
	$(dates).addClass('zenfooter_dates_text');
	$(this.table).append($(tr));
	$(tr).append($(td));
	$(td).append($(dates));

	if (typeof config.footer == 'string' && config.footer == 'range') {
		return;
	}

	// One row for the stats table header
	var tr = document.createElement('tr');
	[ '', 'Metric', 'Ending', 'Minimum', 'Maximum', 'Average' ]
			.forEach(function(s) {
				var th = document.createElement('th');
				$(th).addClass('footer_header');
				$(th).html(s);
				if (s.length == 0) {
					$(th).addClass('zenfooter_box_column');
				}
				$(tr).append($(th));
			});
	$(this.table).append($(tr));

	// One row for each of the metrics
	var self = this;
	this.plots.forEach(function(p) {
		var tr = document.createElement('tr');

		// One column for the color
		var td = document.createElement('td');
		$(td).addClass('zenfooter_box_column');
		var d = document.createElement('div');
		$(d).addClass('zenfooter_box');
		$(d).css('backgroundColor', 'white');
		$(td).append($(d));
		$(tr).append($(td));

		// One column for the metric name
		td = document.createElement('td');
		$(td).addClass('zenfooter_data');
		$(td).addClass('zenfooter_data_text');
		$(tr).append($(td));

		// One col for each of the metrics stats
		[ 1, 2, 3, 4 ].forEach(function(v) {
			td = document.createElement('td');
			$(td).addClass('zenfooter_data');
			$(td).addClass('zenfooter_data_number');
			$(tr).append($(td));
		});

		$(self.table).append($(tr));
	});

	// Fill in the stats table
	this.__updateFooter(data);
}

/**
 * Updates a graph with the changes specified in the given change set. To remove
 * a value from the configuration its value should be set to a negative sign,
 * '-'.
 * 
 * @param {object}
 *            changeset updates to the existing graph's configuration.
 */
zenoss.visualization.Chart.prototype.update = function(changeset) {

	// This function is really meant to only handle given types of changes,
	// i.e. we don't expect that you can change the type of the graph but you
	// should be able to change the date range.
	var self = this;
	this.config = zenoss.visualization.__merge(this.config, changeset);

	// A special check for the removal of items from the config. If the value
	// for any item in the change set is '-', then we delete that key.
	var kill = [];
	for ( var property in this.config) {
		if (this.config.hasOwnProperty(property)) {
			if (this.config[property] == '-') {
				kill.push(property);
			}
		}
	}
	kill.forEach(function(p) {
		delete self.config[p];
	});

	this.request = this.__buildDataRequest(this.config);
	$
			.ajax({
				'url' : zenoss.visualization.url + '/query/performance',
				'type' : 'POST',
				'data' : JSON.stringify(this.request),
				'dataType' : 'json',
				'contentType' : 'application/json',
				'success' : function(data) {
					self.plots = self.__processResult(self.request, data);
					// Set default type of the chart if it was not
					// set
					if (typeof self.config.type == 'undefined') {
						self.config.type = 'line';
					}
					self.__updateData(data)
				},
				'error' : function(res) {
					// Many, many reasons that we might have gotten
					// here, with most of them we are not able to
					// detect why.
					// If we have a readystate of 4 and an response
					// code in the
					// 200s that likely means we were unable to
					// parse the JSON
					// returned from the server. If not that then
					// who knows
					// ....
					if (res.readyState == 4
							&& Math.floor(res.status / 100) == 2) {
						var detail = 'Severe: Unable to parse data returned from Zenoss metric service as JSON object. Please copy / paste the REQUEST and RESPONSE written to your browser\'s Java Console into an email to Zenoss Support';
						console.group('Severe error, please report');
						console.error('REQUEST : POST /query/performance: '
								+ JSON.stringify(request));
						console.error('RESPONSE: ' + res.responseText);
						console.groupEnd();
						zenoss.visualization.__showError(self.name, {}, detail);
					} else {
						try {
							var err = JSON.parse(res.responseText);
							var detail = 'An unexpected failure response was received from the server. The reported message is: '
									+ err.errorSource
									+ ' : '
									+ err.errorMessage;
							console.error(detail);
							zenoss.visualization.__showError(self.name, err,
									detail);
						} catch (e) {
							var detail = 'An unexpected failure response was received from the server. The reported message is: '
									+ res.statusText + ' : ' + res.status;
							console.error(detail);
							zenoss.visualization.__showError(self.name, err,
									detail);
						}
					}
				}
			});
};

/**
 * Constructs a request object that can be POSTed to the Zenoss Data API to
 * retrieve the data for a chart. The request is based on the information in the
 * given config.
 * 
 * @access private
 * @param {object}
 *            config the config from which to build a request
 * @returns {object} a request object that can be POST-ed to the Zenoss
 *          performance metric service
 */
zenoss.visualization.Chart.prototype.__buildDataRequest = function(config) {
	var request = {};
	if (typeof config != 'undefined') {
		if (typeof config.range != 'undefined') {
			if (typeof config.range.start != 'undefined') {
				request.start = config.range.start;
			}
			if (typeof config.range.end != 'undefined') {
				request.end = config.range.end;
			}
		}

		if (typeof config.series != 'undefined') {
			request.series = config.series;
		}

		if (typeof config.downsample != 'undefined') {
			request.downsample = config.downsample;
		}

		if (typeof config.tags != 'undefined') {
			request.tags = config.tags;
		}

		if (typeof config.datapoints != 'undefined') {
			request.metrics = [];
			config.datapoints.forEach(function(dp) {
				var m = {};
				m.metric = dp.metric;
				if (typeof dp.rate != 'undefined') {
					m.rate = dp.rate;
				}
				if (typeof dp.aggregator != 'undefined') {
					m.aggregator = dp.aggregator;
				}
				if (typeof dp.downsample != 'undefined') {
					m.downsample = dp.downsample;
				}
				if (typeof dp.tags != 'undefined') {
					m.tags = {};
					for ( var key in dp.tags) {
						if (dp.tags.hasOwnProperty(key)) {
							m.tags[key] = dp.tags[key];
						}
					}
				}
				request.metrics.push(m);
			});
		}
	}
	return request;
}

/**
 * Processes the result from the Zenoss performance metric query that is in the
 * series format into the data that can be utilized by the chart library.
 * 
 * @access private
 * @param {object}
 *            request the request which generated the data
 * @param {object}
 *            data the data object returned from the query
 * @returns {object} the data in the format that can be utilized by the chart
 *          library.
 */
zenoss.visualization.Chart.prototype.__processResultAsSeries = function(
		request, data) {
	var plots = [];

	data.results.forEach(function(result) {
		// The key for a series plot will be its distinguishing
		// characteristics, which is the metric name and the
		// tags
		var key = result.metric;
		if (typeof result.tags != 'undefined') {
			key += '{';
			var prefix = '';
			for ( var tag in result.tags) {
				if (result.tags.hasOwnProperty(tag)) {
					key += prefix + tag + '=' + result.tags[tag];
					prefix = ',';
				}
			}
			key += '};'
		}
		var plot = {
			'key' : key,
			'values' : []
		};
		result.datapoints.forEach(function(dp) {
			plot.values.push({
				'x' : dp.timestamp * 1000,
				'y' : dp.value
			});
		});
		plots.push(plot);
	});

	return plots;
}

/**
 * Processes the result from the Zenoss performance metric query that is in the
 * default format into the data that can be utilized by the chart library.
 * 
 * @access private
 * @param {object}
 *            request the request which generated the data
 * @param {object}
 *            data the data object returned from the query
 * @returns {object} the data in the format that can be utilized by the chart
 *          library.
 */
zenoss.visualization.Chart.prototype.__processResultAsDefault = function(
		request, data) {

	var plotMap = Array();

	// Create a plot for each metric name, this is essentially
	// grouping the results by metric name. This can cause problems
	// if the request contains multiple queries for the same
	// metric, but this is basically a restriction of the
	// implementation (OpenTSDB) where it doesn't split the results
	// logically when multiple requests are made in a single call.
	data.results.forEach(function(result) {
		var plot = plotMap[result.metric];
		if (typeof plot == 'undefined') {
			plot = {
				'key' : result.metric,
				'values' : []
			};
			plotMap[result.metric] = plot;
		}

		plot.values.push({
			'x' : result.timestamp * 1000,
			'y' : result.value
		});
	});

	// Convert the plotMap into an array of plots for the graph
	// library to process
	var plots = [];
	for ( var key in plotMap) {
		if (plotMap.hasOwnProperty(key)) {
			// Sort the values of the plot as we put them in the
			// plots aray.
			plotMap[key].values.sort(function compare(a, b) {
				if (a.x < b.x)
					return -1;
				if (a.x > b.x)
					return 1;
				return 0;
			});
			plots.push(plotMap[key]);
		}
	}
	return plots;
}

/**
 * Wrapper function that redirects to the proper implementation to processes the
 * result from the Zenoss performance metric query into the data that can be
 * utilized by the chart library. *
 * 
 * @access private
 * @param {object}
 *            request the request which generated the data
 * @param {object}
 *            data the data object returned from the query
 * @returns {object} the data in the format that can be utilized by the chart
 *          library.
 */
zenoss.visualization.Chart.prototype.__processResult = function(request, data) {
	if (data.series) {
		return this.__processResultAsSeries(request, data);
	}
	return this.__processResultAsDefault(request, data);
}

/**
 * Deep object merge. This merge differs significantly from the "extend" method
 * provide by jQuery in that it will merge the value of arrays, but
 * concatenating the arrays together using the jQuery method "merge". Neither of
 * the objects passed are modified and a new object is returned.
 * 
 * @access private
 * @param {object}
 *            base the object to which values are to be merged into
 * @param {object}
 *            extend the object from which values are merged
 * @returns {object} the merged object
 */
zenoss.visualization.__merge = function(base, extend) {
	if (zenoss.visualization.debug) {
		console.groupCollapsed('Object Merge');
		console.group('SOURCES');
		console.log(base);
		console.log(extend);
		console.groupEnd();
	}

	if (typeof base == 'undefined' || base == null) {
		var m = $.extend(true, {}, extend);
		if (zenoss.visualization.debug) {
			console.log(m);
			console.groupEnd();
		}
		return m;
	}
	if (typeof extend == 'undefined' || extend == null) {
		var m = $.extend(true, {}, base);
		if (zenoss.visualization.debug) {
			console.log(m);
			console.groupEnd();
		}
		return m;
	}

	var m = $.extend(true, {}, base);

	for ( var k in extend) {
		if (extend.hasOwnProperty(k)) {
			var v = extend[k];
			if (v.constructor == Number || v.constructor == String) {
				m[k] = v;
			} else if (v instanceof Array) {
				m[k] = $.merge(m[k], v);
			} else if (v instanceof Object) {
				if (typeof m[k] == 'undefined') {
					m[k] = $.extend({}, v);
				} else {
					m[k] = zenoss.visualization.__merge(m[k], v);
				}
			} else {
				m[k] = $.extend(m[k], v);
			}
		}
	}

	if (zenoss.visualization.debug) {
		console.log(m);
		console.groupEnd();
	}
	return m;
}

/**
 * Given a dependency object, checks if the dependencies are already loaded and
 * if so, calls the callback, else loads the dependencies and then calls the
 * callback.
 * 
 * @access private
 * @param {object}
 *            required the dependency object that contains a "defined" key and a
 *            "source" key. The "defined" key is a name (string) that is used to
 *            identify the dependency and the "source" key is an array of
 *            JavaScript and CSS URIs that must be loaded to meet the
 *            dependency.
 * @param {function}
 *            callback called after the dependencies are loaded
 */
zenoss.visualization.__loadDependencies = function(required, callback) {
	if (typeof required == 'undefined') {
		callback();
		return;
	}

	// Check if it is already loaded, using the value in the 'defined' field
	var o;
	try {
		o = eval('zenoss.visualization.__dependencies.' + required.defined
				+ '.state');
	} catch (e) {
		// noop
	}
	if (typeof o != 'undefined') {
		if (o == 'loaded') {
			if (zenoss.visualization.debug) {
				console.log('Dependencies for "' + required.defined
						+ '" already loaded, continuing.');
			}
			// Already loaded, so just invoke the callback
			callback();
		} else {
			// It is in the process of being loaded, so add our callback to the
			// list of callbacks to call when it is loaded.
			if (zenoss.visualization.debug) {
				console
						.log('Dependencies for "'
								+ required.defined
								+ '" in process of being loaded, queuing until loaded.');
			}

			var c = eval('zenoss.visualization.__dependencies.'
					+ required.defined + '.callbacks');
			c.push(callback);
		}
		return;
	} else {
		// OK, not yet loaded or being loaded, so it is ours.
		if (zenoss.visualization.debug) {
			console
					.log('Dependencies for "'
							+ required.defined
							+ '" not loaded nor in process of loading, initiate loading.');
		}

		zenoss.visualization.__dependencies[required.defined] = {};
		var base = zenoss.visualization.__dependencies[required.defined];
		base['state'] = 'loading';
		base['callbacks'] = [];
		base['callbacks'].push(callback);
	}

	// Load the JS and CSS files. Divide the list of files into two lists: JS
	// and CSS as we can load one async, and the other loads sync (CSS).
	var js = [];
	var css = [];
	required.source.forEach(function(v) {
		if (v.endsWith('.js')) {
			js.push(v);
		} else if (v.endsWith('.css')) {
			css.push(v);
		} else {
			console.warn('Unknown required file type, "' + v
					+ '" when loading dependencies for "' + 'unknown'
					+ '". Ignored.')
		}
	});

	var base = zenoss.visualization.__dependencies[required.defined];
	zenoss.visualization.__load(js, css, function() {
		base['state'] = 'loaded';
		base['callbacks'].forEach(function(c) {
			c();
		});
		base['callbacks'].length = 0;
	});
}

zenoss.visualization.Chart.prototype.__updateData = function(data) {
	this.impl.update(this, data);
	this.__updateFooter(data);
}

/**
 * Loads the chart renderer as a dependency and then constructs and renders the
 * chart.
 * 
 * @access private
 * @param {object}
 *            data the data that is being rendered in the graph
 */
zenoss.visualization.Chart.prototype.__render = function(data) {
	var self = this;
	zenoss.visualization
			.__loadDependencies(
					{
						'defined' : self.config.type.replace('.', '_'),
						'source' : [ 'charts/'
								+ self.config.type.replace('.', '/') + '.js' ]
					},
					function() {

						if (typeof self.config.footer == 'undefined'
								|| (typeof self.config.footer == 'boolean' && self.config.footer == true)
								|| (typeof self.config.footer == 'string' && self.config.footer == 'range')) {
							self.__buildFooter(self.config, data);
						}

						self.impl = eval('zenoss.visualization.chart.'
								+ self.config.type);

						// Check the impl to see if a dependency is listed and
						// if so load that.
						zenoss.visualization
								.__loadDependencies(
										self.impl.required,
										function() {
											$(self.svgwrapper)
													.outerHeight(
															$(self.div)
																	.height()
																	- $(
																			self.footer)
																			.outerHeight());
											self.closure = self.impl.build(
													self, data);
											var _closure = self.closure;
											self.impl.render(self);

											// Set the colors in the footer
											// based on the
											// chart that was created.
											if (typeof self.config.footer == 'undefined'
													|| (typeof self.config.footer == 'boolean' && self.config.footer == true)) {
												for ( var i = 0; i < self.plots.length; ++i) {
													self
															.__setFooterBoxColor(
																	i,
																	self.impl
																			.color(
																					self,
																					_closure,
																					i));
												}
											}
										});
					});
}

/**
 * Loads the CSS specified by the URL.
 * 
 * @access private
 * @param {url}
 *            url the url, in string format, of the CSS file to load.
 */
zenoss.visualization.__loadCSS = function(url) {
	var css = document.createElement('link');
	css.rel = 'stylesheet'
	css.type = 'text/css';

	if (!url.startsWith("http")) {
		css.href = zenoss.visualization.url + '/api/' + url;
	} else {
		css.href = url;
	}
	document.getElementsByTagName('head')[0].appendChild(css);
}

/**
 * We would like to use jQuery for dynamic loading of JavaScript files, but it
 * may be that jQuery is not yet loaded, so we first have to dynamically load
 * jQuery. To accomplish this we need a 'bootstrap' loader. This method will
 * load the JavaScript file specified by the URL by creating a new HTML script
 * element on the page and then call the callback once the script has been
 * loaded.
 * 
 * @access private
 * @param {url}
 *            url URL, in string form, of the JavaScript file to load
 * @param {function}
 *            callback the function to call once the JavaScript is loaded
 */
zenoss.visualization.__bootstrapScriptLoader = function(url, callback) {
	this.__failcallback = function() {
		// noop;
	};

	this.fail = function(callback) {
		this.__failcallback = callback;
	};

	var script = document.createElement("script")
	script.type = "text/javascript";
	script.async = true;

	if (script.readyState) { // IE
		script.onreadystatechange = function() {
			if (script.readyState == "loaded"
					|| script.readyState == "complete") {
				script.onreadystatechange = null;
				callback();
			}
		};
	} else { // Others
		script.onload = function() {
			callback();
		};
	}

	script.src = url;
	document.getElementsByTagName("head")[0].appendChild(script);
	return this;
}

/**
 * Loads the array of JavaScript URLs followed by the array of CSS URLs and
 * calls the appropriate callback if the operations succeeded or failed.
 * 
 * @access private
 * @param {uri[]}
 *            js an array of JavaScript files to load
 * @param {uri[]}
 *            css an array of CSS files to load
 * @param {function}
 *            [success] callback to call one everything is loaded
 * @param {function}
 *            [fail] callback to call if there is a failure
 */
zenoss.visualization.__load = function(js, css, success, fail) {
	if (zenoss.visualization.debug) {
		console.log('Request to load "' + js + '" and "' + css + '".');
	}
	if (js.length == 0) {
		// All JavaScript files are loaded, now the loading of CSS can begin
		css.forEach(function(uri) {
			zenoss.visualization.__loadCSS(uri);
			if (zenoss.visualization.debug) {
				console.log('Loaded dependency "' + uri + '".');
			}
		});
		if (typeof success == 'function') {
			success();
		}
		return;
	}

	// Shift the next value off of the JS array and load it.
	var uri = js.shift();
	var _js = js;
	var _css = css;
	var self = this;
	if (!uri.startsWith("http")) {
		uri = zenoss.visualization.url + '/api/' + uri;
	}

	var loader = zenoss.visualization.__bootstrapScriptLoader;
	if (typeof jQuery != 'undefined') {
		loader = $.getScript;
	}
	loader(uri, function() {
		if (zenoss.visualization.debug) {
			console.log('Loaded dependency "' + uri + '".');
		}
		self.__load(_js, _css, success, fail);
	})
			.fail(
					function(jqxhr, settings, exception) {
						console
								.error('Unable to load dependency "'
										+ uri
										+ '", with error "'
										+ exception
										+ '". Loading halted, will continue, but additional errors likely.');
						self.__load(_js, _css, success, fail);
					});
}

/**
 * Loads jQuery and D3 as a dependencies and then calls the appripriate
 * callback.
 * 
 * @access private
 * @param {function}
 *            [success] called if the core dependencies are loaded
 * @param {function}
 *            [fail] called if the core dependencies are not loaded
 */
zenoss.visualization.__bootstrap = function(success, fail) {
	var js = [ 'jquery.min.js' ]
	zenoss.visualization.__loadDependencies({
		'defined' : 'd3',
		'source' : [ 'jquery.min.js', 'd3.v3.min.js', 'jstz-1.0.4.min.js',
				'css/zenoss.css' ]
	}, success, fail);
}