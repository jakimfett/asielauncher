// Initialize libraries (quite a lot of them, too!)
var express = require('express')
  , _ = require('underscore')
  , fs = require('fs')
  , wrench = require('wrench')
  , util = require('./util.js')
  , argv = require('optimist').argv
	, files = require('./also-files.js');

var app = express()
  , config = require("./AsieLauncher/config.json")
  , infoData = {}
  , DEBUG = false
  , DO_LOCAL = false;

config.client_revision = 5;

// Handle parameters
DEBUG = argv.d || argv.debug || false;
DO_LOCAL = argv.l || argv.local || false;

util.setDebug(DEBUG);

if(DO_LOCAL) {
  if(fs.existsSync("./ALWebFiles")) {
    wrench.rmdirSyncRecursive('./ALWebFiles', true);
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

// INITIALIZE ROOT DIRECTORY
addDirectory("", "./AsieLauncher/htdocs");

console.log("Welcome to ALSO");

files.initialize(config);

infoData.jarPatches = util.sortFilesBySubstrings(config.jarPatchOrder,
                        files.getDirectoryList("./AsieLauncher/jarPatches", "jarPatches/", false, "jarPatches/")
                      );

infoData.files = [];
infoData.zips = [];
infoData.options = {};

infoData.size = 0;
infoData.jvmArguments = config.jvmArguments || "";
infoData.client_revision = config.client_revision;
infoData.servers = config.servers || {};
infoData.onlineMode = config.onlineMode || false;
infoData.mcVersion = config.mcVersion || "1.5.2"; // default is 1.5.2

_.each(config.loggedDirs, function(dir) {
	infoData.files = _.union(infoData.files, files.file(dir));
  var dirs = files.getPossibleDirectories(dir);
  _.each(dirs, function(target) {
    addDirectory(dir, target);
		found = true;
  });
  if(dirs.length > 0) util.say("info", "Added directory "+dir);
});

_.each(config.zippedDirs, function(dir) {
  var zip = files.zip(dir, !_.contains(config.noOverwrite, dir));
	if(zip === null) return;
	infoData.zips.push(zip);
	util.say("info", "Added ZIP directory "+dir);
});

infoData.platforms = files.platforms();

_.each(config.options, function(option) {
  var dir = "./AsieLauncher/options/"+option.id;
  if(!fs.existsSync(dir)) return;
  util.say("info", "Adding option "+option.id+" ["+option.name+"]");
  if(!option.zip) { // Directory
    infoData.options[option.id] = _.extend(option, files.getFiles(dir));
  } else { // ZIP
		var name = "./AsieLauncher/zips/option-"+option.id+".zip";
		if(createZip(name, [dir])) {
      infoData.options[option.id]=_.extend(option, {"filename": "option-"+option.id+".zip", "directory": "", "size": util.getSize(zipFile), "md5": util.md5(zipFile)});
		}
  }
});

addDirectory("zips", "./AsieLauncher/zips");
addDirectory("platform", "./AsieLauncher/platform");
addDirectory("options", "./AsieLauncher/options");
addDirectory("jarPatches", "./AsieLauncher/jarPatches");

if(DO_LOCAL) {
  fs.writeFileSync("./ALWebFiles/also.json", JSON.stringify(infoData), {encoding: "utf-8"});
} else {
  app.use("/also.json", function(req, res) {
    res.json(infoData);
  });
}

if(!DO_LOCAL) {
  app.listen(config.port);
  util.say("info", "Ready - listening on port "+config.port+"!");
} else util.say("info", "Files exported to ./ALWebFiles/");
