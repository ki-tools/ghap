'use strict';

describe('Service: PersonalStorage', function () {

  // load the service's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  // instantiate service
  var PersonalStorage;
  beforeEach(inject(function (_PersonalStorage_) {
    PersonalStorage = _PersonalStorage_;
  }));

  it('should do something', function () {
    expect(!!PersonalStorage).toBe(true);
  });

});
