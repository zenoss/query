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
            "type" : "focus",
            "series" : true,
            "type" : "line",
            "range" : {
                "start" : "1h-ago",
                "end" : "now"
            },
            "datapoints" : [ {
                "metric" : "laLoadInt1",
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

  - `GET /chart` - return a list of chart resources.
  
  This operation can take a `start` and `end` query parameter that specifies the range (inclusive) that the requester wishes to get back. This enables paging through the list of available charts. Additionally a `count=true` query parameter can be specified such that the total count of charts will be returned as well. This operation will return
  a JSON structure of the form:
  
        {
            "start" : #,
            "end"   : #,
            "count" : #,
            "ids"   : [ "<id>", "<id>", ...]
        }
  
  - `GET /chart/{id}`
  
  - `POST /chart`
  
  - `PUT /chart/{id}`
  
  - `DELETE /chart/{id}`
  All operations on the `chart` resource assume a chart JSON structure as payload. The structure is 

        {
            "name" : "<chart-name>",
            "type" : <type of chart>,
            "range" : { [optional]
                "start" : <datetime>,
                "end"   : <datetime>,
            },
            "filter" : { [optional]
                "name" : "value", ...
            },
            "datapoints" : [
                {
                    "consolidator" : avg, sum, etc
                    "metric" : metric name
                }, ...
            ]
        }


    __Currently supported chart types are: `line`, `bar`, `pie`, `focus`, `discretebar`, `area`, `dc.stacked`, and `dc.area`.__

  - `POST /query/performance` - return the performance metrics that match the search criteria. The results are the same as for the get request below, the difference is that instead of specifying the criteria as query parameters the criteria is specified as an JSON object in the POST data. The JSON structure that is supported on this POST call follows the following format:

        {
            "start"   : "<datetime>",
            "end"     : "<datetime>",
            "exact"   : true or false,
            "series"  : true or false,
            "metrics" : [
                {
                    "metric" : "<metric name>",
                    "aggregator" : "<aggregator>",
                    "downsample" : "<downsample>",
                    "rate"       : true or false,
                    "tags"       : {
                        "tag-name" : "tag-value", ...
                    } 
                }, ...
            ]
        }

  - `GET /query/performance` - returns the performance metrics that match the search criteria.

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
            "series": false,
            "results" : [ 
                {
                    "metric" : "laLoadInt1",
                    "timestamp" : 1371512029,
                    "value" : 49,
                    "tags" : {
                        "tag1" : "value1",
                        "tag2" : "value2",
                        "tag3" : "value3"
                    }
                },
                {
                    "metric" : "laLoadInt1",
                    "timestamp" : 1371512044,
                    "value" : 52,
                    "tags" : {
                        "tag1" : "value1",
                        "tag2" : "value2",
                        "tag3" : "value3"
                    }
                }
            ]
        }

This response object contains the information which was given as part of the query as well as an array of results for each query. While some of the information in this response object may seem redundant it is not as each data point in the underling storage (OpenTSDB) may have a different set of tags.

Also note the __client id__ attribute in the response object. This is the value that was specified as the __id__ as the query parameter.