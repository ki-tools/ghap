/**
 * Created by Vlad on 04.11.2015.
 */

var fs = require('fs');
var path = require('path');

const KB_SIZE = 1024;
const MB_SIZE = 1024 * KB_SIZE;
const MAX_FILE_SIZE_MB = 60;

/**
 * TstFile constructor
 * @param file_name
 * @param size_in_mb
 * @constructor
 */
function TstFile(file_name, size_in_mb) {
    this.fileName = file_name;
    this.fileSizeMb = size_in_mb;
    this.fileSize = size_in_mb * MB_SIZE;
    this.fullFileName = path.resolve(__dirname, file_name);
}

/**
 * Create new test file if it does'nt exist or has wrong size
 * @returns {Promise<T>}
 */
TstFile.prototype.prepareTstFile = function () {
    var deferred = protractor.promise.defer();

    var self = this;
    fs.stat(this.fullFileName, function(err, stat){
        if (err === null) {
            // file exists
            if (stat.isFile()) {
                if ( (stat.size / MB_SIZE) !== self.fileSizeMb) {
                    console.log("File '%s' exists but has wrong size. Will try to create a new file.", self.fileName);
                    self.createTstFile(deferred)
                } else {
                    self.fileSize = stat.size;
                    console.log("File '%s' exists and has requested size %d MB.", self.fileName, self.fileSizeMb);
                    deferred.fulfill();
                }
            } else {
                deferred.reject(new Error('Invalid type of file '+self.fileName))
            }
        } else {
            if (err.code === 'ENOENT') {
                console.log("File '%s' does not exist. Will try create a new file.", self.fileName);
                self.createTstFile(deferred);
            }
            else
                deferred.reject(err)
        }
    });

    return deferred.promise;
};

/**
 * Create new file and resole deferred when complete
 * @param deferred
 */
TstFile.prototype.createTstFile = function (deferred) {

    var self = this;
    var start_time, exec_time_ms;

    if (self.fileSizeMb > MAX_FILE_SIZE_MB) {
        deferred.reject(new Error('Requested file size (' + self.fileSizeMb + ' MB) exceeds maximum allowed value.'));
        return;
    }

    var wStream = fs.createWriteStream(self.fullFileName);

    wStream.on('error', function(error){
        deferred.reject(error)
    });

    wStream.on('finish', function(){
        exec_time_ms = new Date() - start_time;
        console.log('File saved within %d ms.', exec_time_ms);
    });

    wStream.on('close', function(){
        console.log('Stream closed.');
        deferred.fulfill();
    });

    console.log('Prepare %d Mb buffer...', self.fileSizeMb);
    start_time = new Date();
    var buf = new Buffer(self.fileSizeMb * MB_SIZE);
    self.fileSize = buf.length;
    for (var i = 0; i < self.fileSizeMb * KB_SIZE; i++ )
        buf.write(randomKbChars(), i * KB_SIZE, KB_SIZE);
    exec_time_ms = new Date() - start_time;
    console.log('Buffer filled by random chars within %d ms', exec_time_ms);

    start_time = new Date();
    wStream.end(buf);
};

function randomKbChars() {
    // http://stackoverflow.com/questions/1349404/generate-a-string-of-5-random-characters-in-javascript
    const possible_chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    var str = '';
    for (var i=0; i<1024; i++)
        str += possible_chars.charAt(Math.floor(Math.random() * possible_chars.length));
    return str;

    //var buf = new Array(1024);
    //for (var i=0; i < buf.length; i++)
    //    buf[i] = possible_chars.charAt(Math.floor(Math.random() * possible_chars.length));
    //return buf.toString();
}

/**
 * Create new TstFile instance.
 * @param file_name
 * @param size_in_mb
 * @returns {TstFile}
 */
exports.createNew = function(file_name, size_in_mb) {
    return new TstFile(file_name, size_in_mb);
};