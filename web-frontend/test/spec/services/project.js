'use strict';

describe('Service: project', function () {

  // load the service's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  // instantiate service
  var project;
  beforeEach(inject(function (_project_) {
    project = _project_;
  }));

});
