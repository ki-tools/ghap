'use strict';

describe('Service: VirtualResource', function () {

  // load the service's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  // instantiate service
  var VirtualResource;
  beforeEach(inject(function (_VirtualResource_) {
    VirtualResource = _VirtualResource_;
  }));

  it('should do something', function () {
    expect(!!VirtualResource).toBe(true);
  });

});
