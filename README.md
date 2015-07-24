Zenoss Central Query Service and JavaScript Library
====
This project provides a HTTP/JSON based metric service capability for the information 
that Zenoss stores in its central repository as well as a JavaScript library that allows
a user to easily embed standard graphs in their own web page.

JavaScript Library
----
The purpose of the Zenoss Visualization JavaScript Library is to allow users and
Zenoss developers to quickly and easily define and insert graphs into web pages.
It is this library that Zenoss uses internally to provide the graphs that are
part of the Zenoss application.

In its most basic form the user should only have to include two JavaScript statements
to embed Zenoss Visualizations on their web page. The first loads the Zenoss
Visualization library and the second creates a chart.

        <script type="text/javascript" src="http://zenoss.company.com:8888/api/visualization.js"></script>
        zenoss.visualization.chart.create("id-of-div-to-augment", "name-of-saved-chart");

From this initial starting point the user can expand to override the default values
from a stored chart or even create a complete chart from within the HTML page.

        zenoss.visualization.chart.create("chart1", {
            "type" : "line",
            "series" : true,
            "type" : "line",
            "returnset" : "exact",
            "downsample" : "1m-avg",
            "grouping" : "5m",
            "autoscale" : {
                "ceiling" : 5,
                "factor" : 1024
            },
            "tags" {
                "tagk1" : [ "tagv1", "tagv2" ], 
                "tagk2" : [ "tagv1", "tagv2" ]
            },            
            "range" : {
                "start" : "1h-ago",
                "end" : "now"
            },
            "format" : "%5.2f",
            "datapoints" : [ {
                "name" : "myload1",
                "legend" : "Legend Label for Load 1",
                "color" : "red",
                "fill" : true,
                "metric" : "laLoadInt1",
                "emit" : true,
                "expression" : "rpn:2,*",
                "miny" : 10,
                "maxy" : 10000,
                "rate" : true,              
                "rateOptions" : {
                    "counter" : true,
                    "counterMax" : 20000,
                    "resetThreshold" : 2000
                }
            }, {
                "metric" : "laLoadInt5",
                "aggregator" : "sum",
                "downsample" : "5m-avg"
            }, {
                "metric" : "laLoadInt15",
            } ]
        }); 

The complete documentation for the for the public API can be found by accessing
the `doc/index.html` from a running instance. The complete API documentation,
including "private" methods can be found at `doc/full/index.html`.

