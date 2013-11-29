var JSON_REVISION = 7;

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
  , launcher = require('./also-launcher.js');

var app = express()
  , infoData = {};

function initializeConfig(config) {
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
	infoData.windowName = config.launcher.windowName || "";
	infoData.directoryName = config.launcher.directoryName || config.heartbeat.uuid;
	_.each(config.serverList, function(server) {
		infoData.servers[server.name] = server.ip;
	});
}

function addLocalFolderRoot(zip, localPath, zipPath) {
	util.say("debug", "Resolving " + localPath + " to " + path.resolve(localPath) + " ("+zipPath+")");
	zip.addLocalFolder(path.resolve(localPath), zipPath);
}

exports.run = function(config, serverInfo) {
	if(config.output.mode == "local") {
		util.say("warning", "LOCAL MODE IS CURRENTLY BUGGY! USE AT YOUR OWN RISK");
		if(fs.existsSync(config.output.local.location))
			wrench.rmdirSyncRecursive(config.output.local.location, true);
	}

	// * PREPARING DIRECTORIES *
	util.say("info", "Preparing directories");
	files.initialize(config);
	files.setExpress(app);
	initializeConfig(config);

	util.mkdir(["./AsieLauncher/temp", "./AsieLauncher/temp/zips"]);
	files.addDirectory("", "./AsieLauncher/htdocs");
	
	if(fs.existsSync("./AsieLauncher/temp/AsieLauncher-latest.jar")) {
		launcher.create(config, "./AsieLauncher/temp/launcher.jar", function(target) {
			files.addFile("launcher.jar", target);
		});
	} else util.say("warning", "[Launcher] Launcher not found!");
	
	_.each(config.modpack.directories.file, function(dir) {
		infoData.files = _.union(infoData.files, files.file(dir));
		var dirs = files.getPossibleDirectories(dir);
		_.each(dirs, function(target) {
			files.addDirectory(dir, target);
			found = true;
		});
		if(dirs.length > 0) util.say("info", "Added directory "+dir);
	});

	_.each(config.modpack.directories.zip, function(dir) {
		var zip = files.zip(dir, !_.contains(config.modpack.nonOverwrittenFiles, dir));
		if(zip === null) return;
		infoData.zips.push(zip);
		util.say("info", "Zipped directory "+dir);
	});

	infoData.platforms = files.platforms();

	_.each(config.modpack.optionalComponents, function(option) {
		var dir = "./AsieLauncher/options/"+option.id;
		if(!fs.existsSync(dir)) return;
		
		util.say("info", "Adding option "+option.id+" ["+option.name+"]");
		if(option.output == "file") { // Directory
			infoData.options[option.id] = _.extend(files.getFiles(dir), option);
		} else if(option.output == "zip") { // ZIP
			var name = "./AsieLauncher/temp/zips/option-"+option.id+".zip";
			if(createZip(name, [dir])) {
 				infoData.options[option.id] = _.extend({"filename": "option-"+option.id+".zip",
								"directory": "", "size": util.getSize(zipFile), "md5": util.md5(zipFile)}, option);
			}
		}
		// 0.4.1 launcher compat
		infoData.options[option.id].zip = (option.output == "zip");
	});

	files.addDirectory("zips", "./AsieLauncher/temp/zips");
	files.addDirectory("platform", "./AsieLauncher/platform");
	files.addDirectory("options", "./AsieLauncher/options");
	files.addDirectory("jarPatches", "./AsieLauncher/jarPatches");

	infoData.size = files.getTotalSize();

	// * SERVER/LOCAL SETUP *
	if(config.output.mode == "local") {
		fs.writeFileSync(config.output.local.location + "/also.json", JSON.stringify(infoData));
		util.say("info", "Files exported to " + config.output.local.location);
	}
	if(config.output.mode == "http") {
		app.use("/also.json", function(req, res) { res.json(infoData); });
		app.listen(config.output.http.port);
		util.say("info", "Ready - listening on port "+config.output.http.port+"!");
	}
}
