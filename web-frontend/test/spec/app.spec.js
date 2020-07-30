/**
 * Created by Vlad on 16.02.2016.
 */
'use strict';

describe('APP configuration and run', function () {

    beforeEach(module('bmgfApp', function ($provide) {
        mockServerCalls($provide);
    }));

    it('should get access token from the start URL hash', inject(function () {
        // access token should be extracted from the #hash on load of app.js
        // and saved to the localStorage while instancing of the User service
        var accessToken = localStorage.getItem('access_token');
        expect(accessToken).toBe('test-access-token');
    }));

    it('should save current user data to the localStorage', inject(function () {
        // current user is provided by mocked Settings service
        // and saved to the localStorage while instancing of User service
        var currentUser = localStorage.getItem('current_user');
        var currentUserRoles = localStorage.getItem('current_user_roles');
        expect(currentUser).toContain('tester');
        expect(currentUserRoles).toContain('testerRole');
    }));

    it('should redirect the user with testerRole to the my-account page', inject(function($location){
        expect($location.path()).toContain('/my-account');
    }));

    // http://stackoverflow.com/questions/14765719/how-to-watch-for-a-route-change-in-angularjs
    // You also need to inject "$route" somewhere or these events never fire.
    it('should call Auth.authorize on $routeChangeStart event', inject(function($route, $rootScope, $httpBackend, Auth){

        $httpBackend.expectGET('/locales/locale-en.json').respond(200,'{}');
        $httpBackend.expectGET('undefined/current?token=test-access-token').respond(200,'[{}]');
        $httpBackend.expectGET('views/my-account.html', function(headers){
            return headers.Authorization === 'Bearer test-access-token';
        }).respond(200);

        spyOn(Auth, 'authorize');

        // call digest cycle to fire $routeChangeStart event
        $rootScope.$digest();

        expect(Auth.authorize).toHaveBeenCalled();
    }));

    it('should set inactiveMinutes to 0 on change route event', inject(function($route, $rootScope, $httpBackend){

        $httpBackend.expectGET('/locales/locale-en.json').respond(200,'{}');
        $httpBackend.expectGET('undefined/current?token=test-access-token').respond(200,'[{}]');
        $httpBackend.expectGET('views/my-account.html').respond(200);
        $rootScope.inactiveMinutes = 29;

        // call digest cycle to fire $routeChangeStart event
        $rootScope.$digest();

        expect($rootScope.inactiveMinutes).toBe(0);
    }));

    it('should logout if no user activity within 30 minutes',
        inject(function($route, $rootScope, $httpBackend, $interval, Nav){

        $httpBackend.expectGET('/locales/locale-en.json').respond(200,'{}');
        $httpBackend.expectGET('undefined/current?token=test-access-token').respond(200,'[{}]');
        $httpBackend.expectGET('views/my-account.html').respond(200);
        // $interval.flush does not work if $digest cycle is not executed ???
        $rootScope.$digest();

        $rootScope.inactiveMinutes = 29;
        spyOn(Nav, 'logout');
        spyOn(window, 'alert');

        $interval.flush(60*1000);

        expect($rootScope.inactiveMinutes).toBe(30);
        expect(window.alert).toHaveBeenCalled();
        expect(Nav.logout).toHaveBeenCalled();
    }));

    it('should drop authorization header if no current user is defined',
        inject(function($route, $httpBackend, User){

            User.dropCurrentUser();
            var requestHeaders;
            $httpBackend.expectGET('/locales/locale-en.json').respond(200,'{}');
            $httpBackend.expectGET('undefined/current?token=test-access-token').respond(200,'[{}]');
            $httpBackend.expectGET('views/my-account.html', function(headers){
                requestHeaders = headers;
                return true;
            }).respond(200);
            $httpBackend.flush();
            expect(requestHeaders.Authorization).not.toBeDefined();
        }));
});