'use strict';

describe('Service: syncHttp', function () {

  // load the service's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  // instantiate service
  var syncHttp;
  beforeEach(inject(function (_SyncHttp_) {
    syncHttp = _SyncHttp_;
  }));

  it('should do something', function () {
    expect(!!syncHttp).toBe(true);
  });

});
