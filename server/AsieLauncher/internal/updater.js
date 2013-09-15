var tag = "[Updater] "
  , url = require("url")
  , util = require("./util.js")
  , Zip = require("adm-zip")
  , _ = require("underscore")
  , request = require("request")
  , fs = require("fs")
  , path = require("path");

var urlPrefix = getUrlPrefix();

function getUrlPrefix() {
	var config = JSON.parse(fs.readFileSync("./also-config.json") || '{"version": 0}');
	if(!config.version || config.version < 1) return "http://asie.pl/launcher/";
	var uu = config.updatesURL;
	if(!(/\/$/.test(uu))) uu = uu + "/";
	return uu;
}

function downloadAndUnpack(urlS, target, cb) {
	var filename = url.parse(urlS).pathname.split("/").pop();
	var tempFile = "./AsieLauncher/temp/" + filename;
	util.say("info", tag + "Downloading "+filename+"...");
	util.deleteFile(tempFile);
	util.mkdir(["./AsieLauncher/temp", target || "./"]);
	util.download(urlS, tempFile, function() {
		util.say("info", tag + "Downloaded "+filename+"!" + (target != null ? " Unpacking..." : ""));
		if(target != null) {
			var zip = new Zip(tempFile);
			zip.extractAllTo(path.resolve(target), true);
		}
		cb();
	});
}

exports.updateClient = function(cb) {
	downloadAndUnpack(urlPrefix + "AsieLauncher-latest.jar", null, cb);
}
exports.updateServer = function(cb) {
	downloadAndUnpack(urlPrefix + "AsieLauncher-latest-server.zip", "./AsieLauncher/internal", function() {
		// Check for package.json
		if(fs.existsSync("./AsieLauncher/internal/package.json")) {
			// Check md5
			if(util.md5("./AsieLauncher/internal/package.json") != util.md5("./package.json")) {
				util.copyFileSync("./AsieLauncher/internal/package.json", "./package.json");
				fs.unlinkSync("./AsieLauncher/internal/package.json");
				util.say("warning", "Please run 'npm update' and 'npm install'. The package.json file has changed.");
				setTimeout(function() { process.exit(0); }, 50);
			} else {
				fs.unlinkSync("./AsieLauncher/internal/package.json");
				cb();
			}
		} else cb();
	});
}

exports.getLocalInfo = function() {
	var json = fs.existsSync("./AsieLauncher/internal/info.json")
		? JSON.parse(fs.readFileSync("./AsieLauncher/internal/info.json"))
		: {"client_revision": 0, "server_revision": 0};
	if(!fs.existsSync("./AsieLauncher/temp/AsieLauncher-latest.jar")) json.client_revision = 0;
	return json;
}

exports.saveInfo = function(info) {
	fs.writeFileSync("./AsieLauncher/internal/info.json", JSON.stringify(info));
}

exports.getOnlineInfo = function(cb) {
	request({url: "http://asie.pl/launcher/info.json", json: true}, function(e, r, info) {
			cb(info);
		});
}
