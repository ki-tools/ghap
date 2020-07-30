/**
 * Created by Vlad on 22.08.2016.
 */

exports = module.exports = {};

/**
 * @typedef {Object} GhapBannerType
 * @property {String} id
 * @property {String} title
 * @property {String} message
 * @property {String} color
 * @property {String} startDate format 'yyyy-MM-dd'
 * @property {String} startTime format 'hh:mm'
 * @property {String} endDate format 'yyyy-MM-dd'
 * @property {String} endTime format 'hh:mm'
 */

/**
 * Make new banner object;
 * @constructor
 */
function GhapBanner() {
    this.id = null;
    this.title = "";
    this.message = "";
    this.color = "";
    this.startDate = new Date().toISOString().substring(0,10);
    this.startTime = "00:00";
    this.endDate = this.startDate;
    this.endTime = "23:59";
}

/**
 * Create a new banner instance;
 * @return {GhapBannerType}
 */
exports.makeBanner = function () {
    return new GhapBanner();
};

exports.yeloowColor = "#ffff00";
exports.redColor = "#ff0000";
