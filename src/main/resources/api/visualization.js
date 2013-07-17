var zenoss = {
	visualization : {
		debug : false,
		url : "http://localhost:8080",

		showError : function(name, err, detail) {
			$('#' + name).html('<span class="zenerror">' + detail + '</span>');
		},

		Chart : function(name, template, config) {
			var self = this;
			this.name = name;
			this.config = this.__merge(template, config);
			this.div = $('#' + this.name);
			this.svg = d3.select('#' + this.name).append('svg');
			this.request = this.__buildDataRequest(this.config);

			if (zenoss.visualization.debug) {
				console.groupCollapsed('POST Request Object');
				console.log(this.request);
				console.groupEnd();
			}

			// If the width and/or height are set in the configuration then
			// set those on the div. This really should be left to CSS and not
			// part of the chart API.
			if (typeof this.config != 'undefined') {
				if (typeof this.config.width != 'undefined') {
					this.div.width(this.config.width);
				}
				if (typeof this.config.height != 'undefined') {
					this.div.height(this.config.height);
				}
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
								self.__render();
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
									zenoss.visualization.showError(self.name,
											{}, detail);
								} else {
									try {
										var err = JSON.parse(res.responseText);
										var detail = 'An unexpected failure response was received from the server. The reported message is: '
												+ err.errorSource
												+ ' : '
												+ err.errorMessage;
										console.error(detail);
										zenoss.visualization.showError(
												self.name, err, detail);
									} catch (e) {
										var detail = 'An unexpected failure response was received from the server. The reported message is: '
												+ res.statusText
												+ ' : '
												+ res.status;
										console.error(detail);
										zenoss.visualization.showError(
												self.name, err, detail);
									}
								}
							}
						});
			}
		},

		chart : {
			all : {},
			create : function(name, arg1, arg2, complete, error) {

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
					if (typeof nv == 'undefined') {
						loadRequiredLibraries(function() {
							loadChart(arg1, function(template) {
								return new zenoss.visualization.Chart(name,
										template, config);
							}, function(err, detail) {
								zenoss.visualization.showError(name, err,
										detail);
							});
						});
						return;
					}
					loadChart(arg1, function(template) {
						return new zenoss.visualization.Chart(name, template,
								config);
					}, function(err, detail) {
						zenoss.visualization.showError(name, err, detail);
					});
					return;
				}

				var template = null;
				var config = arg1;

				if (typeof jQuery == 'undefined') {
					loadRequiredLibraries(function() {
						return new zenoss.visualization.Chart(name, template,
								config);
					});
					return;
				}
				new zenoss.visualization.Chart(name, template, config);
			}
		},

		load : function(callback) {
			function loadScript(url, async, callback) {
				var script = document.createElement("script")
				script.type = "text/javascript";
				if (async) {
					script.async = true;
				}

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
			}

			function includeCSS(url) {
				var css = document.createElement('link');
				css.rel = 'stylesheet'
				css.type = 'text/css';
				css.href = url;
				document.getElementsByTagName('head')[0].appendChild(css);
			}

			// Script order matters and as this is all loaded asynchronously the
			// loads are nested. Bootstrap load JQuery and then use JQuery to
			// load everything else
			loadScript(
					'http://localhost:8888/api/jquery.min.js',
					true,
					function() {
						if (zenoss.visualization.debug) {
							console.log('JQuery loaded');
						}
						$
								.getScript(
										'http://localhost:8888/api/d3.v3.min.js',
										function() {
											if (zenoss.visualization.debug) {
												console.log('D3 loaded');
											}
											$
													.getScript(
															'http://localhost:8888/api/crossfilter.min.js',
															function() {
																if (zenoss.visualization.debug) {
																	console
																			.log('Crossfilter Loaded');
																}
																$
																		.getScript(
																				'http://localhost:8888/api/dc.min.js',
																				function() {
																					if (zenoss.visualization.debug) {
																						console
																								.log('DC Loaded');
																					}
																					$
																							.getScript(
																									'http://localhost:8888/api/nv.d3.min.js',
																									function() {
																										if (zenoss.visualization.debug) {
																											console
																													.log('NV.D3 Loaded');
																										}
																										includeCSS('http://localhost:8888/api/css/dc.css');
																										if (zenoss.visualization.debug) {
																											console
																													.log("Loaded DC CSS");
																										}
																										includeCSS('http://localhost:8888/api/css/nv.d3.css');
																										if (zenoss.visualization.debug) {
																											console
																													.log("Loaded NV.D3 CSS");
																										}
																										includeCSS('http://localhost:8888/api/css/zenoss.css');
																										if (zenoss.visualization.debug) {
																											console
																													.log("Loaded Zenoss CSS");
																										}
																										callback();
																									});
																				});
															});
										});
					});

		}
	}
}

