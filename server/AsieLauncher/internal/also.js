var VERSION = "0.4.0-rc4";
var JSON_REVISION = 5;

// Initialize libraries (quite a lot of them, too!)
var express = require('express')
  , _ = require('underscore')
  , fs = require('fs')
  , wrench = require('wrench')
  , util = require('./util.js')
  , argv = require('optimist').argv
  , Zip = require('adm-zip')
  , path = require('path')
  , request = require('request')
  , files = require('./also-files.js')
  , archiver = require('archiver')
  , uuid = require('node-uuid')
  , fileParser = require('./fileParser.js')
  , configHandler = null
  , modList = null
  , pluginList = null;

var app = express()
  , config = {}
  , infoData = {}
  , DEBUG = false
  , DO_LOCAL = false;

// Handle parameters
DEBUG = argv.d || argv.debug || false;
DO_LOCAL = argv.l || argv.local || false;

util.setDebug(DEBUG);

if(DO_LOCAL) {
  if(fs.existsSync("./ALWebFiles")) {
    wrench.rmdirSyncRecursive('./ALWebFiles', true);
  }
}

function addFile(fileName, realFile) {
	util.say("debug", "addFile: " + fileName + " <- " + realFile);
	if(DO_LOCAL) {
		util.copyFileSync(realFile, "./ALWebFiles/"+fileName);
	} else {
		app.use("/" + fileName, function(req,res) { res.sendfile(realFile); });
	}
}

function addDirectory(dirName, realDir) {
	util.say("debug", "addDirectory: " + dirName + " <- " + realDir);
	if(DO_LOCAL) {
		wrench.copyDirSyncRecursive(realDir+"/", './ALWebFiles/'+dirName+"/", {
 			excludeHiddenUnix: false
		});
	} else {
		app.use("/" + dirName, express.static(realDir));
	}
}

function initializeConfig() {
	infoData.jarPatches = util.onlyExtension(util.sortFilesBySubstrings(config.modpack.jarPatchesOrder,
	                        files.getDirectoryList("./AsieLauncher/jarPatches", "jarPatches/", false, "jarPatches/")
	                      ), [".zip", ".jar"]);
	infoData.files = [];
	infoData.zips = [];
	infoData.options = {};
	infoData.size = 0;
	infoData.jvmArguments = config.launcher.javaArguments || "";
	infoData.client_revision = JSON_REVISION;
	infoData.onlineMode = config.launcher.onlineMode || false;
	infoData.mcVersion = config.launcher.minecraftVersion || "1.5.2"; // default is 1.5.2
	infoData.servers = {};
	_.each(config.serverList, function(server) {
		infoData.servers[server.name] = server.ip;
	});
}

function addLocalFolderRoot(zip, localPath, zipPath) {
	util.say("debug", "Resolving " + localPath + " to " + path.resolve(localPath) + " ("+zipPath+")");
	zip.addLocalFolder(path.resolve(localPath), zipPath);
}

function createLauncherJAR() {
	util.say("info", "[Launcher] Generating launcher JAR...");
	util.deleteFile("./AsieLauncher/temp/launcher.jar");
	var archive = archiver("zip");
	var launcher = new Zip("./AsieLauncher/temp/AsieLauncher-latest.jar");
	var outputStream = fs.createWriteStream("./AsieLauncher/temp/launcher.jar");
	archive.pipe(outputStream);
	_.each(launcher.getEntries(), function(entry) {
		if(!(/\/$/.test(entry.entryName))) {
			util.say("debug", "Packing " + entry.entryName);
			archive.append(entry.getData(), {name: entry.entryName});
		}
	});
	_.each(fs.readdirSync("./AsieLauncher/launcherConfig"), function(fn) {
		archive.append(fs.createReadStream("./AsieLauncher/launcherConfig/"+fn),
				{name: "resources/"+fn });
	});
	archive.finalize(function() {
		outputStream.on("close", function() {
			addFile("launcher.jar", "./AsieLauncher/temp/launcher.jar");
			util.say("info", "[Launcher] Launcher ready!");
		});
	});
}

