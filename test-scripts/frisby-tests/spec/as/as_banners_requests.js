var my = require('./../Common/ghap-lib');

// This package adds a toEqualJson matcher to jasmine that will generate nice diffs on error.
require('jasmine-json');

var ghapFrisby = require('./../Common/ghap-frisby');

var bannersUrls = require('./as_banners_urls');

/**
 * Get Current Banner
 * @returns {Promise.<[GhapBannerType]>} array of banners which are currently active
 */
module.exports.getCurrentBanner = function() {
	return ghapFrisby.create({Name:'HEmpty', Value:'noValue'}, 'Get Current Banner')
		.get( bannersUrls.getCurrentBanner_Url() )
		.addHeader('content-type', 'application/json')
		.onSuccess(function (body) {
		    var banners = my.jsonParse(body);
			expect(banners).toBeType(Array);
            if (banners instanceof Array)
                console.log(" %d current bunners found.", banners.length);
                banners.forEach(function (banner) {
                    console.log("'%s' : '%s'", banner.title, banner.message)
                });
		})
		.returnPromise();
};

/**
 * Create a new banner. Update attributes in ghapBanner object.
 * @param {GhapAuthHeader} authHeader
 * @param {GhapBannerType} ghapBanner
 * @param {Function} [onError] optional handler for error
 * @return {Promise.<GhapBannerType>}
 */
module.exports.createBanner = function (authHeader, ghapBanner, onErrorFunc) {
    return ghapFrisby.create(authHeader, 'Create Banner')
        .post(bannersUrls.getBanner_Url(), ghapBanner, {json: true})
        .setLogMessage("Create banner with tile '%s'", ghapBanner.title)
        .onSuccess(function (body) {
            var banner = my.jsonParse(body);
            var isBannerIdDefined = banner.hasOwnProperty('id');
            expect(isBannerIdDefined).toBeTruthy();
            if (isBannerIdDefined) {
                console.log(" new banner id '%s'", banner.id);
                my.copyProperties(banner, ghapBanner);
            }
            return banner;
        })
        .onError(onErrorFunc)
        .returnPromise();
};

/**
 * Delete banner
 * @param {GhapAuthHeader} authHeader
 * @param {GhapBannerType} ghapBanner
 * @return {Promise.<>}
 */
module.exports.deleteBanner = function (authHeader, ghapBanner) {
    const EXPECTED_STATUS = 204;
    return ghapFrisby.create(authHeader, 'Delete Banner', EXPECTED_STATUS)
        .delete(bannersUrls.getBannerById_Url(ghapBanner.id))
        .addHeader('content-type', 'application/json')
        .setLogMessage("Delete banner with id '%s'", ghapBanner.id)
        .returnPromise();
};

/**
 * Get list of defined banners.
 * @param authHeader
 * @return Promise.<[GhapBannersType]>
 */
module.exports.getBanners = function (authHeader) {
    return ghapFrisby.create(authHeader, 'Get List of Defined Banners')
        .get(bannersUrls.getBanner_Url())
        .addHeader('content-type', 'application/json')
        .onSuccess(function (body) {
            var bannersList = my.jsonParse(body);
            expect(bannersList).toBeType(Array);
            if (bannersList instanceof Array) {
                console.log(" %d banners found.", bannersList.length);
            }
            return bannersList;
        })
        .returnPromise();
};

/**
 * Get banner by Id
 * @param {GhapAuthHeader} authHeader
 * @param {String} bannerId
 * @return {Promise.<GhapBannerType>}
 */
module.exports.getBanner = function (authHeader, bannerId) {
    return ghapFrisby.create(authHeader, "Get Banner by Id")
        .get(bannersUrls.getBannerById_Url(bannerId))
        .addHeader('content-type', 'application/json')
        .setLogMessage("Get banner by id '%s'", bannerId)
        .onSuccess(function (body) {
            var banner = my.jsonParse(body);
            var isBannerIdDefined = banner.hasOwnProperty('id');
            expect(isBannerIdDefined).toBeTruthy();
            console.log('.');
            return banner;
        })
        .returnPromise();
};

/**
 * Update banner.
 * @param {GhapAuthHeader} authHeader
 * @param {GhapBannerType} updatedBanner
 * @param {String} [bannerId=updatedBanner.id]
 * @return {Promise.<GhapBannerType>}
 */
module.exports.updateBanner = function (authHeader, updatedBanner, bannerId) {
    if (bannerId == null) bannerId = updatedBanner.id;
    return ghapFrisby.create(authHeader, 'Update Banner')
        .post(bannersUrls.getBannerById_Url(bannerId), updatedBanner, {json: true})
        .addHeader('content-type', 'application/json')
        .setLogMessage("Update banner with id '%s'", bannerId)
        .onSuccess(function (body) {
            console.log('.');
            var banner = my.jsonParse(body);
            expect(body).toEqualJson(JSON.parse(JSON.stringify(updatedBanner)));
            return banner;
        })
        .returnPromise();
};
