var http = require("http"),
	url = require("url"),
	path = require("path"),
	fs = require("fs"),
	port = process.argv[2] || 3006;

// dummy data / mock API routes
var routes = {
	"/api/performance/query/": {
		contentType: "json",
		content: '{"clientId":"not-specified","source":"OpenTSDB","startTime":"1h-ago","startTimeActual":1399633538,"endTime":"0s-ago","endTimeActual":1399637138,"returnset":"exact","series":false,"results":[{"metric":"Zenoss ifHCInOctets","timestamp":1399633594,"value":1109.8666666666666},{"metric":"Zenoss ifHCInOctets","timestamp":1399633654,"value":1110.4},{"metric":"Zenoss ifHCInOctets","timestamp":1399633714,"value":886.4},{"metric":"Zenoss ifHCInOctets","timestamp":1399633774,"value":1186.6666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399633834,"value":1485.3333333333333},{"metric":"Zenoss ifHCInOctets","timestamp":1399633894,"value":1305.6},{"metric":"Zenoss ifHCInOctets","timestamp":1399633954,"value":1404.8},{"metric":"Zenoss ifHCInOctets","timestamp":1399634014,"value":1295.4666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399634074,"value":1169.6},{"metric":"Zenoss ifHCInOctets","timestamp":1399634134,"value":1040.5333333333333},{"metric":"Zenoss ifHCInOctets","timestamp":1399634194,"value":1136.0},{"metric":"Zenoss ifHCInOctets","timestamp":1399634254,"value":1629.8666666666666},{"metric":"Zenoss ifHCInOctets","timestamp":1399634314,"value":1368.5333333333333},{"metric":"Zenoss ifHCInOctets","timestamp":1399634374,"value":1068.2666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399634434,"value":833.6},{"metric":"Zenoss ifHCInOctets","timestamp":1399634494,"value":1153.0666666666666},{"metric":"Zenoss ifHCInOctets","timestamp":1399634554,"value":1005.8666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399634614,"value":1049.0666666666666},{"metric":"Zenoss ifHCInOctets","timestamp":1399634674,"value":1343.4666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399634734,"value":1280.0},{"metric":"Zenoss ifHCInOctets","timestamp":1399634794,"value":1387.2},{"metric":"Zenoss ifHCInOctets","timestamp":1399634854,"value":1256.0},{"metric":"Zenoss ifHCInOctets","timestamp":1399634914,"value":957.8666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399634974,"value":1260.2666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399635034,"value":1321.0666666666666},{"metric":"Zenoss ifHCInOctets","timestamp":1399635094,"value":1036.8},{"metric":"Zenoss ifHCInOctets","timestamp":1399635154,"value":931.2},{"metric":"Zenoss ifHCInOctets","timestamp":1399635214,"value":840.5333333333333},{"metric":"Zenoss ifHCInOctets","timestamp":1399635274,"value":1050.6666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399635334,"value":934.9333333333333},{"metric":"Zenoss ifHCInOctets","timestamp":1399635394,"value":676.8},{"metric":"Zenoss ifHCInOctets","timestamp":1399635454,"value":860.2666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399635514,"value":940.2666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399635574,"value":1217.0666666666666},{"metric":"Zenoss ifHCInOctets","timestamp":1399635634,"value":1011.7333333333333},{"metric":"Zenoss ifHCInOctets","timestamp":1399635694,"value":1061.8666666666666},{"metric":"Zenoss ifHCInOctets","timestamp":1399635754,"value":917.8666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399635814,"value":1478.9333333333334},{"metric":"Zenoss ifHCInOctets","timestamp":1399635874,"value":1221.3333333333333},{"metric":"Zenoss ifHCInOctets","timestamp":1399635934,"value":1457.6},{"metric":"Zenoss ifHCInOctets","timestamp":1399635994,"value":888.5333333333333},{"metric":"Zenoss ifHCInOctets","timestamp":1399636054,"value":1269.8666666666666},{"metric":"Zenoss ifHCInOctets","timestamp":1399636114,"value":1194.1333333333334},{"metric":"Zenoss ifHCInOctets","timestamp":1399636174,"value":1412.2666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399636234,"value":1263.4666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399636294,"value":1366.4},{"metric":"Zenoss ifHCInOctets","timestamp":1399636354,"value":1336.5333333333333},{"metric":"Zenoss ifHCInOctets","timestamp":1399636414,"value":1369.6},{"metric":"Zenoss ifHCInOctets","timestamp":1399636474,"value":1537.6},{"metric":"Zenoss ifHCInOctets","timestamp":1399636534,"value":1950.4},{"metric":"Zenoss ifHCInOctets","timestamp":1399636594,"value":1865.6},{"metric":"Zenoss ifHCInOctets","timestamp":1399636654,"value":1938.6666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399636714,"value":1572.2666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399636774,"value":2268.8},{"metric":"Zenoss ifHCInOctets","timestamp":1399636834,"value":2914.6666666666665},{"metric":"Zenoss ifHCInOctets","timestamp":1399636894,"value":1699.2},{"metric":"Zenoss ifHCInOctets","timestamp":1399636954,"value":1802.6666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399637014,"value":1847.4666666666667},{"metric":"Zenoss ifHCInOctets","timestamp":1399637074,"value":3622.9333333333334},{"metric":"Zenoss ifHCInOctets","timestamp":1399637119,"value":1395.911111111111},{"metric":"Zenoss ifHCOutOctets","timestamp":1399633594,"value":1488.0},{"metric":"Zenoss ifHCOutOctets","timestamp":1399633654,"value":1371.2},{"metric":"Zenoss ifHCOutOctets","timestamp":1399633714,"value":1057.6},{"metric":"Zenoss ifHCOutOctets","timestamp":1399633774,"value":1653.3333333333333},{"metric":"Zenoss ifHCOutOctets","timestamp":1399633834,"value":1795.2},{"metric":"Zenoss ifHCOutOctets","timestamp":1399633894,"value":1619.2},{"metric":"Zenoss ifHCOutOctets","timestamp":1399633954,"value":1524.8},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634014,"value":1705.6},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634074,"value":1170.6666666666667},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634134,"value":1074.6666666666667},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634194,"value":870.4},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634254,"value":1110.4},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634314,"value":1095.4666666666667},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634374,"value":1031.4666666666667},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634434,"value":1850.6666666666667},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634494,"value":1471.4666666666667},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634554,"value":1794.6666666666667},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634614,"value":1377.6},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634674,"value":1254.9333333333334},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634734,"value":1674.1333333333334},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634794,"value":1653.8666666666666},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634854,"value":1393.6},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634914,"value":1365.3333333333333},{"metric":"Zenoss ifHCOutOctets","timestamp":1399634974,"value":1759.4666666666667},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635034,"value":1477.3333333333333},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635094,"value":1066.6666666666667},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635154,"value":1120.0},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635214,"value":1417.0666666666666},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635274,"value":1243.2},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635334,"value":1152.5333333333333},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635394,"value":1010.1333333333333},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635454,"value":1083.2},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635514,"value":985.6},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635574,"value":903.4666666666667},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635634,"value":768.5333333333333},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635694,"value":1189.3333333333333},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635754,"value":1165.3333333333333},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635814,"value":1404.2666666666667},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635874,"value":1081.0666666666666},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635934,"value":1310.4},{"metric":"Zenoss ifHCOutOctets","timestamp":1399635994,"value":941.8666666666667},{"metric":"Zenoss ifHCOutOctets","timestamp":1399636054,"value":1315.7333333333333},{"metric":"Zenoss ifHCOutOctets","timestamp":1399636114,"value":1345.6},{"metric":"Zenoss ifHCOutOctets","timestamp":1399636174,"value":2525.3333333333335},{"metric":"Zenoss ifHCOutOctets","timestamp":1399636234,"value":2412.8},{"metric":"Zenoss ifHCOutOctets","timestamp":1399636294,"value":1596.8},{"metric":"Zenoss ifHCOutOctets","timestamp":1399636354,"value":2011.2},{"metric":"Zenoss ifHCOutOctets","timestamp":1399636414,"value":1210.1333333333334},{"metric":"Zenoss ifHCOutOctets","timestamp":1399636474,"value":1555.2},{"metric":"Zenoss ifHCOutOctets","timestamp":1399636534,"value":1506.6666666666667},{"metric":"Zenoss ifHCOutOctets","timestamp":1399636594,"value":1414.9333333333334},{"metric":"Zenoss ifHCOutOctets","timestamp":1399636654,"value":1690.1333333333334},{"metric":"Zenoss ifHCOutOctets","timestamp":1399636714,"value":1970.6666666666667},{"metric":"Zenoss ifHCOutOctets","timestamp":1399636774,"value":3065.6},{"metric":"Zenoss ifHCOutOctets","timestamp":1399636834,"value":2840.5333333333333},{"metric":"Zenoss ifHCOutOctets","timestamp":1399636894,"value":2057.0666666666666},{"metric":"Zenoss ifHCOutOctets","timestamp":1399636954,"value":2153.0666666666666},{"metric":"Zenoss ifHCOutOctets","timestamp":1399637014,"value":2816.0},{"metric":"Zenoss ifHCOutOctets","timestamp":1399637074,"value":3070.9333333333334},{"metric":"Zenoss ifHCOutOctets","timestamp":1399637119,"value":1879.4666666666667}]}'
	}
};