Resources
----

  The /chart resource has been removed from the API.



  - `POST /query/performance` - return the performance metrics that match the search criteria. The results are the same as for the get request below, the difference is that instead of specifying the criteria as query parameters the criteria is specified as an JSON object in the POST data. The JSON structure that is supported on this POST call follows the following format:

        {
            "start"                : "<datetime>",
            "end"                  : "<datetime>",
            "returnset"            : exact | last | <undefined>
            "downsample"           : request default for downsample over all metrics,
            "downsampleMultiplier" : approximate number of datapoints to retrieve from backend for each
                                     data point requested.
            "tags"                 : request default for tags over all metrics
            "metrics" : [
                {
                    "metric"      : "<metric name>",
                    "name"        : a friendly name for the metric - if supplied, will be returned in
                                    place of "metric" in the results. Name can be used to specify the
                                    series as a participant in a calculated expression (see
                                    "expression" below)
                    "id"          : a caller-defined tag for the series,
                    "aggregator"  : Aggregator to be used by OpenTSDB to combine values when downsampling.
                                    valid values are "avg", "min", "max", and "sum".
                                    If not specified, will default to "avg".
                    "interpolator": The type of interpolator to be used to fill in missing data points for the series.
                                    Valid values are: "linear", "none".
                                    If not specified, defaults to "none"
                    "rate"        : true or false,
                    "rateOptions" : { // optional
                        "counter"        : true or false,
                        "counterMax"     : roll over value for the counter,
                        "resetThreshold" : delta between consecutive values which should be considered
                                           at counter reset
                    },
                    "expression"  : RPN expression to perform on the raw value to get the returned value
                    "tags"        : {
                        "tag-name" : "tag-value", ...
                    } 
                }, ...
            ]
        }

  - `GET /query/performance` - There is no GET API. It has been replaced by POST.

    This resource is loosely based on the OpenTSDB HTTP query API at [OpenTsdb](http://opentsdb.net/http-api.html#/q) and supports the following query parameters:

    - `id = <_string_><br/>`
      This ID is specified by the client and is not checked for uniqueness by the server, nor even used by the server.
    - `start=<_date_ | _relative-date_ | _now_><br/>`
      A date should be specified in the format "yyyy/MM/dd-HH:mm:ss-ZZZ"<br/>
      A relative date is of the format [-]#[ywdhms]-ago<br/>
      "now" translates to the current time when the request is made
    - `end=<_date_ | _relative-date_ | _now_><br/>`
      A date should be specified in the format "yyyy/MM/dd-HH:mm:ss-ZZZ"<br/>
      A relative date is of the format [-]#[ywdhms]-ago<br/>
      "now" translates to the current time when the request is made
    - `exact=<_true_ | _false_>`
    - `series-<_true_ | _false_><br/>`
      Determines is the results are grouped as individual series based on the results based on metric name and tag values not based on the number of queries specified.
    - `query=<_AGG:[rate:][downsample:]metric[{tags}]_>`

        `AGG = min | max | sum | avg`

        `downsample = _like_ 10m-avg`

        `tags = name=tag-value`

        `tag-value = \* __or__ partial\* __or__ tag-value | tag-value` 

        _Multiple "query" parameters can be specified in which case all query results will be generated from a single HTTP/JSON connection._

The results of the query will be a JSON object of the form:

        {
            "clientId" : "2342feo234",
            "source": "OpenTSDB",
            "startTime" : "5s-ago",
            "startTimeActual": "2013/06/19-17:00:00-+0000",
            "endTime" : "now",
            "endTimeActual": "2013/06/19-19:05:00-+0000",
            "exactTimeWindow": true,
            "results" : [
                {
                    "datapoints": [
                        {
                            "timestamp": 1371512029,
                            "value": 49
                        },
                        {
                            "timestamp": 1371512044,
                            "value": 52
                        },
                        {
                            "timestamp": 1371512029,
                            "value": 49
                        }, ...
                    ],
                    "metric" : "laLoadInt1",
                    "tags": {
                        "<tagname>": [ <tagvalue> ...]
                    }
                },
            ]
        }

This response object contains the information which was given as part of the query as well as an array of results for each query. While some of the information in this response object may seem redundant it is not as each data point in the underling storage (OpenTSDB) may have a different set of tags.

Also note the __client id__ attribute in the response object. This is the value that was specified as the __id__ as the query parameter.


  - `POST /api/v2/performance/query` - 
  
  New endpoint for retrieving metrics, does not support all features for the original query endpoint.  This endpoint 
  supports adds support for wildcard, "*", in tag values.
  
  **Request**

  | Name | type | Required | Description | default |
  |---|---|---|---|---|---|
  |start|string|n|start of query see TIMESTAMP| one hour ago |
  |end|string|n|end of query see TIMESTAMP|current time|
  |queries|array|y|one or more metric queries, see METRIC_QUERY|
  |returnset|string|n|For each time series return all metrics (some may be outside timerange), strictly metrics in the time range or the last metric. See RETURNSET|exact|

  **METRIC_QUERY**
  
  |Name|type|Required|Description|default|
  |---|---|---|---|---|---|
  |aggregator|string|n|Name of aggregator. See AGGREGATOR|avg|
  |metric|string|y|name of stored metric||
  |rate|bool|n|return series as rate/deltas|false|
  |rateOptions|map|n|see RATE_OPTIONS|
  |downsample|string|n|downsample data series. See DOWNSAMPLE||
  |tags|map|y|name value pair of tag name and tag value(s). Tag value can is an array of one or more values.  Deviates from OpenTSDB by being an array in the value instead of a pipe (|) separated string. Tag values are an OR operation.||
  |expression|string|n|perform a calculation on the time series. see EXPRESSION||

  **RATE_OPTIONS**

  |Name|type|Required|Description|default|
  |---|---|---|---|---|---|
  |counter|boolean|n|Is the rate a counter. i.e. monotonically increasing and may rollover|false|
  |counterMax|integer|n|The max value for a counter Java Long.MaxValue||
  |resetThreshold|integer|n|An optional value that, when exceeded, will cause the aggregator to return a 0 instead of the calculated rate. Useful when data sources are frequently reset to avoid spurious spikes.|0|

  **RETURNSET**
  
  Valid values are “all”, “exact” and “last”. Return values can contain some values that fall right outside the given 
  time range, the “all” setting returns them all. The “exact” setting strictly returns values in the given time range. 
  The “last” setting returns the last value in the time ranges.  See the “last” api for returning the last datapoint.
  
  **DOWNSAMPLE**
  
  Combination of a time and aggregator. e.g 5min-avg.  See opentsdb for downsample values.
  
  **TIMESTAMP** 
  
  * Timestamp string, acceptable values:
    - timestamp in seconds or millis 
    - `2015/01/31-18:17:25 GMT+0`    
    - `2015/1/31-13:17:25-0500`      
    - `<N><UNIT>-ago`
      - where `<N>` is a positive integer
      - where `<UNIT>` is:               
        - `ms` - Milliseconds              
        - `s` - Seconds                    
        - `m` - Minutes                    
        - `h` - Hours                      
        - `d` - Days (24 hours)            
        - `w` - Weeks (7 days)             
        - `n` - Months (30 days)           
        - `y` - Years (365 days)           
        
  ** Result **
  
  Example request:
  
        {
          "start": "1437520683",
          "end": "1437521231",
          "queries": [
            {
              "metric": "cgroup.cpuacct.user",
              "rate": false,
              "tags": {
                "isvcname": [
                  "elasticsearch-serviced"
                ]
              }
            }
          ]
        }
                                                                                             
  Example Response:
    
        {  
          "series":[  
            {  
              "datapoints":[  
                [1437520683, 107561.0],
                [1437520693,107564.0],
                [1437520703,107568.0],
                [1437520713,107570.0]
              ],
              "metric":"cgroup.cpuacct.user",
              "tags":{  
                "isvc":"true",
                "isvcname":"elasticsearch-serviced"
              }
            }
          ],
          "statuses":[  
            {  
              "message":"",
              "status":"SUCCESS"
            }
          ]
        }
