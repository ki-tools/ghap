'use strict';

describe('Service: nav', function () {

  // load the service's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  // instantiate service
  var nav;
  beforeEach(inject(function (_nav_) {
    nav = _nav_;
  }));

});
