var _ = require('underscore')
  , fs = require('fs')
  , request = require('request')
  , util = require('./util.js');

var bannedUUIDs = [ // Those are UUIDs I accidentally distributed to people.
	"2edaba4e-77ee-48cd-a65e-5b7869446fc5", // rc2
	"0c645216-de7c-4bed-944b-71eec036bfbe" // beta7
];

exports.create = function(config, serverInfo) {
	var heartbeat = {};

	// Generate unique heartbeat UUID
	if(!config.heartbeat.uuid || _.contains(bannedUUIDs, config.heartbeat.uuid)) {
		config.heartbeat.uuid = uuid.v4();
	}
	

	function generateHeartbeat() {
		return {
			heartbeatVersion: 2,
			uuid: config.heartbeat.uuid,
			servers: _.map(serverInfo, function(serverCfg) {
				var server = _.pick(serverCfg, ["ip", "name", "description", "owner", "website", "properties"]);
				var properties = server.properties;
				server.onlineMode = _(properties).contains("online-mode") ? properties["online-mode"] : (server.onlineMode || config.launcher.onlineMode);
				server.whiteList = _(properties).contains("white-list") ? properties["white-list"] : (server.whiteList || false);
				return server;
			}),
			url: config.launcher.serverUrl,
			launcher: config.launcher
		};
	}

	function sendHeartbeat(data, prefix, cb) {
		prefix = prefix || "";
		cb = cb || function(){};
		util.say("debug", "Sending heartbeat "+prefix);
		data.time = util.unix(new Date());
		request.post(config.heartbeat.url + prefix, {"form": data}, cb);
	}

	function runHeartbeat() {
		var initialHeartbeat = { // Contains the "heavyweight" data (mods, etc)
			heartbeatVersion: 2,
			uuid: config.heartbeat.uuid,
			version: JSON.parse(fs.readFileSync("./AsieLauncher/internal/info.json")),
			mods: config.heartbeat.sendMods ? serverInfo.mods : [],
			plugins: config.heartbeat.sendPlugins ? serverInfo.plugins : [],
			time: util.unix(new Date())
		};
		/* HEARTBEAT NOTES:
		   - The reason for plugins and mods being split is plugins being per-server and mods, well... not.
		*/
		var hbFunc = function() {
			// Contains lightweight and/or possibly changeable data (server information, launcher URL, etc)
			sendHeartbeat(generateHeartbeat(config));
		}
		sendHeartbeat(initialHeartbeat, "/init", function(){
			setInterval(hbFunc, 90*1000); // Every 90 seconds should be enough.
			hbFunc();
		});
	}

	runHeartbeat();
}

