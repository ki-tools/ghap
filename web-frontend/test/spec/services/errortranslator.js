'use strict';

describe('Service: errorTranslator', function () {

  // load the service's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  // instantiate service
  var errorTranslator;
  beforeEach(inject(function (_errorTranslator_) {
    errorTranslator = _errorTranslator_;
  }));

});
