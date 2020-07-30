var assert = require('chai').assert;

describe('Array', function() {
    describe('#indexOf()', function() {
        it('should return -1 when the value is not present', function() {
            assert.equal(-1, [1,2,3].indexOf(5));
            assert.equal(-1, [1,2,3].indexOf(0));
        });
    });
});

// https://github.com/danielstjules/mocha.parallel
var parallel = require('mocha.parallel');
var Q  = require('q');

describe('Parallel suites', function () {
    parallel('delays', function() {
        it('test1', function(done) {
            setTimeout(done, 500);
        });

        it('test2', function(done) {
            setTimeout(done, 500);
        });

        it('test3', function() {
            return qDelay(600);
        });
    });
});

var fs = require('fs');
describe('CSV test', function () {
    it ("should append record to test.csv", function () {
        var results =[];
        results.push(new Date().toISOString());
        results.push('Test name');
        results.push(123);
        fs.appendFileSync('test.csv', results.toString()+'\r\n');
    })
});

function qDelay(delay) {
    var deferred = Q.defer();
    setTimeout(function () {
        deferred.resolve()
    }, delay);
    return deferred.promise;
}