http.createServer(function(req, res){
	var uri = url.parse(req.url).pathname,
		filename = path.join(process.cwd(), uri);

	// check api routed before serving up regular
	// ol files
	if(routes[uri]){
		res.setHeader("Content-Type", routes[uri].contentType);
		res.setHeader("Access-Control-Allow-Origin", "*");
		res.writeHead(200);
		res.write(routes[uri].content, "binary");
		res.end();
		return;
	}

	path.exists(filename, function(exists){
		if(!exists){
			res.writeHead(404, {"Content-Type": "text/plain"});
			res.write("404 Not Found\n");
			res.end();
			return;
		}

		if(fs.statSync(filename).isDirectory()){
			filename += "index.html";
		}

		fs.readFile(filename, "binary", function(err, file){
			if(err){
				res.writeHead(500, {"Content-Type": "text/plain"});
				res.write(err + "\n");
				res.end();
				return;
			}

			res.setHeader("Content-Type", getContentType(filename));
			res.setHeader("Access-Control-Allow-Origin", "*");
			res.writeHead(200);
			res.write(file, "binary");
			res.end();
		});
	});
}).listen(parseInt(port, 10));

function getContentType(fileName) {
	var ext = fileName.slice(fileName.lastIndexOf('.')),
		type;

	switch (ext) {
		case ".html":
			type = "text/html";
			break;

		case ".js":
			type = "text/javascript";
			break;

		case ".css":
			type = "text/css";
			break;

		default:
			type = "text/plain";
			break;
	}

	return type;
}

console.log("server running at http://localhost:"+ port);