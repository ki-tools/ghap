'use strict';

describe('Filter: getByUuid', function () {

  // load the filter's module 
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  // initialize a new instance of the filter before each test
  var getByUuid;
  beforeEach(inject(function ($filter) {
    getByUuid = $filter('getByUuid');
  }));

  it('should return the input prefixed with "getByUuid filter:"', function () {
    var o = {uuid: '1'};
    expect(getByUuid([o], '1')).toBe(o);
  });

});
