var VERSION = "0.4.0-beta8";
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
  , configHandler = null;

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
	infoData.jarPatches = util.sortFilesBySubstrings(config.modpack.jarPatchesOrder,
	                        files.getDirectoryList("./AsieLauncher/jarPatches", "jarPatches/", false, "jarPatches/")
	                      );
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

function runHeartbeat() {
	var launcherConfig = JSON.parse(fs.readFileSync("./AsieLauncher/launcherConfig/config.json"));
	var heartbeat = {
		version: 1,
		uuid: config.heartbeat.uuid,
		servers: config.serverList,
		url: launcherConfig.serverUrl,
		launcher: config.launcher,
		version: JSON.parse(fs.readFileSync("./AsieLauncher/internal/info.json"))
	};
	var hbFunc = function() {
		util.say("debug", "Sending heartbeat");
		heartbeat.time = util.unix(new Date());
		request.post(config.heartbeat.url, {"form": heartbeat}, function(){});
	}
	setInterval(hbFunc, 90*1000); // Every 90 seconds should be enough.
	hbFunc();
}

exports.run = function(cwd) {
	util.say("info", "Started ALSO " + VERSION);

	process.chdir(cwd);
	configHandler = require("./config.js");
	config = configHandler.get();

	// Generate unique heartbeat UUID
	if(!config.heartbeat.uuid || // The one below is a lingering thing from beta7.
    	  config.heartbeat.uuid == "0c645216-de7c-4bed-944b-71eec036bfbe") {
		config.heartbeat.uuid = uuid.v4();
	}
	util.mkdir(["./AsieLauncher/temp", "./AsieLauncher/temp/zips"]);
	addDirectory("", "./AsieLauncher/htdocs");
	// This directory has been deprecated in hotfix4.
	wrench.rmdirSyncRecursive("./AsieLauncher/internal/launcher", function(){});

	// * DOWNLOADING LAUNCHER *
	if(fs.existsSync("./AsieLauncher/temp/AsieLauncher-latest.jar")) createLauncherJAR();
	else util.say("warning", "[Launcher] Launcher not found!");

	// * PREPARING DIRECTORIES *
	util.say("info", "Preparing directories");
	files.initialize(config.modpack);
	initializeConfig();

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
		if(option.filename) delete option.filename; // beta8 bugs...
		if(option.directory) delete option.directory; 
		if(option.size) delete option.size; 
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

	if(DO_LOCAL) {
		fs.writeFileSync("./ALWebFiles/also.json", JSON.stringify(infoData));
	} else {
		app.use("/also.json", function(req, res) { res.json(infoData); });
	}

	if(!DO_LOCAL) {
		app.listen(config.webServer.port);
		util.say("info", "Ready - listening on port "+config.webServer.port+"!");
		runHeartbeat();
	} else util.say("info", "Files exported to ./ALWebFiles/");

	configHandler.set(config);
}
