'use strict';

describe('Service: Grant', function () {

  // load the service's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  // instantiate service
  var Grant;
  beforeEach(inject(function (_Grant_) {
    Grant = _Grant_;
  }));

  it('should do something', function () {
    expect(!!Grant).toBe(true);
  });

});
