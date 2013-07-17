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
				console.log(zenoss.visualization.url + '/query/performance')
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
					if (typeof jQuery == 'undefined') {
						zenoss.visualization.__bootstrap(function() {
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
					zenoss.visualization.__bootstrap(function() {
						return new zenoss.visualization.Chart(name, template,
								config);
					});
					return;
				}
				new zenoss.visualization.Chart(name, template, config);
			}
		},

		/**
		 * Used to track dependency loading, including the load state (loaded /
		 * loading) as well as the callback that will be called when a
		 * dependency load has been completed.
		 */
		__dependencies : {},

		/**
		 * Main entry point for web pages. This method is used to first
		 * bootstrap the library and then call the callback to create charts.
		 * Because of the updated dependency loading capability, this method is
		 * not strictly needed any more, but will be left around for posterity.
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
				console.log('Dependencies already loaded, continuing.');
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
					.log('Dependencies not loaded or in process of loading, initiate loading.');
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

zenoss.visualization.Chart.prototype.__render = function() {
	var self = this;
	zenoss.visualization.__loadDependencies({
		'defined' : self.config.type.replace('.', '_'),
		'source' : [ 'charts/' + self.config.type.replace('.', '/') + '.js' ]
	}, function() {
		self.impl = eval('zenoss.visualization.chart.' + self.config.type);

		// Check the impl to see if a dependency is listed and
		// if so load that.
		zenoss.visualization.__loadDependencies(self.impl.required, function() {
			self.impl.build(self);
			self.impl.render();
		});
	});
}

zenoss.visualization.__loadCSS = function(url) {
	var css = document.createElement('link');
	css.rel = 'stylesheet'
	css.type = 'text/css';
	css.href = url;
	document.getElementsByTagName('head')[0].appendChild(css);
}

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
	var uri = 'http://localhost:8888/api/' + js.shift();
	var _js = js;
	var _css = css;
	var self = this;
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
										+ '". Loding halted, will continue, but additional errors likely.');
						self.__load(_js, _css, success, fail);
					});
}

// Script order matters and as this is all loaded asynchronously the
// loads are nested. Bootstrap load JQuery and then use JQuery to
// load everything else
zenoss.visualization.__bootstrap = function(success, fail) {
	var js = [ 'jquery.min.js' ]
	zenoss.visualization.__loadDependencies({
		'defined' : 'd3',
		'source' : [ 'jquery.min.js', 'd3.v3.min.js', 'css/zenoss.css' ]
	}, success);
}