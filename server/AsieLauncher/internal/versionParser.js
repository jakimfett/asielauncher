var _ = require("underscore")
  , util = require("./util.js");

function isNumeric(c) { return c >= '0' && c <= '9'; }
function isCharacter(c) { return c >= 'a' && c <= 'z'; }

function compareVersion(oldVersion, newVersion) {
	var i = 0;
	if(_.isString(oldVersion)) oldVersion = unifyVersion(oldVersion);
	if(_.isString(newVersion)) newVersion = unifyVersion(newVersion);
	while((oldVersion.length > i) || (newVersion.length > i)) {
		var oldC = oldVersion.length <= i ? 0 : oldVersion[i];
		var newC = newVersion.length <= i ? 0 : newVersion[i];
		if(oldC > newC) return -1;
		if(oldC < newC) return 1;
		i = i + 1;
	}
	return 0;
}

function unifyVersion(version) {
	var mode = "none";
	var versionData = [];
	var currentPart = 0;
	var chars = version.split("");
	_.each(chars, function(char) {
		char = char.toLowerCase(); // unify
		var oldMode = mode;
		// Decide on mode
		if(isNumeric(char)) mode = "number";
		else if(isCharacter(char)) mode = "char";
		else mode = "none";
		// React
		if(oldMode != mode) {
			if(oldMode != "none") versionData.push(currentPart);
			currentPart = 0;
		}
		// Push new value to current
		if(mode == "number") {
			currentPart *= 10;
			currentPart += (char.charCodeAt(0) - 48) % 10;
		}
		else if(mode == "char") {
			currentPart *= 26;
			currentPart += (char.charCodeAt(0) - 97) % 26;
		}
	});
	// Remove clearly irrational parts
	var newVersionData = _.filter(versionData, function(data) {
		return (data <= 1000000);
	});
	return newVersionData;
}

exports.unify = unifyVersion;
exports.compare = compareVersion;
