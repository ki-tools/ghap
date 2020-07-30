'use strict';

describe('Service: Settings', function () {

  // load the service's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  // instantiate service
  var Settings;
  beforeEach(inject(function (_Settings_) {
    Settings = _Settings_;
  }));

});
