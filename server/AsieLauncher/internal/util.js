var ansi = require('ansi')
  , cursor = ansi(process.stdout)
  , _ = require('underscore')
  , fs = require('fs')
  , crypto = require('crypto')
  , request = require('request');

var DEBUG = false;

exports.setDebug = function(d) { DEBUG = d; }

var textColors = {
  "debug": "grey",
  "info": "brightWhite",
  "warning": "brightYellow",
  "error": "brightRed"
};

exports.say = function(type, message) {
  if(type == "debug" && !DEBUG) return;
  cursor[textColors[type]]() // Set color
        .bold().write("["+type.toUpperCase()+"]")
        .reset().write(" "+message+'\n');
}

exports.getFilesSize = function(files) {
  var s = 0;
  _.each(files, function(file) { s += file.size; });
  return s;
}

exports.md5 = function(file) {
  return crypto.createHash('md5').update(fs.readFileSync(file)).digest('hex');
}

exports.getSize = function(file) {
  return fs.statSync(file).size;
}

exports.unix = function(date) {
  return Math.floor(new Date(date).getTime()/1000);
}

exports.sortFilesBySubstrings = function(order, fileList) {
  return _.sortBy(fileList, function(file) {
    var i = -1;
    _.each(order, function(e, index) {
      if(i>=0) return;
      if(file.filename.toLowerCase().indexOf(e.toLowerCase()) >= 0) {
        i = index;
      }
    });
    return i >= 0 ? i : order.length;
  });
}


exports.download = function(url, fn, cb) {
	var fStream = fs.createWriteStream(fn);
	fStream.on('close', cb);
	request(url).pipe(fStream);
}

exports.deleteFile = function(fn) {
	if(fs.existsSync(fn)) fs.unlinkSync(fn);
}

exports.mkdir = function(dirs) {
  if(_.isArray(dirs)) {
	_.each(dirs, function(dir) {
		try { fs.mkdirSync(dir); } catch(e){}
	});
  } else try { fs.mkdirSync(dirs); } catch(e){}
}

exports.copyFileSync = function(srcFile, destFile, encoding) {
  var content = fs.readFileSync(srcFile, encoding);
  fs.writeFileSync(destFile, content, encoding);
}
