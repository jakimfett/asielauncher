var JSON_REVISION = 6;

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
  , infoData = {}
  , DO_LOCAL = false;

DO_LOCAL = argv.l || argv.local || false;

// Handle parameters
if(DO_LOCAL && fs.existsSync("./ALWebFiles")) {
	wrench.rmdirSyncRecursive('./ALWebFiles', true);
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

var bannedUUIDs = [ // Those are UUIDs I accidentally distributed to people.
	"2edaba4e-77ee-48cd-a65e-5b7869446fc5", // rc2
	"0c645216-de7c-4bed-944b-71eec036bfbe" // beta7
];

exports.run = function(config, serverInfo) {
	if(DO_LOCAL) util.say("warning", "LOCAL MODE IS CURRENTLY BUGGY! USE AT YOUR OWN RISK");

	// Generate unique heartbeat UUID
	if(!config.heartbeat.uuid || _.contains(bannedUUIDs, config.heartbeat.uuid)) {
		config.heartbeat.uuid = uuid.v4();
	}
	
	util.mkdir(["./AsieLauncher/temp", "./AsieLauncher/temp/zips"]);
	addDirectory("", "./AsieLauncher/htdocs");
	
	if(fs.existsSync("./AsieLauncher/temp/AsieLauncher-latest.jar")) {
		launcher.create(config, "./AsieLauncher/temp/launcher.jar", function(target) {
			addFile("launcher.jar", target);
		});
	} else util.say("warning", "[Launcher] Launcher not found!");
	
	// * PREPARING DIRECTORIES *
	util.say("info", "Preparing directories");
	files.initialize(config.modpack);
	initializeConfig(config);

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
	} else util.say("info", "Files exported to ./ALWebFiles/");
}
