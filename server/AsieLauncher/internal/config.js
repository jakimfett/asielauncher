var fs = require("fs")
  , _ = require("underscore");

var config = fs.existsSync("./also-config.json") ? require("../../also-config.json") : require("../config.json")
  , LATEST_CONFIG = 5;

var saveConfig = function() {
	if(fs.existsSync("./AsieLauncher/config.json")) fs.unlinkSync("./AsieLauncher/config.json");
	fs.writeFileSync("./also-config.json", JSON.stringify(config, undefined, 2));
}

var configUpdaters = {
	4: function() {
		// Add server.id fields
		var usedFirstID = false;
		_.each(config.serverList, function(server) {
			server.id = usedFirstID ? server.name.replace(/\W/g, '').toLowerCase() : config.launcher.directoryName;
			usedFirstID = true;
		});
		config.output = {
			"mode": "http",
			"http": {
				"port": config.webServer.port
			},
			"local": {
				"location": "./ALWebFiles"
			}
		};
		delete config.webServer;
		config.version = 5;
	},
	3: function() {
		var launcherConfig = JSON.parse(fs.readFileSync("./AsieLauncher/launcherConfig/config.json"));
		config.launcher = _.extend(config.launcher, launcherConfig);
		fs.unlinkSync("./AsieLauncher/launcherConfig/config.json");
		config.version = 4;
	},
	2: function() {
		if(config.serverList.length == 1 && fs.existsSync("./server.properties")) {
			config.serverList[0].location = "./";
		} else {
			_.map(config.serverList, function(list) {
				list.location = "";
				return list;
			});
		}
		config.heartbeat.sendMods = true;
		config.heartbeat.sendPlugins = true;
		config.version = 3;
	},
	1: function() {
		var newServerList = [];
		_.each(config.serverList, function(ip, name) {
			newServerList.push({
				"ip": ip, "name": name,
				"description": "", "owner": "", "website": ""
			});
		});
		config.serverList = newServerList;
		config.modpack.lookupDirectories = ["./", "./AsieLauncher/"];
		config.version = 2;
	},
	0: function() {
		var newConfig = {
			"version": 1,
			"serverList": config.servers,
			"webServer": { "port": config.port },
			"modpack": {
				"directories": {
					"file": config.loggedDirs,
					"zip": config.zippedDirs
				},
				"nonOverwrittenFiles": config.noOverwrite,
				"blacklistedFiles": config.blacklist,
				"jarPatchesOrder": config.jarPatchOrder,
				"optionalComponents": config.options
			},
			"launcher": {
				"minecraftVersion": config.mcVersion,
				"onlineMode": config.onlineMode,
				"javaArguments": config.jvmArguments
			},
			"updatesURL": "http://asie.pl/launcher",
			"heartbeat": { // New in v1
				"url": "http://asie.pl:7253/pfudor",
				"enabled": true
			}
		};
		config = newConfig;
	}
}

if(!config.version) { config.version = 0; }

while(config.version < LATEST_CONFIG) {
	configUpdaters[config.version]();
}
saveConfig();

exports.get = function() {
	return config;
}
exports.set = function(c) {
	config = c;
	saveConfig();
}
exports.save = saveConfig;