function generateHeartbeat() {
	var launcherConfig = JSON.parse(fs.readFileSync("./AsieLauncher/launcherConfig/config.json"));
	return {
		heartbeatVersion: 2,
		uuid: config.heartbeat.uuid,
		servers: _.map(config.serverList, function(serverCfg) {
				var server = _.pick(serverCfg, ["ip", "name", "description", "owner", "website"]);
				var properties = {};
				if(serverCfg.location.length > 0) {
					var location = serverCfg.location;
					if(!/\/$/.test(location)) location += "/";
					// Parse server-specific files (if possible)
					properties = fileParser.getServerProperties(serverCfg.location + "server.properties");
				}
				server.onlineMode = _(properties).contains("online-mode") ? properties["online-mode"] : (server.onlineMode || config.launcher.onlineMode);
				server.whiteList = _(properties).contains("white-list") ? properties["white-list"] : (server.whiteList || false);
				return server;
			}),
		url: launcherConfig.serverUrl,
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
	var launcherConfig = JSON.parse(fs.readFileSync("./AsieLauncher/launcherConfig/config.json"));
	var initialHeartbeat = { // Contains the "heavyweight" data (mods, etc)
		heartbeatVersion: 2,
		uuid: config.heartbeat.uuid,
		version: JSON.parse(fs.readFileSync("./AsieLauncher/internal/info.json")),
		mods: config.heartbeat.sendMods ? modList : [],
		plugins: pluginList,
		time: util.unix(new Date())
	};
	/* HEARTBEAT NOTES:
	   - The reason for plugins and mods being split is plugins being per-server and mods, well... not.
	*/
	var hbFunc = function() {
		// Contains lightweight and/or possibly changeable data (server information, launcher URL, etc)
		sendHeartbeat(generateHeartbeat());
	}
	sendHeartbeat(initialHeartbeat, "/init", function(){
		setInterval(hbFunc, 90*1000); // Every 90 seconds should be enough.
		hbFunc();
	});
}

var bannedUUIDs = [ // Those are UUIDs I accidentally distributed to people.
	"2edaba4e-77ee-48cd-a65e-5b7869446fc5", // rc2
	"0c645216-de7c-4bed-944b-71eec036bfbe" // beta7
];

exports.run = function(cwd) {
	util.say("info", "Started ALSO " + VERSION);

	if(DO_LOCAL) util.say("warning", "LOCAL MODE IS CURRENTLY BUGGY! USE AT YOUR OWN RISK");

	process.chdir(cwd);
	configHandler = require("./config.js");
	config = configHandler.get();

	// Generate unique heartbeat UUID
	if(!config.heartbeat.uuid || _.contains(bannedUUIDs, config.heartbeat.uuid)) {
		config.heartbeat.uuid = uuid.v4();
	}
	
	util.mkdir(["./AsieLauncher/temp", "./AsieLauncher/temp/zips"]);
	addDirectory("", "./AsieLauncher/htdocs");
	
	// This directory has been deprecated around beta6.
	wrench.rmdirSyncRecursive("./AsieLauncher/internal/launcher", function(){});

	if(fs.existsSync("./AsieLauncher/temp/AsieLauncher-latest.jar")) createLauncherJAR();
	else util.say("warning", "[Launcher] Launcher not found!");
	
	// * PREPARING DIRECTORIES *
	util.say("info", "Preparing directories");
	files.initialize(config.modpack);
	initializeConfig();

	// * MOD/PLUGIN LIST GENERATION *
	modList = fileParser.getModList(files, ["mods", "coremods", "lib", "jarPatches"]);
	fs.writeFileSync("./" + (DO_LOCAL ? "ALWebFiles/" : "") + "modList.json", JSON.stringify(modList, null, 4));
	
	pluginList = {};
	if(config.heartbeat.sendPlugins) {
		_.each(config.serverList, function(server) {
			if(!server.location || !server.ip) return;
			var location = server.location;
			if(!/\/$/.test(location)) location += "/";
			pluginList[server.ip] = fileParser.getPluginList(files, [server.location+"plugins"]);
		});
		if(_.keys(pluginList).length < 1) { // No plugins found! Try assuming one plugin pack.
			_.each(config.serverList, function(server) {
				if(!server.ip) return;
				pluginList[server.ip] = fileParser.getPluginList(files, ["plugins"]);
			});
		}
		fs.writeFileSync("./" + (DO_LOCAL ? "ALWebFiles/" : "") + "pluginList.json", JSON.stringify(pluginList, null, 4));
	}

	_.each(config.modpack.directories.file, function(dir) {
		infoData.files = _.union(infoData.files, files.file(dir));
		var dirs = files.getPossibleDirectories(dir);
		_.each(dirs, function(target) {
			addDirectory(dir, target);
			found = true;
		});
		if(dirs.length > 0) util.say("info", "Added directory "+dir);
	});

	_.each(config.modpack.directories.zip, function(dir) {
		var zip = files.zip(dir, !_.contains(config.modpack.nonOverwrittenFiles, dir));
		if(zip === null) return;
		infoData.zips.push(zip);
		util.say("info", "Added ZIP directory "+dir);
	});

	infoData.platforms = files.platforms();

	_.each(config.modpack.optionalComponents, function(option) {
		// A bug from beta8/9 causes us to delete the following stuff.
		if(option.filename) delete option.filename;
		if(option.directory) delete option.directory; 
		if(option["size"]) delete option["size"]; 
		if(option.md5) delete option.md5; 
		if(option.files) delete option.files;
		
		var dir = "./AsieLauncher/options/"+option.id;
		if(!fs.existsSync(dir)) return;
		
		util.say("info", "Adding option "+option.id+" ["+option.name+"]");
		if(!option.zip) { // Directory
			infoData.options[option.id] = _.extend(files.getFiles(dir), option);
		} else { // ZIP
			var name = "./AsieLauncher/temp/zips/option-"+option.id+".zip";
			if(createZip(name, [dir])) {
 				infoData.options[option.id] = _.extend({"filename": "option-"+option.id+".zip",
								"directory": "", "size": util.getSize(zipFile), "md5": util.md5(zipFile)}, option);
			}
		}
	});

	addDirectory("zips", "./AsieLauncher/temp/zips");
	addDirectory("platform", "./AsieLauncher/platform");
	addDirectory("options", "./AsieLauncher/options");
	addDirectory("jarPatches", "./AsieLauncher/jarPatches");

	infoData.size = files.getTotalSize();

	// * SERVER/LOCAL SETUP *
	if(DO_LOCAL) {
		fs.writeFileSync("./ALWebFiles/also.json", JSON.stringify(infoData));
	} else {
		app.use("/also.json", function(req, res) { res.json(infoData); });
	}

	if(!DO_LOCAL) {
		app.listen(config.webServer.port);
		util.say("info", "Ready - listening on port "+config.webServer.port+"!");
		if(config.heartbeat.enabled) runHeartbeat();
	} else util.say("info", "Files exported to ./ALWebFiles/");

	configHandler.set(config);
}