/**
 * Constructs a request object that can be POSTed to the Zenoss Data API to
 * retrieve the data for a chart. The request is based on the information in the
 * given config
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
				if (typeof dp.filter != 'undefined') {
					m.tags = {};
					for ( var key in dp.filter) {
						if (dp.filter.hasOwnProperty(key)) {
							m.tags[key] = dp.filter[key];
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
 * Deep object merge. This merge differs significantly from the "extend" method
 * provide by jQuery in that it will merge the value of arrays, but
 * concatenating the arrays together using the jQuery method "merge". Neither of
 * the objects passed are modified and a new object is returned.
 * 
 * @param a
 *            object 1
 * @param b
 *            object 2
 * @returns the merged object
 */
zenoss.visualization.Chart.prototype.__merge = function(a, b) {
	if (zenoss.visualization.debug) {
		console.groupCollapsed('Object Merge');
		console.group('SOURCES');
		console.log(a);
		console.log(b);
		console.groupEnd();
	}

	if (typeof a == 'undefined' || a == null) {
		var m = $.extend(true, {}, b);
		if (zenoss.visualization.debug) {
			console.log(m);
			console.groupEnd();
		}
		return m;
	}
	if (typeof b == 'undefined' || b == null) {
		var m = $.extend(true, {}, a);
		if (zenoss.visualization.debug) {
			console.log(m);
			console.groupEnd();
		}
		return m;
	}

	var m = $.extend(true, {}, a);

	for ( var k in b) {
		if (b.hasOwnProperty(k)) {
			var v = b[k];
			if (v.constructor == Number || v.constructor == String) {
				m[k] = v;
			} else if (v instanceof Array) {
				m[k] = $.merge(m[k], v);
			} else if (v instanceof Object) {
				if (typeof m[k] == 'undefined') {
					m[k] = $.extend({}, v);
				} else {
					m[k] = merge(m[k], v);
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

zenoss.visualization.Chart.prototype.__processResultAsSeries = function(
		request, data) {
	var plots = [];

	data.results.forEach(function(result) {
		// The key for a series plot will be its distinguishing
		// characteristics, which is the metric name and the
		// tags / filter
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

zenoss.visualization.Chart.prototype.__processResult = function(request, data) {
	if (data.series) {
		return this.__processResultAsSeries(request, data);
	}
	return this.__processResultAsDefault(request, data);
}

zenoss.visualization.Chart.prototype.__render = function() {
	var self = this;

	try {
		self.impl = eval('zenoss.visualization.chart.' + self.config.type);
	} catch (e) {
	}
	if (typeof self.impl == 'undefined') {
		if (zenoss.visualization.debug) {
			console.log('Loading chart type, "' + self.config.type
					+ '" from URL "http://localhost:8888/api/charts/'
					+ self.config.type.replace('.', '/') + '.js.');
		}
		$.getScript(
				'http://localhost:8888/api/charts/'
						+ self.config.type.replace('.', '/') + '.js',
				function() {
					self.impl = eval('zenoss.visualization.chart.'
							+ self.config.type);
					self.impl.loaded = true;
					self.impl.build(self);
					self.impl.render();
				}).fail(
				function(a, b, exception) {
					var msg = 'Unable to load chart implementation for "'
							+ self.config.type + '", error reported was "'
							+ exception + '".';
					console.error(zenoss);
					zenoss.visualization.showError(self.name, {}, msg);
					console.error(msg);
				});
	} else {
		if (zenoss.visualization.debug) {
			console.log('Chart type, "' + self.config.type
					+ '" already loaded.');
		}
		self.impl.build(self);
		self.impl.render();
	}
}
