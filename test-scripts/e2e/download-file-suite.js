/**
 * Created by Vlad on 11.02.2016.
 */

var fs = require('fs');
var path = require('path');

var my = require('./ghap-e2e-lib');

exports.testSuite = function(suite_title, it_start_downloading, file_name, file_size_bytes) {
    const KB_SIZE = 1024;
    const MB_SIZE = 1024 * KB_SIZE;
    var max_download_time_sec = (Math.floor(file_size_bytes / MB_SIZE)+1) * 5;

    return my.createStdSuite(suite_title, function () {

        // Q: protractor test download file
        // http://stackoverflow.com/questions/21935696/protractor-e2e-test-case-for-downloading-pdf-file

        var download_path;
        var download_file;
        it('- download path should be defined', function(){
            download_path = my.getDownloadPath();
            expect(download_path).not.toBeNull();
            if (download_path) {
                download_file = path.join(download_path, file_name);
                my.deleteFileIfExists(download_file);
            }
        });

        it_start_downloading();

        it('- file should be downloaded within ' + max_download_time_sec + 's', function(){
            var start = new Date();

            /**
             * @typedef {webdriver.WebDriver} browser
             */
            browser.wait( function() {
                    var is_downloaded = false;
                    try {
                        var stats = fs.statSync(download_file);
                        if (stats.isFile()) {
                            is_downloaded = ( stats.size  >= file_size_bytes );
                        }
                    }
                    catch (e) {
                        is_downloaded = false
                    }
                    return is_downloaded;
                }, max_download_time_sec * 1000, 'File download is not finished in time.')
                .then( function() {
                    var download_time_ms = new Date() - start;
                    console.log("File have been downloaded within %d s", download_time_ms/1000);
                    expect(download_time_ms).toBeLessThan(max_download_time_sec * 1000);
                })

        }, max_download_time_sec * 1000 + 5000);

    });
};