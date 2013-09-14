var tag = "[Updater] "
  , urlPrefix = "http://asie.pl/launcher/"
  , url = require("url")
  , util = require("./util.js")
  , Zip = require("adm-zip")
  , _ = require("underscore")
  , request = require("request")
  , fs = require("fs")
  , path = require("path");

function downloadAndUnpack(urlS, target, cb) {
	var filename = url.parse(urlS).pathname.split("/").pop();
	var tempFile = "./AsieLauncher/temp/" + filename;
	util.say("info", tag + "Downloading "+filename+"...");
	util.deleteFile(tempFile);
	util.mkdir(["./AsieLauncher/temp", target]);
	util.download(urlS, tempFile, function() {
		util.say("info", tag + "Downloaded "+filename+"! Unpacking...");
		var zip = new Zip(tempFile);
		zip.extractAllTo(path.resolve(target), true);
		cb();
	});
}

exports.updateClient = function(cb) {
	downloadAndUnpack(urlPrefix + "AsieLauncher-latest.jar", "./AsieLauncher/internal/launcher", cb);
}
exports.updateServer = function(cb) {
	downloadAndUnpack(urlPrefix + "AsieLauncher-latest-server.zip", "./AsieLauncher/internal", cb);
}

exports.getLocalInfo = function() {
	return fs.existsSync("./AsieLauncher/internal/info.json")
		? JSON.parse(fs.readFileSync("./AsieLauncher/internal/info.json"))
		: {"client_revision": 0, "server_revision": 0};
}

exports.saveInfo = function(info) {
	fs.writeFileSync("./AsieLauncher/internal/info.json", JSON.stringify(info));
}

exports.getOnlineInfo = function(cb) {
	request({url: "http://asie.pl/launcher/info.json", json: true}, function(e, r, info) {
			cb(info);
		});
}
