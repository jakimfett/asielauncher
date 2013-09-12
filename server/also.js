// Initialize libraries (quite a lot of them, too!)
var express = require('express')
  , _ = require('underscore')
  , fs = require('fs')
  , wrench = require('wrench')
  , util = require('./util.js')
  , Zip = require('adm-zip')
  , argv = require('optimist').argv;

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
addDirectory("", "./AsieLauncher/htdocs");

function getDirectoryList(name, destName, addLocation, prefix) {
  if(!_.isString(prefix)) prefix = "";
  if(!fs.existsSync(name)) return [];
  return _.chain(wrench.readdirSyncRecursive(name))
          .filter(function(file) {
            var location = name+"/"+file;
            var blacklisted = false;
            _.each(config.blacklist, function(test) {
              if(location.indexOf(test) >= 0) {
                blacklisted = true;
                util.say("info", "Blacklisted "+file);
              }
            });
            if(blacklisted) return false;
            else return !(fs.lstatSync(name+"/"+file).isDirectory());
          })
          .map(function(file) {
            var fileNew = {"filename": prefix+file, "size": util.getSize(name+"/"+file), "md5": util.md5(name+"/"+file)};
						var destination = destName+file;
            if(addLocation) fileNew.location = name+"/"+file;
				    fileNew.overwrite = !(_.contains(config.noOverwrite, file));
						if(fileNew.overwrite)
				      fileNew.overwrite = !(_.some(config.noOverwrite, function(nameNO) {
				        return (destination.indexOf(nameNO) == 0);
				      }));
            return fileNew;
          })
          .value();          
}

function getPossibleDirectories(name) {
  return [name, name+"-client", "./AsieLauncher/"+name, "./AsieLauncher/"+name+"-client"];
}

function getDirectoriesList(name, addLocation) {
  var destName = name+"/";
  if(name == "root") destName = ""; // Special case
  var dirs = [];
  _.each(getPossibleDirectories(name), function(dir) {
    dirs = _.union(dirs, getDirectoryList(dir, destName, addLocation));
  });
  return dirs;
}

console.log("Welcome to ALSO");

// init folders
fs.mkdir("AsieLauncher/zips", function(){ });
addDirectory("platform", "./AsieLauncher/platform");
addDirectory("options", "./AsieLauncher/options");
addDirectory("jarPatches", "./AsieLauncher/jarPatches");

infoData.jarPatches = util.sortFilesBySubstrings(config.jarPatchOrder,
                        getDirectoryList("./AsieLauncher/jarPatches", "jarPatches/", false, "jarPatches/")
                      );

infoData.files = [];
infoData.zips = [];
infoData.options = {};
infoData.platforms = {};

infoData.size = 0;
infoData.jvmArguments = config.jvmArguments || "";
infoData.client_revision = config.client_revision;
infoData.servers = config.servers || {};
infoData.onlineMode = config.onlineMode || false;
infoData.mcVersion = config.mcVersion || "1.5.2"; // default is 1.5.2

_.each(config.loggedDirs, function(dir) {
  var found = false;
  var list = getDirectoriesList(dir, false);
  infoData.files = _.union(infoData.files, _.map(list, function(file) {
    file.filename = dir+"/"+file.filename;
    infoData.size += file.size;
    return file;
  }));
  // Express
  _.each(getPossibleDirectories(dir), function(target) {
    if(fs.existsSync(dir)) { addDirectory(dir, target); found = true; }
  });
  if(found) util.say("info", "Added directory "+dir);
});

_.each(config.zippedDirs, function(dir) {
  var found = false
    , zip = new Zip();
  _.each(getPossibleDirectories(dir), function(dir){
    if(!fs.existsSync(dir)) return;
    found = true;
    zip.addLocalFolder(dir);
  });
  if(!found) return;
  util.say("info", "Added ZIP directory "+dir);
  zip.writeZip("./AsieLauncher/zips/"+dir+".zip");
  var zipData = {"filename": dir+".zip", "directory": dir == "root" ? "" : dir, "size": util.getSize("./AsieLauncher/zips/"+dir+".zip"),
		 "md5": util.md5("./AsieLauncher/zips/"+dir+".zip"), "overwrite": !(_.contains(config.noOverwrite, dir)) };
  infoData.zips.push(zipData);
  infoData.size += zipData.size;
});

_.each(fs.readdirSync("./AsieLauncher/platform"), function(platform) {
  util.say("debug", "Adding platform "+platform);
  var list = getDirectoryList("./AsieLauncher/platform/"+platform, "", false);
  var size = util.getFilesSize(list);
  infoData.platforms[platform] = {"files": list, "size": size};
});

_.each(config.options, function(option) {
  var dir = "./AsieLauncher/options/"+option.id;
  if(!fs.existsSync(dir)) return;
  util.say("info", "Adding option "+option.id+" ["+option.name+"]");
  if(!option.zip) { // Directory
    var list = getDirectoryList(dir, "", false);
    var size = util.getFilesSize(list);
    infoData.options[option.id]=_.extend(option, {"files": list, "size": size});
  } else { // ZIP
    var zip = new Zip()
      , zipFile = "./AsieLauncher/zips/option-"+option.id+".zip";
    zip.addLocalFolder(dir);
    zip.writeZip(zipFile);
   infoData.options[option.id]=_.extend(option, {"filename": "option-"+option.id+".zip", "directory": "", "size": util.getSize(zipFile), "md5": util.md5(zipFile)});
  }
});

if(DO_LOCAL) {
  fs.writeFileSync("./ALWebFiles/also.json", JSON.stringify(infoData), {encoding: "utf-8"});
} else {
  app.use("/also.json", function(req, res) {
    res.json(infoData);
  });
}

addDirectory("zips", "./AsieLauncher/zips");

if(!DO_LOCAL) {
  app.listen(config.port);
  util.say("info", "Ready - listening on port "+config.port+"!");
} else util.say("info", "Files exported to ./ALWebFiles/");
