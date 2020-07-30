'use strict';

describe('Service: auth', function () {

  // load the service's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  // instantiate service
  var auth;
  beforeEach(inject(function (_auth_) {
    auth = _auth_;
  }));

});
