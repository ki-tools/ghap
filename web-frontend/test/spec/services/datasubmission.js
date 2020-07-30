'use strict';

describe('Service: DataSubmission', function () {

  // load the service's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  // instantiate service
  var DataSubmission;
  beforeEach(inject(function (_DataSubmission_) {
    DataSubmission = _DataSubmission_;
  }));

});
