// Initialize libraries (quite a lot of them, too!)
var express = require('express')
  , _ = require('underscore')
  , fs = require('fs')
  , wrench = require('wrench')
  , util = require('util')
  , Zip = require('adm-zip')
  , crypto = require('crypto');

var app = express()
  , config = require("./AsieLauncher/config.json")
  , infoData = {};

function getDirectoryList(name, destName, addLocation) {
  if(!fs.existsSync(name)) return [];
  return _.chain(wrench.readdirSyncRecursive(name))
          .filter(function(file) {
            var location = name+"/"+file;
            var blacklisted = false;
            _.each(config.blacklist, function(test) {
              if(location.indexOf(test) >= 0) {
                blacklisted = true;
                console.log("Blacklisted "+file);
              }
            });
            if(blacklisted) return false;
            else return !(fs.lstatSync(name+"/"+file).isDirectory());
          })
          .map(function(file) {
            var fileNew = {"filename": file, "size": getSize(name+"/"+file), "md5": md5(name+"/"+file)};
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

function md5(file) {
  return crypto.createHash('md5').update(fs.readFileSync(file)).digest('hex');
}

function getSize(file) {
  return fs.statSync(file).size;
}

function getDirectoriesList(name, addLocation) {
  var destName = name+"/";
  if(name == "root") destName = "";
  return _.union(getDirectoryList(name, destName, addLocation),
                 getDirectoryList(name+"-client", destName, addLocation),
                 getDirectoryList("AsieLauncher/"+name, destName, addLocation)
         );
}

function getDirectoriesSize(name) {
  var s = 0;
  _.each(name, function(file) { s += file.size; });
  return s;
}

function toUnix(date) {
  return Math.floor(new Date(date).getTime()/1000);
}

console.log("ALSO");

// init folders
fs.mkdir("AsieLauncher/zips", function(){ });
app.use("/AsieLauncher/zips", express.static("./zips"));
app.use("/AsieLauncher/platform", express.static("./platform"));
app.use("/AsieLauncher/options", express.static("./options"));

infoData.files = [];
infoData.zips = [];
infoData.options = {};
infoData.platforms = {};
infoData.size = 0;
infoData.client_revision = config.client_revision;

_.each(config.loggedDirs, function(dir) {
	if(!fs.existsSync(dir) && !fs.existsSync(dir+"-client") && !fs.existsSync("./AsieLauncher/"+dir)) {
		console.log("WARNING: Directory "+dir+" not found!");
		return;
	}
  console.log("Adding logged directory "+dir);
  var list = getDirectoriesList(dir, false);
  infoData.files = _.union(infoData.files, _.map(list, function(file) {
    file.filename = dir+"/"+file.filename;
    infoData.size += file.size;
    return file;
  }));
  // Express
  if(fs.existsSync(dir)) app.use("/"+dir, express.static("./"+dir));
  if(fs.existsSync(dir+"-client")) app.use("/"+dir, express.static("./"+dir+"-client"));
  if(fs.existsSync("./AsieLauncher/"+dir)) app.use("/"+dir, express.static("./AsieLauncher/"+dir));
});

_.each(config.zippedDirs, function(dir) {
	if(!fs.existsSync(dir)) {
		console.log("WARNING: Directory "+dir+" not found!");
		return;
	}
  console.log("Adding zipped directory "+dir);
  var list = getDirectoriesList(dir, true)
    , zip = new Zip();
  zip.addLocalFolder(dir);
  if(fs.existsSync(dir+"-client")) zip.addLocalFolder(dir+"-client");
  zip.writeZip("./AsieLauncher/zips/"+dir+".zip");
  var zipData = {"filename": dir+".zip", "directory": dir == "root" ? "" : dir, "size": getSize("./AsieLauncher/zips/"+dir+".zip"), "md5": md5("./AsieLauncher/zips/"+dir+".zip"),
                 "overwrite": !(_.contains(config.noOverwrite, dir)) };
  infoData.zips.push(zipData);
  infoData.size += zipData.size;
});

_.each(fs.readdirSync("./AsieLauncher/platform"), function(platform) {
  console.log("Adding platform "+platform);
  var list = getDirectoryList("./AsieLauncher/platform/"+platform, "", false);
  var size = getDirectoriesSize(list);
  infoData.platforms[platform] = {"files": list, "size": size};
});

_.each(config.options, function(option) {
  var dir = "./AsieLauncher/options/"+option.id;
  if(!fs.existsSync(dir)) return;
  console.log("Adding option "+option.id+" ["+option.name+"]");
  if(!option.zip) { // Directory
    var list = getDirectoryList(dir, "", false);
    var size = getDirectoriesSize(list);
    infoData.options[option.id]=_.extend(option, {"files": list, "size": size});
  } else { // ZIP
    var zip = new Zip()
      , zipFile = "./AsieLauncher/zips/option-"+option.id+".zip";
    zip.addLocalFolder(dir);
    zip.writeZip(zipFile);
   infoData.options[option.id]=_.extend(option, {"filename": "option-"+option.id+".zip", "directory": "", "size": getSize(zipFile), "md5": md5(zipFile)});
  }
});

app.use("/also.json", function(req, res) {
  res.json(infoData);
});

app.use("/", express.static("./AsieLauncher/htdocs"));

app.listen(config.port);

console.log("Ready - listening on port "+config.port+"!");
